import { deriveBrand, tokens } from '@schoolos/design-tokens';
import { describe, expect, it } from 'vitest';
import { applyVariables, scaleVariables, schemeVariables } from './theme';

describe('web theme (spec 0001 AC-7, spec 0002)', () => {
  it('derives from design tokens', () => {
    const scale = scaleVariables();
    expect(scale['--sos-spacing-md']).toBe(`${tokens.spacing.md}px`);
    expect(scale['--sos-text-xl']).toBe(`${tokens.typography.scale.xl.size}px`);
    expect(scale['--sos-text-xl-line-height']).toBe(`${tokens.typography.scale.xl.lineHeight}px`);
    expect(scale['--sos-layout-card-max-width']).toBe(`${tokens.layout.cardMaxWidth}px`);
    expect(scale['--sos-layer-overlay']).toBe(String(tokens.layer.overlay));
    expect(scale['--sos-duration-fast']).toBe(`${tokens.motion.durationMs.fast}ms`);
    expect(scale['--sos-ease-standard']).toBe(`cubic-bezier(${tokens.motion.easing.standard.join(', ')})`);

    const light = schemeVariables('light');
    expect(light['--sos-text-primary']).toBe(tokens.semantic.light.text.primary);
    expect(light['--sos-brand-solid']).toBe(tokens.semantic.light.brand.solid);
    expect(light['--sos-elevation-card']).toBe(tokens.elevation.light.card);
  });

  it('switches every colour with the scheme', () => {
    const dark = schemeVariables('dark');
    expect(dark['--sos-bg-canvas']).toBe(tokens.semantic.dark.bg.canvas);
    expect(dark['--sos-text-primary']).toBe(tokens.semantic.dark.text.primary);
    expect(dark['--sos-elevation-card']).toBe(tokens.elevation.dark.card);
  });

  it('follows a change in the token source', () => {
    const changed = { ...tokens, spacing: { ...tokens.spacing, md: 18 } } as unknown as typeof tokens;
    expect(scaleVariables(changed)['--sos-spacing-md']).toBe('18px');
  });

  it('brand colour flows into the theme', () => {
    const vars = schemeVariables('light', '#FFFF00');
    const brand = deriveBrand('#FFFF00', 'light');
    expect(vars['--sos-brand-solid']).toBe('#FFFF00');
    expect(vars['--sos-brand-on-solid']).toBe(brand.onSolid);
    expect(vars['--sos-brand-text']).toBe(brand.text);
    expect(vars['--sos-focus']).toBe(brand.text);
  });

  it('an invalid brand colour keeps the default brand', () => {
    expect(schemeVariables('dark', 'nope')['--sos-brand-solid']).toBe(tokens.semantic.dark.brand.solid);
  });

  // Review 1, L-2: replaces spec 0001's "one variable per token and nothing else" for the token v2 shape.
  describe('has one variable per token and nothing else', () => {
    const kebab = (name: string) => name.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`);
    const named = (prefix: string, group: object) =>
      Object.keys(group).map((k) => `--sos-${prefix}-${kebab(k)}`);

    it('knows every token group (a new group needs its web variables first)', () => {
      expect(Object.keys(tokens).sort()).toEqual(
        [
          'borderWidth',
          'elevation',
          'layer',
          'layout',
          'motion',
          'opacity',
          'palette',
          'radius',
          'semantic',
          'size',
          'spacing',
          'typography',
        ].sort(),
      );
    });

    it('scale variables', () => {
      const expected = [
        '--sos-font-sans',
        ...Object.keys(tokens.typography.scale).flatMap((k) => [
          `--sos-text-${kebab(k)}`,
          `--sos-text-${kebab(k)}-line-height`,
        ]),
        ...named('weight', tokens.typography.weight),
        ...named('spacing', tokens.spacing),
        ...named('size', tokens.size),
        ...named('layout', tokens.layout),
        ...named('radius', tokens.radius),
        ...named('border', tokens.borderWidth),
        ...named('layer', tokens.layer),
        ...named('opacity', tokens.opacity),
        ...named('duration', tokens.motion.durationMs),
        ...named('ease', tokens.motion.easing),
        '--sos-press-scale',
      ];

      expect(Object.keys(scaleVariables()).sort()).toEqual(expected.sort());
    });

    it.each(['light', 'dark'] as const)(
      '%s scheme variables: every semantic role and elevation',
      (scheme) => {
        const leaves = (node: object, path: string[] = []): string[] =>
          Object.entries(node).flatMap(([key, value]) =>
            typeof value === 'object'
              ? leaves(value as object, [...path, key])
              : [[...path, key].map(kebab).join('-')],
          );
        // `shadow` is the base colour of the elevation tokens and reaches CSS only through --sos-elevation-*.
        const roles = leaves(tokens.semantic[scheme]).filter((role) => role !== 'shadow');
        const expected = [
          ...roles.map((role) => `--sos-${role}`),
          ...named('elevation', tokens.elevation[scheme]),
        ];

        expect(Object.keys(schemeVariables(scheme)).sort()).toEqual(expected.sort());
      },
    );
  });

  it('applies the variables to an element', () => {
    const element = document.createElement('div');
    applyVariables(element, schemeVariables('light'));
    expect(element.style.getPropertyValue('--sos-brand-solid')).toBe(tokens.semantic.light.brand.solid);
  });
});
