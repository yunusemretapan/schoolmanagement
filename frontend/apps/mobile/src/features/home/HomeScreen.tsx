import type { CurrentUser } from '@schoolos/api-client';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useApi } from '../../api/ApiProvider';
import { AppearancePanel } from '../../appearance/AppearancePanel';
import { useAppearance } from '../../appearance/AppearanceProvider';
import { useAuth } from '../../auth/AuthProvider';
import type { NativeTheme } from '../../theme';

/** Spec 0003, Req. 10 (mobile): who is signed in, for which institution, with which roles; status, showcase, sign-out. */
export function HomeScreen({
  onOpenStatus,
  onOpenShowcase,
}: {
  onOpenStatus: () => void;
  onOpenShowcase: () => void;
}) {
  const { t } = useTranslation();
  const api = useApi();
  const { signOut } = useAuth();
  const { theme } = useAppearance();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const [signingOut, setSigningOut] = useState(false);
  const me = useQuery<CurrentUser>({
    queryKey: ['me'],
    queryFn: ({ signal }) => api.getMe(signal),
    retry: false,
  });

  async function logout() {
    setSigningOut(true);
    await signOut();
  }

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Text role="heading" style={styles.title}>
          {t('home.title')}
        </Text>
        <AppearancePanel />
      </View>

      <View style={styles.card} accessibilityLiveRegion="polite">
        {me.isPending && <Text style={styles.muted}>{t('home.loading')}</Text>}
        {me.isError && (
          <>
            <Text role="alert" style={styles.error}>
              {t('home.error')}
            </Text>
            <Pressable
              role="button"
              accessibilityLabel={t('home.retry')}
              onPress={() => void me.refetch()}
              style={styles.secondary}
            >
              <Text style={styles.secondaryLabel}>{t('home.retry')}</Text>
            </Pressable>
          </>
        )}
        {me.isSuccess && (
          <>
            <Text style={styles.greeting}>{t('home.greeting', { name: me.data.fullName })}</Text>
            <Text style={styles.muted}>{t('home.institution')}</Text>
            <Text style={styles.value}>{me.data.institutionName}</Text>
            <Text style={styles.muted}>{t('home.roles')}</Text>
            <View testID="roles" style={styles.roles}>
              {me.data.roles.map((role) => (
                <Text key={role} style={styles.role}>
                  {t(`roles.${role}`)}
                </Text>
              ))}
            </View>
          </>
        )}
      </View>

      <Pressable
        role="link"
        accessibilityLabel={t('home.status')}
        onPress={onOpenStatus}
        style={styles.secondary}
      >
        <Text style={styles.secondaryLabel}>{t('home.status')}</Text>
      </Pressable>
      <Pressable
        role="link"
        accessibilityLabel={t('home.showcase')}
        onPress={onOpenShowcase}
        style={styles.secondary}
      >
        <Text style={styles.secondaryLabel}>{t('home.showcase')}</Text>
      </Pressable>
      <Pressable
        role="button"
        accessibilityLabel={t('home.logout')}
        accessibilityState={{ disabled: signingOut }}
        disabled={signingOut}
        onPress={() => void logout()}
        style={({ pressed }) => [styles.button, pressed && styles.pressed]}
      >
        <Text style={styles.buttonLabel}>{t('home.logout')}</Text>
      </Pressable>
    </ScrollView>
  );
}

function createStyles(theme: NativeTheme) {
  const { colors } = theme;
  return StyleSheet.create({
    screen: { flex: 1, backgroundColor: colors.canvas },
    content: { padding: theme.spacing.lg, gap: theme.spacing.md },
    header: { flexDirection: 'row', alignItems: 'center', gap: theme.spacing.sm },
    title: { ...theme.text('xl', 'bold'), color: colors.ink, flex: 1 },
    card: {
      backgroundColor: colors.raised,
      borderColor: colors.line,
      borderWidth: theme.borderWidth.hairline,
      borderRadius: theme.radius.lg,
      padding: theme.spacing.xl,
      gap: theme.spacing.xs,
      boxShadow: theme.shadow.card,
    },
    greeting: { ...theme.text('lg', 'semibold'), color: colors.ink, marginBottom: theme.spacing.sm },
    muted: { ...theme.text('sm'), color: colors.inkMuted },
    value: { ...theme.text('md'), color: colors.ink },
    error: { ...theme.text('md', 'semibold'), color: colors.danger },
    roles: { flexDirection: 'row', flexWrap: 'wrap', gap: theme.spacing.xs },
    role: {
      ...theme.text('sm', 'medium'),
      color: colors.brandText,
      backgroundColor: colors.sunken,
      borderRadius: theme.radius.pill,
      paddingHorizontal: theme.spacing.sm,
    },
    secondary: {
      minHeight: theme.size.control,
      borderColor: colors.line,
      borderWidth: theme.borderWidth.hairline,
      borderRadius: theme.radius.md,
      paddingHorizontal: theme.spacing.md,
      justifyContent: 'center',
    },
    secondaryLabel: { ...theme.text('md', 'medium'), color: colors.ink },
    button: {
      minHeight: theme.size.control,
      backgroundColor: colors.brand,
      borderRadius: theme.radius.md,
      alignItems: 'center',
      justifyContent: 'center',
    },
    pressed: { opacity: theme.opacity.pressed },
    buttonLabel: { ...theme.text('md', 'semibold'), color: colors.onBrand },
  });
}
