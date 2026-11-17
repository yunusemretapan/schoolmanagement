import {
  deriveBrand,
  parseHexColor,
  tokens,
  withAlpha,
  type ColorScheme,
  type Tokens,
} from '@schoolos/design-tokens';

/**
 * Web theme = CSS custom properties derived from @schoolos/design-tokens (spec 0001 AC-7, spec 0002).
 * Two layers: scale variables (same for every appearance) and scheme variables (colours, shadow) that
 * change with light/dark and the institution's brand colour. index.css maps them to Tailwind utilities.
 */

const kebab = (segment: string) => segment.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`);

/** Typography, spacing, sizes, layout, radius, borders, opacity, motion: --sos-<group>-<name>. */
export function scaleVariables(source: Tokens = tokens): Record<string, string> {
  const vars: Record<string, string> = {
    '--sos-font-sans': source.typography.fontFamily.sansStack,
  };
  for (const [name, step] of Object.entries(source.typography.scale)) {
    vars[`--sos-text-${kebab(name)}`] = `${step.size}px`;
    vars[`--sos-text-${kebab(name)}-line-height`] = `${step.lineHeight}px`;
  }
  for (const [name, weight] of Object.entries(source.typography.weight))
    vars[`--sos-weight-${name}`] = weight;
  const px = (group: string, values: Record<string, number>) => {
    for (const [name, value] of Object.entries(values)) vars[`--sos-${group}-${kebab(name)}`] = `${value}px`;
  };
  px('spacing', source.spacing);
  px('size', source.size);
  px('layout', source.layout);
  px('radius', source.radius);
  px('border', source.borderWidth);
  for (const [name, value] of Object.entries(source.opacity))
    vars[`--sos-opacity-${kebab(name)}`] = String(value);
  for (const [name, value] of Object.entries(source.motion.durationMs)) {
    vars[`--sos-duration-${kebab(name)}`] = `${value}ms`;
  }
  for (const [name, curve] of Object.entries(source.motion.easing)) {
    vars[`--sos-ease-${kebab(name)}`] = `cubic-bezier(${curve.join(', ')})`;
  }
  vars['--sos-press-scale'] = String(source.motion.pressScale);
  return vars;
}

/** Colours and shadow for one appearance, with the institution's brand colour if valid (AC-5, AC-6). */
export function schemeVariables(
  scheme: ColorScheme,
  brandColor?: string | null,
  source: Tokens = tokens,
): Record<string, string> {
  const s = source.semantic[scheme];
  const brand = deriveBrand(brandColor, scheme);
  const customBrand = parseHexColor(brandColor) !== null;
  return {
    '--sos-bg-canvas': s.bg.canvas,
    '--sos-bg-surface': s.bg.surface,
    '--sos-bg-sunken': s.bg.sunken,
    '--sos-border-subtle': s.border.subtle,
    '--sos-border-control': s.border.control,
    '--sos-text-primary': s.text.primary,
    '--sos-text-secondary': s.text.secondary,
    '--sos-brand-solid': brand.solid,
    '--sos-brand-on-solid': brand.onSolid,
    '--sos-brand-text': brand.text,
    '--sos-brand-subtle': customBrand ? withAlpha(brand.solid, source.opacity.glow) : s.brand.subtle,
    '--sos-status-success': s.status.success,
    '--sos-status-danger': s.status.danger,
    '--sos-status-pending': s.status.pending,
    '--sos-focus': customBrand ? brand.text : s.focus,
    '--sos-elevation-card': source.elevation[scheme].card,
  };
}

export function applyVariables(element: HTMLElement, vars: Record<string, string>): void {
  for (const [name, value] of Object.entries(vars)) element.style.setProperty(name, value);
}
