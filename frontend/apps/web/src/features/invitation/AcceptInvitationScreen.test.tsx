import { ApiError, ERROR_CODES, type AcceptedInvitation, type InvitationInfo } from '@schoolos/api-client';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { fakeApi, pathname, renderApp } from '../../test/renderApp';

/** Spec 0004, AC-5 and AC-6 on the web: the invitation link screen, without a session. */

const INFO: InvitationInfo = { fullName: 'Ali Öğretmen', institutionName: 'Örnek Koleji' };
const PATH = '/davet#inv.secret';

describe('AcceptInvitationScreen (web)', () => {
  it('a valid link shows whom it invites; the token comes from the fragment', async () => {
    const getInvitation = vi.fn(async () => INFO);
    renderApp({ api: fakeApi({ getInvitation }), path: PATH });

    expect(await screen.findByText('Ali Öğretmen, Örnek Koleji sizi davet etti.')).toBeInTheDocument();
    expect(getInvitation).toHaveBeenCalledWith('inv.secret', expect.anything());
  });

  it('shows loading first', async () => {
    renderApp({
      api: fakeApi({ getInvitation: () => new Promise<InvitationInfo>(() => undefined) }),
      path: PATH,
    });

    expect(await screen.findByText('Yükleniyor')).toBeInTheDocument();
  });

  it('expired, used, unknown and missing links all get the same message and no name', async () => {
    const invalid = () => Promise.reject(new ApiError(404, ERROR_CODES.notFound));
    for (const path of [PATH, '/davet']) {
      const view = renderApp({ api: fakeApi({ getInvitation: invalid }), path });
      expect(await screen.findByRole('alert')).toHaveTextContent(
        'Bu davet bağlantısı geçerli değil. Kurumunuzun yöneticisinden yeni bir bağlantı isteyin.',
      );
      expect(screen.queryByText(/Ali/)).toBeNull();
      expect(screen.queryByLabelText('Şifre')).toBeNull();
      view.unmount();
    }
  });

  it('9 characters and mismatched passwords are refused before sending; 10 are accepted', async () => {
    const user = userEvent.setup();
    const acceptInvitation = vi.fn(async (): Promise<AcceptedInvitation> => ({
      identifier: 'ali@ornek.test',
    }));
    renderApp({ api: fakeApi({ getInvitation: async () => INFO, acceptInvitation }), path: PATH });

    await user.type(await screen.findByLabelText('Şifre'), '123456789');
    await user.type(screen.getByLabelText('Şifre (tekrar)'), '123456789');
    await user.click(screen.getByRole('button', { name: 'Şifremi belirle' }));
    expect(screen.getByLabelText('Şifre')).toHaveAccessibleDescription('Şifre en az 10 karakter olmalı');

    await user.type(screen.getByLabelText('Şifre'), '0');
    await user.click(screen.getByRole('button', { name: 'Şifremi belirle' }));
    expect(screen.getByLabelText('Şifre (tekrar)')).toHaveAccessibleDescription('Şifreler eşleşmiyor');
    expect(acceptInvitation).not.toHaveBeenCalled();

    await user.type(screen.getByLabelText('Şifre (tekrar)'), '0');
    await user.click(screen.getByRole('button', { name: 'Şifremi belirle' }));
    expect(acceptInvitation).toHaveBeenCalledWith('inv.secret', '1234567890');
  });

  it('success goes to the sign-in screen with the identifier filled in and the confirmation shown', async () => {
    const user = userEvent.setup();
    renderApp({
      api: fakeApi({
        getInvitation: async () => INFO,
        acceptInvitation: async () => ({ identifier: 'ali@ornek.test' }),
      }),
      path: PATH,
    });

    await user.type(await screen.findByLabelText('Şifre'), 'Yeni-Sifre-2026');
    await user.type(screen.getByLabelText('Şifre (tekrar)'), 'Yeni-Sifre-2026');
    await user.click(screen.getByRole('button', { name: 'Şifremi belirle' }));

    expect(await screen.findByRole('heading', { name: 'Giriş yap' })).toBeInTheDocument();
    expect(pathname()).toBe('/giris');
    expect(screen.getByLabelText('E-posta veya telefon')).toHaveValue('ali@ornek.test');
    expect(screen.getByRole('status')).toHaveTextContent('Şifreniz belirlendi, giriş yapabilirsiniz');
  });

  it('while sending the button is disabled; a network error and too many tries are shown', async () => {
    const user = userEvent.setup();
    let reject: (e: unknown) => void = () => undefined;
    const acceptInvitation = vi
      .fn<() => Promise<AcceptedInvitation>>()
      .mockImplementationOnce(() => new Promise((_, r) => (reject = r)))
      .mockRejectedValueOnce(new ApiError(429, ERROR_CODES.rateLimited));
    renderApp({ api: fakeApi({ getInvitation: async () => INFO, acceptInvitation }), path: PATH });

    await user.type(await screen.findByLabelText('Şifre'), 'Yeni-Sifre-2026');
    await user.type(screen.getByLabelText('Şifre (tekrar)'), 'Yeni-Sifre-2026');
    await user.click(screen.getByRole('button', { name: 'Şifremi belirle' }));
    expect(screen.getByRole('button', { name: 'Kaydediliyor…' })).toBeDisabled();
    reject(new ApiError(undefined, undefined));
    expect(await screen.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor, tekrar deneyin');

    await user.click(screen.getByRole('button', { name: 'Şifremi belirle' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Çok fazla deneme yapıldı, biraz sonra tekrar deneyin',
    );
  });
});
