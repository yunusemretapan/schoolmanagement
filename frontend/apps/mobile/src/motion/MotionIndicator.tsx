import { useEffect, useMemo } from 'react';
import { StyleSheet } from 'react-native';
import Animated, {
  Easing,
  cancelAnimation,
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withTiming,
} from 'react-native-reanimated';
import { useAppearance } from '../appearance/AppearanceProvider';
import type { NativeTheme } from '../theme';

const FULL_TURN_DEG = 360;

/**
 * Spec 0002 AC-2, AC-8 (mobile): a busy ring with a brand-coloured arc. With motion on it turns once per
 * `motion.durationMs.loop`; with reduced motion it stays a static ring. This replaces the native
 * ActivityIndicator, which cannot be stopped while still shown (spec Known constraint).
 */
export function MotionIndicator({ label }: { label: string }) {
  const { theme, reducedMotion } = useAppearance();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const turn = useSharedValue(0);
  // The token object is shared by every theme (toNativeTheme passes it through), so this dependency is stable.
  const { motion } = theme;

  useEffect(() => {
    if (reducedMotion) {
      cancelAnimation(turn);
      turn.value = 0;
      return;
    }
    turn.value = withRepeat(
      withTiming(1, {
        duration: motion.durationMs.loop,
        easing: Easing.bezier(...motion.easing.linear),
      }),
      -1,
    );
    return () => cancelAnimation(turn);
  }, [reducedMotion, motion, turn]);

  const spin = useAnimatedStyle(() => ({
    transform: [{ rotate: `${turn.value * FULL_TURN_DEG}deg` }],
  }));

  return (
    <Animated.View
      testID="motion-indicator"
      accessible
      role="progressbar"
      accessibilityLabel={label}
      accessibilityState={{ busy: true }}
      style={reducedMotion ? styles.ring : [styles.ring, spin]}
    />
  );
}

function createStyles(theme: NativeTheme) {
  return StyleSheet.create({
    ring: {
      width: theme.size.spinner,
      height: theme.size.spinner,
      borderRadius: theme.radius.pill,
      borderWidth: theme.borderWidth.thick,
      borderColor: theme.colors.line,
      borderTopColor: theme.colors.brand,
    },
  });
}
