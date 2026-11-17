import { ApiError, type ApiClient, type SystemStatus } from '@schoolos/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ApiProvider } from '../../api/ApiProvider';
import { AppearanceProvider } from '../../appearance/AppearanceProvider';
import { createMemoryStorage } from '../../test/fakes';
import { StatusScreen } from './StatusScreen';

function renderWith(getSystemStatus: ApiClient['getSystemStatus']) {
  renderWithContainer(getSystemStatus);
}

function renderWithContainer(getSystemStatus: ApiClient['getSystemStatus']) {
  const client = new QueryClient({
    // gcTime Infinity: no cache-eviction timer outlives the test (docs/testing.md, Resource hygiene).
    defaultOptions: { queries: { retry: false, gcTime: Infinity } },
  });
  return render(
    <AppearanceProvider storage={createMemoryStorage()} configuredBrand={null}>
      <QueryClientProvider client={client}>
        <ApiProvider client={{ getSystemStatus }}>
          <StatusScreen />
        </ApiProvider>
      </QueryClientProvider>
    </AppearanceProvider>,
  );
}

const up: SystemStatus = { status: 'UP', version: '0.1.0' };

describe('StatusScreen (web)', () => {
  it('shows checking state', () => {
    renderWith(() => new Promise<SystemStatus>(() => undefined));

    expect(screen.getByRole('status')).toHaveTextContent('Sistem kontrol ediliyor…');
    expect(screen.getByRole('progressbar', { name: 'Yükleniyor' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Tekrar dene' })).not.toBeInTheDocument();
  });

  it('shows running state with version', async () => {
    renderWith(async () => up);

    expect(await screen.findByText('Sistem çalışıyor')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Sürüm 0.1.0');
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('pulses only while checking (review 1, M-1)', async () => {
    const { container } = renderWithContainer(async () => up);

    expect(container.querySelector('[data-pulse]')).not.toBeNull();
    await screen.findByText('Sistem çalışıyor');
    expect(container.querySelector('[data-pulse]')).toBeNull();
  });

  it.each([
    ['the backend answers with an error', new ApiError(503, 'INTERNAL_ERROR')],
    ['the backend does not answer in 5 s', new ApiError(undefined, 'TIMEOUT')],
    ['the network is down', new ApiError(undefined, undefined)],
  ])('shows unreachable state when %s', async (_case, error) => {
    renderWith(async () => {
      throw error;
    });

    expect(await screen.findByText('Sunucuya ulaşılamıyor')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tekrar dene' })).toBeInTheDocument();
  });

  it('shows unreachable state and retries', async () => {
    const user = userEvent.setup();
    let answer: () => Promise<SystemStatus> = async () => {
      throw new ApiError(undefined, 'TIMEOUT');
    };
    const getSystemStatus = vi.fn(() => answer());
    renderWith(getSystemStatus);
    const retry = await screen.findByRole('button', { name: 'Tekrar dene' });

    answer = async () => up;
    await user.click(retry);

    expect(await screen.findByText('Sistem çalışıyor')).toBeInTheDocument();
    expect(getSystemStatus).toHaveBeenCalledTimes(2);
  });

  it('retries from the keyboard', async () => {
    const user = userEvent.setup();
    let answer: () => Promise<SystemStatus> = async () => {
      throw new ApiError(503, undefined);
    };
    renderWith(() => answer());
    await screen.findByRole('button', { name: 'Tekrar dene' });

    answer = async () => up;
    await user.tab();
    expect(screen.getByRole('button', { name: 'Görünüm' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Tekrar dene' })).toHaveFocus();
    await user.keyboard('{Enter}');

    expect(await screen.findByText('Sistem çalışıyor')).toBeInTheDocument();
  });

  it('goes back to checking while a retry is in flight', async () => {
    const user = userEvent.setup();
    let answer: () => Promise<SystemStatus> = async () => {
      throw new ApiError(503, undefined);
    };
    renderWith(() => answer());
    const retry = await screen.findByRole('button', { name: 'Tekrar dene' });

    answer = () => new Promise<SystemStatus>(() => undefined);
    await user.click(retry);

    expect(screen.getByRole('status')).toHaveTextContent('Sistem kontrol ediliyor…');
  });
});
