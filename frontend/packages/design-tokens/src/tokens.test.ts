import { describe, expect, it } from 'vitest';
import { contrastRatio, tokens, withAlpha } from './index';

const AA_NORMAL_TEXT = 4.5;

describe('contrastRatio', () => {
  it('matches the WCAG reference values', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 5);
    expect(contrastRatio('#FFFFFF', '#FFFFFF')).toBeCloseTo(1, 5);
  });

  it('rejects anything that is not #RRGGBB', () => {
    expect(() => contrastRatio('red', '#FFFFFF')).toThrow();
  });
});

describe('elevation (review 1, L-3)', () => {
  it('derives the card shadow colour from the primary text colour', () => {
    expect(withAlpha('#0F172A', 0.25)).toBe('rgba(15, 23, 42, 0.25)');
    expect(tokens.elevation.card).toContain(withAlpha(tokens.color.text.primary, 0.25));
  });
});

describe('design tokens (spec 0001, Accessibility: text contrast at least WCAG AA)', () => {
  const { text, surface, brand, status } = tokens.color;

  it.each([
    ['primary text on raised surface', text.primary, surface.raised],
    ['primary text on canvas', text.primary, surface.canvas],
    ['secondary text on raised surface', text.secondary, surface.raised],
    ['button label on brand', brand.primaryContrast, brand.primary],
    ['success text on raised surface', status.success, surface.raised],
    ['danger text on raised surface', status.danger, surface.raised],
    ['pending text on raised surface', status.pending, surface.raised],
  ])('%s reaches 4.5:1', (_name, foreground, background) => {
    expect(contrastRatio(foreground, background)).toBeGreaterThanOrEqual(AA_NORMAL_TEXT);
  });
});
