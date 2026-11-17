import { tokens, type Tokens } from '@schoolos/design-tokens';

/**
 * The React Native theme, always derived from the shared design tokens (spec 0001, AC-7).
 * Numbers stay unit-less (density-independent pixels); font weights stay strings.
 */
export function toNativeTheme(source: Tokens) {
  const { color, typography, spacing, radius, layout, elevation, motion } = source;
  return {
    colors: {
      brand: color.brand.primary,
      onBrand: color.brand.primaryContrast,
      accent: color.brand.accent,
      canvas: color.surface.canvas,
      raised: color.surface.raised,
      line: color.surface.border,
      ink: color.text.primary,
      inkMuted: color.text.secondary,
      success: color.status.success,
      danger: color.status.danger,
      pending: color.status.pending,
    },
    fontSize: { ...typography.size },
    fontWeight: { ...typography.weight },
    spacing: { ...spacing },
    radius: { ...radius },
    layout: { ...layout },
    shadow: { ...elevation },
    durationMs: { ...motion.durationMs },
  };
}

export type NativeTheme = ReturnType<typeof toNativeTheme>;

export const nativeTheme: NativeTheme = toNativeTheme(tokens);
