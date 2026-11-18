import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  APPEARANCE_OPTIONS,
  MOTION_OPTIONS,
  STORAGE_KEYS,
  parsePreference,
  resolveReducedMotion,
  resolveScheme,
  tokens,
  type AppearancePreference,
  type ColorScheme,
  type MotionPreference,
} from '@schoolos/design-tokens';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { AccessibilityInfo, useColorScheme } from 'react-native';
import { toNativeTheme, type NativeTheme } from '../theme';
import { configuredBrandColor } from './brandConfig';

/** The part of AsyncStorage the provider needs; tests pass an in-memory one. */
export interface PreferenceStorage {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
}

export interface AppearanceState {
  appearance: AppearancePreference;
  motion: MotionPreference;
  scheme: ColorScheme;
  reducedMotion: boolean;
  /** The brand colour in effect: a showcase preview if set, else the configured one (may be invalid → default). */
  brandColor: string | null;
  /** The live native theme for the scheme and brand colour above. */
  theme: NativeTheme;
  setAppearance: (value: AppearancePreference) => void;
  setMotion: (value: MotionPreference) => void;
  /** Showcase-only preview; never persisted (spec 0002 Requirement 11). null clears it. */
  setBrandPreview: (value: string | null) => void;
}

const AppearanceContext = createContext<AppearanceState | null>(null);

async function readStorage(storage: PreferenceStorage, key: string): Promise<string | null> {
  try {
    return await storage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(storage: PreferenceStorage, key: string, value: string): void {
  // A failed write keeps the choice for this session (same contract as web).
  storage.setItem(key, value).catch(() => undefined);
}

/**
 * The device's "reduce motion" setting, kept live (AC-2). `known` turns true once the first read settles
 * (a failed read counts as "not reduced"), so nothing animates before the setting is known (review 1, L-3).
 */
function useSystemReduceMotion(): { reduced: boolean; known: boolean } {
  const [reduced, setReduced] = useState(false);
  const [known, setKnown] = useState(false);
  useEffect(() => {
    let active = true;
    AccessibilityInfo.isReduceMotionEnabled()
      .then((value) => {
        if (active) setReduced(value);
      })
      .catch(() => undefined)
      .finally(() => {
        if (active) setKnown(true);
      });
    const subscription = AccessibilityInfo.addEventListener('reduceMotionChanged', (value) => {
      setReduced(value);
      setKnown(true);
    });
    return () => {
      active = false;
      subscription.remove();
    };
  }, []);
  return { reduced, known };
}

/**
 * Spec 0002 AC-1..AC-3, AC-5, AC-6 (mobile): holds the user's appearance and motion choice, follows the
 * device while "system" is chosen and persists the choice in AsyncStorage. Saved choices and the device's
 * reduce-motion setting load asynchronously, so children render only once both are known (no wrong-theme
 * first frame, plan R5; no motion before the setting is read, review 1 L-3);
 * `onReady` lets the app hide the splash screen at that moment.
 */
export function AppearanceProvider({
  children,
  storage = AsyncStorage,
  configuredBrand = configuredBrandColor(),
  onReady,
}: {
  children: ReactNode;
  storage?: PreferenceStorage;
  configuredBrand?: string | null;
  onReady?: () => void;
}) {
  const [loaded, setLoaded] = useState(false);
  const [appearance, setAppearanceState] = useState<AppearancePreference>('system');
  const [motion, setMotionState] = useState<MotionPreference>('system');
  const [brandPreview, setBrandPreview] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    void Promise.all([
      readStorage(storage, STORAGE_KEYS.appearance),
      readStorage(storage, STORAGE_KEYS.motion),
    ]).then(([savedAppearance, savedMotion]) => {
      if (!active) return;
      setAppearanceState(parsePreference(savedAppearance, APPEARANCE_OPTIONS));
      setMotionState(parsePreference(savedMotion, MOTION_OPTIONS));
      setLoaded(true);
    });
    return () => {
      active = false;
    };
  }, [storage]);

  const systemDark = useColorScheme() === 'dark';
  const { reduced: systemReduce, known: reduceKnown } = useSystemReduceMotion();
  const ready = loaded && reduceKnown;

  useEffect(() => {
    if (ready) onReady?.();
  }, [ready, onReady]);

  const scheme = resolveScheme(appearance, systemDark);
  const reducedMotion = resolveReducedMotion(motion, systemReduce);
  const brandColor = brandPreview ?? configuredBrand;
  const theme = useMemo(() => toNativeTheme(tokens, scheme, brandColor), [scheme, brandColor]);

  const setAppearance = useCallback(
    (value: AppearancePreference) => {
      setAppearanceState(value);
      writeStorage(storage, STORAGE_KEYS.appearance, value);
    },
    [storage],
  );
  const setMotion = useCallback(
    (value: MotionPreference) => {
      setMotionState(value);
      writeStorage(storage, STORAGE_KEYS.motion, value);
    },
    [storage],
  );

  const value = useMemo<AppearanceState>(
    () => ({
      appearance,
      motion,
      scheme,
      reducedMotion,
      brandColor,
      theme,
      setAppearance,
      setMotion,
      setBrandPreview,
    }),
    [appearance, motion, scheme, reducedMotion, brandColor, theme, setAppearance, setMotion],
  );

  if (!ready) return null;
  return <AppearanceContext.Provider value={value}>{children}</AppearanceContext.Provider>;
}

export function useAppearance(): AppearanceState {
  const state = useContext(AppearanceContext);
  if (state === null) {
    throw new Error('useAppearance must be used inside <AppearanceProvider>');
  }
  return state;
}
