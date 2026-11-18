import { STORAGE_KEYS, contrastRatio, deriveBrand, tokens } from '@schoolos/design-tokens';
import { act, render, screen, userEvent, within } from '@testing-library/react-native';
import { getAnimatedStyle } from 'react-native-reanimated';
import { AppearanceProvider } from '../../appearance/AppearanceProvider';
import { createMemoryStorage } from '../../test/device';
import { DesignShowcase } from './DesignShowcase';

const SECTIONS = ['Kurum rengi', 'Renkler', 'Yazı', 'Boşluk, köşe ve gölge', 'Hareket'];

async function renderShowcase(saved: Record<string, string> = {}, onBack = jest.fn()) {
  await render(
    <AppearanceProvider storage={createMemoryStorage(saved)} configuredBrand={null}>
      <DesignShowcase onBack={onBack} />
    </AppearanceProvider>,
  );
  await screen.findByRole('heading', { name: 'Tasarım vitrini' });
  return onBack;
}

const section = (name: string) => within(screen.getByTestId(`section-${name}`));
const ratio = (fg: string, bg: string) => `Kontrast ${contrastRatio(fg, bg).toFixed(2)}:1`;
const colorRow = (role: string) => screen.getByTestId(`color-${role}`);
/** The dot's live offset, read from Reanimated's animated style (not the render-time props). */
// Typed from the web stub (no parameters); under Jest on native platforms it takes the element.
const readAnimatedStyle = getAnimatedStyle as unknown as (element: unknown) => {
  transform?: { translateX: number }[];
};
const translateX = () => readAnimatedStyle(screen.getByTestId('motion-dot')).transform?.[0]?.translateX;
const ONE_FRAME_MS = 16;

describe('DesignShowcase (mobile, spec 0002 AC-9)', () => {
  it('shows every token family', async () => {
    await renderShowcase();

    for (const name of SECTIONS) {
      expect(screen.getByRole('heading', { name })).toBeOnTheScreen();
    }
    expect(section('Yazı').getAllByText('Veliyle iletişim tek yerde')).toHaveLength(
      Object.keys(tokens.typography.scale).length,
    );
    for (const [name, value] of Object.entries(tokens.spacing)) {
      expect(section('Boşluk, köşe ve gölge').getByText(`${name} · ${value}`)).toBeOnTheScreen();
    }
    for (const name of Object.keys(tokens.radius)) {
      expect(section('Boşluk, köşe ve gölge').getByText(name)).toBeOnTheScreen();
    }
    expect(section('Boşluk, köşe ve gölge').getByText('Kart gölgesi')).toBeOnTheScreen();
    for (const [name, ms] of Object.entries(tokens.motion.durationMs)) {
      expect(section('Hareket').getByText(`${name} · ${ms} ms`)).toBeOnTheScreen();
    }
  });

  it('colour roles show their contrast for the current appearance', async () => {
    const user = userEvent.setup();
    await renderShowcase();
    const light = tokens.semantic.light;
    expect(colorRow('text.secondary')).toHaveTextContent(ratio(light.text.secondary, light.bg.surface), {
      exact: false,
    });

    await user.press(screen.getByRole('button', { name: 'Görünüm' }));
    await user.press(await screen.findByRole('radio', { name: 'Koyu' }));

    const dark = tokens.semantic.dark;
    expect(colorRow('text.secondary')).toHaveTextContent(ratio(dark.text.secondary, dark.bg.surface), {
      exact: false,
    });
  });

  it('a brand colour preview updates the brand rows', async () => {
    const user = userEvent.setup();
    await renderShowcase();

    await user.type(screen.getByLabelText('Kurum rengi (#RRGGBB)'), '#FEF08A');

    const brand = deriveBrand('#FEF08A', 'light');
    expect(colorRow('brand.solid')).toHaveTextContent(ratio(brand.onSolid, brand.solid), { exact: false });
    expect(contrastRatio(brand.onSolid, brand.solid)).toBeGreaterThanOrEqual(4.5);
  });

  it('the motion example says whether motion is reduced', async () => {
    await renderShowcase({ [STORAGE_KEYS.motion]: 'reduced' });

    expect(screen.getByText('Hareket azaltılmış: geçişler anında gösterilir.')).toBeOnTheScreen();
  });

  it('"Oynat" moves the example dot over the slow duration', async () => {
    jest.useFakeTimers();
    try {
      const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
      await renderShowcase({ [STORAGE_KEYS.motion]: 'normal' });
      expect(translateX()).toBe(0);

      await user.press(screen.getByRole('button', { name: 'Oynat' }));
      await act(async () => jest.advanceTimersByTime(ONE_FRAME_MS));
      // Still on its way: the move takes motion.durationMs.slow, not an instant jump.
      expect(translateX()).toBeLessThan(tokens.layout.motionDemoTravel);

      await act(async () => jest.advanceTimersByTime(tokens.motion.durationMs.slow * 2));
      expect(translateX()).toBe(tokens.layout.motionDemoTravel);
    } finally {
      jest.useRealTimers();
    }
  });

  it('with reduced motion "Oynat" moves the dot at once', async () => {
    jest.useFakeTimers();
    try {
      const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
      await renderShowcase({ [STORAGE_KEYS.motion]: 'reduced' });

      await user.press(screen.getByRole('button', { name: 'Oynat' }));
      await act(async () => jest.advanceTimersByTime(ONE_FRAME_MS));

      expect(translateX()).toBe(tokens.layout.motionDemoTravel);
    } finally {
      jest.useRealTimers();
    }
  });

  it('"Geri" returns to the status screen', async () => {
    const user = userEvent.setup();
    const onBack = await renderShowcase();

    await user.press(screen.getByRole('button', { name: 'Geri' }));

    expect(onBack).toHaveBeenCalledTimes(1);
  });
});
