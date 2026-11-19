import { useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { AccessibilityInfo, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { AppearancePanel } from '../../appearance/AppearancePanel';
import { useAppearance } from '../../appearance/AppearanceProvider';
import { MotionIndicator } from '../../motion/MotionIndicator';
import type { NativeTheme } from '../../theme';
import { useSystemStatus, type StatusView } from './useSystemStatus';

const DOT: Record<StatusView['kind'], keyof NativeTheme['colors']> = {
  checking: 'pending',
  running: 'success',
  unreachable: 'danger',
};

/**
 * `onOpenShowcase` shows the "Tasarım vitrini" link — signed-in users only since spec 0003 (AC-11, B-10).
 * `onBack` shows "Geri", returning to the login or home screen. Android's back gesture is left to the system here
 * (the shipped spec 0002 test pins that the status screen does not capture it).
 */
export function StatusScreen({
  onOpenShowcase,
  onBack,
}: {
  onOpenShowcase?: () => void;
  onBack?: () => void;
}) {
  const { t } = useTranslation();
  const { view, retry } = useSystemStatus();
  const { theme } = useAppearance();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const announcement =
    view.kind === 'checking'
      ? t('status.checking')
      : view.kind === 'running'
        ? t('status.runningAnnouncement', { version: view.version })
        : t('status.unreachable');

  // accessibilityLiveRegion below covers Android; iOS VoiceOver needs an explicit announcement (review 1, M-2).
  useEffect(() => {
    if (Platform.OS === 'ios') {
      AccessibilityInfo.announceForAccessibility(announcement);
    }
  }, [announcement]);

  return (
    <View style={styles.screen}>
      <View style={styles.card}>
        {onBack && (
          <Pressable
            role="button"
            accessibilityLabel={t('status.back')}
            onPress={onBack}
            style={({ pressed }) => [styles.back, pressed && styles.buttonPressed]}
          >
            <Text style={styles.linkLabel}>{t('status.back')}</Text>
          </Pressable>
        )}
        <View style={styles.header}>
          <View style={styles.logo}>
            <Text style={styles.logoLetter}>{t('app.name').charAt(0)}</Text>
          </View>
          <View style={styles.brandText}>
            <Text style={styles.appName}>{t('app.name')}</Text>
            <Text style={styles.tagline}>{t('app.tagline')}</Text>
          </View>
          <AppearancePanel />
        </View>

        <View testID="status-region" accessibilityLiveRegion="polite" style={styles.status}>
          <View style={[styles.dot, { backgroundColor: theme.colors[DOT[view.kind]] }]} />
          <View style={styles.statusText}>
            {view.kind === 'checking' && (
              <>
                <Text style={styles.title}>{t('status.checking')}</Text>
                <View style={styles.spinner}>
                  <MotionIndicator label={t('status.loading')} />
                </View>
              </>
            )}
            {view.kind === 'running' && (
              <>
                <Text style={[styles.title, { color: theme.colors.success }]}>{t('status.running')}</Text>
                <Text style={styles.version}>{t('status.version', { version: view.version })}</Text>
              </>
            )}
            {view.kind === 'unreachable' && (
              <Text style={[styles.title, { color: theme.colors.danger }]}>{t('status.unreachable')}</Text>
            )}
          </View>
        </View>

        {view.kind === 'unreachable' && (
          <Pressable
            role="button"
            accessibilityLabel={t('status.retry')}
            onPress={retry}
            style={({ pressed }) => [styles.button, pressed && styles.buttonPressed]}
          >
            <Text style={styles.buttonLabel}>{t('status.retry')}</Text>
          </Pressable>
        )}

        {onOpenShowcase && (
          <Pressable
            role="link"
            accessibilityLabel={t('showcase.open')}
            onPress={onOpenShowcase}
            style={({ pressed }) => [styles.link, pressed && styles.buttonPressed]}
          >
            <Text style={styles.linkLabel}>{t('showcase.open')}</Text>
          </Pressable>
        )}
      </View>
    </View>
  );
}

function createStyles(theme: NativeTheme) {
  return StyleSheet.create({
    screen: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: theme.colors.canvas,
      padding: theme.spacing.lg,
    },
    card: {
      width: '100%',
      maxWidth: theme.layout.cardMaxWidth,
      backgroundColor: theme.colors.raised,
      borderColor: theme.colors.line,
      borderWidth: theme.borderWidth.hairline,
      borderRadius: theme.radius.lg,
      padding: theme.spacing.xl,
      boxShadow: theme.shadow.card,
    },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: theme.spacing.sm,
      marginBottom: theme.spacing.xl,
    },
    logo: {
      width: theme.size.logo,
      height: theme.size.logo,
      borderRadius: theme.radius.md,
      backgroundColor: theme.colors.brand,
      alignItems: 'center',
      justifyContent: 'center',
    },
    logoLetter: { ...theme.text('lg', 'bold'), color: theme.colors.onBrand },
    brandText: { flex: 1 },
    appName: { ...theme.text('md', 'bold'), color: theme.colors.ink },
    tagline: { ...theme.text('sm'), color: theme.colors.inkMuted },
    status: { flexDirection: 'row', alignItems: 'center', gap: theme.spacing.md },
    dot: { width: theme.size.dot, height: theme.size.dot, borderRadius: theme.radius.pill },
    statusText: { flexShrink: 1 },
    title: { ...theme.text('lg', 'semibold'), color: theme.colors.ink },
    version: { ...theme.text('sm'), color: theme.colors.inkMuted },
    spinner: { alignSelf: 'flex-start', marginTop: theme.spacing.xs },
    button: {
      marginTop: theme.spacing.lg,
      backgroundColor: theme.colors.brand,
      borderRadius: theme.radius.md,
      paddingVertical: theme.spacing.sm + theme.spacing.xs,
      minHeight: theme.size.control,
      alignItems: 'center',
      justifyContent: 'center',
    },
    buttonPressed: { opacity: theme.opacity.pressed, transform: [{ scale: theme.motion.pressScale }] },
    buttonLabel: { ...theme.text('md', 'semibold'), color: theme.colors.onBrand },
    back: {
      minHeight: theme.size.control,
      alignSelf: 'flex-start',
      justifyContent: 'center',
      marginBottom: theme.spacing.sm,
    },
    link: {
      marginTop: theme.spacing.lg,
      minHeight: theme.size.control,
      alignSelf: 'flex-start',
      justifyContent: 'center',
    },
    linkLabel: {
      ...theme.text('sm', 'semibold'),
      color: theme.colors.brandText,
      textDecorationLine: 'underline',
    },
  });
}
