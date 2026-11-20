import { ApiError, type CurrentUser, type StaffPage, type StaffSummary } from '@schoolos/api-client';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { fakeApi, ME, renderApp } from '../../test/renderApp';

/** Spec 0004, AC-1 (no permission) and AC-3 (list, search, paging, states) on the web. */

export const ADMIN: CurrentUser = { ...ME, roles: ['ADMIN'] };

export function person(n: number, overrides: Partial<StaffSummary> = {}): StaffSummary {
  return {
    id: `01900000-0000-7000-8000-${String(n).padStart(12, '0')}`,
    fullName: `Personel ${String(n).padStart(2, '0')}`,
    email: `p${n}@ornek.test`,
    phone: null,
    roles: ['TEACHER'],
    status: 'ACTIVE',
    ...overrides,
  };
}

function page(items: StaffSummary[], total = items.length, pageNo = 0): StaffPage {
  return { items, page: pageNo, pageSize: 50, total };
}

const asAdmin = (overrides: Parameters<typeof fakeApi>[0] = {}) =>
  fakeApi({ restoreSession: async () => true, getMe: async () => ADMIN, ...overrides });

describe('StaffListScreen (web)', () => {
  it('shows each person with contact, roles and a written status, Veli as a badge', async () => {
    renderApp({
      api: asAdmin({
        listStaff: async () =>
          page([
            person(1, { fullName: 'Ali Öğretmen', phone: '+905321234567' }),
            person(2, { fullName: 'Ayşe', roles: ['TEACHER', 'GUARDIAN'], status: 'INVITED' }),
            person(3, { fullName: 'Can', status: 'INACTIVE' }),
          ]),
      }),
      path: '/personel',
    });

    const rows = await screen.findAllByRole('row');
    expect(within(rows[1]!).getByRole('link', { name: 'Ali Öğretmen' })).toHaveAttribute(
      'href',
      '/personel/01900000-0000-7000-8000-000000000001',
    );
    expect(within(rows[1]!).getByText('p1@ornek.test · +905321234567')).toBeInTheDocument();
    expect(within(rows[1]!).getByText('Aktif')).toBeInTheDocument();
    expect(within(rows[2]!).getByText('Veli')).toBeInTheDocument();
    expect(within(rows[2]!).getByText('Davet bekliyor')).toBeInTheDocument();
    expect(within(rows[3]!).getByText('Pasif')).toBeInTheDocument();
  });

  it('shows loading first', async () => {
    renderApp({
      api: asAdmin({ listStaff: () => new Promise<StaffPage>(() => undefined) }),
      path: '/personel',
    });

    expect(await screen.findByText('Yükleniyor')).toBeInTheDocument();
  });

  it('searches as the administrator types, from the first page', async () => {
    const user = userEvent.setup();
    const listStaff = vi.fn(async () => page([person(1)]));
    renderApp({ api: asAdmin({ listStaff }), path: '/personel' });

    await user.type(await screen.findByLabelText('Ad, e-posta veya telefonla ara'), '0532 123');

    expect(listStaff).toHaveBeenLastCalledWith({ q: '0532 123', page: 0 }, expect.anything());
  });

  it('says so when nothing matches', async () => {
    renderApp({ api: asAdmin({ listStaff: async () => page([]) }), path: '/personel' });

    expect(await screen.findByText('Aramanızla eşleşen personel yok')).toBeInTheDocument();
  });

  it('51 people make two pages of 50 and 1', async () => {
    const user = userEvent.setup();
    const first = Array.from({ length: 50 }, (_, i) => person(i));
    const listStaff = vi.fn(async ({ page: p }: { page?: number }) =>
      p === 1 ? page([person(50)], 51, 1) : page(first, 51, 0),
    );
    renderApp({ api: asAdmin({ listStaff }), path: '/personel' });

    expect(await screen.findByText('Sayfa 1 / 2')).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(51); // header + 50
    expect(screen.getByRole('button', { name: 'Önceki' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: 'Sonraki' }));

    expect(await screen.findByText('Sayfa 2 / 2')).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Sonraki' })).toBeDisabled();
  });

  it('a failed load shows the error and "Tekrar dene" loads again', async () => {
    const user = userEvent.setup();
    let fail = true;
    const listStaff = vi.fn(async () => {
      if (fail) throw new ApiError(undefined, undefined);
      return page([person(1)]);
    });
    renderApp({ api: asAdmin({ listStaff }), path: '/personel' });

    expect(await screen.findByRole('alert')).toHaveTextContent('Personel listesi yüklenemedi');
    fail = false;
    await user.click(screen.getByRole('button', { name: 'Tekrar dene' }));

    expect(await screen.findByText('Personel 01')).toBeInTheDocument();
  });

  it('a teacher who opens the address sees the no-permission state and no list call is made', async () => {
    const listStaff = vi.fn(async () => page([]));
    renderApp({ api: fakeApi({ restoreSession: async () => true, listStaff }), path: '/personel' });

    expect(await screen.findByRole('alert')).toHaveTextContent('Bu sayfayı görüntüleme yetkiniz yok');
    expect(listStaff).not.toHaveBeenCalled();
  });
});
