import { STORAGE_KEYS, tokens } from '@schoolos/design-tokens';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import {
  createFakeMedia,
  createMemoryStorage,
  DARK_QUERY,
  REDUCE_QUERY,
  type FakeMedia,
} from '../test/fakes';
import { AppearanceProvider, useAppearance } from './AppearanceProvider';

function Probe() {
  const { setAppearance, setMotion, setBrandPreview } = useAppearance();
  return (
    <>
      <button onClick={() => setAppearance('light')}>light</button>
      <button onClick={() => setAppearance('system')}>system</button>
      <button onClick={() => setMotion('normal')}>normal</button>
      <button onClick={() => setMotion('reduced')}>reduced</button>
      <button onClick={() => setBrandPreview('#FFFF00')}>preview</button>
      <button onClick={() => setBrandPreview(null)}>clear</button>
    </>
  );
}

const root = () => document.documentElement;
const cssVar = (name: string) => root().style.getPropertyValue(name);

let media: FakeMedia;

beforeEach(() => {
  media = createFakeMedia({ [DARK_QUERY]: false, [REDUCE_QUERY]: false });
  media.install();
});

function renderProvider(storage = createMemoryStorage(), configuredBrand: string | null = null) {
  render(
    <AppearanceProvider storage={storage} configuredBrand={configuredBrand}>
      <Probe />
    </AppearanceProvider>,
  );
  return storage;
}

describe('AppearanceProvider (web)', () => {
  it('follows the system scheme until overridden', async () => {
    const user = userEvent.setup();
    renderProvider();
    expect(root().dataset.theme).toBe('light');
    expect(cssVar('--sos-bg-canvas')).toBe(tokens.semantic.light.bg.canvas);

    act(() => media.set(DARK_QUERY, true));
    expect(root().dataset.theme).toBe('dark');
    expect(cssVar('--sos-bg-canvas')).toBe(tokens.semantic.dark.bg.canvas);

    await user.click(screen.getByText('light'));
    act(() => media.set(DARK_QUERY, true));
    expect(root().dataset.theme).toBe('light');

    await user.click(screen.getByText('system'));
    expect(root().dataset.theme).toBe('dark');
  });

  it('reduced motion sets data-motion off', async () => {
    const user = userEvent.setup();
    renderProvider();
    expect(root().dataset.motion).toBe('on');

    act(() => media.set(REDUCE_QUERY, true));
    expect(root().dataset.motion).toBe('off');

    await user.click(screen.getByText('normal'));
    expect(root().dataset.motion).toBe('on');

    act(() => media.set(REDUCE_QUERY, false));
    await user.click(screen.getByText('reduced'));
    expect(root().dataset.motion).toBe('off');
  });

  it('restores saved preferences', async () => {
    const user = userEvent.setup();
    const storage = createMemoryStorage({
      [STORAGE_KEYS.appearance]: 'dark',
      [STORAGE_KEYS.motion]: 'reduced',
    });
    renderProvider(storage);
    expect(root().dataset.theme).toBe('dark');
    expect(root().dataset.motion).toBe('off');

    await user.click(screen.getByText('light'));
    await user.click(screen.getByText('normal'));
    expect(storage.getItem(STORAGE_KEYS.appearance)).toBe('light');
    expect(storage.getItem(STORAGE_KEYS.motion)).toBe('normal');
  });

  it('ignores a corrupted stored value', () => {
    renderProvider(createMemoryStorage({ [STORAGE_KEYS.appearance]: 'sepia' }));
    expect(root().dataset.theme).toBe('light');
  });

  it('applies the configured brand colour and a non-persisted preview', async () => {
    const user = userEvent.setup();
    const storage = renderProvider(createMemoryStorage(), '#1E3A8A');
    expect(cssVar('--sos-brand-solid')).toBe('#1E3A8A');

    await user.click(screen.getByText('preview'));
    expect(cssVar('--sos-brand-solid')).toBe('#FFFF00');
    expect(storage.length).toBe(0);

    await user.click(screen.getByText('clear'));
    expect(cssVar('--sos-brand-solid')).toBe('#1E3A8A');
  });

  it('an invalid configured brand colour keeps the default brand', () => {
    renderProvider(createMemoryStorage(), 'not-a-colour');
    expect(cssVar('--sos-brand-solid')).toBe(tokens.semantic.light.brand.solid);
  });
});
