import createClient from 'openapi-fetch';
import type { components, paths } from './schema';

export type SystemStatus = components['schemas']['SystemStatusResponse'];
export type ProblemDetails = components['schemas']['ProblemDetails'];
export type CurrentUser = components['schemas']['CurrentUserResponse'];
export type Role = components['schemas']['Role'];
export type ClientType = components['schemas']['ClientType'];
type TokenResponse = components['schemas']['TokenResponse'];
export type AccountStatus = components['schemas']['AccountStatus'];
export type StaffBody = components['schemas']['StaffBody'];
export type StaffSummary = components['schemas']['StaffSummary'];
export type StaffPage = components['schemas']['StaffPage'];
export type StaffDetail = components['schemas']['StaffDetail'];
export type InvitationLink = components['schemas']['InvitationLink'];
export type InvitationInfo = components['schemas']['InvitationInfo'];
export type AcceptedInvitation = components['schemas']['AcceptedInvitation'];
// spec 0005
export type AcademicStructure = components['schemas']['AcademicStructure'];
export type TermView = components['schemas']['TermView'];
export type CampusView = components['schemas']['CampusView'];
export type GradeLevelView = components['schemas']['GradeLevelView'];
export type TermBody = components['schemas']['TermBody'];
export type ClassGroupBody = components['schemas']['ClassGroupBody'];
export type ClassGroupView = components['schemas']['ClassGroupView'];
export type ClassGroupList = components['schemas']['ClassGroupList'];
export type MoveDirection = components['schemas']['MoveBody']['direction'];

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
  notFound: 'NOT_FOUND',
  forbidden: 'AUTH_FORBIDDEN',
  // spec 0004
  identifierTaken: 'IDENTITY_IDENTIFIER_TAKEN',
  identifierLocked: 'IDENTITY_IDENTIFIER_LOCKED',
  lastAdmin: 'IDENTITY_LAST_ADMIN',
  selfChange: 'IDENTITY_SELF_CHANGE',
  staffState: 'IDENTITY_STAFF_STATE',
  // spec 0005
  academicNameTaken: 'ACADEMIC_NAME_TAKEN',
  termOverlap: 'ACADEMIC_TERM_OVERLAP',
  lastCampus: 'ACADEMIC_LAST_CAMPUS',
  activeTerm: 'ACADEMIC_ACTIVE_TERM',
  inUse: 'ACADEMIC_IN_USE',
  gradeLevelLimit: 'ACADEMIC_GRADE_LEVEL_LIMIT',
  gradeLevelsNotEmpty: 'ACADEMIC_GRADE_LEVELS_NOT_EMPTY',
  cannotMove: 'ACADEMIC_CANNOT_MOVE',
  noTerm: 'ACADEMIC_NO_TERM',
} as const;

export class ApiError extends Error {
  constructor(
    readonly status: number | undefined,
    readonly code: string | undefined,
    /** VALIDATION_FAILED only: field → error code. */
    readonly fieldErrors: Readonly<Record<string, string>> = {},
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
  /**
   * Cross-tab lock for web refreshes (review 1, L-3): tabs share the refresh cookie, so two tabs refreshing at once
   * would present the same token twice — the server treats that as reuse and ends the session. Defaults to the
   * browser's Web Locks API when present.
   */
  locks?: RefreshLock;
}

/** The slice of the Web Locks API the client needs. */
export interface RefreshLock {
  request<T>(name: string, callback: () => Promise<T>): Promise<T>;
}

export const REFRESH_LOCK_NAME = 'sos-refresh';

function browserLocks(): RefreshLock | undefined {
  const locks = (globalThis.navigator as { locks?: RefreshLock } | undefined)?.locks;
  return locks === undefined ? undefined : { request: (name, callback) => locks.request(name, callback) };
}

export interface ApiClient {
  getSystemStatus(signal?: AbortSignal): Promise<SystemStatus>;
  login(identifier: string, password: string, signal?: AbortSignal): Promise<void>;
  /**
   * Opens the app on an existing session, if there is one: true / false. Rejects with a network ApiError when the
   * server cannot be reached; the stored session is then kept for the next try (review 1, L-2).
   */
  restoreSession(): Promise<boolean>;
  logout(): Promise<void>;
  getMe(signal?: AbortSignal): Promise<CurrentUser>;
  // --- staff management (spec 0004), administrators only ---
  listStaff(query: { q?: string; page?: number }, signal?: AbortSignal): Promise<StaffPage>;
  getStaff(staffId: string, signal?: AbortSignal): Promise<StaffDetail>;
  inviteStaff(body: StaffBody): Promise<InvitationLink>;
  updateStaff(staffId: string, body: StaffBody): Promise<StaffDetail>;
  renewInvitation(staffId: string): Promise<InvitationLink>;
  deactivateStaff(staffId: string): Promise<StaffDetail>;
  reactivateStaff(staffId: string): Promise<StaffDetail>;
  // --- invitation link (spec 0004), no session ---
  getInvitation(token: string, signal?: AbortSignal): Promise<InvitationInfo>;
  acceptInvitation(token: string, password: string): Promise<AcceptedInvitation>;
  // spec 0005 — academic structure (administrators)
  getAcademicStructure(signal?: AbortSignal): Promise<AcademicStructure>;
  createCampus(name: string): Promise<AcademicStructure>;
  renameCampus(campusId: string, name: string): Promise<AcademicStructure>;
  deleteCampus(campusId: string): Promise<AcademicStructure>;
  createTerm(body: TermBody): Promise<AcademicStructure>;
  updateTerm(termId: string, body: TermBody): Promise<AcademicStructure>;
  activateTerm(termId: string): Promise<AcademicStructure>;
  deleteTerm(termId: string): Promise<AcademicStructure>;
  createGradeLevel(name: string): Promise<AcademicStructure>;
  addStandardGradeLevels(): Promise<AcademicStructure>;
  renameGradeLevel(gradeLevelId: string, name: string): Promise<AcademicStructure>;
  moveGradeLevel(gradeLevelId: string, direction: MoveDirection): Promise<AcademicStructure>;
  deleteGradeLevel(gradeLevelId: string): Promise<AcademicStructure>;
  listClassGroups(
    query: { termId?: string; campusId?: string },
    signal?: AbortSignal,
  ): Promise<ClassGroupList>;
  createClassGroup(body: ClassGroupBody): Promise<ClassGroupView>;
  updateClassGroup(classGroupId: string, body: ClassGroupBody): Promise<ClassGroupView>;
  deleteClassGroup(classGroupId: string): Promise<void>;
}

export function createApiClient(
  baseUrl: string,
  fetchImpl: typeof fetch = globalThis.fetch,
  auth: AuthOptions = { clientType: 'WEB', tokens: createMemoryTokenStore() },
): ApiClient {
  const client = createClient<paths>({ baseUrl, fetch: (request) => fetchImpl(request) });
  const { clientType, tokens } = auth;
  const locks = clientType === 'WEB' ? (auth.locks ?? browserLocks()) : undefined;
  /** ok: new tokens · refused: the server ended the session · unreachable: no answer; tokens kept (review 1, L-2). */
  type RefreshResult = 'ok' | 'refused' | 'unreachable';
  let refreshing: Promise<RefreshResult> | null = null;

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
  function refresh(): Promise<RefreshResult> {
    refreshing ??= (async (): Promise<RefreshResult> => {
      try {
        const refreshToken = clientType === 'MOBILE' ? await tokens.loadRefreshToken() : null;
        if (clientType === 'MOBILE' && refreshToken === null) {
          return 'refused';
        }
        const send = () =>
          withTimeout(LOGIN_TIMEOUT_MS, undefined, (signal) =>
            client.POST('/api/v1/auth/refresh', {
              body: refreshToken === null ? undefined : { refreshToken },
              signal,
            }),
          );
        const { data, response } =
          locks === undefined ? await send() : await locks.request(REFRESH_LOCK_NAME, send);
        if (data !== undefined) {
          await store(data);
          return 'ok';
        }
        if (response.status === 401) {
          await forget();
          return 'refused';
        }
        return 'unreachable'; // a server error is not a verdict on the session
      } catch {
        return 'unreachable';
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
    if (result.response.status === 401) {
      const refreshed = await refresh();
      if (refreshed === 'unreachable') {
        throw new ApiError(undefined, undefined);
      }
      if (refreshed === 'ok') {
        result = await call(bearer());
      }
    }
    if (result.response.status === 401) {
      await forget();
      auth.onSessionExpired?.();
      throw new ApiError(401, ERROR_CODES.sessionExpired);
    }
    if (result.data === undefined && result.response.status !== 204) {
      throw problem(result.response.status, result.error);
    }
    return result.data as T;
  }

  /** A signed-in call with the usual time limit. */
  function signedIn<T>(
    signal: AbortSignal | undefined,
    call: (
      headers: Record<string, string>,
      signal: AbortSignal,
    ) => Promise<{ data?: T; error?: unknown; response: Response }>,
  ): Promise<T> {
    return withTimeout(LOGIN_TIMEOUT_MS, signal, (combined) =>
      authorized((headers) => call(headers, combined)),
    );
  }

  /** A call without a session (the invitation link): no token, no refresh. */
  function open<T>(
    signal: AbortSignal | undefined,
    call: (signal: AbortSignal) => Promise<{ data?: T; error?: unknown; response: Response }>,
  ): Promise<T> {
    return withTimeout(LOGIN_TIMEOUT_MS, signal, async (combined) => {
      const { data, error, response } = await call(combined);
      if (data === undefined) {
        throw problem(response.status, error);
      }
      return data;
    });
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

    async restoreSession() {
      const result = await refresh();
      if (result === 'unreachable') {
        throw new ApiError(undefined, undefined);
      }
      return result === 'ok';
    },

    /** Always asks the server, even with an expired access token (review 1, H-1): the refresh token proves the session. */
    async logout() {
      try {
        const refreshToken = clientType === 'MOBILE' ? await tokens.loadRefreshToken() : null;
        await client.POST('/api/v1/auth/logout', {
          headers: bearer(),
          body: refreshToken === null ? undefined : { refreshToken },
        });
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

    listStaff({ q, page }, signal) {
      return signedIn(signal, (headers, combined) =>
        client.GET('/api/v1/staff', {
          headers,
          params: { query: { ...(q ? { q } : {}), page: page ?? 0 } },
          signal: combined,
        }),
      );
    },

    getStaff(staffId, signal) {
      return signedIn(signal, (headers, combined) =>
        client.GET('/api/v1/staff/{staffId}', { headers, params: { path: { staffId } }, signal: combined }),
      );
    },

    inviteStaff(body) {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/staff', { headers, body, signal: combined }),
      );
    },

    updateStaff(staffId, body) {
      return signedIn(undefined, (headers, combined) =>
        client.PUT('/api/v1/staff/{staffId}', {
          headers,
          params: { path: { staffId } },
          body,
          signal: combined,
        }),
      );
    },

    renewInvitation(staffId) {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/staff/{staffId}/invitation', {
          headers,
          params: { path: { staffId } },
          signal: combined,
        }),
      );
    },

    deactivateStaff(staffId) {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/staff/{staffId}/deactivation', {
          headers,
          params: { path: { staffId } },
          signal: combined,
        }),
      );
    },

    reactivateStaff(staffId) {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/staff/{staffId}/reactivation', {
          headers,
          params: { path: { staffId } },
          signal: combined,
        }),
      );
    },

    getInvitation(token, signal) {
      return open(signal, (combined) =>
        client.POST('/api/v1/invitations/lookup', { body: { token }, signal: combined }),
      );
    },

    acceptInvitation(token, password) {
      return open(undefined, (combined) =>
        client.POST('/api/v1/invitations/accept', { body: { token, password }, signal: combined }),
      );
    },

    getAcademicStructure(signal) {
      return signedIn(signal, (headers, combined) =>
        client.GET('/api/v1/academic-structure', { headers, signal: combined }),
      );
    },

    createCampus(name) {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/academic-structure/campuses', { headers, body: { name }, signal: combined }),
      );
    },

    renameCampus(campusId, name) {
      return signedIn(undefined, (headers, combined) =>
        client.PUT('/api/v1/academic-structure/campuses/{campusId}', {
          headers,
          params: { path: { campusId } },
          body: { name },
          signal: combined,
        }),
      );
    },

    deleteCampus(campusId) {
      return signedIn(undefined, (headers, combined) =>
        client.DELETE('/api/v1/academic-structure/campuses/{campusId}', {
          headers,
          params: { path: { campusId } },
          signal: combined,
        }),
      );
    },

    createTerm(body) {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/academic-structure/terms', { headers, body, signal: combined }),
      );
    },

    updateTerm(termId, body) {
      return signedIn(undefined, (headers, combined) =>
        client.PUT('/api/v1/academic-structure/terms/{termId}', {
          headers,
          params: { path: { termId } },
          body,
          signal: combined,
        }),
      );
    },

    activateTerm(termId) {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/academic-structure/terms/{termId}/activation', {
          headers,
          params: { path: { termId } },
          signal: combined,
        }),
      );
    },

    deleteTerm(termId) {
      return signedIn(undefined, (headers, combined) =>
        client.DELETE('/api/v1/academic-structure/terms/{termId}', {
          headers,
          params: { path: { termId } },
          signal: combined,
        }),
      );
    },

    createGradeLevel(name) {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/academic-structure/grade-levels', { headers, body: { name }, signal: combined }),
      );
    },

    addStandardGradeLevels() {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/academic-structure/grade-levels/standard', { headers, signal: combined }),
      );
    },

    renameGradeLevel(gradeLevelId, name) {
      return signedIn(undefined, (headers, combined) =>
        client.PUT('/api/v1/academic-structure/grade-levels/{gradeLevelId}', {
          headers,
          params: { path: { gradeLevelId } },
          body: { name },
          signal: combined,
        }),
      );
    },

    moveGradeLevel(gradeLevelId, direction) {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/academic-structure/grade-levels/{gradeLevelId}/move', {
          headers,
          params: { path: { gradeLevelId } },
          body: { direction },
          signal: combined,
        }),
      );
    },

    deleteGradeLevel(gradeLevelId) {
      return signedIn(undefined, (headers, combined) =>
        client.DELETE('/api/v1/academic-structure/grade-levels/{gradeLevelId}', {
          headers,
          params: { path: { gradeLevelId } },
          signal: combined,
        }),
      );
    },

    listClassGroups({ termId, campusId }, signal) {
      return signedIn(signal, (headers, combined) =>
        client.GET('/api/v1/academic-structure/class-groups', {
          headers,
          params: { query: { ...(termId ? { termId } : {}), ...(campusId ? { campusId } : {}) } },
          signal: combined,
        }),
      );
    },

    createClassGroup(body) {
      return signedIn(undefined, (headers, combined) =>
        client.POST('/api/v1/academic-structure/class-groups', { headers, body, signal: combined }),
      );
    },

    updateClassGroup(classGroupId, body) {
      return signedIn(undefined, (headers, combined) =>
        client.PUT('/api/v1/academic-structure/class-groups/{classGroupId}', {
          headers,
          params: { path: { classGroupId } },
          body,
          signal: combined,
        }),
      );
    },

    async deleteClassGroup(classGroupId) {
      await signedIn(undefined, (headers, combined) =>
        client.DELETE('/api/v1/academic-structure/class-groups/{classGroupId}', {
          headers,
          params: { path: { classGroupId } },
          signal: combined,
        }),
      );
    },
  };
}

function problem(status: number, error: unknown): ApiError {
  const details = error as ProblemDetails | undefined;
  return new ApiError(status, details?.code, details?.errors ?? {});
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
