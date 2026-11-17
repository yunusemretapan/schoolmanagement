import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  AccessibilityInfo,
  ActivityIndicator,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { nativeTheme as theme } from '../../theme';
import { useSystemStatus, type StatusView } from './useSystemStatus';

const DOT: Record<StatusView['kind'], string> = {
  checking: theme.colors.pending,
  running: theme.colors.success,
  unreachable: theme.colors.danger,
};

export function StatusScreen() {
  const { t } = useTranslation();
  const { view, retry } = useSystemStatus();
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
        <View style={styles.header}>
          <View style={styles.logo}>
            <Text style={styles.logoLetter}>{t('app.name').charAt(0)}</Text>
          </View>
          <View>
            <Text style={styles.appName}>{t('app.name')}</Text>
            <Text style={styles.tagline}>{t('app.tagline')}</Text>
          </View>
        </View>

        <View testID="status-region" accessibilityLiveRegion="polite" style={styles.status}>
          <View style={[styles.dot, { backgroundColor: DOT[view.kind] }]} />
          <View style={styles.statusText}>
            {view.kind === 'checking' && (
              <>
                <Text style={styles.title}>{t('status.checking')}</Text>
                <ActivityIndicator
                  accessibilityLabel={t('status.loading')}
                  color={theme.colors.brand}
                  style={styles.spinner}
                />
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
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
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
    borderWidth: 1,
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
    width: 40,
    height: 40,
    borderRadius: theme.radius.md,
    backgroundColor: theme.colors.brand,
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoLetter: { color: theme.colors.onBrand, fontSize: theme.fontSize.lg, fontWeight: theme.fontWeight.bold },
  appName: { color: theme.colors.ink, fontSize: theme.fontSize.md, fontWeight: theme.fontWeight.bold },
  tagline: { color: theme.colors.inkMuted, fontSize: theme.fontSize.sm },
  status: { flexDirection: 'row', alignItems: 'center', gap: theme.spacing.md },
  dot: { width: 12, height: 12, borderRadius: theme.radius.pill },
  statusText: { flexShrink: 1 },
  title: { color: theme.colors.ink, fontSize: theme.fontSize.lg, fontWeight: theme.fontWeight.semibold },
  version: { color: theme.colors.inkMuted, fontSize: theme.fontSize.sm },
  spinner: { alignSelf: 'flex-start', marginTop: theme.spacing.xs },
  button: {
    marginTop: theme.spacing.lg,
    backgroundColor: theme.colors.brand,
    borderRadius: theme.radius.md,
    paddingVertical: theme.spacing.sm + theme.spacing.xs,
    alignItems: 'center',
  },
  buttonPressed: { opacity: 0.85, transform: [{ scale: 0.98 }] },
  buttonLabel: {
    color: theme.colors.onBrand,
    fontSize: theme.fontSize.md,
    fontWeight: theme.fontWeight.semibold,
  },
});
