import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';
import '../i18n';

// docs/testing.md, Determinism: the app's motion switch is off under test.
document.documentElement.dataset.motion = 'off';

afterEach(() => {
  cleanup();
});
