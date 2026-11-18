import { deriveBrand, tokens, withAlpha, type ColorScheme, type Tokens } from '@schoolos/design-tokens';

type ScaleStep = keyof Tokens['typography']['scale'];
type WeightName = keyof Tokens['typography']['weight'];

/**
 * Native fonts carry one weight per family (Android ignores fontWeight on custom fonts), so every weight
 * maps to its own loaded face: "Inter" + 600 → "Inter_600SemiBold" (the @expo-google-fonts names, App.tsx).
 */
const FACE_SUFFIX: Record<WeightName, string> = {
  regular: 'Regular',
  medium: 'Medium',
  semibold: 'SemiBold',
  bold: 'Bold',
};

function fontFaces(source: Tokens): Record<WeightName, string> {
  const { sans } = source.typography.fontFamily;
  const names = Object.keys(source.typography.weight) as WeightName[];
  return Object.fromEntries(
    names.map((w) => [w, `${sans}_${source.typography.weight[w]}${FACE_SUFFIX[w]}`]),
  ) as Record<WeightName, string>;
}

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
  const fontFace = fontFaces(source);
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
      scrim: withAlpha(s.shadow, source.opacity.scrim),
    },
    fontFamily: typography.fontFamily.sans,
    fontFace,
    /** Text style for one scale step and weight: face, size and line height together. */
    text: (step: ScaleStep, weight: WeightName = 'regular') => ({
      fontFamily: fontFace[weight],
      fontSize: typography.scale[step].size,
      lineHeight: typography.scale[step].lineHeight,
    }),
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

/** Light theme with the default brand. Screens read the live one from `useAppearance().theme`. */
export const nativeTheme: NativeTheme = toNativeTheme();
