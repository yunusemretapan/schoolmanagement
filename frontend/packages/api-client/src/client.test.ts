import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, createApiClient, STATUS_TIMEOUT_MS } from './index';

function json(body: unknown, status = 200, contentType = 'application/json'): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } });
}

/** A server that never answers, until the request is aborted. */
const hangingFetch: typeof fetch = (input) =>
  new Promise((_resolve, reject) => {
    const signal = (input as Request).signal;
    signal.addEventListener('abort', () => reject(signal.reason));
  });

describe('getSystemStatus', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('returns status and version from the backend', async () => {
    const fetchImpl = vi.fn<typeof fetch>(async () => json({ status: 'UP', version: '0.1.0' }));

    await expect(createApiClient('http://api.test', fetchImpl).getSystemStatus()).resolves.toEqual({
      status: 'UP',
      version: '0.1.0',
    });
    expect((fetchImpl.mock.calls[0]?.[0] as Request).url).toBe('http://api.test/api/v1/system/status');
  });

  it('fails with the ProblemDetails code when the backend answers with an error', async () => {
    const fetchImpl: typeof fetch = async () =>
      json(
        { status: 503, title: 'Service Unavailable', code: 'INTERNAL_ERROR' },
        503,
        'application/problem+json',
      );

    await expect(createApiClient('http://api.test', fetchImpl).getSystemStatus()).rejects.toMatchObject({
      status: 503,
      code: 'INTERNAL_ERROR',
    });
  });

  it('fails as a network error when the server cannot be reached', async () => {
    const fetchImpl: typeof fetch = async () => {
      throw new TypeError('Failed to fetch');
    };

    await expect(createApiClient('http://api.test', fetchImpl).getSystemStatus()).rejects.toMatchObject({
      status: undefined,
      message: 'NETWORK_ERROR',
    });
  });

  it('is still waiting just before 5 s', async () => {
    let settled = false;
    const pending = createApiClient('http://api.test', hangingFetch)
      .getSystemStatus()
      .finally(() => {
        settled = true;
      });
    pending.catch(() => undefined);

    await vi.advanceTimersByTimeAsync(STATUS_TIMEOUT_MS - 1);

    expect(settled).toBe(false);
    await vi.advanceTimersByTimeAsync(1);
  });

  it('times out after 5 s', async () => {
    const pending = createApiClient('http://api.test', hangingFetch).getSystemStatus();
    const assertion = expect(pending).rejects.toEqual(new ApiError(undefined, 'TIMEOUT'));

    await vi.advanceTimersByTimeAsync(STATUS_TIMEOUT_MS);

    await assertion;
    expect(STATUS_TIMEOUT_MS).toBe(5_000);
  });
});
