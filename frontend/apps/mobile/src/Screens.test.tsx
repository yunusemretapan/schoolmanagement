import type { ApiClient, SystemStatus } from '@schoolos/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, screen, userEvent } from '@testing-library/react-native';
import { BackHandler } from 'react-native';
import { ApiProvider } from './api/ApiProvider';
import { AppearanceProvider } from './appearance/AppearanceProvider';
import { Screens } from './Screens';
import { createMemoryStorage } from './test/device';

const up: ApiClient['getSystemStatus'] = async (): Promise<SystemStatus> => ({
  status: 'UP',
  version: '0.1.0',
});

async function renderScreens() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } });
  await render(
    <QueryClientProvider client={client}>
      <ApiProvider client={{ getSystemStatus: up }}>
        <AppearanceProvider storage={createMemoryStorage()} configuredBrand={null}>
          <Screens />
        </AppearanceProvider>
      </ApiProvider>
    </QueryClientProvider>,
  );
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
