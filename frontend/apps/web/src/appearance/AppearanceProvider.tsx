import {
  APPEARANCE_OPTIONS,
  MOTION_OPTIONS,
  STORAGE_KEYS,
  parsePreference,
  resolveReducedMotion,
  resolveScheme,
  type AppearancePreference,
  type ColorScheme,
  type MotionPreference,
} from '@schoolos/design-tokens';
import {
  createContext,
  useCallback,
  useContext,
  useLayoutEffect,
  useMemo,
  useState,
  useSyncExternalStore,
  type ReactNode,
} from 'react';
import { applyVariables, scaleVariables, schemeVariables } from '../theme';
import { configuredBrandColor } from './brandConfig';

const DARK_QUERY = '(prefers-color-scheme: dark)';
const REDUCE_QUERY = '(prefers-reduced-motion: reduce)';

export interface AppearanceState {
  appearance: AppearancePreference;
  motion: MotionPreference;
  scheme: ColorScheme;
  reducedMotion: boolean;
  /** The brand colour in effect: a showcase preview if set, else the configured one (may be invalid → default). */
  brandColor: string | null;
  setAppearance: (value: AppearancePreference) => void;
  setMotion: (value: MotionPreference) => void;
  /** Showcase-only preview; never persisted (spec 0002 Requirement 11). null clears it. */
  setBrandPreview: (value: string | null) => void;
}

const AppearanceContext = createContext<AppearanceState | null>(null);

function readStorage(storage: Storage | undefined, key: string): string | null {
  try {
    return storage?.getItem(key) ?? null;
  } catch {
    return null;
  }
}

function writeStorage(storage: Storage | undefined, key: string, value: string): void {
  try {
    storage?.setItem(key, value);
  } catch {
    // Private mode / quota: the choice still applies for this session.
  }
}

function useMediaQuery(query: string): boolean {
  const subscribe = useCallback(
    (onChange: () => void) => {
      const list = window.matchMedia(query);
      list.addEventListener('change', onChange);
      return () => list.removeEventListener('change', onChange);
    },
    [query],
  );
  return useSyncExternalStore(subscribe, () => window.matchMedia(query).matches);
}

/**
 * Spec 0002 AC-1..AC-3, AC-5, AC-6: holds the user's appearance and motion choice, follows the device
 * while "system" is chosen, persists the choice on this device and applies the theme to <html>.
 */
export function AppearanceProvider({
  children,
  storage = window.localStorage,
  configuredBrand = configuredBrandColor(),
}: {
  children: ReactNode;
  storage?: Storage;
  configuredBrand?: string | null;
}) {
  const [appearance, setAppearanceState] = useState<AppearancePreference>(() =>
    parsePreference(readStorage(storage, STORAGE_KEYS.appearance), APPEARANCE_OPTIONS),
  );
  const [motion, setMotionState] = useState<MotionPreference>(() =>
    parsePreference(readStorage(storage, STORAGE_KEYS.motion), MOTION_OPTIONS),
  );
  const [brandPreview, setBrandPreview] = useState<string | null>(null);

  const systemDark = useMediaQuery(DARK_QUERY);
  const systemReduce = useMediaQuery(REDUCE_QUERY);
  const scheme = resolveScheme(appearance, systemDark);
  const reducedMotion = resolveReducedMotion(motion, systemReduce);
  const brandColor = brandPreview ?? configuredBrand;

  useLayoutEffect(() => {
    const root = document.documentElement;
    root.dataset.theme = scheme;
    root.dataset.motion = reducedMotion ? 'off' : 'on';
    applyVariables(root, { ...scaleVariables(), ...schemeVariables(scheme, brandColor) });
  }, [scheme, reducedMotion, brandColor]);

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
      setAppearance,
      setMotion,
      setBrandPreview,
    }),
    [appearance, motion, scheme, reducedMotion, brandColor, setAppearance, setMotion],
  );

  return <AppearanceContext.Provider value={value}>{children}</AppearanceContext.Provider>;
}

export function useAppearance(): AppearanceState {
  const state = useContext(AppearanceContext);
  if (state === null) {
    throw new Error('useAppearance must be used inside <AppearanceProvider>');
  }
  return state;
}
