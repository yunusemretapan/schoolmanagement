import { ApiError } from '@schoolos/api-client';
import { act, screen, userEvent } from '@testing-library/react-native';
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

  it('offline at start: "Sunucuya ulaşılamıyor" with retry, and retry opens the app (review 1, L-2)', async () => {
    const user = userEvent.setup();
    let online = false;
    const restoreSession = jest.fn(async () => {
      if (!online) throw new ApiError(undefined, undefined);
      return true;
    });
    await renderMobile({ api: fakeApi({ restoreSession }) });

    expect(await screen.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor');
    expect(screen.queryByRole('heading', { name: 'Giriş yap' })).toBeNull();
    online = true;
    await user.press(screen.getByRole('button', { name: 'Tekrar dene' }));

    expect(await screen.findByText('Hoş geldiniz, Ayşe Yılmaz')).toBeOnTheScreen();
    expect(restoreSession).toHaveBeenCalledTimes(2);
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
