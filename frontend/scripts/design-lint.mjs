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

const TAILWIND_NUMERIC_PREFIXES =
  'p|px|py|pt|pb|pl|pr|ps|pe|m|mx|my|mt|mb|ml|mr|ms|me|gap|gap-x|gap-y|space-x|space-y|size|w|h|min-w|min-h|max-w|max-h|' +
  'top|left|right|bottom|inset|inset-x|inset-y|start|end|text|leading|tracking|rounded|rounded-[trbl]{1,2}|border|border-[trblxy]|' +
  'outline|outline-offset|ring|ring-offset|opacity|duration|delay|blur|brightness|contrast|saturate|scale|scale-[xy]|rotate|' +
  'translate-[xy]|z|basis|grow|shrink|order|columns|aspect|stroke|underline-offset|decoration';

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
    id: 'rn-style-number',
    applies: (file) => /\.(tsx?|jsx?)$/.test(file) && file.includes(`${sep}mobile${sep}`),
    pattern: new RegExp(`\\b(?:${RN_STYLE_KEYS})\\s*:\\s*(-?\\d*\\.?\\d+)\\b`, 'g'),
    keep: (match) => !RN_ALLOWED_NUMBERS.has(match[1] ?? ''),
    message:
      'React Native style number written by value — use theme.spacing / size / radius / opacity / motion',
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
    for (const match of source.matchAll(rule.pattern)) {
      if (rule.keep && !rule.keep(match)) continue;
      const line = source.slice(0, match.index).split('\n').length;
      violations.push({ rule: rule.id, line, text: match[0].trim(), message: rule.message });
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
  if (count > 0) {
    console.log(`design-lint: ${count} violation(s) in ${files.length} file(s)`);
    process.exit(1);
  }
  console.log(`design-lint: clean (${files.length} files)`);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main();
}
