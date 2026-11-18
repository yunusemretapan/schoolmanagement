import type { ColorScheme } from './tokens';

/** Spec 0002: what the user picks. "system" follows the device setting. */
export type AppearancePreference = 'system' | 'light' | 'dark';
export type MotionPreference = 'system' | 'reduced' | 'normal';

export const APPEARANCE_OPTIONS: readonly AppearancePreference[] = ['system', 'light', 'dark'];
export const MOTION_OPTIONS: readonly MotionPreference[] = ['system', 'reduced', 'normal'];

export const STORAGE_KEYS = {
  appearance: 'sos.appearance',
  motion: 'sos.motion',
} as const;

/** AC-1: an explicit choice wins; "system" follows the device. */
export function resolveScheme(preference: AppearancePreference, systemPrefersDark: boolean): ColorScheme {
  if (preference === 'system') return systemPrefersDark ? 'dark' : 'light';
  return preference;
}

/** AC-2: true means no animation or transition may play. */
export function resolveReducedMotion(preference: MotionPreference, systemPrefersReduced: boolean): boolean {
  if (preference === 'system') return systemPrefersReduced;
  return preference === 'reduced';
}

/** AC-3: a stored value that is missing or unknown (older version, tampering) falls back to "system". */
export function parsePreference<T extends string>(
  raw: string | null | undefined,
  options: readonly T[],
): T | 'system' {
  return options.includes(raw as T) ? (raw as T) : 'system';
}
