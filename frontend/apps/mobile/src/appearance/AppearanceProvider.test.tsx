import { STORAGE_KEYS, tokens } from '@schoolos/design-tokens';
import { act, render, screen, userEvent } from '@testing-library/react-native';
import { Pressable, Text } from 'react-native';
import { createMemoryStorage, device } from '../test/device';
import { AppearanceProvider, useAppearance } from './AppearanceProvider';

function Probe() {
  const { scheme, reducedMotion, theme, setAppearance, setMotion, setBrandPreview } = useAppearance();
  const button = (label: string, onPress: () => void) => (
    <Pressable role="button" accessibilityLabel={label} onPress={onPress}>
      <Text>{label}</Text>
    </Pressable>
  );
  return (
    <>
      <Text testID="scheme">{scheme}</Text>
      <Text testID="motion">{reducedMotion ? 'off' : 'on'}</Text>
      <Text testID="canvas">{theme.colors.canvas}</Text>
      <Text testID="brand">{theme.colors.brand}</Text>
      {button('light', () => setAppearance('light'))}
      {button('system', () => setAppearance('system'))}
      {button('normal', () => setMotion('normal'))}
      {button('reduced', () => setMotion('reduced'))}
      {button('preview', () => setBrandPreview('#FFFF00'))}
      {button('clear', () => setBrandPreview(null))}
    </>
  );
}

const text = (id: string) => screen.getByTestId(id).props.children as string;

async function renderProvider(storage = createMemoryStorage(), configuredBrand: string | null = null) {
  const onReady = jest.fn();
  await render(
    <AppearanceProvider storage={storage} configuredBrand={configuredBrand} onReady={onReady}>
      <Probe />
    </AppearanceProvider>,
  );
  await screen.findByTestId('scheme');
  return { storage, onReady };
}

describe('AppearanceProvider (mobile)', () => {
  it('follows the system scheme until overridden', async () => {
    const user = userEvent.setup();
    await renderProvider();
    expect(text('scheme')).toBe('light');
    expect(text('canvas')).toBe(tokens.semantic.light.bg.canvas);

    await act(async () => device.setColorScheme('dark'));
    expect(text('scheme')).toBe('dark');
    expect(text('canvas')).toBe(tokens.semantic.dark.bg.canvas);

    await user.press(screen.getByRole('button', { name: 'light' }));
    await act(async () => device.setColorScheme('dark'));
    expect(text('scheme')).toBe('light');

    await user.press(screen.getByRole('button', { name: 'system' }));
    expect(text('scheme')).toBe('dark');
  });

  it('follows the device reduce-motion setting until overridden', async () => {
    const user = userEvent.setup();
    await renderProvider();
    expect(text('motion')).toBe('on');

    await act(async () => device.setReduceMotion(true));
    expect(text('motion')).toBe('off');

    await user.press(screen.getByRole('button', { name: 'normal' }));
    expect(text('motion')).toBe('on');

    await act(async () => device.setReduceMotion(false));
    await user.press(screen.getByRole('button', { name: 'reduced' }));
    expect(text('motion')).toBe('off');
  });

  it('reads the reduce-motion setting the device already has at start', async () => {
    device.setReduceMotion(true);
    await renderProvider();

    expect(await screen.findByText('off')).toBeOnTheScreen();
  });

  it('restores saved preferences', async () => {
    const storage = createMemoryStorage({
      [STORAGE_KEYS.appearance]: 'dark',
      [STORAGE_KEYS.motion]: 'reduced',
    });
    const { onReady } = await renderProvider(storage);

    expect(text('scheme')).toBe('dark');
    expect(text('motion')).toBe('off');
    expect(onReady).toHaveBeenCalledTimes(1);
  });

  it('saves each choice on this device', async () => {
    const user = userEvent.setup();
    const { storage } = await renderProvider();

    await user.press(screen.getByRole('button', { name: 'light' }));
    await user.press(screen.getByRole('button', { name: 'reduced' }));

    expect(storage.values.get(STORAGE_KEYS.appearance)).toBe('light');
    expect(storage.values.get(STORAGE_KEYS.motion)).toBe('reduced');
  });

  it('falls back to system when a saved value is unknown or unreadable', async () => {
    const broken = createMemoryStorage({ [STORAGE_KEYS.appearance]: 'sepia' });
    broken.getItem = async (key) => {
      if (key === STORAGE_KEYS.motion) throw new Error('disk');
      return broken.values.get(key) ?? null;
    };
    await renderProvider(broken);
    await act(async () => device.setColorScheme('dark'));

    expect(text('scheme')).toBe('dark');
    expect(text('motion')).toBe('on');
  });

  it('renders nothing until saved preferences are known (no wrong-theme first frame)', async () => {
    const pending: ((value: string | null) => void)[] = [];
    const slow = createMemoryStorage();
    slow.getItem = () => new Promise((resolve) => pending.push(resolve));
    const onReady = jest.fn();
    await render(
      <AppearanceProvider storage={slow} configuredBrand={null} onReady={onReady}>
        <Probe />
      </AppearanceProvider>,
    );

    expect(screen.queryByTestId('scheme')).not.toBeOnTheScreen();
    expect(onReady).not.toHaveBeenCalled();

    await act(async () => pending.forEach((resolve) => resolve(null)));
    expect(await screen.findByTestId('scheme')).toBeOnTheScreen();
    expect(onReady).toHaveBeenCalledTimes(1);
  });

  it('uses the configured brand colour, a showcase preview over it, and never saves the preview', async () => {
    const user = userEvent.setup();
    const { storage } = await renderProvider(createMemoryStorage(), '#0B1E3F');
    expect(text('brand')).toBe('#0B1E3F');

    await user.press(screen.getByRole('button', { name: 'preview' }));
    expect(text('brand')).toBe('#FFFF00');

    await user.press(screen.getByRole('button', { name: 'clear' }));
    expect(text('brand')).toBe('#0B1E3F');
    expect([...storage.values.values()]).not.toContain('#FFFF00');
  });

  it('falls back to the default brand for an invalid configured colour', async () => {
    await renderProvider(createMemoryStorage(), 'mavi');

    expect(text('brand')).toBe(tokens.semantic.light.brand.solid);
  });
});
