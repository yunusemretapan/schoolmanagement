import { contrastRatio } from '@schoolos/design-tokens';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { BackHandler, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import Animated, { Easing, useAnimatedStyle, useSharedValue, withTiming } from 'react-native-reanimated';
import { AppearancePanel } from '../../appearance/AppearancePanel';
import { useAppearance } from '../../appearance/AppearanceProvider';
import { MotionIndicator } from '../../motion/MotionIndicator';
import type { NativeTheme } from '../../theme';
import { BrandColorField } from './BrandColorField';

type Styles = ReturnType<typeof createStyles>;
type ScaleStep = keyof NativeTheme['fontSize'];

/**
 * Spec 0002 AC-9 (mobile): every token family on one screen, live with the appearance, motion and brand
 * choice. Opened from the status screen; "Geri" and Android's back gesture return to it.
 */
export function DesignShowcase({ onBack }: { onBack: () => void }) {
  const { t } = useTranslation();
  const { theme } = useAppearance();
  const styles = useMemo(() => createStyles(theme), [theme]);

  useEffect(() => {
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      onBack();
      return true;
    });
    return () => subscription.remove();
  }, [onBack]);

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Pressable
          role="button"
          accessibilityLabel={t('showcase.back')}
          onPress={onBack}
          style={({ pressed }) => [styles.secondaryButton, pressed && styles.pressed]}
        >
          <Text style={styles.secondaryLabel}>{t('showcase.back')}</Text>
        </Pressable>
        <AppearancePanel />
      </View>
      <View>
        <Text role="heading" style={styles.title}>
          {t('showcase.title')}
        </Text>
        <Text style={styles.subtitle}>{t('showcase.subtitle')}</Text>
      </View>

      <Section title={t('showcase.brand.title')} styles={styles}>
        <BrandColorField />
      </Section>
      <ColorSection styles={styles} />
      <TypographySection styles={styles} />
      <ScaleSection styles={styles} />
      <MotionSection styles={styles} />
    </ScrollView>
  );
}

function Section({ title, children, styles }: { title: string; children: ReactNode; styles: Styles }) {
  return (
    <View testID={`section-${title}`} style={styles.section}>
      <Text role="heading" style={styles.sectionTitle}>
        {title}
      </Text>
      {children}
    </View>
  );
}

function ColorSection({ styles }: { styles: Styles }) {
  const { t } = useTranslation();
  const { colors } = useAppearance().theme;
  const roles: { name: string; swatch: string; fg: string; on: string }[] = [
    { name: 'text.primary', swatch: colors.ink, fg: colors.ink, on: colors.raised },
    { name: 'text.secondary', swatch: colors.inkMuted, fg: colors.inkMuted, on: colors.raised },
    { name: 'brand.text', swatch: colors.brandText, fg: colors.brandText, on: colors.raised },
    { name: 'brand.solid', swatch: colors.brand, fg: colors.onBrand, on: colors.brand },
    { name: 'status.success', swatch: colors.success, fg: colors.success, on: colors.raised },
    { name: 'status.danger', swatch: colors.danger, fg: colors.danger, on: colors.raised },
    { name: 'status.pending', swatch: colors.pending, fg: colors.pending, on: colors.raised },
    { name: 'bg.canvas', swatch: colors.canvas, fg: colors.ink, on: colors.canvas },
    { name: 'bg.sunken', swatch: colors.sunken, fg: colors.ink, on: colors.sunken },
    { name: 'border.control', swatch: colors.lineStrong, fg: colors.lineStrong, on: colors.raised },
  ];
  return (
    <Section title={t('showcase.colors.title')} styles={styles}>
      <View style={styles.list}>
        {roles.map((role) => (
          <View key={role.name} testID={`color-${role.name}`} accessible style={styles.colorRow}>
            <View style={[styles.swatch, { backgroundColor: role.swatch }]} />
            <View>
              <Text style={styles.rowTitle}>{role.name}</Text>
              <Text style={styles.caption}>
                {t('showcase.colors.contrast', { ratio: contrastRatio(role.fg, role.on).toFixed(2) })}
              </Text>
            </View>
          </View>
        ))}
      </View>
    </Section>
  );
}

function TypographySection({ styles }: { styles: Styles }) {
  const { t } = useTranslation();
  const { theme } = useAppearance();
  return (
    <Section title={t('showcase.typography.title')} styles={styles}>
      <View style={styles.list}>
        {(Object.keys(theme.fontSize) as ScaleStep[]).map((step) => (
          <View key={step} style={styles.scaleRow}>
            <Text style={[styles.caption, styles.labelColumn]}>
              {t('showcase.typography.step', {
                step,
                size: theme.fontSize[step],
                lineHeight: theme.lineHeight[step],
              })}
            </Text>
            <Text style={[styles.sample, theme.text(step, 'semibold')]}>
              {t('showcase.typography.sample')}
            </Text>
          </View>
        ))}
      </View>
    </Section>
  );
}

function ScaleSection({ styles }: { styles: Styles }) {
  const { t } = useTranslation();
  const { theme } = useAppearance();
  return (
    <Section title={t('showcase.scales.title')} styles={styles}>
      <View style={styles.list}>
        {Object.entries(theme.spacing).map(([name, value]) => (
          <View key={name} style={styles.scaleRow}>
            <Text style={[styles.caption, styles.labelColumn]}>
              {t('showcase.scales.spacing', { name, value })}
            </Text>
            <View style={[styles.spacingBar, { width: value }]} />
          </View>
        ))}
      </View>
      <View style={styles.wrap}>
        {Object.entries(theme.radius).map(([name, value]) => (
          <View key={name} style={[styles.radiusSwatch, { borderRadius: value }]}>
            <Text style={styles.caption}>{name}</Text>
          </View>
        ))}
        <View style={styles.shadowSample}>
          <Text style={styles.caption}>{t('showcase.scales.shadow')}</Text>
        </View>
      </View>
    </Section>
  );
}

function MotionSection({ styles }: { styles: Styles }) {
  const { t } = useTranslation();
  const { theme, reducedMotion } = useAppearance();
  const [moved, setMoved] = useState(false);
  const offset = useSharedValue(0);

  // AC-2, AC-8: the example moves with the slow duration and standard curve; with reduced motion it jumps.
  useEffect(() => {
    const target = moved ? theme.layout.motionDemoTravel : 0;
    offset.value = reducedMotion
      ? target
      : withTiming(target, {
          duration: theme.motion.durationMs.slow,
          easing: Easing.bezier(...theme.motion.easing.standard),
        });
  }, [moved, reducedMotion, offset, theme]);

  const slide = useAnimatedStyle(() => ({ transform: [{ translateX: offset.value }] }));

  return (
    <Section title={t('showcase.motion.title')} styles={styles}>
      <Text style={styles.caption}>
        {reducedMotion ? t('showcase.motion.reduced') : t('showcase.motion.normal')}
      </Text>
      <View style={styles.wrap}>
        {Object.entries(theme.motion.durationMs).map(([name, ms]) => (
          <Text key={name} style={[styles.caption, styles.chip]}>
            {t('showcase.motion.duration', { name, ms })}
          </Text>
        ))}
      </View>
      <View style={styles.motionRow}>
        <Pressable
          role="button"
          accessibilityLabel={t('showcase.motion.play')}
          onPress={() => setMoved((m) => !m)}
          style={({ pressed }) => [styles.primaryButton, pressed && styles.pressed]}
        >
          <Text style={styles.primaryLabel}>{t('showcase.motion.play')}</Text>
        </Pressable>
        <View style={styles.track}>
          <Animated.View testID="motion-dot" style={[styles.motionDot, slide]} />
        </View>
        <MotionIndicator label={t('status.loading')} />
      </View>
    </Section>
  );
}

function createStyles(theme: NativeTheme) {
  const { colors, spacing, radius, size } = theme;
  return StyleSheet.create({
    screen: { flex: 1, backgroundColor: colors.canvas },
    content: {
      width: '100%',
      maxWidth: theme.layout.contentMaxWidth,
      alignSelf: 'center',
      gap: spacing.lg,
      padding: spacing.lg,
    },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    title: { ...theme.text('xxl', 'bold'), color: colors.ink },
    subtitle: { ...theme.text('md'), color: colors.inkMuted },
    section: {
      gap: spacing.md,
      padding: spacing.lg,
      borderRadius: radius.lg,
      borderWidth: theme.borderWidth.hairline,
      borderColor: colors.line,
      backgroundColor: colors.raised,
      boxShadow: theme.shadow.card,
    },
    sectionTitle: { ...theme.text('lg', 'semibold'), color: colors.ink },
    list: { gap: spacing.sm },
    wrap: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.md },
    colorRow: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.md,
      padding: spacing.sm,
      borderRadius: radius.md,
      backgroundColor: colors.sunken,
    },
    swatch: {
      width: size.swatch,
      height: size.swatch,
      borderRadius: radius.md,
      borderWidth: theme.borderWidth.hairline,
      borderColor: colors.line,
    },
    rowTitle: { ...theme.text('sm', 'semibold'), color: colors.ink },
    caption: { ...theme.text('xs'), color: colors.inkMuted },
    scaleRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.md },
    labelColumn: { width: theme.layout.labelColumn },
    sample: { flexShrink: 1, color: colors.ink },
    spacingBar: { height: spacing.sm, borderRadius: radius.sm, backgroundColor: colors.brand },
    radiusSwatch: {
      width: size.swatch,
      height: size.swatch,
      alignItems: 'center',
      justifyContent: 'center',
      borderWidth: theme.borderWidth.hairline,
      borderColor: colors.lineStrong,
      backgroundColor: colors.sunken,
    },
    shadowSample: {
      justifyContent: 'center',
      paddingHorizontal: spacing.lg,
      borderRadius: radius.lg,
      backgroundColor: colors.raised,
      boxShadow: theme.shadow.card,
    },
    chip: {
      paddingHorizontal: spacing.sm,
      paddingVertical: spacing.xs,
      borderRadius: radius.md,
      backgroundColor: colors.sunken,
      overflow: 'hidden',
    },
    motionRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.md },
    track: {
      flexGrow: 1,
      height: size.logo,
      justifyContent: 'center',
      padding: spacing.xs,
      borderRadius: radius.pill,
      backgroundColor: colors.sunken,
    },
    motionDot: {
      width: size.spinner,
      height: size.spinner,
      borderRadius: radius.pill,
      backgroundColor: colors.brand,
    },
    primaryButton: {
      minHeight: size.control,
      justifyContent: 'center',
      paddingHorizontal: spacing.md,
      borderRadius: radius.md,
      backgroundColor: colors.brand,
    },
    primaryLabel: { ...theme.text('sm', 'semibold'), color: colors.onBrand },
    secondaryButton: {
      minHeight: size.control,
      justifyContent: 'center',
      paddingHorizontal: spacing.md,
      borderRadius: radius.md,
      backgroundColor: colors.sunken,
    },
    secondaryLabel: { ...theme.text('sm', 'medium'), color: colors.ink },
    pressed: { opacity: theme.opacity.pressed },
  });
}
