// Per-weight entry points: the package index would bundle all 18 Inter faces (~6 MB) instead of these 4.
import { Inter_400Regular } from '@expo-google-fonts/inter/400Regular';
import { Inter_500Medium } from '@expo-google-fonts/inter/500Medium';
import { Inter_600SemiBold } from '@expo-google-fonts/inter/600SemiBold';
import { Inter_700Bold } from '@expo-google-fonts/inter/700Bold';
import { createApiClient } from '@schoolos/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useFonts } from 'expo-font';
import * as SplashScreen from 'expo-splash-screen';
import { useCallback, useEffect, useState } from 'react';
import { ApiProvider } from './src/api/ApiProvider';
import { AppearanceProvider } from './src/appearance/AppearanceProvider';
import { Screens } from './src/Screens';
import { nativeTheme } from './src/theme';
import './src/i18n';

const api = createApiClient(process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080');

// Keep the splash until fonts and saved appearance are known: no wrong-theme or fallback-font first frame
// (spec 0002, plan R5).
void SplashScreen.preventAutoHideAsync().catch(() => undefined);

// Keys are the face names the theme asks for (theme.fontFace, e.g. "Inter_600SemiBold").
const FONTS = {
  [nativeTheme.fontFace.regular]: Inter_400Regular,
  [nativeTheme.fontFace.medium]: Inter_500Medium,
  [nativeTheme.fontFace.semibold]: Inter_600SemiBold,
  [nativeTheme.fontFace.bold]: Inter_700Bold,
};

export default function App() {
  const [queryClient] = useState(() => new QueryClient());
  const [fontsLoaded, fontError] = useFonts(FONTS);
  const fontsSettled = fontsLoaded || fontError !== null;
  const [appearanceReady, setAppearanceReady] = useState(false);
  const onAppearanceReady = useCallback(() => setAppearanceReady(true), []);

  const ready = fontsSettled && appearanceReady;

  useEffect(() => {
    if (ready) void SplashScreen.hideAsync().catch(() => undefined);
  }, [ready]);

  return (
    <QueryClientProvider client={queryClient}>
      <ApiProvider client={api}>
        <AppearanceProvider onReady={onAppearanceReady}>
          {/* A font that fails to load falls back to the system font rather than blocking the app. */}
          {fontsSettled && <Screens />}
        </AppearanceProvider>
      </ApiProvider>
    </QueryClientProvider>
  );
}
