import { tokens } from '@schoolos/design-tokens';
import { curve, fadeIn, timing } from './timing';

/** Plan 0002 A-7 / spec 0002 AC-8: duration and curve always come from the motion tokens. */
describe('motion timing (mobile)', () => {
  const { motion } = tokens;
  const curveNames = Object.keys(motion.easing) as (keyof typeof motion.easing)[];

  it.each(curveNames)('the %s curve is the token curve', (name) => {
    expect(curve(motion, name)).toMatchObject({ bezier: [...motion.easing[name]] });
  });

  it('timing carries the token duration and curve', () => {
    const config = timing(motion, 'slow', 'standard');

    expect(config.duration).toBe(motion.durationMs.slow);
    expect(config.easing).toMatchObject({ bezier: [...motion.easing.standard] });
  });

  it('an entering fade names its duration and curve, so no library default is used', () => {
    const builder = fadeIn(motion, 'fast', 'enter');

    expect(builder.getDuration()).toBe(motion.durationMs.fast);
    // Reanimated keeps the chosen curve on the builder; without it the animation falls back to its default.
    expect(builder).toHaveProperty('easingV', expect.objectContaining({ bezier: [...motion.easing.enter] }));
  });
});
