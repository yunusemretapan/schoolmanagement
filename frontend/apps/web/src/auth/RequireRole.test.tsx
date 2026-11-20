import type { CurrentUser } from '@schoolos/api-client';
import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { fakeApi, ME, renderApp } from '../test/renderApp';

/** Spec 0004, AC-1: staff screens only for administrators; the home screen links there only for them. */
describe('RequireRole and the staff link (web)', () => {
  const signedIn = (me: CurrentUser, overrides: Parameters<typeof fakeApi>[0] = {}) =>
    fakeApi({ restoreSession: async () => true, getMe: async () => me, ...overrides });

  for (const roles of [['TEACHER'], ['ACCOUNTANT'], ['GUARDIAN']] as const) {
    it(`${roles[0]}: no "Personel" link, and every staff address shows "no permission"`, async () => {
      const listStaff = vi.fn();
      const me = { ...ME, roles: [...roles] };
      for (const path of ['/personel', '/personel/davet', '/personel/01900000-0000-7000-8000-000000000001']) {
        const view = renderApp({ api: signedIn(me, { listStaff }), path });
        expect(await screen.findByRole('alert')).toHaveTextContent('Bu sayfayı görüntüleme yetkiniz yok');
        view.unmount();
      }
      renderApp({ api: signedIn(me) });
      expect(await screen.findByRole('link', { name: 'Sistem durumu' })).toBeInTheDocument();
      expect(screen.queryByRole('link', { name: 'Personel' })).toBeNull();
      expect(listStaff).not.toHaveBeenCalled();
    });
  }

  it('an administrator sees the "Personel" link', async () => {
    renderApp({ api: signedIn({ ...ME, roles: ['ADMIN'] }) });

    expect(await screen.findByRole('link', { name: 'Personel' })).toHaveAttribute('href', '/personel');
  });

  it('signed out, a staff address goes to the login screen', async () => {
    renderApp({ api: fakeApi(), path: '/personel' });

    expect(await screen.findByRole('heading', { name: 'Giriş yap' })).toBeInTheDocument();
  });
});
