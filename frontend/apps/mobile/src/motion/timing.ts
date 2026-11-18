import type { Tokens } from '@schoolos/design-tokens';
import { Easing, FadeIn, withTiming, type WithTimingConfig } from 'react-native-reanimated';

/**
 * Plan 0002 A-7 (spec 0002 AC-8): the only place mobile code reaches Reanimated's timing. Every animation
 * names its duration and curve as motion tokens, so nothing can fall back to a library default
 * (withTiming's Easing.inOut(quad), a layout animation's 300 ms). design-lint forbids withTiming /
 * withSpring / entering-exiting builders outside src/motion/.
 */
type Motion = Tokens['motion'];
export type DurationName = keyof Motion['durationMs'];
export type CurveName = keyof Motion['easing'];

export function curve(motion: Motion, name: CurveName) {
  const [x1, y1, x2, y2] = motion.easing[name];
  return Easing.bezier(x1, y1, x2, y2);
}

export function timing(motion: Motion, duration: DurationName, easing: CurveName): WithTimingConfig {
  return { duration: motion.durationMs[duration], easing: curve(motion, easing) };
}

/** withTiming with token duration and curve. */
export function animateTo(motion: Motion, value: number, duration: DurationName, easing: CurveName) {
  return withTiming(value, timing(motion, duration, easing));
}

/** An `entering` fade with token duration and curve. */
export function fadeIn(motion: Motion, duration: DurationName, easing: CurveName) {
  return FadeIn.duration(motion.durationMs[duration]).easing(curve(motion, easing));
}
