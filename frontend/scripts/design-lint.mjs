#!/usr/bin/env node
// Design lint (spec 0002 AC-7, AC-8): screens take every colour, size and duration from
// @schoolos/design-tokens. This script fails when app source under apps/*/src writes one by value.
// Tests, translation files and the token package itself are not scanned.
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

/** Allowed raw numbers in React Native styles: 0 and 1 are structural (flex: 1, opacity: 0, borderWidth: 0). */
const RN_ALLOWED_NUMBERS = new Set(['0', '1']);

const RN_STYLE_KEYS =
  'width|height|minWidth|minHeight|maxWidth|maxHeight|margin\\w*|padding\\w*|gap|rowGap|columnGap|top|left|right|bottom|' +
  'fontSize|lineHeight|letterSpacing|borderRadius|border\\w*Width|opacity|elevation|duration|delay|scale|shadowRadius|shadowOpacity';

/** Reanimated entering/exiting animation families (FadeIn, SlideOutLeft, ZoomInEasyDown…). */
const RN_LAYOUT_ANIMATIONS = 'Fade|Slide|Zoom|Bounce|Flip|Stretch|Rotate|Roll|LightSpeed|Pinwheel';

/**
 * src/motion/timing.ts is the one gateway that turns motion tokens into Reanimated timing (plan 0002 A-7);
 * only that file is exempt — components in src/motion/ are checked like any screen (A-8).
 */
const inMotionHelpers = (file) => file.endsWith(`${sep}mobile${sep}src${sep}motion${sep}timing.ts`);

/** A JSX inline style object: style={{ … }} (flat objects; the first "}}" closes it). */
const INLINE_STYLE = /style=\{\{[\s\S]*?\}\}/g;

const TAILWIND_COLOR_PREFIXES =
  'bg|text|border|border-[trblxy]|ring|ring-offset|outline|fill|stroke|from|via|to|shadow|decoration|accent|caret|divide|placeholder';
// Checked against tailwindcss/theme.css by the tests (every default colour family must be listed).
export const TAILWIND_PALETTE =
  'slate|gray|zinc|neutral|stone|taupe|mauve|mist|olive|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose';

/**
 * Plan 0002 A-7: Tailwind's default theme namespaces that carry a visual value (colour, size, shadow, curve…)
 * are switched off at the source; the test checks this list against tailwindcss/theme.css, so a Tailwind
 * upgrade that adds a namespace fails until it is locked here or listed in UNLOCKED_NAMESPACES.
 */
export const LOCKED_NAMESPACES = [
  'color',
  'font',
  'font-weight',
  'text',
  'leading',
  'radius',
  'shadow',
  'blur',
  'container',
  'ease',
  'animate',
  'drop-shadow',
  'inset-shadow',
  'text-shadow',
  'tracking',
  'perspective',
  'breakpoint',
];

/** Namespaces left open, each with its reason. */
export const UNLOCKED_NAMESPACES = {
  spacing:
    'numeric spacing classes are forbidden by tailwind-numeric; token spacing names are defined in index.css',
  aspect: 'a ratio (16 / 9), not a size, colour or duration',
  'max-width':
    'only max-w-prose (65ch, a reading measure); max-w-* by value is forbidden by tailwind-numeric',
  default:
    'every --default-* value is in BOUND_DEFAULTS or OPEN_DEFAULTS (checked against theme.css and dist/lib.js)',
};

/**
 * Plan 0002 A-8: Tailwind's --default-* fallbacks. Some live in theme.css, others only in dist/lib.js
 * (e.g. a bare `border` falls back to 1px there). Each is bound to a token in index.css or left open with a
 * reason; the test reads both files, so a Tailwind upgrade that adds one fails until it is listed here.
 */
export const BOUND_DEFAULTS = {
  'transition-duration': 'var(--sos-duration-',
  'transition-timing-function': 'var(--sos-ease-',
  'border-width': 'var(--sos-border-',
  'outline-width': 'var(--sos-border-',
  'ring-width': 'var(--sos-border-',
  'ring-color': 'var(--sos-',
};

export const OPEN_DEFAULTS = {
  'font-family': 'reads --font-sans, which index.css binds to the token font stack',
  'font-feature-settings': 'reads --font-sans--font-feature-settings: not set, resolves to initial',
  'font-variation-settings': 'reads --font-sans--font-variation-settings: not set, resolves to initial',
  'mono-font-family': 'reads --font-mono, locked by --font-*: initial (browser monospace, no Tailwind value)',
  'mono-font-feature-settings': 'reads a locked --font-mono setting: resolves to initial',
  'mono-font-variation-settings': 'reads a locked --font-mono setting: resolves to initial',
};

/**
 * VERIFY 1 F-1, plan 0002 A-7/A-8: lines the web stylesheet must hold. Removing one brings a Tailwind
 * default (palette, scale, curve, width) back without any other signal.
 */
export const REQUIRED_LOCKS = [
  ...LOCKED_NAMESPACES.map((namespace) => `--${namespace}-*: initial`),
  ...Object.entries(BOUND_DEFAULTS).map(([name, value]) => `--default-${name}: ${value}`),
];
export const LOCKED_STYLESHEET = join('apps', 'web', 'src', 'index.css');

/** The required lock lines missing from the stylesheet text (comments ignored). */
export function missingLocks(css) {
  const source = stripComments(css);
  return REQUIRED_LOCKS.filter((lock) => !source.includes(lock));
}

const TAILWIND_NUMERIC_PREFIXES =
  'p|px|py|pt|pb|pl|pr|ps|pe|m|mx|my|mt|mb|ml|mr|ms|me|gap|gap-x|gap-y|space-x|space-y|size|w|h|min-w|min-h|max-w|max-h|' +
  'top|left|right|bottom|inset|inset-x|inset-y|start|end|text|leading|tracking|rounded|rounded-[trbl]{1,2}|border|border-[trblxy]|' +
  'outline|outline-offset|ring|ring-offset|opacity|duration|delay|blur|brightness|contrast|saturate|scale|scale-[xy]|rotate|' +
  'translate-[xy]|z|basis|order|columns|aspect|stroke|underline-offset|decoration';
// grow-0 / shrink-0 are structural flex switches (like RN flex: 1), not visual sizes, so they are not listed.

export const RULES = [
  {
    id: 'color-literal',
    applies: () => true,
    pattern: /#[0-9a-fA-F]{3,8}\b|\b(?:rgba?|hsla?|oklch|oklab)\(/g,
    message: 'colour written by value — use a semantic token',
  },
  {
    id: 'tailwind-arbitrary',
    applies: (file) => /\.(tsx?|jsx?)$/.test(file),
    // An arbitrary value is allowed only when it reads a design token variable.
    // Variant selectors such as data-[state=checked]: are not values and end in "]:".
    pattern: /[a-z0-9]-\[(?!(?:length:|color:|number:)?var\(--sos-)[^\]\s]*\](?!:)/g,
    message: 'Tailwind arbitrary value — use a token utility or var(--sos-…)',
  },
  {
    id: 'tailwind-numeric',
    applies: (file) => /\.(tsx?|jsx?)$/.test(file) && !file.includes(`${sep}mobile${sep}`),
    pattern: new RegExp(
      `(?<![\\w-])-?(?:[a-z-]+:)*-?(?:${TAILWIND_NUMERIC_PREFIXES})-\\d+(?:\\.\\d+)?(?:xl)?(?![\\w/.])`,
      'g',
    ),
    message: 'Tailwind default-scale value — use a token name (p-md, size-logo, rounded-lg…)',
  },
  {
    id: 'tailwind-breakpoint',
    applies: (file) => /\.(tsx?|jsx?)$/.test(file) && !file.includes(`${sep}mobile${sep}`),
    // Breakpoints are Tailwind's default sizes and media queries cannot read token variables: lay out with
    // flex-wrap + basis-[var(--sos-…)] instead.
    pattern: /(?<![\w-])(?:sm|md|lg|xl|2xl):(?=[a-z-])/g,
    message: 'Tailwind default breakpoint — use a token-width flex layout',
  },
  {
    id: 'rn-style-number',
    applies: (file) => /\.(tsx?|jsx?)$/.test(file) && file.includes(`${sep}mobile${sep}`),
    pattern: new RegExp(`\\b(?:${RN_STYLE_KEYS})\\s*:\\s*(-?\\d*\\.?\\d+)\\b`, 'g'),
    keep: (match) => !RN_ALLOWED_NUMBERS.has(match[1] ?? ''),
    message:
      'React Native style number written by value — use theme.spacing / size / radius / opacity / motion',
  },
  {
    // VERIFY 1, F-1: a default-palette class silently does nothing once the palette is off; say so instead.
    id: 'tailwind-palette',
    applies: (file) => /\.(tsx?|jsx?)$/.test(file) && !file.includes(`${sep}mobile${sep}`),
    pattern: new RegExp(
      `(?<![\\w-])(?:[a-z-]+:)*(?:${TAILWIND_COLOR_PREFIXES})-(?:(?:${TAILWIND_PALETTE})-\\d{2,3}|black|white)(?![\\w-])`,
      'g',
    ),
    message: 'Tailwind default colour palette — use a semantic colour (bg-surface, text-ink, bg-brand…)',
  },
  {
    // VERIFY 1, F-2 (AC-8): easing curves come from motion.easing; a keyword or a curve by value is not allowed.
    id: 'css-easing-literal',
    applies: (file) => file.endsWith('.css'),
    pattern:
      /(?<![\w-])(?:linear|ease|ease-in|ease-out|ease-in-out|step-start|step-end|cubic-bezier|steps)(?![\w-])/g,
    message: 'CSS easing written by value — use var(--sos-ease-…)',
  },
  {
    // Plan 0002 A-7: outside src/motion/ an animation cannot be timed directly, so it cannot forget its
    // duration or curve and fall back to Reanimated's default; use animateTo / timing / fadeIn instead.
    id: 'rn-motion-direct',
    applies: (file) =>
      /\.(tsx?|jsx?)$/.test(file) && file.includes(`${sep}mobile${sep}`) && !inMotionHelpers(file),
    pattern: new RegExp(
      `\\b(?:with(?:Timing|Spring|Decay)|(?:${RN_LAYOUT_ANIMATIONS})\\w*(?:In|Out)\\w*|\\w+Transition)\\b`,
      'g',
    ),
    message: 'Reanimated timing used directly — use src/motion/timing (animateTo, timing, fadeIn)',
  },
  {
    id: 'rn-easing-literal',
    applies: (file) =>
      /\.(tsx?|jsx?)$/.test(file) && file.includes(`${sep}mobile${sep}`) && !inMotionHelpers(file),
    // Allowed: Easing.bezier(...theme.motion.easing.<name>) or (...motion.easing.<name>) — the token spread in.
    pattern: /\bEasing\.(?!bezier\(\s*\.\.\.\s*(?:theme\.)?motion\.easing\.)\w+(?:\([^)]*\))?/g,
    message: 'React Native easing written by value — use Easing.bezier(...theme.motion.easing.<name>)',
  },
  {
    // Review 1, L-4: web inline style objects (style={{ … }}) follow the same rule as RN styles.
    id: 'web-style-number',
    applies: (file) => /\.(tsx|jsx)$/.test(file) && !file.includes(`${sep}mobile${sep}`),
    within: INLINE_STYLE,
    pattern: new RegExp(`\\b(?:${RN_STYLE_KEYS}|inset|zIndex|flexBasis)\\s*:\\s*(-?\\d*\\.?\\d+)\\b`, 'g'),
    keep: (match) => !RN_ALLOWED_NUMBERS.has(match[1] ?? ''),
    message: 'inline style number written by value — use var(--sos-…)',
  },
  {
    id: 'web-style-unit',
    applies: (file) => /\.(tsx|jsx)$/.test(file) && !file.includes(`${sep}mobile${sep}`),
    within: INLINE_STYLE,
    pattern: /(?<![\w-])\d*\.?\d+(?:px|rem|em|ms|s|vh|vw|deg)\b/g,
    message: 'inline style length or duration written by value — use var(--sos-…)',
  },
  {
    id: 'css-unit-literal',
    applies: (file) => file.endsWith('.css'),
    pattern: /(?<![\w-])\d*\.?\d+(?:px|rem|em|ms|s|vh|vw)\b/g,
    message: 'CSS length or duration written by value — use var(--sos-…)',
  },
];

/** Removes comments so documentation (e.g. "ör. #1E40AF") is not linted; keeps line numbers. */
export function stripComments(text) {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, (block) => block.replace(/[^\n]/g, ' '))
    .replace(/(^|[^:'"`])\/\/[^\n]*/g, (line, lead) => lead + ' '.repeat(line.length - lead.length));
}

export function lintSource(text, file) {
  const source = stripComments(text);
  const violations = [];
  for (const rule of RULES) {
    if (!rule.applies(file)) continue;
    // A rule with `within` looks only inside those regions (inline style objects).
    const regions = rule.within
      ? [...source.matchAll(rule.within)].map((m) => ({ text: m[0], offset: m.index }))
      : [{ text: source, offset: 0 }];
    for (const region of regions) {
      for (const match of region.text.matchAll(rule.pattern)) {
        if (rule.keep && !rule.keep(match)) continue;
        const line = source.slice(0, region.offset + match.index).split('\n').length;
        violations.push({ rule: rule.id, line, text: match[0].trim(), message: rule.message });
      }
    }
  }
  return violations;
}

export function isScanned(path) {
  const normalized = path.split(sep).join('/');
  return (
    /(?:^|\/)apps\/[^/]+\/src\//.test(normalized) &&
    /\.(tsx?|jsx?|css)$/.test(normalized) &&
    !/\.test\.[tj]sx?$/.test(normalized) &&
    !/\/test\//.test(normalized)
  );
}

function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    if (name === 'node_modules' || name.startsWith('.')) continue;
    const full = join(dir, name);
    if (statSync(full).isDirectory()) walk(full, out);
    else if (isScanned(full)) out.push(full);
  }
  return out;
}

function main() {
  const root = fileURLToPath(new URL('..', import.meta.url));
  const files = walk(join(root, 'apps'));
  let count = 0;
  for (const file of files) {
    for (const v of lintSource(readFileSync(file, 'utf8'), file)) {
      count++;
      console.log(`${relative(root, file)}:${v.line}  ${v.rule}  ${v.text}  — ${v.message}`);
    }
  }
  const stylesheet = join(root, LOCKED_STYLESHEET);
  for (const lock of missingLocks(readFileSync(stylesheet, 'utf8'))) {
    count++;
    console.log(
      `${LOCKED_STYLESHEET}  tailwind-lock  ${lock}  — default scale must stay switched off (spec 0002 AC-7)`,
    );
  }
  if (count > 0) {
    console.log(`design-lint: ${count} violation(s) in ${files.length} file(s)`);
    process.exit(1);
  }
  console.log(`design-lint: clean (${files.length} files)`);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main();
}
