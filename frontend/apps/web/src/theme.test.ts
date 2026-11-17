import { tokens } from '@schoolos/design-tokens';
import { describe, expect, it } from 'vitest';
import { applyTheme, toCssVariables, webTheme } from './theme';

describe('web theme (spec 0001, AC-7)', () => {
  it('derives from design tokens', () => {
    expect(webTheme['--sos-color-brand-primary']).toBe(tokens.color.brand.primary);
    expect(webTheme['--sos-color-text-primary']).toBe(tokens.color.text.primary);
    expect(webTheme['--sos-spacing-md']).toBe(`${tokens.spacing.md}px`);
    expect(webTheme['--sos-typography-size-xl']).toBe(`${tokens.typography.size.xl}px`);
    expect(webTheme['--sos-layout-card-max-width']).toBe(`${tokens.layout.cardMaxWidth}px`);
    expect(webTheme['--sos-elevation-card']).toBe(tokens.elevation.card);
    expect(webTheme['--sos-motion-duration-ms-fast']).toBe(`${tokens.motion.durationMs.fast}ms`);
  });

  it('follows a change in the token source', () => {
    const changed = {
      ...tokens,
      color: { ...tokens.color, brand: { ...tokens.color.brand, primary: '#123456' } },
    };

    expect(toCssVariables(changed)['--sos-color-brand-primary']).toBe('#123456');
  });

  it('has one variable per token and nothing else', () => {
    const leafCount = (node: object): number =>
      Object.values(node).reduce<number>(
        (sum, value) => sum + (typeof value === 'object' ? leafCount(value as object) : 1),
        0,
      );

    expect(Object.keys(webTheme)).toHaveLength(leafCount(tokens));
  });

  it('applies the variables to an element', () => {
    const element = document.createElement('div');

    applyTheme(element);

    expect(element.style.getPropertyValue('--sos-color-brand-primary')).toBe(tokens.color.brand.primary);
  });
});
