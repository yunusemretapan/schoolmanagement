import { act, screen, userEvent } from '@testing-library/react-native';
import { BackHandler } from 'react-native';
import { fakeApi, renderMobile } from './test/renderMobile';

/**
 * Spec 0002's showcase journey, now from a signed-in session (spec 0003, AC-11 closes the showcase to signed-out
 * users, B-10): home → status screen → showcase. The assertions below are unchanged.
 */
async function renderScreens() {
  const user = userEvent.setup();
  await renderMobile({ api: fakeApi({ restoreSession: async () => true }) });
  await user.press(await screen.findByRole('link', { name: 'Sistem durumu' }));
  await screen.findByText('Sistem çalışıyor');
}

describe('Screens (mobile, spec 0002 User experience — access)', () => {
  it('opens the showcase from the status screen link and returns with "Geri"', async () => {
    const user = userEvent.setup();
    await renderScreens();

    await user.press(screen.getByRole('link', { name: 'Tasarım vitrini' }));
    expect(await screen.findByRole('heading', { name: 'Tasarım vitrini' })).toBeOnTheScreen();

    await user.press(screen.getByRole('button', { name: 'Geri' }));
    expect(await screen.findByText('Sistem çalışıyor')).toBeOnTheScreen();
  });

  it("Android's back gesture leaves the showcase and is handled there", async () => {
    type BackListener = Parameters<typeof BackHandler.addEventListener>[1];
    const handlers: BackListener[] = [];
    const listen = jest.spyOn(BackHandler, 'addEventListener').mockImplementation((_event, handler) => {
      handlers.push(handler);
      return { remove: () => handlers.splice(handlers.indexOf(handler), 1) && undefined };
    });
    try {
      const user = userEvent.setup();
      await renderScreens();
      expect(handlers).toHaveLength(0);

      await user.press(screen.getByRole('link', { name: 'Tasarım vitrini' }));
      await screen.findByRole('heading', { name: 'Tasarım vitrini' });
      expect(handlers).toHaveLength(1);

      let handled: boolean | null | undefined;
      await act(async () => {
        handled = handlers[0]?.({} as Parameters<BackListener>[0]);
      });

      expect(handled).toBe(true);
      expect(await screen.findByText('Sistem çalışıyor')).toBeOnTheScreen();
      expect(handlers).toHaveLength(0);
    } finally {
      listen.mockRestore();
    }
  });
});

describe('Screens (mobile, spec 0003 — access)', () => {
  it('signed out: the login screen, with the status screen one link away and back', async () => {
    const user = userEvent.setup();
    await renderMobile();

    expect(await screen.findByRole('heading', { name: 'Giriş yap' })).toBeOnTheScreen();
    await user.press(screen.getByRole('link', { name: 'Sistem durumu' }));
    expect(await screen.findByText('Sistem çalışıyor')).toBeOnTheScreen();

    await user.press(screen.getByRole('button', { name: 'Geri' }));
    expect(await screen.findByRole('heading', { name: 'Giriş yap' })).toBeOnTheScreen();
  });

  it('signed out: the status screen offers no way into the showcase (AC-11)', async () => {
    const user = userEvent.setup();
    await renderMobile();

    await user.press(await screen.findByRole('link', { name: 'Sistem durumu' }));
    await screen.findByText('Sistem çalışıyor');

    expect(screen.queryByRole('link', { name: 'Tasarım vitrini' })).toBeNull();
  });

  it('while the stored session is being checked, nothing signed-in is shown', async () => {
    await renderMobile({ api: fakeApi({ restoreSession: () => new Promise<boolean>(() => undefined) }) });

    expect(await screen.findByText('Oturum kontrol ediliyor…')).toBeOnTheScreen();
    expect(screen.queryByText(/Hoş geldiniz/)).toBeNull();
  });

  it('a session that ends on its own returns to the login screen with the notice, even from the showcase', async () => {
    const user = userEvent.setup();
    const { expiry } = await renderMobile({ api: fakeApi({ restoreSession: async () => true }) });
    await user.press(await screen.findByRole('link', { name: 'Tasarım vitrini' }));
    await screen.findByRole('heading', { name: 'Tasarım vitrini' });

    await act(async () => expiry.notify());

    expect(await screen.findByRole('heading', { name: 'Giriş yap' })).toBeOnTheScreen();
    expect(screen.getByTestId('session-expired')).toHaveTextContent(
      'Oturumunuz sona erdi, lütfen tekrar giriş yapın',
    );
    expect(screen.queryByRole('heading', { name: 'Tasarım vitrini' })).toBeNull();
  });
});
