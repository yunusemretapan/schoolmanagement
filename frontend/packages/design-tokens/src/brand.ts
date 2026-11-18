import { contrastRatio } from './contrast';
import { palette } from './palette';
import { tokens, type ColorScheme } from './tokens';

const AA = 4.5;
const MAX_STEPS = 20;

export interface BrandColors {
  /** Fill colour of brand elements (buttons, logo tile). */
  solid: string;
  /** Text / icon colour on top of `solid`. */
  onSolid: string;
  /** Brand colour used as text or line on the scheme's backgrounds. */
  text: string;
}

/** "#rrggbb" (case-insensitive, surrounding spaces allowed) → "#RRGGBB"; anything else → null. */
export function parseHexColor(input: string | null | undefined): string | null {
  const match = /^\s*#([0-9a-f]{6})\s*$/i.exec(input ?? '');
  return match?.[1] ? `#${match[1].toUpperCase()}` : null;
}

/**
 * Brand colours for one appearance, guaranteed readable (spec 0002 AC-5):
 * - `onSolid` is whichever of white / near-black / black contrasts most with the brand colour —
 *   for any colour one of them reaches at least 4.5:1;
 * - `text` is the brand colour, mixed step by step towards black (light) or white (dark) until it
 *   reaches 4.5:1 on every background of that scheme.
 * An invalid or missing colour falls back to the scheme's default brand (AC-6).
 */
export function deriveBrand(input: string | null | undefined, scheme: ColorScheme): BrandColors {
  const hex = parseHexColor(input);
  const semantic = tokens.semantic[scheme];
  if (hex === null) {
    return { solid: semantic.brand.solid, onSolid: semantic.brand.onSolid, text: semantic.brand.text };
  }

  const candidates = [palette.white, palette.neutral[950], palette.black];
  const onSolid = candidates.reduce((best, c) =>
    contrastRatio(c, hex) > contrastRatio(best, hex) ? c : best,
  );

  const backgrounds = Object.values(semantic.bg);
  const target = scheme === 'light' ? palette.black : palette.white;
  let text = hex;
  for (let step = 1; step <= MAX_STEPS && !readableOnAll(text, backgrounds); step++) {
    text = mix(hex, target, step / MAX_STEPS);
  }
  return { solid: hex, onSolid, text };
}

function readableOnAll(color: string, backgrounds: string[]): boolean {
  return backgrounds.every((bg) => contrastRatio(color, bg) >= AA);
}

function mix(from: string, to: string, amount: number): string {
  const a = channels(from);
  const b = channels(to);
  const mixed = a.map((value, i) => Math.round(value + ((b[i] ?? value) - value) * amount));
  return `#${mixed
    .map((v) => v.toString(16).padStart(2, '0'))
    .join('')
    .toUpperCase()}`;
}

function channels(hex: string): [number, number, number] {
  const value = parseInt(hex.slice(1), 16);
  return [(value >> 16) & 0xff, (value >> 8) & 0xff, value & 0xff];
}
