import { parseHexColor } from '@schoolos/design-tokens';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AccessibilityInfo, Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { useAppearance } from '../../appearance/AppearanceProvider';
import type { NativeTheme } from '../../theme';

/**
 * Spec 0002 AC-5, AC-6 (mobile): preview an institution colour. A valid #RRGGBB applies at once; an invalid
 * entry keeps the configured / default brand and says why (announced to screen readers); empty means
 * "default" without an error. Never persisted.
 */
export function BrandColorField() {
  const { t } = useTranslation();
  const { theme, setBrandPreview } = useAppearance();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const [value, setValue] = useState('');
  const invalid = value.trim() !== '' && parseHexColor(value) === null;
  const error = t('showcase.brand.invalid');

  // accessibilityLiveRegion covers Android; iOS VoiceOver needs an explicit announcement (as on the status screen).
  useEffect(() => {
    if (invalid && Platform.OS === 'ios') AccessibilityInfo.announceForAccessibility(error);
  }, [invalid, error]);

  const onChange = (next: string) => {
    setValue(next);
    setBrandPreview(parseHexColor(next));
  };

  return (
    <View style={styles.field}>
      <Text style={styles.label} importantForAccessibility="no" accessibilityElementsHidden>
        {t('showcase.brand.label')}
      </Text>
      <View style={styles.row}>
        <TextInput
          value={value}
          onChangeText={onChange}
          accessibilityLabel={t('showcase.brand.label')}
          accessibilityHint={invalid ? error : undefined}
          placeholder={t('showcase.brand.placeholder')}
          placeholderTextColor={theme.colors.inkMuted}
          autoCapitalize="none"
          autoCorrect={false}
          spellCheck={false}
          style={[styles.input, invalid && styles.inputInvalid]}
        />
        <Pressable
          role="button"
          accessibilityLabel={t('showcase.brand.reset')}
          onPress={() => onChange('')}
          style={({ pressed }) => [styles.reset, pressed && styles.pressed]}
        >
          <Text style={styles.resetLabel}>{t('showcase.brand.reset')}</Text>
        </Pressable>
      </View>
      {invalid && (
        <Text testID="brand-error" role="alert" accessibilityLiveRegion="assertive" style={styles.error}>
          {error}
        </Text>
      )}
    </View>
  );
}

function createStyles(theme: NativeTheme) {
  return StyleSheet.create({
    field: { gap: theme.spacing.xs },
    label: { ...theme.text('sm', 'semibold'), color: theme.colors.ink },
    row: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: theme.spacing.sm },
    input: {
      ...theme.text('md', 'medium'),
      flexGrow: 1,
      minHeight: theme.size.control,
      paddingHorizontal: theme.spacing.md,
      borderRadius: theme.radius.md,
      borderWidth: theme.borderWidth.hairline,
      borderColor: theme.colors.lineStrong,
      backgroundColor: theme.colors.raised,
      color: theme.colors.ink,
    },
    inputInvalid: { borderColor: theme.colors.danger, borderWidth: theme.borderWidth.thick },
    reset: {
      minHeight: theme.size.control,
      justifyContent: 'center',
      paddingHorizontal: theme.spacing.md,
      borderRadius: theme.radius.md,
      backgroundColor: theme.colors.sunken,
    },
    resetLabel: { ...theme.text('sm', 'medium'), color: theme.colors.ink },
    pressed: { opacity: theme.opacity.pressed },
    error: { ...theme.text('sm', 'medium'), color: theme.colors.danger },
  });
}
