import { deriveBrand, tokens, type ColorScheme, type Tokens } from '@schoolos/design-tokens';

type ScaleStep = keyof Tokens['typography']['scale'];

/**
 * The React Native theme, always derived from the shared design tokens (spec 0001 AC-7, spec 0002).
 * Numbers stay unit-less (density-independent pixels); font weights stay strings.
 * Colours depend on the appearance (light / dark) and the institution's brand colour (AC-5, AC-6).
 */
export function toNativeTheme(
  source: Tokens = tokens,
  scheme: ColorScheme = 'light',
  brandColor?: string | null,
) {
  const s = source.semantic[scheme];
  const brand = deriveBrand(brandColor, scheme);
  const { typography } = source;
  const steps = Object.keys(typography.scale) as ScaleStep[];
  return {
    scheme,
    colors: {
      brand: brand.solid,
      onBrand: brand.onSolid,
      brandText: brand.text,
      canvas: s.bg.canvas,
      raised: s.bg.surface,
      sunken: s.bg.sunken,
      line: s.border.subtle,
      lineStrong: s.border.control,
      ink: s.text.primary,
      inkMuted: s.text.secondary,
      success: s.status.success,
      danger: s.status.danger,
      pending: s.status.pending,
      focus: s.focus,
    },
    fontFamily: typography.fontFamily.sans,
    fontSize: Object.fromEntries(steps.map((k) => [k, typography.scale[k].size])) as Record<
      ScaleStep,
      number
    >,
    lineHeight: Object.fromEntries(steps.map((k) => [k, typography.scale[k].lineHeight])) as Record<
      ScaleStep,
      number
    >,
    fontWeight: { ...typography.weight },
    spacing: { ...source.spacing },
    size: { ...source.size },
    layout: { ...source.layout },
    radius: { ...source.radius },
    borderWidth: { ...source.borderWidth },
    opacity: { ...source.opacity },
    shadow: { ...source.elevation[scheme] },
    motion: source.motion,
  };
}

export type NativeTheme = ReturnType<typeof toNativeTheme>;

/** Light theme with the default brand. The appearance provider (spec 0002, step 5) supplies the live one. */
export const nativeTheme: NativeTheme = toNativeTheme();
