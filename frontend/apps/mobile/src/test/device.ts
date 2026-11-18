import { useSyncExternalStore } from 'react';
import { AccessibilityInfo } from 'react-native';
import type { PreferenceStorage } from '../appearance/AppearanceProvider';

/**
 * A controllable device for tests (spec 0002 AC-1, AC-2): the system colour scheme and the
 * "reduce motion" setting, both changeable while the app is mounted.
 */
type Scheme = 'light' | 'dark';

let scheme: Scheme = 'light';
let reduceMotion = false;
const schemeListeners = new Set<() => void>();
const reduceListeners = new Set<(value: boolean) => void>();

/** Stands in for react-native's useColorScheme (mocked in afterEnv.ts). */
export function useFakeColorScheme(): Scheme {
  return useSyncExternalStore(
    (listener) => {
      schemeListeners.add(listener);
      return () => schemeListeners.delete(listener);
    },
    () => scheme,
  );
}

export const device = {
  setColorScheme(value: Scheme) {
    scheme = value;
    schemeListeners.forEach((listener) => listener());
  },
  setReduceMotion(value: boolean) {
    reduceMotion = value;
    reduceListeners.forEach((listener) => listener(value));
  },
  /** Back to a light, full-motion device; wires AccessibilityInfo's jest mock to this fake. */
  reset() {
    scheme = 'light';
    reduceMotion = false;
    schemeListeners.clear();
    reduceListeners.clear();
    jest
      .mocked(AccessibilityInfo.isReduceMotionEnabled)
      .mockImplementation(() => Promise.resolve(reduceMotion));
    jest.mocked(AccessibilityInfo.addEventListener).mockImplementation(((
      event: string,
      handler: (value: boolean) => void,
    ) => {
      if (event === 'reduceMotionChanged') reduceListeners.add(handler);
      return { remove: () => reduceListeners.delete(handler) };
    }) as unknown as typeof AccessibilityInfo.addEventListener);
  },
};

export type MemoryStorage = PreferenceStorage & { values: Map<string, string> };

export function createMemoryStorage(initial: Record<string, string> = {}): MemoryStorage {
  const values = new Map(Object.entries(initial));
  return {
    values,
    getItem: async (key) => values.get(key) ?? null,
    setItem: async (key, value) => {
      values.set(key, value);
    },
  };
}
