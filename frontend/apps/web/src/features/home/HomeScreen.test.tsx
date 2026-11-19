import type { CurrentUser } from '@schoolos/api-client';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { fakeApi, pathname, renderApp } from '../../test/renderApp';

/** Spec 0003, AC-2 (name, institution, every role) and the home screen states — web. */
describe('HomeScreen (web)', () => {
  const signedIn = (overrides: Parameters<typeof fakeApi>[0] = {}) =>
    fakeApi({ restoreSession: async () => true, ...overrides });

  it('shows loading while the account is fetched', async () => {
    renderApp({ api: signedIn({ getMe: () => new Promise<CurrentUser>(() => undefined) }) });

    expect(await screen.findByText('Yükleniyor')).toBeInTheDocument();
  });

  it('shows the name, the institution and every role', async () => {
    renderApp({ api: signedIn() });

    expect(await screen.findByText('Hoş geldiniz, Ayşe Yılmaz')).toBeInTheDocument();
    expect(screen.getByText('Örnek Koleji')).toBeInTheDocument();
    const roles = within(screen.getByRole('list'));
    expect(roles.getByText('Öğretmen')).toBeInTheDocument();
    expect(roles.getByText('Veli')).toBeInTheDocument();
  });

  it('links to the status screen and the design showcase', async () => {
    renderApp({ api: signedIn() });

    expect(await screen.findByRole('link', { name: 'Sistem durumu' })).toHaveAttribute('href', '/durum');
    expect(screen.getByRole('link', { name: 'Tasarım vitrini' })).toHaveAttribute('href', '/tasarim');
  });

  it('signing out ends the session and returns to the login screen', async () => {
    const user = userEvent.setup();
    const logout = vi.fn(async () => undefined);
    renderApp({ api: signedIn({ logout }) });

    await user.click(await screen.findByRole('button', { name: 'Çıkış yap' }));

    expect(await screen.findByRole('heading', { name: 'Giriş yap' })).toBeInTheDocument();
    expect(logout).toHaveBeenCalledTimes(1);
    expect(pathname()).toBe('/giris');
  });
});
