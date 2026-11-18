import { ApiError, type ApiClient, type SystemStatus } from '@schoolos/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, userEvent } from '@testing-library/react-native';
import { AccessibilityInfo, Platform } from 'react-native';
import { ApiProvider } from '../../api/ApiProvider';
import { AppearanceProvider } from '../../appearance/AppearanceProvider';
import { createMemoryStorage } from '../../test/device';
import { StatusScreen } from './StatusScreen';

async function renderWith(getSystemStatus: ApiClient['getSystemStatus']) {
  const client = new QueryClient({
    // gcTime Infinity: no cache-eviction timer outlives the test (docs/testing.md, Resource hygiene).
    defaultOptions: { queries: { retry: false, gcTime: Infinity } },
  });
  await render(
    <QueryClientProvider client={client}>
      <ApiProvider client={{ getSystemStatus }}>
        <AppearanceProvider storage={createMemoryStorage()} configuredBrand={null}>
          <StatusScreen />
        </AppearanceProvider>
      </ApiProvider>
    </QueryClientProvider>,
  );
  // Saved appearance loads asynchronously; the screen appears once it is known.
  await screen.findByTestId('status-region');
}

const up: SystemStatus = { status: 'UP', version: '0.1.0' };

describe('StatusScreen (mobile)', () => {
  it('offers the appearance panel from the header (spec 0002 AC-1)', async () => {
    await renderWith(async () => up);

    expect(screen.getByRole('button', { name: 'Görünüm' })).toBeOnTheScreen();
  });

  it('shows checking state', async () => {
    await renderWith(() => new Promise<SystemStatus>(() => undefined));

    expect(screen.getByText('Sistem kontrol ediliyor…')).toBeOnTheScreen();
    expect(screen.getByLabelText('Yükleniyor')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Tekrar dene' })).not.toBeOnTheScreen();
  });

  it('shows running state with version', async () => {
    await renderWith(async () => up);

    expect(await screen.findByText('Sistem çalışıyor')).toBeOnTheScreen();
    expect(screen.getByText('Sürüm 0.1.0')).toBeOnTheScreen();
    expect(screen.queryByLabelText('Yükleniyor')).not.toBeOnTheScreen();
  });

  it.each([
    ['the backend answers with an error', new ApiError(503, 'INTERNAL_ERROR')],
    ['the backend does not answer in 5 s', new ApiError(undefined, 'TIMEOUT')],
    ['the network is down', new ApiError(undefined, undefined)],
  ])('shows unreachable state when %s', async (_case, error) => {
    await renderWith(async () => {
      throw error;
    });

    expect(await screen.findByText('Sunucuya ulaşılamıyor')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Tekrar dene' })).toBeOnTheScreen();
  });

  it('shows unreachable state and retries', async () => {
    const user = userEvent.setup();
    let answer: () => Promise<SystemStatus> = async () => {
      throw new ApiError(undefined, 'TIMEOUT');
    };
    const getSystemStatus = jest.fn(() => answer());
    await renderWith(getSystemStatus);
    const retry = await screen.findByRole('button', { name: 'Tekrar dene' });

    answer = async () => up;
    await user.press(retry);

    expect(await screen.findByText('Sistem çalışıyor')).toBeOnTheScreen();
    expect(getSystemStatus).toHaveBeenCalledTimes(2);
  });

  it('announces status changes to screen readers on Android (live region)', async () => {
    await renderWith(async () => up);

    expect(screen.getByTestId('status-region')).toHaveProp('accessibilityLiveRegion', 'polite');
  });

  it('announces each status change to VoiceOver on iOS (review 1, M-2)', async () => {
    expect(Platform.OS).toBe('ios');
    const announce = jest
      .spyOn(AccessibilityInfo, 'announceForAccessibility')
      .mockImplementation(() => undefined);
    // React Native's jest setup already mocks it: clear calls made by earlier tests.
    announce.mockClear();
    try {
      await renderWith(async () => up);
      await screen.findByText('Sistem çalışıyor');

      expect(announce.mock.calls.map(([text]) => text)).toEqual([
        'Sistem kontrol ediliyor…',
        'Sistem çalışıyor, Sürüm 0.1.0',
      ]);
    } finally {
      announce.mockRestore();
    }
  });

  it('announces the unreachable state to VoiceOver on iOS', async () => {
    const announce = jest
      .spyOn(AccessibilityInfo, 'announceForAccessibility')
      .mockImplementation(() => undefined);
    // React Native's jest setup already mocks it: clear calls made by earlier tests.
    announce.mockClear();
    try {
      await renderWith(async () => {
        throw new ApiError(undefined, 'TIMEOUT');
      });
      await screen.findByText('Sunucuya ulaşılamıyor');

      expect(announce).toHaveBeenLastCalledWith('Sunucuya ulaşılamıyor');
    } finally {
      announce.mockRestore();
    }
  });
});
