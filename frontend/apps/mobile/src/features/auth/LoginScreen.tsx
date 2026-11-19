import { ApiError, ERROR_CODES } from '@schoolos/api-client';
import { validateLogin, type LoginFieldErrors } from '@schoolos/auth';
import { useMemo, useRef, useState, type ComponentRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { AppearancePanel } from '../../appearance/AppearancePanel';
import { useAppearance } from '../../appearance/AppearanceProvider';
import { useAuth } from '../../auth/AuthProvider';
import { MotionIndicator } from '../../motion/MotionIndicator';
import type { NativeTheme } from '../../theme';

type FormError = 'invalidCredentials' | 'rateLimited' | 'network';

function formErrorOf(error: unknown): FormError {
  if (error instanceof ApiError) {
    if (error.code === ERROR_CODES.invalidCredentials) return 'invalidCredentials';
    if (error.code === ERROR_CODES.rateLimited) return 'rateLimited';
    if (error.status === 401) return 'invalidCredentials';
  }
  return 'network';
}

/**
 * Spec 0003 login screen (mobile): empty, field errors (nothing sent), submitting (no double submit), refused,
 * network error (incl. 10 s timeout). After any error the identifier stays and the password is cleared. The
 * keyboard's "next" moves identifier → password, and "go" on the password submits.
 */
export function LoginScreen({ onOpenStatus }: { onOpenStatus: () => void }) {
  const { t } = useTranslation();
  const { expired, signIn } = useAuth();
  const { theme } = useAppearance();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const passwordRef = useRef<ComponentRef<typeof TextInput>>(null);

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<LoginFieldErrors>({});
  const [formError, setFormError] = useState<FormError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit() {
    if (submitting) return;
    const errors = validateLogin({ identifier, password });
    setFieldErrors(errors);
    setFormError(null);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      await signIn(identifier.trim(), password);
    } catch (error) {
      setFormError(formErrorOf(error));
      setPassword('');
      setSubmitting(false);
    }
  }

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
    >
      <View style={styles.card}>
        <View style={styles.header}>
          <View style={styles.logo}>
            <Text style={styles.logoLetter}>{t('app.name').charAt(0)}</Text>
          </View>
          <View style={styles.brandText}>
            <Text role="heading" style={styles.title}>
              {t('auth.title')}
            </Text>
            <Text style={styles.subtitle}>{t('auth.subtitle')}</Text>
          </View>
          <AppearancePanel />
        </View>

        {expired && formError === null && (
          <Text accessibilityLiveRegion="polite" testID="session-expired" style={styles.notice}>
            {t('auth.sessionExpired')}
          </Text>
        )}
        {formError !== null && (
          <Text role="alert" accessibilityLiveRegion="assertive" style={[styles.notice, styles.formError]}>
            {t(`auth.errors.${formError}`)}
          </Text>
        )}

        <View style={styles.field}>
          <Text style={styles.label}>{t('auth.identifier')}</Text>
          <TextInput
            accessibilityLabel={t('auth.identifier')}
            accessibilityHint={fieldErrors.identifier === undefined ? undefined : t(fieldErrors.identifier)}
            aria-invalid={fieldErrors.identifier !== undefined}
            value={identifier}
            onChangeText={setIdentifier}
            autoCapitalize="none"
            autoCorrect={false}
            keyboardType="email-address"
            textContentType="username"
            autoComplete="username"
            returnKeyType="next"
            onSubmitEditing={() => passwordRef.current?.focus()}
            submitBehavior="submit"
            style={[styles.input, fieldErrors.identifier !== undefined && styles.inputInvalid]}
          />
          {fieldErrors.identifier !== undefined && (
            <Text role="alert" style={styles.fieldError}>
              {t(fieldErrors.identifier)}
            </Text>
          )}
        </View>

        <View style={styles.field}>
          <Text style={styles.label}>{t('auth.password')}</Text>
          <TextInput
            ref={passwordRef}
            accessibilityLabel={t('auth.password')}
            accessibilityHint={fieldErrors.password === undefined ? undefined : t(fieldErrors.password)}
            aria-invalid={fieldErrors.password !== undefined}
            value={password}
            onChangeText={setPassword}
            secureTextEntry={!showPassword}
            autoCapitalize="none"
            autoCorrect={false}
            textContentType="password"
            autoComplete="current-password"
            returnKeyType="go"
            onSubmitEditing={() => void submit()}
            style={[styles.input, fieldErrors.password !== undefined && styles.inputInvalid]}
          />
          {fieldErrors.password !== undefined && (
            <Text role="alert" style={styles.fieldError}>
              {t(fieldErrors.password)}
            </Text>
          )}
        </View>

        <Pressable
          role="checkbox"
          accessibilityLabel={t('auth.showPassword')}
          accessibilityState={{ checked: showPassword }}
          onPress={() => setShowPassword((shown) => !shown)}
          style={styles.toggle}
        >
          <View style={[styles.box, showPassword && styles.boxChecked]} />
          <Text style={styles.toggleLabel}>{t('auth.showPassword')}</Text>
        </Pressable>

        <Pressable
          role="button"
          accessibilityLabel={submitting ? t('auth.submitting') : t('auth.submit')}
          accessibilityState={{ disabled: submitting, busy: submitting }}
          disabled={submitting}
          onPress={() => void submit()}
          style={({ pressed }) => [styles.button, (pressed || submitting) && styles.buttonPressed]}
        >
          {submitting && <MotionIndicator label={t('auth.submitting')} />}
          <Text style={styles.buttonLabel}>{submitting ? t('auth.submitting') : t('auth.submit')}</Text>
        </Pressable>

        <Pressable
          role="link"
          accessibilityLabel={t('auth.statusLink')}
          onPress={onOpenStatus}
          style={styles.link}
        >
          <Text style={styles.linkLabel}>{t('auth.statusLink')}</Text>
        </Pressable>
      </View>
    </ScrollView>
  );
}

function createStyles(theme: NativeTheme) {
  const { colors } = theme;
  return StyleSheet.create({
    screen: { flex: 1, backgroundColor: colors.canvas },
    content: { flexGrow: 1, alignItems: 'center', justifyContent: 'center', padding: theme.spacing.lg },
    card: {
      width: '100%',
      maxWidth: theme.layout.cardMaxWidth,
      backgroundColor: colors.raised,
      borderColor: colors.line,
      borderWidth: theme.borderWidth.hairline,
      borderRadius: theme.radius.lg,
      padding: theme.spacing.xl,
      gap: theme.spacing.md,
      boxShadow: theme.shadow.card,
    },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: theme.spacing.sm,
      marginBottom: theme.spacing.sm,
    },
    logo: {
      width: theme.size.logo,
      height: theme.size.logo,
      borderRadius: theme.radius.md,
      backgroundColor: colors.brand,
      alignItems: 'center',
      justifyContent: 'center',
    },
    logoLetter: { ...theme.text('lg', 'bold'), color: colors.onBrand },
    brandText: { flex: 1 },
    title: { ...theme.text('lg', 'bold'), color: colors.ink },
    subtitle: { ...theme.text('sm'), color: colors.inkMuted },
    notice: {
      ...theme.text('sm'),
      color: colors.ink,
      backgroundColor: colors.sunken,
      borderRadius: theme.radius.md,
      padding: theme.spacing.sm,
    },
    formError: { ...theme.text('sm', 'semibold'), color: colors.danger },
    field: { gap: theme.spacing.xs },
    label: { ...theme.text('sm', 'medium'), color: colors.ink },
    input: {
      ...theme.text('md'),
      color: colors.ink,
      minHeight: theme.size.control,
      borderColor: colors.lineStrong,
      borderWidth: theme.borderWidth.hairline,
      borderRadius: theme.radius.md,
      paddingHorizontal: theme.spacing.md,
      backgroundColor: colors.raised,
    },
    inputInvalid: { borderColor: colors.danger },
    fieldError: { ...theme.text('sm'), color: colors.danger },
    toggle: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: theme.spacing.sm,
      minHeight: theme.size.control,
    },
    box: {
      width: theme.size.dot,
      height: theme.size.dot,
      borderRadius: theme.radius.sm,
      borderColor: colors.lineStrong,
      borderWidth: theme.borderWidth.hairline,
    },
    boxChecked: { backgroundColor: colors.brand, borderColor: colors.brand },
    toggleLabel: { ...theme.text('sm'), color: colors.inkMuted },
    button: {
      flexDirection: 'row',
      gap: theme.spacing.sm,
      backgroundColor: colors.brand,
      borderRadius: theme.radius.md,
      minHeight: theme.size.control,
      alignItems: 'center',
      justifyContent: 'center',
    },
    buttonPressed: { opacity: theme.opacity.pressed },
    buttonLabel: { ...theme.text('md', 'semibold'), color: colors.onBrand },
    link: { minHeight: theme.size.control, alignSelf: 'center', justifyContent: 'center' },
    linkLabel: { ...theme.text('sm', 'semibold'), color: colors.brandText, textDecorationLine: 'underline' },
  });
}
