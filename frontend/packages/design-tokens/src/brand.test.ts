import { describe, expect, it } from 'vitest';
import { contrastRatio, deriveBrand, parseHexColor, tokens } from './index';

describe('parseHexColor', () => {
  it.each([
    ['#1e40af', '#1E40AF'],
    ['  #1E40AF ', '#1E40AF'],
  ])('accepts %j', (input, expected) => {
    expect(parseHexColor(input)).toBe(expected);
  });

  it.each(['', '1E40AF', '#1E40A', '#1E40AFF', '#GGGGGG', 'blue', 'rgb(1,2,3)', null, undefined])(
    'rejects %j',
    (input) => {
      expect(parseHexColor(input)).toBeNull();
    },
  );
});

describe('deriveBrand (spec 0002 AC-5)', () => {
  const extremes = ['#FFFF00', '#FEF08A', '#808080', '#0B1E3F', '#FFFFFF', '#000000', '#1E40AF', '#E11D48'];

  describe.each(['light', 'dark'] as const)('%s', (scheme) => {
    const backgrounds = Object.values(tokens.semantic[scheme].bg);

    it.each(extremes)('extreme brand colours stay AA: %s', (hex) => {
      const brand = deriveBrand(hex, scheme);

      expect(brand.solid).toBe(hex);
      expect(contrastRatio(brand.onSolid, brand.solid)).toBeGreaterThanOrEqual(4.5);
      for (const bg of backgrounds) {
        expect(contrastRatio(brand.text, bg)).toBeGreaterThanOrEqual(4.5);
      }
    });

    it('keeps an already readable brand colour as text unchanged', () => {
      const readable = scheme === 'light' ? '#1E3A8A' : '#BFDBFE';
      expect(deriveBrand(readable, scheme).text).toBe(readable);
    });
  });
});

describe('deriveBrand fallback (spec 0002 AC-6)', () => {
  it.each(['', 'not-a-colour', '#12345', null, undefined])('invalid colours fall back: %j', (input) => {
    for (const scheme of ['light', 'dark'] as const) {
      const fallback = tokens.semantic[scheme].brand;
      expect(deriveBrand(input, scheme)).toEqual({
        solid: fallback.solid,
        onSolid: fallback.onSolid,
        text: fallback.text,
      });
    }
  });
});
