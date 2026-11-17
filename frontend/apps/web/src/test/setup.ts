import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, beforeEach } from 'vitest';
import '../i18n';
import { createFakeMedia, REDUCE_QUERY } from './fakes';

// Radix Popover measures with ResizeObserver, which jsdom lacks.
class NoopResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver ??= NoopResizeObserver as unknown as typeof ResizeObserver;

beforeEach(() => {
  // docs/testing.md, Determinism: the device asks for reduced motion, so the app's motion switch is off;
  // tests that exercise appearance install their own fake.
  createFakeMedia({ [REDUCE_QUERY]: true }).install();
  document.documentElement.dataset.motion = 'off';
});

afterEach(() => {
  cleanup();
});
