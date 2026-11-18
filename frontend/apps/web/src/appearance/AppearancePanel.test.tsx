import { STORAGE_KEYS } from '@schoolos/design-tokens';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { createFakeMedia, createMemoryStorage, DARK_QUERY, REDUCE_QUERY } from '../test/fakes';
import { AppearancePanel } from './AppearancePanel';
import { AppearanceProvider } from './AppearanceProvider';

beforeEach(() => {
  createFakeMedia({ [DARK_QUERY]: false, [REDUCE_QUERY]: false }).install();
});

function renderPanel(storage = createMemoryStorage()) {
  render(
    <AppearanceProvider storage={storage} configuredBrand={null}>
      <AppearancePanel />
    </AppearanceProvider>,
  );
  return storage;
}

describe('AppearancePanel (web, spec 0002 AC-10)', () => {
  it('opens from the keyboard, moves focus into the panel and names both groups', async () => {
    const user = userEvent.setup();
    renderPanel();

    await user.tab();
    expect(screen.getByRole('button', { name: 'Görünüm' })).toHaveFocus();
    await user.keyboard('{Enter}');

    const panel = await screen.findByRole('dialog', { name: 'Görünüm ayarları' });
    expect(panel).toContainElement(document.activeElement as HTMLElement);
    const scheme = within(panel).getByRole('radiogroup', { name: 'Görünüm' });
    const motion = within(panel).getByRole('radiogroup', { name: 'Hareket' });
    expect(within(scheme).getByRole('radio', { name: 'Sistem' })).toBeChecked();
    expect(within(motion).getByRole('radio', { name: 'Sistem' })).toBeChecked();
    expect(
      within(scheme)
        .getAllByRole('radio')
        .map((r) => r.textContent),
    ).toEqual(['Sistem', 'Açık', 'Koyu']);
    expect(
      within(motion)
        .getAllByRole('radio')
        .map((r) => r.textContent),
    ).toEqual(['Sistem', 'Azaltılmış', 'Normal']);
  });

  it('arrow keys move within a group, Space chooses, Tab moves to the next group', async () => {
    // jsdom does not reproduce Radix's "arrow key also checks" (real browsers do; VERIFY observes it there),
    // so this test drives the other half of the ARIA radio pattern: arrows move focus, Space selects.
    const user = userEvent.setup();
    const storage = renderPanel();
    await user.click(screen.getByRole('button', { name: 'Görünüm' }));
    const panel = await screen.findByRole('dialog');
    const scheme = within(panel).getByRole('radiogroup', { name: 'Görünüm' });
    expect(within(scheme).getByRole('radio', { name: 'Sistem' })).toHaveFocus();

    await user.keyboard('{ArrowRight}{ArrowRight}');
    expect(within(scheme).getByRole('radio', { name: 'Koyu' })).toHaveFocus();
    await user.keyboard(' ');
    expect(within(scheme).getByRole('radio', { name: 'Koyu' })).toBeChecked();
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(storage.getItem(STORAGE_KEYS.appearance)).toBe('dark');

    await user.tab();
    const motion = within(panel).getByRole('radiogroup', { name: 'Hareket' });
    expect(motion).toContainElement(document.activeElement as HTMLElement);
    await user.keyboard('{ArrowRight} ');
    expect(within(motion).getByRole('radio', { name: 'Azaltılmış' })).toBeChecked();
    expect(document.documentElement.dataset.motion).toBe('off');
  });

  it('Esc closes the panel and returns focus to the button', async () => {
    const user = userEvent.setup();
    renderPanel();
    const trigger = screen.getByRole('button', { name: 'Görünüm' });
    await user.click(trigger);
    await screen.findByRole('dialog');

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it('the close button closes the panel and returns focus', async () => {
    const user = userEvent.setup();
    renderPanel();
    const trigger = screen.getByRole('button', { name: 'Görünüm' });
    await user.click(trigger);

    await user.click(await screen.findByRole('button', { name: 'Kapat' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });
});
