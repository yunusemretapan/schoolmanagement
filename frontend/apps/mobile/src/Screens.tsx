import { StatusBar } from 'expo-status-bar';
import { useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { StyleSheet, Text, View } from 'react-native';
import { useAppearance } from './appearance/AppearanceProvider';
import { useAuth } from './auth/AuthProvider';
import { LoginScreen } from './features/auth/LoginScreen';
import { DesignShowcase } from './features/design/DesignShowcase';
import { HomeScreen } from './features/home/HomeScreen';
import { StatusScreen } from './features/system/StatusScreen';
import type { NativeTheme } from './theme';

type Screen = 'main' | 'status' | 'showcase';

/**
 * Screen switch (no navigation library; plan 0002). Signed out: login, with the status screen one link away.
 * Signed in: home → status / showcase. The showcase is reachable only signed in (spec 0003, AC-11; B-10).
 */
export function Screens() {
  const { t } = useTranslation();
  const { scheme, theme } = useAppearance();
  const { status } = useAuth();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const [screen, setScreen] = useState<Screen>('main');
  // The showcase goes back to where it was opened from (home or the status screen).
  const [showcaseFrom, setShowcaseFrom] = useState<'main' | 'status'>('main');
  const toMain = useCallback(() => setScreen('main'), []);
  const toStatus = useCallback(() => setScreen('status'), []);
  const showcaseFromMain = useCallback(() => {
    setShowcaseFrom('main');
    setScreen('showcase');
  }, []);
  const showcaseFromStatus = useCallback(() => {
    setShowcaseFrom('status');
    setScreen('showcase');
  }, []);
  const backFromShowcase = useCallback(() => setScreen(showcaseFrom), [showcaseFrom]);
  const signedIn = status === 'signedIn';
  const current: Screen = !signedIn && screen === 'showcase' ? 'main' : screen;

  let content;
  if (status === 'restoring') {
    content = (
      <View style={styles.center}>
        <Text accessibilityLiveRegion="polite" style={styles.muted}>
          {t('auth.restoring')}
        </Text>
      </View>
    );
  } else if (current === 'status') {
    content = <StatusScreen onBack={toMain} onOpenShowcase={signedIn ? showcaseFromStatus : undefined} />;
  } else if (current === 'showcase') {
    content = <DesignShowcase onBack={backFromShowcase} />;
  } else if (signedIn) {
    content = <HomeScreen onOpenStatus={toStatus} onOpenShowcase={showcaseFromMain} />;
  } else {
    content = <LoginScreen onOpenStatus={toStatus} />;
  }

  return (
    <>
      {content}
      <StatusBar style={scheme === 'dark' ? 'light' : 'dark'} />
    </>
  );
}

function createStyles(theme: NativeTheme) {
  return StyleSheet.create({
    center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: theme.colors.canvas },
    muted: { ...theme.text('md'), color: theme.colors.inkMuted },
  });
}
