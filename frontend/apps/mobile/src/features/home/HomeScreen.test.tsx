import type { CurrentUser } from '@schoolos/api-client';
import { screen, userEvent, within } from '@testing-library/react-native';
import { fakeApi, renderMobile } from '../../test/renderMobile';

/** Spec 0003, AC-2 (name, institution, every role) and the home screen states — mobile. */
describe('HomeScreen (mobile)', () => {
  const signedIn = (overrides: Parameters<typeof fakeApi>[0] = {}) =>
    fakeApi({ restoreSession: async () => true, ...overrides });

  it('shows loading while the account is fetched', async () => {
    await renderMobile({ api: signedIn({ getMe: () => new Promise<CurrentUser>(() => undefined) }) });

    expect(await screen.findByText('Yükleniyor')).toBeOnTheScreen();
  });

  it('shows the name, the institution and every role', async () => {
    await renderMobile({ api: signedIn() });

    expect(await screen.findByText('Hoş geldiniz, Ayşe Yılmaz')).toBeOnTheScreen();
    expect(screen.getByText('Örnek Koleji')).toBeOnTheScreen();
    const roles = within(screen.getByTestId('roles'));
    expect(roles.getByText('Öğretmen')).toBeOnTheScreen();
    expect(roles.getByText('Veli')).toBeOnTheScreen();
  });

  it('opens the design showcase and returns with "Geri"', async () => {
    const user = userEvent.setup();
    await renderMobile({ api: signedIn() });

    await user.press(await screen.findByRole('link', { name: 'Tasarım vitrini' }));
    expect(await screen.findByRole('heading', { name: 'Tasarım vitrini' })).toBeOnTheScreen();

    await user.press(screen.getByRole('button', { name: 'Geri' }));
    expect(await screen.findByText('Hoş geldiniz, Ayşe Yılmaz')).toBeOnTheScreen();
  });

  it('signing out ends the session and shows the login screen', async () => {
    const user = userEvent.setup();
    const logout = jest.fn(async () => undefined);
    await renderMobile({ api: signedIn({ logout }) });

    await user.press(await screen.findByRole('button', { name: 'Çıkış yap' }));

    expect(await screen.findByRole('heading', { name: 'Giriş yap' })).toBeOnTheScreen();
    expect(logout).toHaveBeenCalledTimes(1);
  });
});
