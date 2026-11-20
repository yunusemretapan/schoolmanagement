import { describe, expect, it, vi } from 'vitest';
import { ApiError, createApiClient, createMemoryTokenStore, ERROR_CODES } from './index';

/** Spec 0004: staff calls carry the session, the invitation calls do not; errors keep their code and fields. */

function json(body: unknown, status = 200, contentType = 'application/json'): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } });
}

function problem(status: number, code: string, errors?: Record<string, string>): Response {
  return json(
    { status, title: 'x', code, ...(errors ? { errors } : {}) },
    status,
    'application/problem+json',
  );
}

const ID = '01900000-0000-7000-8000-000000000001';
const DETAIL = {
  id: ID,
  fullName: 'Ali',
  email: 'ali@ornek.test',
  phone: null,
  roles: ['TEACHER'],
  status: 'ACTIVE',
  invitationExpiresAt: null,
  self: false,
  lastActiveAdmin: false,
};
const LINK = { staffId: ID, token: 'inv.secret', expiresAt: '2026-11-23T09:00:00Z' };

function backend(routes: Record<string, (request: Request) => Response>) {
  const requests: Request[] = [];
  const fetchImpl = vi.fn<typeof fetch>(async (input) => {
    const request = input as Request;
    requests.push(request.clone());
    const url = new URL(request.url);
    const route = routes[`${request.method} ${url.pathname}`];
    if (route === undefined) {
      throw new Error(`unexpected ${request.method} ${request.url}`);
    }
    return route(request);
  });
  return { fetchImpl, requests };
}

function signedInClient(routes: Record<string, (request: Request) => Response>) {
  const tokens = createMemoryTokenStore();
  tokens.accessToken = 'a1';
  const { fetchImpl, requests } = backend(routes);
  return { api: createApiClient('http://api.test', fetchImpl, { clientType: 'WEB', tokens }), requests };
}

describe('staff calls', () => {
  it('list sends the search and page with the bearer token', async () => {
    const { api, requests } = signedInClient({
      'GET /api/v1/staff': () => json({ items: [], page: 1, pageSize: 50, total: 51 }),
    });

    const page = await api.listStaff({ q: '0532 123', page: 1 });

    expect(page.total).toBe(51);
    const url = new URL(requests[0]!.url);
    expect(url.searchParams.get('q')).toBe('0532 123');
    expect(url.searchParams.get('page')).toBe('1');
    expect(requests[0]!.headers.get('Authorization')).toBe('Bearer a1');
  });

  it('an empty search is not sent', async () => {
    const { api, requests } = signedInClient({
      'GET /api/v1/staff': () => json({ items: [], page: 0, pageSize: 50, total: 0 }),
    });

    await api.listStaff({ q: '' });

    expect(new URL(requests[0]!.url).searchParams.has('q')).toBe(false);
  });

  it('invite, update, renew, deactivate and reactivate reach their endpoints', async () => {
    const { api, requests } = signedInClient({
      'POST /api/v1/staff': () => json(LINK, 201),
      [`PUT /api/v1/staff/${ID}`]: () => json(DETAIL),
      [`POST /api/v1/staff/${ID}/invitation`]: () => json(LINK),
      [`POST /api/v1/staff/${ID}/deactivation`]: () => json({ ...DETAIL, status: 'INACTIVE' }),
      [`POST /api/v1/staff/${ID}/reactivation`]: () => json(DETAIL),
      [`GET /api/v1/staff/${ID}`]: () => json(DETAIL),
    });
    const body = { fullName: 'Ali', email: 'ali@ornek.test', phone: null, roles: ['TEACHER' as const] };

    expect(await api.inviteStaff(body)).toEqual(LINK);
    expect(await api.updateStaff(ID, body)).toEqual(DETAIL);
    expect(await api.renewInvitation(ID)).toEqual(LINK);
    expect((await api.deactivateStaff(ID)).status).toBe('INACTIVE');
    expect(await api.reactivateStaff(ID)).toEqual(DETAIL);
    expect(await api.getStaff(ID)).toEqual(DETAIL);
    expect(await requests[0]!.json()).toEqual(body);
  });

  it('a refused rule keeps its code; a validation error keeps its fields', async () => {
    const { api } = signedInClient({
      [`POST /api/v1/staff/${ID}/deactivation`]: () => problem(422, ERROR_CODES.lastAdmin),
      'POST /api/v1/staff': () => problem(400, ERROR_CODES.validationFailed, { roles: 'ROLE_REQUIRED' }),
    });

    await expect(api.deactivateStaff(ID)).rejects.toMatchObject({ status: 422, code: 'IDENTITY_LAST_ADMIN' });
    const error = await api.inviteStaff({ fullName: 'A', roles: [] }).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).fieldErrors).toEqual({ roles: 'ROLE_REQUIRED' });
  });
});

describe('invitation link calls', () => {
  it('send the token in the body and no bearer token, even when signed in', async () => {
    const { api, requests } = signedInClient({
      'POST /api/v1/invitations/lookup': () => json({ fullName: 'Ali', institutionName: 'Örnek Koleji' }),
      'POST /api/v1/invitations/accept': () => json({ identifier: 'ali@ornek.test' }),
    });

    expect(await api.getInvitation('inv.secret')).toEqual({
      fullName: 'Ali',
      institutionName: 'Örnek Koleji',
    });
    expect(await api.acceptInvitation('inv.secret', 'Yeni-Sifre-2026')).toEqual({
      identifier: 'ali@ornek.test',
    });

    expect(requests[0]!.headers.get('Authorization')).toBeNull();
    expect(await requests[0]!.json()).toEqual({ token: 'inv.secret' });
    expect(new URL(requests[1]!.url).search).toBe('');
    expect(await requests[1]!.json()).toEqual({ token: 'inv.secret', password: 'Yeni-Sifre-2026' });
  });

  it('an invalid link is a 404 and does not end any session', async () => {
    const onSessionExpired = vi.fn();
    const { fetchImpl } = backend({
      'POST /api/v1/invitations/lookup': () => problem(404, ERROR_CODES.notFound),
    });
    const api = createApiClient('http://api.test', fetchImpl, {
      clientType: 'WEB',
      tokens: createMemoryTokenStore(),
      onSessionExpired,
    });

    await expect(api.getInvitation('x')).rejects.toMatchObject({ status: 404, code: 'NOT_FOUND' });
    expect(onSessionExpired).not.toHaveBeenCalled();
  });
});
