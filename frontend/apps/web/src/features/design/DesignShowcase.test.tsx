import { contrastRatio, deriveBrand, tokens } from '@schoolos/design-tokens';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { AppearanceProvider } from '../../appearance/AppearanceProvider';
import {
  createFakeMedia,
  createMemoryStorage,
  DARK_QUERY,
  REDUCE_QUERY,
  type FakeMedia,
} from '../../test/fakes';
import { DesignShowcase } from './DesignShowcase';

let media: FakeMedia;

beforeEach(() => {
  media = createFakeMedia({ [DARK_QUERY]: false, [REDUCE_QUERY]: false });
  media.install();
});

function renderShowcase() {
  render(
    <AppearanceProvider storage={createMemoryStorage()} configuredBrand={null}>
      <DesignShowcase />
    </AppearanceProvider>,
  );
}

const ratio = (fg: string, bg: string) => `Kontrast ${contrastRatio(fg, bg).toFixed(2)}:1`;

describe('DesignShowcase (web, spec 0002 AC-9)', () => {
  it('shows every token family', () => {
    renderShowcase();

    expect(screen.getByRole('heading', { level: 1, name: 'Tasarım vitrini' })).toBeInTheDocument();
    for (const name of ['Kurum rengi', 'Renkler', 'Yazı', 'Boşluk, köşe ve gölge', 'Hareket']) {
      expect(screen.getByRole('region', { name })).toBeInTheDocument();
    }
    const typography = screen.getByRole('region', { name: 'Yazı' });
    expect(within(typography).getAllByText('Veliyle iletişim tek yerde')).toHaveLength(
      Object.keys(tokens.typography.scale).length,
    );
    const scales = screen.getByRole('region', { name: 'Boşluk, köşe ve gölge' });
    for (const [name, value] of Object.entries(tokens.spacing)) {
      expect(within(scales).getByText(`${name} · ${value}`)).toBeInTheDocument();
    }
    const motion = screen.getByRole('region', { name: 'Hareket' });
    for (const [name, ms] of Object.entries(tokens.motion.durationMs)) {
      expect(within(motion).getByText(`${name} · ${ms} ms`)).toBeInTheDocument();
    }
  });

  it('colour roles show their contrast for the current appearance', async () => {
    const user = userEvent.setup();
    renderShowcase();
    const colors = screen.getByRole('region', { name: 'Renkler' });
    const light = tokens.semantic.light;
    const row = (role: string) => within(colors).getByText(role).closest('li') as HTMLElement;
    expect(row('text.secondary')).toHaveTextContent(ratio(light.text.secondary, light.bg.surface));

    await user.click(screen.getByRole('button', { name: 'Görünüm' }));
    await user.click(await screen.findByRole('radio', { name: 'Koyu' }));

    const dark = tokens.semantic.dark;
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(row('text.secondary')).toHaveTextContent(ratio(dark.text.secondary, dark.bg.surface));
  });

  it('a brand colour preview updates the brand rows', async () => {
    const user = userEvent.setup();
    renderShowcase();
    await user.type(screen.getByLabelText('Kurum rengi (#RRGGBB)'), '#FEF08A');

    const colors = screen.getByRole('region', { name: 'Renkler' });
    const brand = deriveBrand('#FEF08A', 'light');
    const solidRow = within(colors).getByText('brand.solid').closest('li') as HTMLElement;
    expect(solidRow).toHaveTextContent(ratio(brand.onSolid, brand.solid));
    expect(contrastRatio(brand.onSolid, brand.solid)).toBeGreaterThanOrEqual(4.5);
  });

  it('the motion example says whether motion is reduced', () => {
    media.set(REDUCE_QUERY, true);
    renderShowcase();
    expect(screen.getByText('Hareket azaltılmış: geçişler anında gösterilir.')).toBeInTheDocument();
    expect(document.documentElement.dataset.motion).toBe('off');
  });

  it('"Oynat" moves the example dot', async () => {
    const user = userEvent.setup();
    renderShowcase();
    const dot = screen.getByTestId('motion-dot');
    expect(dot).toHaveAttribute('data-moved', 'false');
    await user.click(screen.getByRole('button', { name: 'Oynat' }));
    expect(dot).toHaveAttribute('data-moved', 'true');
    // Travel comes from its own token (review 1, I-1), which the theme publishes as a variable.
    expect(dot.className).toContain('translate-x-[var(--sos-layout-motion-demo-travel)]');
    expect(document.documentElement.style.getPropertyValue('--sos-layout-motion-demo-travel')).toBe(
      `${tokens.layout.motionDemoTravel}px`,
    );
  });
});
