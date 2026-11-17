import { tokens } from '@schoolos/design-tokens';
import { nativeTheme, toNativeTheme } from './theme';

describe('mobile theme (spec 0001, AC-7)', () => {
  it('derives from design tokens', () => {
    expect(nativeTheme.colors.brand).toBe(tokens.color.brand.primary);
    expect(nativeTheme.colors.ink).toBe(tokens.color.text.primary);
    expect(nativeTheme.colors.danger).toBe(tokens.color.status.danger);
    expect(nativeTheme.spacing).toEqual(tokens.spacing);
    expect(nativeTheme.fontSize).toEqual(tokens.typography.size);
    expect(nativeTheme.radius).toEqual(tokens.radius);
    expect(nativeTheme.layout).toEqual(tokens.layout);
    expect(nativeTheme.shadow.card).toBe(tokens.elevation.card);
  });

  it('follows a change in the token source', () => {
    const changed = {
      ...tokens,
      color: { ...tokens.color, brand: { ...tokens.color.brand, primary: '#123456' } },
    } as unknown as typeof tokens;

    expect(toNativeTheme(changed).colors.brand).toBe('#123456');
  });
});
