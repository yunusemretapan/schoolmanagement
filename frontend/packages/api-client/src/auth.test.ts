import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  ApiError,
  createApiClient,
  createMemoryTokenStore,
  LOGIN_TIMEOUT_MS,
  type TokenStore,
} from './index';

/** Spec 0003: token transport per client, refresh-and-retry, session expiry (AC-8), login timeout (AC-10). */

function json(body: unknown, status = 200, contentType = 'application/json'): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } });
}

function problem(status: number, code: string): Response {
  return json({ status, title: 'x', code }, status, 'application/problem+json');
}

const ME = { userId: '0190', fullName: 'Ayşe Yılmaz', institutionName: 'Örnek Koleji', roles: ['TEACHER'] };

type Route = (request: Request) => Response | Promise<Response>;

/** A fake backend: one handler per "METHOD path", every request recorded. */
function backend(routes: Record<string, Route>) {
  const requests: Request[] = [];
  const fetchImpl = vi.fn<typeof fetch>(async (input) => {
    const request = input as Request;
    requests.push(request.clone());
    const route = routes[`${request.method} ${new URL(request.url).pathname}`];
    if (route === undefined) {
      throw new Error(`unexpected ${request.method} ${request.url}`);
    }
    return route(request);
  });
  return { fetchImpl, requests };
}

describe('login', () => {
  it('web keeps the access token in memory and leaves the refresh token to the cookie', async () => {
    const tokens = createMemoryTokenStore();
    const { fetchImpl, requests } = backend({
      'POST /api/v1/auth/login': () => json({ accessToken: 'a1', expiresInSeconds: 900 }),
    });

    await createApiClient('http://api.test', fetchImpl, { clientType: 'WEB', tokens }).login(
      'veli@ornek.test',
      'pw',
    );

    expect(tokens.accessToken).toBe('a1');
    expect(await tokens.loadRefreshToken()).toBeNull();
    expect(await requests[0]?.json()).toEqual({
      identifier: 'veli@ornek.test',
      password: 'pw',
      clientType: 'WEB',
    });
  });

  it('mobile stores the refresh token from the body', async () => {
    const tokens = createMemoryTokenStore();
    const { fetchImpl } = backend({
      'POST /api/v1/auth/login': () => json({ accessToken: 'a1', expiresInSeconds: 900, refreshToken: 'r1' }),
    });

    await createApiClient('http://api.test', fetchImpl, { clientType: 'MOBILE', tokens }).login(
      'x@y.co',
      'pw',
    );

    expect(await tokens.loadRefreshToken()).toBe('r1');
  });

  it('fails with the server code on a refused login', async () => {
    const { fetchImpl } = backend({
      'POST /api/v1/auth/login': () => problem(401, 'IDENTITY_INVALID_CREDENTIALS'),
    });

    await expect(createApiClient('http://api.test', fetchImpl).login('x@y.co', 'pw')).rejects.toEqual(
      new ApiError(401, 'IDENTITY_INVALID_CREDENTIALS'),
    );
  });

  describe('timeout', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });
    afterEach(() => {
      vi.useRealTimers();
    });

    it('gives up after 10 s', async () => {
      const hanging: typeof fetch = (input) =>
        new Promise((_resolve, reject) => {
          const signal = (input as Request).signal;
          signal.addEventListener('abort', () => reject(signal.reason));
        });
      let settled = false;
      const pending = createApiClient('http://api.test', hanging)
        .login('x@y.co', 'pw')
        .finally(() => {
          settled = true;
        });
      const assertion = expect(pending).rejects.toEqual(new ApiError(undefined, 'TIMEOUT'));

      await vi.advanceTimersByTimeAsync(LOGIN_TIMEOUT_MS - 1);
      expect(settled).toBe(false);
      await vi.advanceTimersByTimeAsync(1);

      await assertion;
      expect(LOGIN_TIMEOUT_MS).toBe(10_000);
    });
  });
});

describe('signed-in calls', () => {
  function signedIn(clientType: 'WEB' | 'MOBILE', routes: Record<string, Route>) {
    const tokens: TokenStore = createMemoryTokenStore();
    tokens.accessToken = 'old-access';
    void tokens.saveRefreshToken(clientType === 'MOBILE' ? 'r1' : null);
    const onSessionExpired = vi.fn();
    const { fetchImpl, requests } = backend(routes);
    const api = createApiClient('http://api.test', fetchImpl, { clientType, tokens, onSessionExpired });
    return { api, tokens, onSessionExpired, requests };
  }

  it('sends the bearer token', async () => {
    const { api, requests } = signedIn('WEB', { 'GET /api/v1/me': () => json(ME) });

    await expect(api.getMe()).resolves.toEqual(ME);
    expect(requests[0]?.headers.get('Authorization')).toBe('Bearer old-access');
  });

  it('on 401 refreshes once and retries with the new token', async () => {
    const { api, tokens, requests } = signedIn('MOBILE', {
      'GET /api/v1/me': (request) =>
        request.headers.get('Authorization') === 'Bearer new-access'
          ? json(ME)
          : problem(401, 'AUTH_UNAUTHENTICATED'),
      'POST /api/v1/auth/refresh': () =>
        json({ accessToken: 'new-access', expiresInSeconds: 900, refreshToken: 'r2' }),
    });

    await expect(api.getMe()).resolves.toEqual(ME);
    expect(tokens.accessToken).toBe('new-access');
    expect(await tokens.loadRefreshToken()).toBe('r2');
    const refreshCall = requests.find((r) => r.url.endsWith('/refresh'));
    expect(await refreshCall?.json()).toEqual({ refreshToken: 'r1' });
  });

  it('web refresh sends no body: the browser sends the cookie', async () => {
    const { api, requests } = signedIn('WEB', {
      'GET /api/v1/me': (request) =>
        request.headers.get('Authorization') === 'Bearer new-access'
          ? json(ME)
          : problem(401, 'AUTH_UNAUTHENTICATED'),
      'POST /api/v1/auth/refresh': () => json({ accessToken: 'new-access', expiresInSeconds: 900 }),
    });

    await api.getMe();

    const refreshCall = requests.find((r) => r.url.endsWith('/refresh'));
    expect(await refreshCall?.text()).toBe('');
  });

  it('a failed refresh ends the session: tokens forgotten, callback called once', async () => {
    const { api, tokens, onSessionExpired } = signedIn('MOBILE', {
      'GET /api/v1/me': () => problem(401, 'AUTH_UNAUTHENTICATED'),
      'POST /api/v1/auth/refresh': () => problem(401, 'IDENTITY_SESSION_EXPIRED'),
    });

    await expect(api.getMe()).rejects.toEqual(new ApiError(401, 'IDENTITY_SESSION_EXPIRED'));
    expect(tokens.accessToken).toBeNull();
    expect(await tokens.loadRefreshToken()).toBeNull();
    expect(onSessionExpired).toHaveBeenCalledTimes(1);
  });

  it('parallel 401s share one refresh (a second one would look like token reuse)', async () => {
    let refreshes = 0;
    const { api } = signedIn('MOBILE', {
      'GET /api/v1/me': (request) =>
        request.headers.get('Authorization') === 'Bearer new-access'
          ? json(ME)
          : problem(401, 'AUTH_UNAUTHENTICATED'),
      'POST /api/v1/auth/refresh': async () => {
        refreshes += 1;
        return json({ accessToken: 'new-access', expiresInSeconds: 900, refreshToken: 'r2' });
      },
    });

    await Promise.all([api.getMe(), api.getMe(), api.getMe()]);

    expect(refreshes).toBe(1);
  });
});

describe('logout with an expired access token (review 1, H-1)', () => {
  it('web still asks the server (the cookie identifies the session) and forgets the access token', async () => {
    const tokens = createMemoryTokenStore();
    const { fetchImpl, requests } = backend({
      'POST /api/v1/auth/logout': () => new Response(null, { status: 204 }),
    });

    await createApiClient('http://api.test', fetchImpl, { clientType: 'WEB', tokens }).logout();

    expect(requests).toHaveLength(1);
    expect(requests[0]?.headers.get('Authorization')).toBeNull();
    expect(await requests[0]?.text()).toBe('');
  });

  it('mobile sends the stored refresh token and then forgets it', async () => {
    const tokens = createMemoryTokenStore();
    tokens.accessToken = 'expired-access';
    await tokens.saveRefreshToken('r1');
    const { fetchImpl, requests } = backend({
      'POST /api/v1/auth/logout': () => new Response(null, { status: 204 }),
    });

    await createApiClient('http://api.test', fetchImpl, { clientType: 'MOBILE', tokens }).logout();

    expect(await requests[0]?.json()).toEqual({ refreshToken: 'r1' });
    expect(requests[0]?.headers.get('Authorization')).toBe('Bearer expired-access');
    expect(await tokens.loadRefreshToken()).toBeNull();
    expect(tokens.accessToken).toBeNull();
  });
});

describe('an unreachable server is not the end of the session (review 1, L-2)', () => {
  const offline: typeof fetch = async () => {
    throw new TypeError('offline');
  };

  it('a signed-in call keeps the stored refresh token and fails as a network error', async () => {
    const tokens = createMemoryTokenStore();
    tokens.accessToken = 'old-access';
    await tokens.saveRefreshToken('r1');
    const onSessionExpired = vi.fn();
    const { fetchImpl } = backend({
      'GET /api/v1/me': () => problem(401, 'AUTH_UNAUTHENTICATED'),
      'POST /api/v1/auth/refresh': () => offline(new Request('http://x')),
    });

    await expect(
      createApiClient('http://api.test', fetchImpl, {
        clientType: 'MOBILE',
        tokens,
        onSessionExpired,
      }).getMe(),
    ).rejects.toEqual(new ApiError(undefined, undefined));
    expect(await tokens.loadRefreshToken()).toBe('r1');
    expect(onSessionExpired).not.toHaveBeenCalled();
  });

  it('a server error during refresh keeps the session too', async () => {
    const tokens = createMemoryTokenStore();
    await tokens.saveRefreshToken('r1');
    const { fetchImpl } = backend({ 'POST /api/v1/auth/refresh': () => problem(503, 'INTERNAL_ERROR') });

    await expect(
      createApiClient('http://api.test', fetchImpl, { clientType: 'MOBILE', tokens }).restoreSession(),
    ).rejects.toEqual(new ApiError(undefined, undefined));
    expect(await tokens.loadRefreshToken()).toBe('r1');
  });

  it('restoring offline rejects as a network error and keeps the stored token', async () => {
    const tokens = createMemoryTokenStore();
    await tokens.saveRefreshToken('r1');

    await expect(
      createApiClient('http://api.test', offline, { clientType: 'MOBILE', tokens }).restoreSession(),
    ).rejects.toEqual(new ApiError(undefined, undefined));
    expect(await tokens.loadRefreshToken()).toBe('r1');
  });
});

describe('web refresh across tabs (review 1, L-3)', () => {
  it('runs inside the shared refresh lock, one tab at a time', async () => {
    const order: string[] = [];
    let queue = Promise.resolve();
    const locks = {
      request<T>(name: string, callback: () => Promise<T>): Promise<T> {
        const run = queue.then(async () => {
          order.push(`enter ${name}`);
          const result = await callback();
          order.push(`leave ${name}`);
          return result;
        });
        queue = run.then(
          () => undefined,
          () => undefined,
        );
        return run;
      },
    };
    const { fetchImpl } = backend({
      'POST /api/v1/auth/refresh': () => json({ accessToken: 'a', expiresInSeconds: 900 }),
    });
    // Two tabs: two clients sharing one cookie jar (the browser's).
    const tabA = createApiClient('http://api.test', fetchImpl, {
      clientType: 'WEB',
      tokens: createMemoryTokenStore(),
      locks,
    });
    const tabB = createApiClient('http://api.test', fetchImpl, {
      clientType: 'WEB',
      tokens: createMemoryTokenStore(),
      locks,
    });

    await Promise.all([tabA.restoreSession(), tabB.restoreSession()]);

    expect(order).toEqual([
      'enter sos-refresh',
      'leave sos-refresh',
      'enter sos-refresh',
      'leave sos-refresh',
    ]);
  });

  it('mobile does not use the browser lock', async () => {
    const locks = { request: vi.fn() };
    const tokens = createMemoryTokenStore();
    await tokens.saveRefreshToken('r1');
    const { fetchImpl } = backend({
      'POST /api/v1/auth/refresh': () =>
        json({ accessToken: 'a', expiresInSeconds: 900, refreshToken: 'r2' }),
    });

    await createApiClient('http://api.test', fetchImpl, {
      clientType: 'MOBILE',
      tokens,
      locks,
    }).restoreSession();

    expect(locks.request).not.toHaveBeenCalled();
  });
});

describe('restoreSession and logout', () => {
  it('mobile without a stored refresh token has no session to restore and asks nothing', async () => {
    const { fetchImpl } = backend({});

    await expect(
      createApiClient('http://api.test', fetchImpl, {
        clientType: 'MOBILE',
        tokens: createMemoryTokenStore(),
      }).restoreSession(),
    ).resolves.toBe(false);
    expect(fetchImpl).not.toHaveBeenCalled();
  });

  it('web restores from the cookie', async () => {
    const tokens = createMemoryTokenStore();
    const { fetchImpl } = backend({
      'POST /api/v1/auth/refresh': () => json({ accessToken: 'a9', expiresInSeconds: 900 }),
    });

    await expect(
      createApiClient('http://api.test', fetchImpl, { clientType: 'WEB', tokens }).restoreSession(),
    ).resolves.toBe(true);
    expect(tokens.accessToken).toBe('a9');
  });

  it('logout tells the server and forgets every token, even offline', async () => {
    const tokens = createMemoryTokenStore();
    tokens.accessToken = 'a1';
    await tokens.saveRefreshToken('r1');
    const fetchImpl: typeof fetch = async () => {
      throw new TypeError('offline');
    };

    await createApiClient('http://api.test', fetchImpl, { clientType: 'MOBILE', tokens }).logout();

    expect(tokens.accessToken).toBeNull();
    expect(await tokens.loadRefreshToken()).toBeNull();
  });
});
