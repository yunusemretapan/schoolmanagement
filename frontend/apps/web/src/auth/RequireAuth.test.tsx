import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { fakeApi, goBack, pathname, renderApp } from '../test/renderApp';

/** Spec 0003, AC-11 (guarded addresses, return after login) and AC-9 (Back shows no guarded screen) — web. */
describe('RequireAuth (web)', () => {
  it('a signed-out visitor of /tasarim lands on the login screen and returns there after signing in', async () => {
    const user = userEvent.setup();
    renderApp({ path: '/tasarim' });

    expect(await screen.findByRole('heading', { name: 'Giriş yap' })).toBeInTheDocument();
    expect(pathname()).toBe('/giris');
    expect(screen.queryByRole('heading', { name: 'Tasarım vitrini' })).not.toBeInTheDocument();

    await user.type(screen.getByLabelText('E-posta veya telefon'), 'veli@ornek.test');
    await user.type(screen.getByLabelText('Şifre'), 'Ornek-Sifre-2026');
    await user.click(screen.getByRole('button', { name: 'Giriş yap' }));

    expect(await screen.findByRole('heading', { name: 'Tasarım vitrini' })).toBeInTheDocument();
    expect(pathname()).toBe('/tasarim');
  });

  it('an existing session opens the guarded screen directly', async () => {
    renderApp({ api: fakeApi({ restoreSession: async () => true }), path: '/tasarim' });

    expect(await screen.findByRole('heading', { name: 'Tasarım vitrini' })).toBeInTheDocument();
  });

  it('while the session is being checked, nothing guarded is shown', async () => {
    renderApp({ api: fakeApi({ restoreSession: () => new Promise<boolean>(() => undefined) }), path: '/' });

    expect(await screen.findByText('Oturum kontrol ediliyor…')).toBeInTheDocument();
    expect(screen.queryByText(/Hoş geldiniz/)).not.toBeInTheDocument();
  });

  it('after signing out, Back does not show the guarded screen', async () => {
    const user = userEvent.setup();
    renderApp({ api: fakeApi({ restoreSession: async () => true }), history: ['/giris', '/tasarim', '/'] });

    await user.click(await screen.findByRole('button', { name: 'Çıkış yap' }));
    await screen.findByRole('heading', { name: 'Giriş yap' });

    await act(goBack);

    expect(pathname()).toBe('/giris');
    expect(screen.getByRole('heading', { name: 'Giriş yap' })).toBeInTheDocument();
    expect(screen.queryByText(/Hoş geldiniz/)).not.toBeInTheDocument();
  });

  it('when the session ends on its own, the login screen says so', async () => {
    const { expiry } = renderApp({ api: fakeApi({ restoreSession: async () => true }), path: '/' });
    await screen.findByText('Hoş geldiniz, Ayşe Yılmaz');

    act(() => expiry.notify());

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Oturumunuz sona erdi, lütfen tekrar giriş yapın',
    );
    expect(pathname()).toBe('/giris');
  });

  it('a signed-out user never fetches the account', async () => {
    const getMe = vi.fn();
    renderApp({ api: fakeApi({ getMe }), path: '/' });

    await screen.findByRole('heading', { name: 'Giriş yap' });
    expect(getMe).not.toHaveBeenCalled();
  });
});
