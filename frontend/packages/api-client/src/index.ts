import createClient from 'openapi-fetch';
import type { components, paths } from './schema';

export type SystemStatus = components['schemas']['SystemStatusResponse'];
export type ProblemDetails = components['schemas']['ProblemDetails'];

/** Spec 0001: no answer within 5 seconds means "unreachable". */
export const STATUS_TIMEOUT_MS = 5_000;

export class ApiError extends Error {
  constructor(
    readonly status: number | undefined,
    readonly code: string | undefined,
  ) {
    super(code ?? (status === undefined ? 'NETWORK_ERROR' : `HTTP_${status}`));
    this.name = 'ApiError';
  }
}

export interface ApiClient {
  getSystemStatus(signal?: AbortSignal): Promise<SystemStatus>;
}

export function createApiClient(baseUrl: string, fetchImpl: typeof fetch = globalThis.fetch): ApiClient {
  const client = createClient<paths>({ baseUrl, fetch: (request) => fetchImpl(request) });

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
  };
}

/** setTimeout-based (not AbortSignal.timeout) so tests can drive it with fake timers. */
async function withTimeout<T>(
  ms: number,
  outer: AbortSignal | undefined,
  run: (signal: AbortSignal) => Promise<T>,
): Promise<T> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(new ApiError(undefined, 'TIMEOUT')), ms);
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
