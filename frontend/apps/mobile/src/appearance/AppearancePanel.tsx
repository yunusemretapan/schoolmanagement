import {
  APPEARANCE_OPTIONS,
  MOTION_OPTIONS,
  type AppearancePreference,
  type MotionPreference,
} from '@schoolos/design-tokens';
import { useMemo, useRef, useState, type ComponentRef } from 'react';
import { useTranslation } from 'react-i18next';
import { AccessibilityInfo, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { fadeIn } from '../motion/timing';
import type { NativeTheme } from '../theme';
import { useAppearance } from './AppearanceProvider';

/**
 * Spec 0002 AC-1, AC-2, AC-10 (mobile): the "Görünüm" button and its panel. The panel is a modal: screen
 * readers stay inside it, Android's back gesture closes it (onRequestClose), and on close the reader's
 * focus returns to the button. Options are radio groups announced with their name and checked state.
 */
export function AppearancePanel() {
  const { t } = useTranslation();
  const { appearance, setAppearance, motion, setMotion, theme, reducedMotion } = useAppearance();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const [open, setOpen] = useState(false);
  const trigger = useRef<ComponentRef<typeof View>>(null);

  const close = () => {
    setOpen(false);
    if (trigger.current) AccessibilityInfo.sendAccessibilityEvent(trigger.current, 'focus');
  };

  return (
    <>
      <Pressable
        ref={trigger}
        role="button"
        accessibilityLabel={t('appearance.open')}
        accessibilityState={{ expanded: open }}
        onPress={() => setOpen(true)}
        style={({ pressed }) => [styles.trigger, pressed && styles.pressed]}
      >
        <Text style={styles.triggerLabel}>{t('appearance.open')}</Text>
      </Pressable>
      <Modal testID="appearance-modal" visible={open} transparent animationType="none" onRequestClose={close}>
        <View style={styles.backdrop}>
          <Pressable
            testID="appearance-backdrop"
            accessible={false}
            importantForAccessibility="no"
            onPress={close}
            style={StyleSheet.absoluteFill}
          />
          <Animated.View
            testID="appearance-panel"
            accessibilityViewIsModal
            aria-modal
            entering={reducedMotion ? undefined : fadeIn(theme.motion, 'fast', 'enter')}
            style={styles.panel}
          >
            <Text role="heading" style={styles.title}>
              {t('appearance.title')}
            </Text>
            <OptionGroup<AppearancePreference>
              testID="appearance-scheme"
              label={t('appearance.scheme.label')}
              value={appearance}
              options={APPEARANCE_OPTIONS}
              optionLabel={(option) => t(`appearance.scheme.${option}`)}
              onChange={setAppearance}
              styles={styles}
            />
            <OptionGroup<MotionPreference>
              testID="appearance-motion"
              label={t('appearance.motion.label')}
              value={motion}
              options={MOTION_OPTIONS}
              optionLabel={(option) => t(`appearance.motion.${option}`)}
              onChange={setMotion}
              styles={styles}
            />
            <Pressable
              role="button"
              accessibilityLabel={t('appearance.close')}
              onPress={close}
              style={({ pressed }) => [styles.close, pressed && styles.pressed]}
            >
              <Text style={styles.closeLabel}>{t('appearance.close')}</Text>
            </Pressable>
          </Animated.View>
        </View>
      </Modal>
    </>
  );
}

function OptionGroup<T extends string>({
  testID,
  label,
  value,
  options,
  optionLabel,
  onChange,
  styles,
}: {
  testID: string;
  label: string;
  value: T;
  options: readonly T[];
  optionLabel: (option: T) => string;
  onChange: (value: T) => void;
  styles: ReturnType<typeof createStyles>;
}) {
  return (
    <View style={styles.group}>
      <Text style={styles.groupLabel} importantForAccessibility="no" accessibilityElementsHidden>
        {label}
      </Text>
      <View testID={testID} role="radiogroup" accessibilityLabel={label} style={styles.segments}>
        {options.map((option) => {
          const checked = option === value;
          return (
            <Pressable
              key={option}
              role="radio"
              accessibilityLabel={optionLabel(option)}
              accessibilityState={{ checked }}
              onPress={() => onChange(option)}
              style={[styles.segment, checked && styles.segmentChecked]}
            >
              <Text style={[styles.segmentLabel, checked && styles.segmentLabelChecked]}>
                {optionLabel(option)}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

function createStyles(theme: NativeTheme) {
  return StyleSheet.create({
    trigger: {
      minHeight: theme.size.control,
      paddingHorizontal: theme.spacing.md,
      justifyContent: 'center',
      borderRadius: theme.radius.md,
      borderWidth: theme.borderWidth.hairline,
      borderColor: theme.colors.lineStrong,
      backgroundColor: theme.colors.raised,
    },
    triggerLabel: { ...theme.text('sm', 'medium'), color: theme.colors.ink },
    pressed: { opacity: theme.opacity.pressed },
    backdrop: {
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
      padding: theme.spacing.lg,
      backgroundColor: theme.colors.scrim,
    },
    panel: {
      width: '100%',
      maxWidth: theme.layout.cardMaxWidth,
      gap: theme.spacing.lg,
      padding: theme.spacing.lg,
      borderRadius: theme.radius.lg,
      borderWidth: theme.borderWidth.hairline,
      borderColor: theme.colors.line,
      backgroundColor: theme.colors.raised,
      boxShadow: theme.shadow.card,
    },
    title: { ...theme.text('lg', 'semibold'), color: theme.colors.ink },
    group: { gap: theme.spacing.sm },
    groupLabel: { ...theme.text('sm', 'semibold'), color: theme.colors.inkMuted },
    segments: {
      flexDirection: 'row',
      gap: theme.spacing.xs,
      padding: theme.spacing.xxs,
      borderRadius: theme.radius.md,
      backgroundColor: theme.colors.sunken,
    },
    segment: {
      flex: 1,
      minHeight: theme.size.control,
      alignItems: 'center',
      justifyContent: 'center',
      paddingHorizontal: theme.spacing.sm,
      borderRadius: theme.radius.sm,
    },
    segmentChecked: { backgroundColor: theme.colors.raised, boxShadow: theme.shadow.card },
    segmentLabel: { ...theme.text('sm', 'medium'), color: theme.colors.inkMuted },
    segmentLabelChecked: { color: theme.colors.ink },
    close: {
      minHeight: theme.size.control,
      alignItems: 'center',
      justifyContent: 'center',
      borderRadius: theme.radius.md,
      backgroundColor: theme.colors.sunken,
    },
    closeLabel: { ...theme.text('sm', 'medium'), color: theme.colors.ink },
  });
}
