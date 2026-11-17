import { describe, expect, it } from 'vitest';
import {
  APPEARANCE_OPTIONS,
  MOTION_OPTIONS,
  parsePreference,
  resolveReducedMotion,
  resolveScheme,
} from './index';

describe('resolveScheme (spec 0002 AC-1)', () => {
  it.each([
    ['system', false, 'light'],
    ['system', true, 'dark'],
    ['light', true, 'light'],
    ['dark', false, 'dark'],
  ] as const)('%s with system dark=%s → %s', (preference, systemDark, expected) => {
    expect(resolveScheme(preference, systemDark)).toBe(expected);
  });
});

describe('resolveReducedMotion (spec 0002 AC-2)', () => {
  it.each([
    ['system', false, false],
    ['system', true, true],
    ['reduced', false, true],
    ['normal', true, false],
  ] as const)('%s with system reduce=%s → %s', (preference, systemReduce, expected) => {
    expect(resolveReducedMotion(preference, systemReduce)).toBe(expected);
  });
});

describe('parsePreference (spec 0002 AC-3)', () => {
  it('keeps a known stored value', () => {
    expect(parsePreference('dark', APPEARANCE_OPTIONS)).toBe('dark');
    expect(parsePreference('reduced', MOTION_OPTIONS)).toBe('reduced');
  });

  it.each([null, undefined, '', 'sepia', 'DARK'])('falls back to system for %j', (raw) => {
    expect(parsePreference(raw, APPEARANCE_OPTIONS)).toBe('system');
  });
});
