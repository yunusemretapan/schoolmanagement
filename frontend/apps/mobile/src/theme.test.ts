import { deriveBrand, tokens } from '@schoolos/design-tokens';
import { nativeTheme, toNativeTheme } from './theme';

describe('mobile theme (spec 0001 AC-7, spec 0002)', () => {
  it('derives from design tokens', () => {
    expect(nativeTheme.colors.brand).toBe(tokens.semantic.light.brand.solid);
    expect(nativeTheme.colors.ink).toBe(tokens.semantic.light.text.primary);
    expect(nativeTheme.colors.danger).toBe(tokens.semantic.light.status.danger);
    expect(nativeTheme.spacing).toEqual(tokens.spacing);
    expect(nativeTheme.fontSize.lg).toBe(tokens.typography.scale.lg.size);
    expect(nativeTheme.lineHeight.lg).toBe(tokens.typography.scale.lg.lineHeight);
    expect(nativeTheme.radius).toEqual(tokens.radius);
    expect(nativeTheme.layout).toEqual(tokens.layout);
    expect(nativeTheme.size).toEqual(tokens.size);
    expect(nativeTheme.shadow.card).toBe(tokens.elevation.light.card);
  });

  it('switches every colour with the scheme', () => {
    const dark = toNativeTheme(tokens, 'dark');
    expect(dark.colors.canvas).toBe(tokens.semantic.dark.bg.canvas);
    expect(dark.colors.ink).toBe(tokens.semantic.dark.text.primary);
    expect(dark.shadow.card).toBe(tokens.elevation.dark.card);
  });

  it('follows a change in the token source', () => {
    const changed = { ...tokens, spacing: { ...tokens.spacing, md: 18 } } as unknown as typeof tokens;

    expect(toNativeTheme(changed).spacing.md).toBe(18);
  });

  it('brand colour flows into the theme', () => {
    const theme = toNativeTheme(tokens, 'dark', '#FEF08A');
    const brand = deriveBrand('#FEF08A', 'dark');
    expect(theme.colors.brand).toBe('#FEF08A');
    expect(theme.colors.onBrand).toBe(brand.onSolid);
    expect(theme.colors.brandText).toBe(brand.text);
  });
});
