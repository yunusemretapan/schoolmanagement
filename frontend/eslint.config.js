// @ts-check
import js from '@eslint/js';
import i18next from 'eslint-plugin-i18next';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';
import tseslint from 'typescript-eslint';

const appSources = ['apps/*/src/**/*.{ts,tsx}', 'apps/*/App.tsx'];
const tests = ['**/*.test.{ts,tsx}', '**/test/**'];

export default tseslint.config(
  {
    ignores: [
      '**/node_modules/**',
      '**/dist/**',
      '**/.expo/**',
      '**/web-build/**',
      'packages/api-client/src/schema.ts',
    ],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    languageOptions: { globals: { ...globals.browser, ...globals.node } },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      ...reactHooks.configs.recommended.rules,
      '@typescript-eslint/no-explicit-any': 'error',
    },
  },
  {
    // docs/architecture.md, Forbidden dependencies 6: apps reach the API only through @schoolos/api-client.
    files: appSources,
    rules: {
      'no-restricted-globals': [
        'error',
        { name: 'fetch', message: 'Use @schoolos/api-client (docs/architecture.md, rule 6).' },
      ],
      'no-restricted-imports': [
        'error',
        {
          paths: [
            { name: 'openapi-fetch', message: 'Use @schoolos/api-client (docs/architecture.md, rule 6).' },
          ],
          patterns: [
            { group: ['axios', 'ky'], message: 'Use @schoolos/api-client (docs/architecture.md, rule 6).' },
          ],
        },
      ],
    },
  },
  {
    // Spec 0001, AC-8: user-visible text comes from the tr-TR translation file, never a literal in a screen.
    files: appSources,
    ignores: tests,
    plugins: { i18next },
    rules: {
      'i18next/no-literal-string': [
        'error',
        {
          mode: 'jsx-only',
          // Only attributes a user can see or hear; technical props (testID, role, className…) are not text.
          'jsx-attributes': {
            include: ['aria-label', 'accessibilityLabel', 'accessibilityHint', 'title', 'alt', 'placeholder'],
          },
        },
      ],
    },
  },
);
