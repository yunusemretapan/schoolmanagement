import { tokens, type Tokens } from '@schoolos/design-tokens';

type TokenTree = { readonly [key: string]: string | number | TokenTree };

const PX_GROUPS = new Set(['size', 'spacing', 'radius', 'layout']);

/**
 * Flattens the design tokens into CSS custom properties: tokens.color.brand.primary → --sos-color-brand-primary.
 * The web theme is always derived from the shared tokens, never written by hand (spec 0001, AC-7).
 */
export function toCssVariables(source: Tokens | TokenTree): Record<string, string> {
  const result: Record<string, string> = {};
  const walk = (node: TokenTree, path: string[]) => {
    for (const [key, value] of Object.entries(node)) {
      const next = [...path, key];
      if (typeof value === 'object') {
        walk(value, next);
      } else {
        result[`--sos-${next.map(kebab).join('-')}`] = unit(next, value);
      }
    }
  };
  walk(source as TokenTree, []);
  return result;
}

function unit(path: string[], value: string | number): string {
  if (typeof value === 'string') return value;
  if (path.includes('durationMs')) return `${value}ms`;
  if (path.some((segment) => PX_GROUPS.has(segment))) return `${value}px`;
  return String(value);
}

function kebab(segment: string): string {
  return segment.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`);
}

export const webTheme = toCssVariables(tokens);

export function applyTheme(element: HTMLElement, theme: Record<string, string> = webTheme): void {
  for (const [name, value] of Object.entries(theme)) {
    element.style.setProperty(name, value);
  }
}
