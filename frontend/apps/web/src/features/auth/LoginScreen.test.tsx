import { ApiError } from '@schoolos/api-client';
import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { fakeApi, pathname, renderApp } from '../../test/renderApp';

/** Spec 0003, AC-3, AC-10 (every login screen state) and the status link of AC-11 — web. */
describe('LoginScreen (web)', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  async function openLogin(overrides: Parameters<typeof fakeApi>[0] = {}) {
    const login = vi.fn(async () => undefined);
    const api = fakeApi({ login, ...overrides });
    renderApp({ api, path: '/giris' });
    await screen.findByRole('heading', { name: 'Giriş yap' });
    return { api, login: (overrides.login as typeof login | undefined) ?? login };
  }

  const identifierField = () => screen.getByLabelText('E-posta veya telefon');
  const passwordField = () => screen.getByLabelText('Şifre');
  const submitButton = () => screen.getByRole('button', { name: /Giriş yap/ });

  it('starts empty with the sign-in button enabled', async () => {
    await openLogin();

    expect(identifierField()).toHaveValue('');
    expect(passwordField()).toHaveValue('');
    expect(passwordField()).toHaveAttribute('type', 'password');
    expect(submitButton()).toBeEnabled();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows field errors and sends nothing when fields are empty', async () => {
    const user = userEvent.setup();
    const { login } = await openLogin();

    await user.click(submitButton());

    expect(screen.getAllByText('Bu alan zorunlu')).toHaveLength(2);
    expect(identifierField()).toHaveAttribute('aria-invalid', 'true');
    expect(identifierField()).toHaveAccessibleDescription('Bu alan zorunlu');
    expect(login).not.toHaveBeenCalled();
  });

  it.each(['532123456', '0532abc4567'])('rejects %s before sending', async (bad) => {
    const user = userEvent.setup();
    const { login } = await openLogin();

    await user.type(identifierField(), bad);
    await user.type(passwordField(), 'Ornek-Sifre-2026');
    await user.click(submitButton());

    expect(screen.getByText('Geçerli bir e-posta veya telefon girin')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it('disables the button while submitting and does not submit twice', async () => {
    const user = userEvent.setup();
    let finish: () => void = () => undefined;
    const login = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    await openLogin({ login });

    await user.type(identifierField(), 'veli@ornek.test');
    await user.type(passwordField(), 'Ornek-Sifre-2026');
    await user.click(submitButton());

    expect(submitButton()).toBeDisabled();
    expect(submitButton()).toHaveTextContent('Giriş yapılıyor…');
    await user.keyboard('{Enter}');
    expect(login).toHaveBeenCalledTimes(1);
    await act(async () => finish());
  });

  it('refused login: generic message, identifier kept, password cleared', async () => {
    const user = userEvent.setup();
    await openLogin({
      login: async () => {
        throw new ApiError(401, 'IDENTITY_INVALID_CREDENTIALS');
      },
    });

    await user.type(identifierField(), '0532 123 45 67');
    await user.type(passwordField(), 'Yanlis-Sifre');
    await user.click(submitButton());

    expect(await screen.findByRole('alert')).toHaveTextContent('E-posta/telefon veya şifre hatalı');
    expect(identifierField()).toHaveValue('0532 123 45 67');
    expect(passwordField()).toHaveValue('');
  });

  it.each([
    ['the server cannot be reached', new ApiError(undefined, undefined)],
    ['the server does not answer in 10 s', new ApiError(undefined, 'TIMEOUT')],
  ])('network error when %s: message, identifier kept, password cleared', async (_case, error) => {
    const user = userEvent.setup();
    await openLogin({
      login: async () => {
        throw error;
      },
    });

    await user.type(identifierField(), 'veli@ornek.test');
    await user.type(passwordField(), 'Ornek-Sifre-2026');
    await user.click(submitButton());

    expect(await screen.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor, tekrar deneyin');
    expect(identifierField()).toHaveValue('veli@ornek.test');
    expect(passwordField()).toHaveValue('');
  });

  it('keyboard: Tab goes identifier → password → sign in, and Enter submits', async () => {
    const user = userEvent.setup();
    const { login } = await openLogin();

    await user.tab();
    expect(identifierField()).toHaveFocus();
    await user.keyboard('veli@ornek.test');
    await user.tab();
    expect(passwordField()).toHaveFocus();
    await user.keyboard('Ornek-Sifre-2026');
    await user.tab();
    expect(submitButton()).toHaveFocus();
    await user.click(passwordField());
    await user.keyboard('{Enter}');

    await waitFor(() => expect(login).toHaveBeenCalledWith('veli@ornek.test', 'Ornek-Sifre-2026'));
  });

  it('the show-password switch is labelled and reveals the password', async () => {
    const user = userEvent.setup();
    await openLogin();

    await user.click(screen.getByRole('checkbox', { name: 'Şifreyi göster' }));

    expect(passwordField()).toHaveAttribute('type', 'text');
  });

  it('successful login lands on the home screen', async () => {
    const user = userEvent.setup();
    await openLogin();

    await user.type(identifierField(), 'veli@ornek.test');
    await user.type(passwordField(), 'Ornek-Sifre-2026');
    await user.click(submitButton());

    expect(await screen.findByText('Hoş geldiniz, Ayşe Yılmaz')).toBeInTheDocument();
    expect(pathname()).toBe('/');
  });

  it('links to the system status screen without signing in (AC-11)', async () => {
    const user = userEvent.setup();
    await openLogin();

    await user.click(screen.getByRole('link', { name: 'Sistem durumu' }));

    expect(await screen.findByText('Sistem çalışıyor')).toBeInTheDocument();
    expect(pathname()).toBe('/durum');
  });
});
