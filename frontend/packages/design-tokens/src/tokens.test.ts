import { describe, expect, it } from 'vitest';
import { contrastRatio, tokens, withAlpha } from './index';

const AA_TEXT = 4.5;
const AA_NON_TEXT = 3;

describe('contrastRatio', () => {
  it('matches the WCAG reference values', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 5);
    expect(contrastRatio('#FFFFFF', '#FFFFFF')).toBeCloseTo(1, 5);
  });

  it('rejects anything that is not #RRGGBB', () => {
    expect(() => contrastRatio('red', '#FFFFFF')).toThrow();
  });
});

describe.each(['light', 'dark'] as const)('%s scheme (spec 0002 AC-4)', (scheme) => {
  const s = tokens.semantic[scheme];
  const backgrounds = Object.entries(s.bg);
  const textColours: [string, string][] = [
    ['text.primary', s.text.primary],
    ['text.secondary', s.text.secondary],
    ['brand.text', s.brand.text],
    ['status.success', s.status.success],
    ['status.danger', s.status.danger],
    ['status.pending', s.status.pending],
  ];

  it('every semantic text/background pair reaches AA in light and dark', () => {
    const failures = backgrounds.flatMap(([bgName, bg]) =>
      textColours
        .filter(([, fg]) => contrastRatio(fg, bg) < AA_TEXT)
        .map(([fgName, fg]) => `${fgName} on bg.${bgName}: ${contrastRatio(fg, bg).toFixed(2)}`),
    );
    expect(failures).toEqual([]);
  });

  it('text on the brand fill reaches AA', () => {
    expect(contrastRatio(s.brand.onSolid, s.brand.solid)).toBeGreaterThanOrEqual(AA_TEXT);
  });

  it('control borders and the focus ring reach 3:1 on every background', () => {
    const failures = backgrounds.flatMap(([bgName, bg]) =>
      [
        ['border.control', s.border.control],
        ['focus', s.focus],
      ]
        .filter(([, fg]) => contrastRatio(fg as string, bg) < AA_NON_TEXT)
        .map(([name]) => `${name} on bg.${bgName}`),
    );
    expect(failures).toEqual([]);
  });
});

describe('scales', () => {
  it('typography sizes and line heights grow monotonically', () => {
    const steps = Object.values(tokens.typography.scale);
    for (let i = 1; i < steps.length; i++) {
      expect(steps[i]!.size).toBeGreaterThan(steps[i - 1]!.size);
      expect(steps[i]!.lineHeight).toBeGreaterThanOrEqual(steps[i]!.size);
    }
  });

  it('spacing grows monotonically', () => {
    const values = Object.values(tokens.spacing);
    expect([...values].sort((a, b) => a - b)).toEqual(values);
  });

  it('the touch target is at least 44', () => {
    expect(tokens.size.control).toBeGreaterThanOrEqual(44);
  });
});

describe('motion tokens', () => {
  it('durations are ordered and easing curves are valid cubic-beziers', () => {
    const { fast, normal, slow } = tokens.motion.durationMs;
    expect(fast).toBeLessThan(normal);
    expect(normal).toBeLessThan(slow);
    for (const curve of Object.values(tokens.motion.easing)) {
      expect(curve).toHaveLength(4);
      expect(curve[0]).toBeGreaterThanOrEqual(0);
      expect(curve[0]).toBeLessThanOrEqual(1);
      expect(curve[2]).toBeGreaterThanOrEqual(0);
      expect(curve[2]).toBeLessThanOrEqual(1);
    }
  });
});

describe('elevation', () => {
  it('derives each shadow colour from its scheme', () => {
    expect(withAlpha('#0F172A', 0.25)).toBe('rgba(15, 23, 42, 0.25)');
    expect(tokens.elevation.light.card).toContain(withAlpha(tokens.semantic.light.shadow, 0.25));
    expect(tokens.elevation.dark.card).toContain(withAlpha(tokens.semantic.dark.shadow, 0.6));
  });
});
