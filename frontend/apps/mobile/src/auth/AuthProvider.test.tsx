import { act, screen } from '@testing-library/react-native';
import { fakeApi, renderMobile } from '../test/renderMobile';

/** Spec 0003, AC-8 (mobile): a stored session opens the app; an ended one says so and forgets cached data. */
describe('AuthProvider (mobile)', () => {
  it('a stored session opens the app on the home screen', async () => {
    await renderMobile({ api: fakeApi({ restoreSession: async () => true }) });

    expect(await screen.findByText('Hoş geldiniz, Ayşe Yılmaz')).toBeOnTheScreen();
  });

  it('no stored session opens the login screen without the expiry notice', async () => {
    await renderMobile();

    expect(await screen.findByRole('heading', { name: 'Giriş yap' })).toBeOnTheScreen();
    expect(screen.queryByTestId('session-expired')).toBeNull();
  });

  it('when the session ends, the login screen says "Oturumunuz sona erdi" and the cache is cleared', async () => {
    const { expiry, client } = await renderMobile({ api: fakeApi({ restoreSession: async () => true }) });
    await screen.findByText('Hoş geldiniz, Ayşe Yılmaz');
    expect(client.getQueryData(['me'])).toBeDefined();

    await act(async () => expiry.notify());

    expect(await screen.findByTestId('session-expired')).toHaveTextContent(
      'Oturumunuz sona erdi, lütfen tekrar giriş yapın',
    );
    expect(client.getQueryData(['me'])).toBeUndefined();
  });
});
