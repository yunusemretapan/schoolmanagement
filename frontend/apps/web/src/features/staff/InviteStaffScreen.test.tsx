import { ApiError, ERROR_CODES, type InvitationLink } from '@schoolos/api-client';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { fakeApi, renderApp } from '../../test/renderApp';
import { ADMIN } from './StaffListScreen.test';

/** Spec 0004, AC-4: the invite form's states, on the web. */

const LINK: InvitationLink = {
  staffId: '01900000-0000-7000-8000-000000000009',
  token: 'inv.secret',
  expiresAt: '2026-11-23T09:00:00Z',
};

const asAdmin = (overrides: Parameters<typeof fakeApi>[0] = {}) =>
  fakeApi({ restoreSession: async () => true, getMe: async () => ADMIN, ...overrides });

async function fill(user: ReturnType<typeof userEvent.setup>, values: { name?: string; email?: string }) {
  if (values.name) await user.type(await screen.findByLabelText('Ad soyad'), values.name);
  if (values.email) await user.type(screen.getByLabelText('E-posta'), values.email);
}

describe('InviteStaffScreen (web)', () => {
  afterEach(() => vi.restoreAllMocks());

  it('field errors appear under their fields and nothing is sent', async () => {
    const user = userEvent.setup();
    const inviteStaff = vi.fn(async () => LINK);
    renderApp({ api: asAdmin({ inviteStaff }), path: '/personel/davet' });

    await user.click(await screen.findByRole('button', { name: 'Davet oluştur' }));

    expect(screen.getByLabelText('Ad soyad')).toHaveAccessibleDescription('Bu alan zorunlu');
    expect(screen.getByLabelText('E-posta')).toHaveAccessibleDescription(
      'E-posta veya telefondan en az birini girin',
    );
    expect(screen.getByText('En az bir rol seçin')).toBeInTheDocument();

    await user.type(screen.getByLabelText('E-posta'), 'ali@');
    await user.type(screen.getByLabelText('Cep telefonu'), '0212 123 45 67');
    await user.click(screen.getByRole('button', { name: 'Davet oluştur' }));

    expect(screen.getByLabelText('E-posta')).toHaveAccessibleDescription('Geçerli bir e-posta girin');
    expect(screen.getByLabelText('Cep telefonu')).toHaveAccessibleDescription(
      'Geçerli bir cep telefonu girin',
    );
    expect(inviteStaff).not.toHaveBeenCalled();
  });

  it('only staff roles are offered: no Veli checkbox', async () => {
    renderApp({ api: asAdmin(), path: '/personel/davet' });

    expect(await screen.findByRole('checkbox', { name: 'Kurucu / Yönetici' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Öğretmen' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Muhasebe / İdari personel' })).toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: 'Veli' })).toBeNull();
  });

  it('while sending the button is disabled and a second press sends nothing more', async () => {
    const user = userEvent.setup();
    let resolve: (link: InvitationLink) => void = () => undefined;
    const inviteStaff = vi.fn(() => new Promise<InvitationLink>((r) => (resolve = r)));
    renderApp({ api: asAdmin({ inviteStaff }), path: '/personel/davet' });

    await fill(user, { name: 'Ali Öğretmen', email: 'ali@ornek.test' });
    await user.click(screen.getByRole('checkbox', { name: 'Öğretmen' }));
    await user.click(screen.getByRole('button', { name: 'Davet oluştur' }));

    const busy = screen.getByRole('button', { name: 'Davet oluşturuluyor…' });
    expect(busy).toBeDisabled();
    await user.click(busy);
    expect(inviteStaff).toHaveBeenCalledTimes(1);
    expect(inviteStaff).toHaveBeenCalledWith({
      fullName: 'Ali Öğretmen',
      email: 'ali@ornek.test',
      phone: null,
      roles: ['TEACHER'],
    });
    resolve(LINK);
    expect(await screen.findByText(/Davet bağlantısı hazır/)).toBeInTheDocument();
  });

  it('success shows the link with its expiry, and copying it is announced', async () => {
    const user = userEvent.setup();
    const writeText = vi.fn(async () => undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    renderApp({ api: asAdmin({ inviteStaff: async () => LINK }), path: '/personel/davet' });

    await fill(user, { name: 'Ali', email: 'ali@ornek.test' });
    await user.click(screen.getByRole('checkbox', { name: 'Öğretmen' }));
    await user.click(screen.getByRole('button', { name: 'Davet oluştur' }));

    const url = `${window.location.origin}/davet#inv.secret`;
    expect(await screen.findByTestId('invitation-url')).toHaveTextContent(url);
    expect(screen.getByText('Bağlantı 23 Kasım 2026 12:00 tarihine kadar geçerli')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Bağlantıyı kopyala' }));

    expect(writeText).toHaveBeenCalledWith(url);
    expect(screen.getByRole('status')).toHaveTextContent('Bağlantı kopyalandı');
  });

  it('an identifier in use is refused without saying where; the input stays', async () => {
    const user = userEvent.setup();
    renderApp({
      api: asAdmin({
        inviteStaff: async () => {
          throw new ApiError(422, ERROR_CODES.identifierTaken);
        },
      }),
      path: '/personel/davet',
    });

    await fill(user, { name: 'Ali', email: 'ali@ornek.test' });
    await user.click(screen.getByRole('checkbox', { name: 'Öğretmen' }));
    await user.click(screen.getByRole('button', { name: 'Davet oluştur' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Bu e-posta veya telefon kullanılamıyor');
    expect(screen.getByLabelText('E-posta')).toHaveValue('ali@ornek.test');
  });

  it('a network error keeps everything that was typed', async () => {
    const user = userEvent.setup();
    renderApp({
      api: asAdmin({
        inviteStaff: async () => {
          throw new ApiError(undefined, undefined);
        },
      }),
      path: '/personel/davet',
    });

    await fill(user, { name: 'Ali', email: 'ali@ornek.test' });
    await user.click(screen.getByRole('checkbox', { name: 'Öğretmen' }));
    await user.click(screen.getByRole('button', { name: 'Davet oluştur' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor, tekrar deneyin');
    expect(screen.getByLabelText('Ad soyad')).toHaveValue('Ali');
    expect(screen.getByRole('checkbox', { name: 'Öğretmen' })).toBeChecked();
  });
});
