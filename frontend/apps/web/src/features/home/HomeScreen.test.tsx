import { ApiError, type CurrentUser } from '@schoolos/api-client';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { fakeApi, ME, pathname, renderApp } from '../../test/renderApp';

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

  it('a failed load shows the error, and "Tekrar dene" loads again (A-3; review 1, L-1)', async () => {
    const user = userEvent.setup();
    let fail = true;
    const getMe = vi.fn(async () => {
      if (fail) throw new ApiError(undefined, undefined);
      return ME;
    });
    renderApp({ api: signedIn({ getMe }) });

    expect(await screen.findByRole('alert')).toHaveTextContent('Bilgileriniz yüklenemedi');
    fail = false;
    await user.click(screen.getByRole('button', { name: 'Tekrar dene' }));

    expect(await screen.findByText('Hoş geldiniz, Ayşe Yılmaz')).toBeInTheDocument();
    expect(getMe).toHaveBeenCalledTimes(2);
  });

  it('an administrator sees the staff and academic structure links; others do not (spec 0004, 0005 AC-1)', async () => {
    renderApp({ api: signedIn({ getMe: async () => ({ ...ME, roles: ['ADMIN'] }) }) });

    expect(await screen.findByRole('link', { name: 'Akademik yapı' })).toHaveAttribute(
      'href',
      '/akademik-yapi',
    );
    expect(screen.getByRole('link', { name: 'Personel' })).toHaveAttribute('href', '/personel');
  });

  it('a teacher sees neither administrator link', async () => {
    renderApp({ api: signedIn() });

    expect(await screen.findByRole('link', { name: 'Sistem durumu' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Akademik yapı' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Personel' })).toBeNull();
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
