import createClient from 'openapi-fetch';
import type { components, paths } from './schema';

export type SystemStatus = components['schemas']['SystemStatusResponse'];
export type ProblemDetails = components['schemas']['ProblemDetails'];
export type CurrentUser = components['schemas']['CurrentUserResponse'];
export type Role = components['schemas']['Role'];
export type ClientType = components['schemas']['ClientType'];
type TokenResponse = components['schemas']['TokenResponse'];

/** Spec 0001: no answer within 5 seconds means "unreachable". */
export const STATUS_TIMEOUT_MS = 5_000;

/** Spec 0003, AC-10: the login form gives up after 10 seconds. Other signed-in calls use the same limit. */
export const LOGIN_TIMEOUT_MS = 10_000;

/** Codes the apps react to (backend IdentityErrorCodes / AUTH_*). */
export const ERROR_CODES = {
  invalidCredentials: 'IDENTITY_INVALID_CREDENTIALS',
  sessionExpired: 'IDENTITY_SESSION_EXPIRED',
  rateLimited: 'IDENTITY_RATE_LIMITED',
  validationFailed: 'VALIDATION_FAILED',
  timeout: 'TIMEOUT',
} as const;

export class ApiError extends Error {
  constructor(
    readonly status: number | undefined,
    readonly code: string | undefined,
  ) {
    super(code ?? (status === undefined ? 'NETWORK_ERROR' : `HTTP_${status}`));
    this.name = 'ApiError';
  }
}

/**
 * Where tokens live (plan 0003, R2). The access token is always memory-only. The refresh token: web → none here (an
 * HttpOnly cookie the browser sends itself); mobile → SecureStore; Expo web → memory.
 */
export interface TokenStore {
  accessToken: string | null;
  loadRefreshToken(): Promise<string | null>;
  saveRefreshToken(token: string | null): Promise<void>;
}

export function createMemoryTokenStore(): TokenStore {
  let refresh: string | null = null;
  return {
    accessToken: null,
    loadRefreshToken: async () => refresh,
    saveRefreshToken: async (token) => {
      refresh = token;
    },
  };
}

export interface AuthOptions {
  clientType: ClientType;
  tokens: TokenStore;
  /** Called once when a signed-in call finds the session over (expired, ended elsewhere, or reused). */
  onSessionExpired?: () => void;
}

export interface ApiClient {
  getSystemStatus(signal?: AbortSignal): Promise<SystemStatus>;
  login(identifier: string, password: string, signal?: AbortSignal): Promise<void>;
  /** Opens the app on an existing session, if there is one. */
  restoreSession(): Promise<boolean>;
  logout(): Promise<void>;
  getMe(signal?: AbortSignal): Promise<CurrentUser>;
}

export function createApiClient(
  baseUrl: string,
  fetchImpl: typeof fetch = globalThis.fetch,
  auth: AuthOptions = { clientType: 'WEB', tokens: createMemoryTokenStore() },
): ApiClient {
  const client = createClient<paths>({ baseUrl, fetch: (request) => fetchImpl(request) });
  const { clientType, tokens } = auth;
  let refreshing: Promise<boolean> | null = null;

  async function store(response: TokenResponse): Promise<void> {
    tokens.accessToken = response.accessToken;
    if (clientType === 'MOBILE') {
      await tokens.saveRefreshToken(response.refreshToken ?? null);
    }
  }

  async function forget(): Promise<void> {
    tokens.accessToken = null;
    await tokens.saveRefreshToken(null);
  }

  /**
   * One refresh at a time: two parallel refreshes would present the same token twice, which the server treats as
   * reuse and answers by ending the session (spec 0003, Req. 6).
   */
  function refresh(): Promise<boolean> {
    refreshing ??= (async () => {
      try {
        const refreshToken = clientType === 'MOBILE' ? await tokens.loadRefreshToken() : null;
        if (clientType === 'MOBILE' && refreshToken === null) {
          return false;
        }
        const { data } = await client.POST('/api/v1/auth/refresh', {
          body: refreshToken === null ? undefined : { refreshToken },
        });
        if (data === undefined) {
          await forget();
          return false;
        }
        await store(data);
        return true;
      } catch {
        return false;
      } finally {
        refreshing = null;
      }
    })();
    return refreshing;
  }

  function bearer(): Record<string, string> {
    return tokens.accessToken === null ? {} : { Authorization: `Bearer ${tokens.accessToken}` };
  }

  /** A signed-in call: on 401, refresh once and retry; if that fails, the session is over. */
  async function authorized<T>(
    call: (headers: Record<string, string>) => Promise<{ data?: T; error?: unknown; response: Response }>,
  ): Promise<T> {
    let result = await call(bearer());
    if (result.response.status === 401 && (await refresh())) {
      result = await call(bearer());
    }
    if (result.response.status === 401) {
      await forget();
      auth.onSessionExpired?.();
      throw new ApiError(401, ERROR_CODES.sessionExpired);
    }
    if (result.data === undefined && result.response.status !== 204) {
      throw new ApiError(result.response.status, (result.error as ProblemDetails | undefined)?.code);
    }
    return result.data as T;
  }

  return {
    async getSystemStatus(signal) {
      return withTimeout(STATUS_TIMEOUT_MS, signal, async (combined) => {
        const { data, error, response } = await client.GET('/api/v1/system/status', { signal: combined });
        if (data === undefined) {
          throw new ApiError(response.status, (error as ProblemDetails | undefined)?.code);
        }
        return data;
      });
    },

    async login(identifier, password, signal) {
      await withTimeout(LOGIN_TIMEOUT_MS, signal, async (combined) => {
        const { data, error, response } = await client.POST('/api/v1/auth/login', {
          body: { identifier, password, clientType },
          signal: combined,
        });
        if (data === undefined) {
          throw new ApiError(response.status, (error as ProblemDetails | undefined)?.code);
        }
        await store(data);
      });
    },

    restoreSession: () => refresh(),

    async logout() {
      try {
        if (tokens.accessToken !== null) {
          await client.POST('/api/v1/auth/logout', { headers: bearer() });
        }
      } catch {
        // Signing out locally must work offline too; the server session then simply expires.
      } finally {
        await forget();
      }
    },

    async getMe(signal) {
      return withTimeout(LOGIN_TIMEOUT_MS, signal, (combined) =>
        authorized((headers) => client.GET('/api/v1/me', { headers, signal: combined })),
      );
    },
  };
}

/** setTimeout-based (not AbortSignal.timeout) so tests can drive it with fake timers. */
async function withTimeout<T>(
  ms: number,
  outer: AbortSignal | undefined,
  run: (signal: AbortSignal) => Promise<T>,
): Promise<T> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(new ApiError(undefined, ERROR_CODES.timeout)), ms);
  const forward = () => controller.abort(outer?.reason);
  outer?.addEventListener('abort', forward, { once: true });
  try {
    return await run(controller.signal);
  } catch (cause) {
    if (controller.signal.aborted && controller.signal.reason instanceof ApiError) {
      throw controller.signal.reason;
    }
    if (cause instanceof ApiError) {
      throw cause;
    }
    throw new ApiError(undefined, undefined);
  } finally {
    clearTimeout(timer);
    outer?.removeEventListener('abort', forward);
  }
}
