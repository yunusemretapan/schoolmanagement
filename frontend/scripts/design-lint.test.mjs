// Unit tests for design-lint (spec 0002 AC-7). Run: node --test scripts/design-lint.test.mjs
import assert from 'node:assert/strict';
import { join } from 'node:path';
import { test } from 'node:test';
import { REQUIRED_LOCKS, isScanned, lintSource, missingLocks } from './design-lint.mjs';

const web = join('apps', 'web', 'src', 'X.tsx');
const mobile = join('apps', 'mobile', 'src', 'X.tsx');
const css = join('apps', 'web', 'src', 'index.css');
const rules = (text, file) => lintSource(text, file).map((v) => v.rule);

test('flags colours written by value', () => {
  assert.deepEqual(rules(`const c = '#1E40AF';`, web), ['color-literal']);
  assert.deepEqual(rules(`style={{ color: 'rgba(0, 0, 0, 0.5)' }}`, mobile), ['color-literal']);
  assert.deepEqual(rules(`a { color: hsl(10 20% 30%); }`, css), ['color-literal']);
});

test('ignores colours in comments', () => {
  assert.deepEqual(rules(`// e.g. #1E40AF\n/* rgb(1,2,3) */ const x = 1;`, web), []);
});

test('flags Tailwind arbitrary values unless they read a token variable', () => {
  assert.deepEqual(rules(`<div className="h-[36rem] scale-[0.98]" />`, web), [
    'tailwind-arbitrary',
    'tailwind-arbitrary',
  ]);
  assert.deepEqual(
    rules(
      `<div className="opacity-[var(--sos-opacity-pulse)] border-[length:var(--sos-border-thick)]" />`,
      web,
    ),
    [],
  );
});

test('allows variant selectors such as data-[state=checked]:', () => {
  assert.deepEqual(
    rules(`<div className="data-[state=checked]:bg-surface aria-[selected=true]:text-ink" />`, web),
    [],
  );
});

test('flags Tailwind default-scale numbers, keeps token names and fractions', () => {
  assert.deepEqual(rules(`<div className="p-4 size-10 -top-40 hover:opacity-60 text-2xl" />`, web), [
    'tailwind-numeric',
    'tailwind-numeric',
    'tailwind-numeric',
    'tailwind-numeric',
    'tailwind-numeric',
  ]);
  assert.deepEqual(rules(`<div className="p-md size-logo left-1/2 -translate-x-1/2 max-w-card" />`, web), []);
});

test('allows structural flex switches grow-0 / shrink-0', () => {
  assert.deepEqual(rules(`<div className="shrink-0 grow-0 grow" />`, web), []);
});

test('flags Tailwind default breakpoints but not object keys', () => {
  assert.deepEqual(rules(`<ul className="grid sm:grid-cols-2 lg:gap-md" />`, web), [
    'tailwind-breakpoint',
    'tailwind-breakpoint',
  ]);
  assert.deepEqual(rules(`const TEXT = { sm: 'text-sm', md: 'text-md' };`, web), []);
});

test('flags React Native style numbers except 0 and 1', () => {
  assert.deepEqual(rules(`const s = StyleSheet.create({ a: { width: 40, flex: 1, opacity: 0 } });`, mobile), [
    'rn-style-number',
  ]);
  assert.deepEqual(rules(`transform: [{ scale: 0.98 }]`, mobile), ['rn-style-number']);
  assert.deepEqual(rules(`{ padding: theme.spacing.md, maxWidth: theme.layout.cardMaxWidth }`, mobile), []);
});

test('flags web inline style numbers and units written by value (review 1, L-4)', () => {
  assert.deepEqual(rules(`<div style={{ width: 40, padding: 12, flex: 1 }} />`, web), [
    'web-style-number',
    'web-style-number',
  ]);
  assert.deepEqual(rules(`<div style={{ transitionDuration: "300ms", width: '2rem' }} />`, web), [
    'web-style-unit',
    'web-style-unit',
  ]);
  assert.deepEqual(
    rules(
      '<span style={{ width: `var(--sos-spacing-${name})`, background: `var(--sos-bg-canvas)` }} />',
      web,
    ),
    [],
  );
  // Outside a style object the same text is not a style (e.g. an option object or a timeout).
  assert.deepEqual(rules(`const delay = { duration: 300 }; setTimeout(run, 300);`, web), []);
});

test('flags Tailwind default palette classes (VERIFY 1, F-1)', () => {
  assert.deepEqual(
    rules(`<p className="bg-red-500 text-gray-700 border-blue-600 hover:bg-white/50" />`, web),
    ['tailwind-palette', 'tailwind-palette', 'tailwind-palette', 'tailwind-palette'],
  );
  assert.deepEqual(
    rules(
      `<p className="bg-surface text-ink border-line bg-brand text-on-brand ring-focus text-danger" />`,
      web,
    ),
    [],
  );
});

test('the web stylesheet keeps the default scales switched off (VERIFY 1, F-1)', () => {
  const locked = REQUIRED_LOCKS.map((lock) => `  ${lock};`).join('\n');
  assert.deepEqual(missingLocks(`@theme inline {\n${locked}\n}`), []);
  const withoutPalette = locked.replace('--color-*: initial;', '');
  assert.deepEqual(missingLocks(`@theme inline {\n${withoutPalette}\n}`), ['--color-*: initial']);
  // A lock that only survives inside a comment does not count.
  assert.deepEqual(missingLocks(`/* --color-*: initial; */\n${withoutPalette}`), ['--color-*: initial']);
});

test('flags easing written by value (VERIFY 1, F-2)', () => {
  assert.deepEqual(rules(`a { animation: spin var(--sos-duration-loop) linear infinite; }`, css), [
    'css-easing-literal',
  ]);
  assert.deepEqual(rules(`a { transition-timing-function: cubic-bezier(0.2, 0, 0, 1); }`, css), [
    'css-easing-literal',
  ]);
  assert.deepEqual(
    rules(
      `a { animation: spin var(--sos-duration-loop) var(--sos-ease-linear) infinite; --ease-standard: x; }`,
      css,
    ),
    [],
  );
  assert.deepEqual(rules(`withTiming(1, { easing: Easing.linear })`, mobile), ['rn-easing-literal']);
  assert.deepEqual(rules(`withTiming(1, { easing: Easing.bezier(0.2, 0, 0, 1) })`, mobile), [
    'rn-easing-literal',
  ]);
  assert.deepEqual(
    rules(`withTiming(1, { easing: Easing.bezier(...theme.motion.easing.standard) })`, mobile),
    [],
  );
  assert.deepEqual(rules(`withTiming(1, { easing: Easing.bezier(...motion.easing.linear) })`, mobile), []);
});

test('flags CSS lengths and durations written by value', () => {
  assert.deepEqual(rules(`a { transition: opacity 150ms; margin: 4px; }`, css), [
    'css-unit-literal',
    'css-unit-literal',
  ]);
  assert.deepEqual(rules(`a { margin: var(--sos-spacing-md); width: calc(var(--x) * 100%); }`, css), []);
});

test('scans app sources only', () => {
  assert.equal(isScanned(join('apps', 'web', 'src', 'a.tsx')), true);
  assert.equal(isScanned(join('apps', 'web', 'src', 'a.test.tsx')), false);
  assert.equal(isScanned(join('apps', 'web', 'src', 'test', 'setup.ts')), false);
  assert.equal(isScanned(join('apps', 'web', 'src', 'i18n', 'tr-TR.json')), false);
  assert.equal(isScanned(join('packages', 'design-tokens', 'src', 'tokens.ts')), false);
});
