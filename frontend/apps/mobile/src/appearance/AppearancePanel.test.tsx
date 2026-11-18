import { STORAGE_KEYS, tokens } from '@schoolos/design-tokens';
import { act, fireEvent, render, screen, userEvent, within } from '@testing-library/react-native';
import { AccessibilityInfo, Text } from 'react-native';
import { createMemoryStorage } from '../test/device';
import { AppearancePanel } from './AppearancePanel';
import { AppearanceProvider, useAppearance } from './AppearanceProvider';

function Canvas() {
  const { theme } = useAppearance();
  return <Text testID="canvas">{theme.colors.canvas}</Text>;
}

async function renderPanel(storage = createMemoryStorage()) {
  await render(
    <AppearanceProvider storage={storage} configuredBrand={null}>
      <AppearancePanel />
      <Canvas />
    </AppearanceProvider>,
  );
  await screen.findByRole('button', { name: 'Görünüm' });
  return storage;
}

async function openPanel() {
  const user = userEvent.setup();
  await user.press(screen.getByRole('button', { name: 'Görünüm' }));
  const panel = await screen.findByTestId('appearance-panel');
  return { user, panel };
}

/**
 * A radio group container is not itself an accessibility element (that would hide its radios from
 * VoiceOver), so tests find it by testID and check the role and name it carries for screen readers.
 */
function group(panel: Parameters<typeof within>[0], id: string, name: string) {
  const element = within(panel).getByTestId(id);
  expect(element).toHaveProp('role', 'radiogroup');
  expect(element).toHaveProp('accessibilityLabel', name);
  return within(element);
}

const labels = (group: ReturnType<typeof within>) =>
  group.getAllByRole('radio').map((radio) => radio.props.accessibilityLabel as string);

describe('AppearancePanel (mobile, spec 0002 AC-10)', () => {
  it('opens a modal panel with two named radio groups, "Sistem" checked in each', async () => {
    await renderPanel();
    expect(screen.queryByTestId('appearance-panel')).not.toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Görünüm' })).toBeCollapsed();

    const { panel } = await openPanel();

    expect(panel).toHaveProp('accessibilityViewIsModal', true);
    expect(within(panel).getByRole('heading', { name: 'Görünüm ayarları' })).toBeOnTheScreen();
    const scheme = group(panel, 'appearance-scheme', 'Görünüm');
    const motion = group(panel, 'appearance-motion', 'Hareket');
    expect(labels(scheme)).toEqual(['Sistem', 'Açık', 'Koyu']);
    expect(labels(motion)).toEqual(['Sistem', 'Azaltılmış', 'Normal']);
    expect(scheme.getByRole('radio', { name: 'Sistem' })).toBeChecked();
    expect(scheme.getByRole('radio', { name: 'Koyu' })).not.toBeChecked();
    expect(motion.getByRole('radio', { name: 'Sistem' })).toBeChecked();
  });

  it('applies a choice at once, announces it as checked and saves it', async () => {
    const storage = await renderPanel();
    const { user, panel } = await openPanel();
    const scheme = group(panel, 'appearance-scheme', 'Görünüm');
    const motion = group(panel, 'appearance-motion', 'Hareket');

    await user.press(scheme.getByRole('radio', { name: 'Koyu' }));
    await user.press(motion.getByRole('radio', { name: 'Azaltılmış' }));

    expect(scheme.getByRole('radio', { name: 'Koyu' })).toBeChecked();
    expect(scheme.getByRole('radio', { name: 'Sistem' })).not.toBeChecked();
    expect(motion.getByRole('radio', { name: 'Azaltılmış' })).toBeChecked();
    expect(screen.getByTestId('canvas')).toHaveTextContent(tokens.semantic.dark.bg.canvas);
    expect(storage.values.get(STORAGE_KEYS.appearance)).toBe('dark');
    expect(storage.values.get(STORAGE_KEYS.motion)).toBe('reduced');
  });

  it('closes with the back gesture (onRequestClose) and returns reader focus to the button', async () => {
    await renderPanel();
    const focus = jest.spyOn(AccessibilityInfo, 'sendAccessibilityEvent');
    focus.mockClear();
    try {
      await openPanel();
      await act(async () => fireEvent(screen.getByTestId('appearance-modal'), 'requestClose'));

      expect(screen.queryByTestId('appearance-panel')).not.toBeOnTheScreen();
      expect(focus).toHaveBeenCalledWith(expect.anything(), 'focus');
      expect(screen.getByRole('button', { name: 'Görünüm' })).toBeCollapsed();
    } finally {
      focus.mockRestore();
    }
  });

  it('closes with the close button and with a tap outside the panel', async () => {
    await renderPanel();
    const { user } = await openPanel();
    await user.press(screen.getByRole('button', { name: 'Kapat' }));
    expect(screen.queryByTestId('appearance-panel')).not.toBeOnTheScreen();

    await openPanel();
    // The backdrop is outside the modal view, so screen readers skip it: a tap target for sighted users only.
    const backdrop = screen.getByTestId('appearance-backdrop', { includeHiddenElements: true });
    await act(async () => fireEvent.press(backdrop));
    expect(screen.queryByTestId('appearance-panel')).not.toBeOnTheScreen();
  });
});
