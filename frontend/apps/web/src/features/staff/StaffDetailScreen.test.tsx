import { ApiError, ERROR_CODES, type StaffDetail } from '@schoolos/api-client';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { fakeApi, renderApp } from '../../test/renderApp';
import { ADMIN } from './StaffListScreen.test';

/** Spec 0004, AC-7, AC-8, AC-9 and the detail screen's states, on the web. */

const ID = '01900000-0000-7000-8000-000000000002';
const PATH = `/personel/${ID}`;

function detail(overrides: Partial<StaffDetail> = {}): StaffDetail {
  return {
    id: ID,
    fullName: 'Ali Öğretmen',
    email: 'ali@ornek.test',
    phone: null,
    roles: ['TEACHER'],
    status: 'ACTIVE',
    invitationExpiresAt: null,
    self: false,
    lastActiveAdmin: false,
    ...overrides,
  };
}

const asAdmin = (overrides: Parameters<typeof fakeApi>[0] = {}) =>
  fakeApi({ restoreSession: async () => true, getMe: async () => ADMIN, ...overrides });

describe('StaffDetailScreen (web)', () => {
  it('saves new roles, announces it and shows the saving state meanwhile', async () => {
    const user = userEvent.setup();
    let resolve: (d: StaffDetail) => void = () => undefined;
    const updateStaff = vi.fn(() => new Promise<StaffDetail>((r) => (resolve = r)));
    renderApp({ api: asAdmin({ getStaff: async () => detail(), updateStaff }), path: PATH });

    await user.click(await screen.findByRole('checkbox', { name: 'Muhasebe / İdari personel' }));
    await user.click(screen.getByRole('checkbox', { name: 'Öğretmen' }));
    await user.click(screen.getByRole('button', { name: 'Kaydet' }));

    expect(screen.getByRole('button', { name: 'Kaydediliyor…' })).toBeDisabled();
    expect(updateStaff).toHaveBeenCalledWith(ID, {
      fullName: 'Ali Öğretmen',
      email: 'ali@ornek.test',
      phone: null,
      roles: ['ACCOUNTANT'],
    });
    resolve(detail({ roles: ['ACCOUNTANT'] }));
    expect(await screen.findByText('Değişiklikler kaydedildi')).toBeInTheDocument();
  });

  it('removing every role is refused before anything is sent', async () => {
    const user = userEvent.setup();
    const updateStaff = vi.fn(async () => detail());
    renderApp({ api: asAdmin({ getStaff: async () => detail(), updateStaff }), path: PATH });

    await user.click(await screen.findByRole('checkbox', { name: 'Öğretmen' }));
    await user.click(screen.getByRole('button', { name: 'Kaydet' }));

    expect(screen.getByText('En az bir rol seçin')).toBeInTheDocument();
    expect(updateStaff).not.toHaveBeenCalled();
  });

  it('an active account cannot edit e-mail or phone; an invited one can', async () => {
    renderApp({ api: asAdmin({ getStaff: async () => detail() }), path: PATH });

    expect(await screen.findByLabelText('E-posta')).toBeDisabled();
    expect(screen.getByLabelText('E-posta')).toHaveAccessibleDescription(
      'E-posta ve telefon yalnızca davet beklerken değiştirilebilir',
    );
    expect(screen.getByLabelText('Ad soyad')).toBeEnabled();
  });

  it('an invited account shows its link expiry and can get a new link', async () => {
    const user = userEvent.setup();
    const renewInvitation = vi.fn(async () => ({
      staffId: ID,
      token: 'new.secret',
      expiresAt: '2026-11-24T09:00:00Z',
    }));
    renderApp({
      api: asAdmin({
        getStaff: async () => detail({ status: 'INVITED', invitationExpiresAt: '2026-11-23T09:00:00Z' }),
        renewInvitation,
      }),
      path: PATH,
    });

    expect(await screen.findByLabelText('E-posta')).toBeEnabled();
    expect(screen.getByText('Bağlantı 23 Kasım 2026 12:00 tarihine kadar geçerli')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Yeni davet bağlantısı oluştur' }));

    expect(await screen.findByTestId('invitation-url')).toHaveTextContent('/davet#new.secret');
  });

  it('deactivation asks first; cancelling changes nothing and returns focus', async () => {
    const user = userEvent.setup();
    const deactivateStaff = vi.fn(async () => detail({ status: 'INACTIVE' }));
    renderApp({ api: asAdmin({ getStaff: async () => detail(), deactivateStaff }), path: PATH });

    const trigger = await screen.findByRole('button', { name: 'Pasifleştir' });
    await user.click(trigger);
    const dialog = screen.getByRole('dialog', { name: 'Personeli pasifleştir' });
    expect(dialog).toHaveAccessibleDescription(
      'Ali Öğretmen artık sisteme giremeyecek. Pasifleştirilsin mi?',
    );
    expect(screen.getByRole('button', { name: 'Vazgeç' })).toHaveFocus();

    await user.click(screen.getByRole('button', { name: 'Vazgeç' }));

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(deactivateStaff).not.toHaveBeenCalled();
    expect(trigger).toHaveFocus();
  });

  it('confirming deactivates, and the person can be reactivated', async () => {
    const user = userEvent.setup();
    const deactivateStaff = vi.fn(async () => detail({ status: 'INACTIVE' }));
    const reactivateStaff = vi.fn(async () => detail());
    renderApp({
      api: asAdmin({ getStaff: async () => detail(), deactivateStaff, reactivateStaff }),
      path: PATH,
    });

    await user.click(await screen.findByRole('button', { name: 'Pasifleştir' }));
    const confirm = screen.getAllByRole('button', { name: 'Pasifleştir' }).at(-1)!;
    await user.click(confirm);

    expect(deactivateStaff).toHaveBeenCalledWith(ID);
    expect(await screen.findByText('Pasif')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Yeniden etkinleştir' }));
    expect(reactivateStaff).toHaveBeenCalledWith(ID);
    expect(await screen.findByText('Aktif')).toBeInTheDocument();
  });

  it('after confirming, focus lands on the button that took its place, and back again (review 1, L-3)', async () => {
    const user = userEvent.setup();
    renderApp({
      api: asAdmin({
        getStaff: async () => detail(),
        deactivateStaff: async () => detail({ status: 'INACTIVE' }),
        reactivateStaff: async () => detail(),
      }),
      path: PATH,
    });

    await user.click(await screen.findByRole('button', { name: 'Pasifleştir' }));
    await user.click(screen.getAllByRole('button', { name: 'Pasifleştir' }).at(-1)!);

    expect(await screen.findByRole('button', { name: 'Yeniden etkinleştir' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Yeniden etkinleştir' }));
    await vi.waitFor(() => expect(screen.getByRole('button', { name: 'Pasifleştir' })).toHaveFocus());
  });

  it('the last active administrator: role and deactivation are disabled with the reason', async () => {
    renderApp({
      api: asAdmin({ getStaff: async () => detail({ roles: ['ADMIN'], lastActiveAdmin: true }) }),
      path: PATH,
    });

    const adminRole = await screen.findByRole('checkbox', { name: 'Kurucu / Yönetici' });
    expect(adminRole).toBeDisabled();
    expect(adminRole).toHaveAccessibleDescription('Kurumda en az bir aktif yönetici kalmalı');
    const deactivate = screen.getByRole('button', { name: 'Pasifleştir' });
    expect(deactivate).toBeDisabled();
    expect(deactivate).toHaveAccessibleDescription('Kurumda en az bir aktif yönetici kalmalı');
  });

  it('their own account: the administrator cannot deactivate it or drop their own administrator role', async () => {
    renderApp({
      api: asAdmin({ getStaff: async () => detail({ roles: ['ADMIN'], self: true }) }),
      path: PATH,
    });

    expect(await screen.findByRole('checkbox', { name: 'Kurucu / Yönetici' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Pasifleştir' })).toBeDisabled();
  });

  it('a rule the server refuses is shown as an error', async () => {
    const user = userEvent.setup();
    renderApp({
      api: asAdmin({
        getStaff: async () => detail(),
        updateStaff: async () => {
          throw new ApiError(422, ERROR_CODES.lastAdmin);
        },
      }),
      path: PATH,
    });

    await user.click(await screen.findByRole('button', { name: 'Kaydet' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Kurumda en az bir aktif yönetici kalmalı');
  });

  it('another institution or an unknown id shows "not found"', async () => {
    renderApp({
      api: asAdmin({
        getStaff: async () => {
          throw new ApiError(404, ERROR_CODES.notFound);
        },
      }),
      path: PATH,
    });

    expect(await screen.findByRole('alert')).toHaveTextContent('Personel bulunamadı');
  });
});
