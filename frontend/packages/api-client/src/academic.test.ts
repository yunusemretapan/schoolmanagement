import { describe, expect, it, vi } from 'vitest';
import { ApiError, createApiClient, createMemoryTokenStore, ERROR_CODES } from './index';

/** Spec 0005: academic structure calls carry the session; refusals keep their code and field errors. */

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
const STRUCTURE = {
  activeTermId: ID,
  terms: [
    { id: ID, name: '2026–2027', startsOn: '2026-09-14', endsOn: '2027-06-26', active: true, inUse: false },
  ],
  campuses: [{ id: ID, name: 'Merkez', inUse: false }],
  gradeLevels: [],
};
const GROUP = { id: ID, name: '7/A', campusId: ID, termId: ID, gradeLevelId: ID };

function signedInClient(routes: Record<string, (request: Request) => Response>) {
  const tokens = createMemoryTokenStore();
  tokens.accessToken = 'a1';
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
  return { api: createApiClient('http://api.test', fetchImpl, { clientType: 'WEB', tokens }), requests };
}

const BASE = '/api/v1/academic-structure';

describe('academic structure calls', () => {
  it('reads the structure with the bearer token', async () => {
    const { api, requests } = signedInClient({ [`GET ${BASE}`]: () => json(STRUCTURE) });

    expect(await api.getAcademicStructure()).toEqual(STRUCTURE);
    expect(requests[0]!.headers.get('Authorization')).toBe('Bearer a1');
  });

  it('sends each change to its address with its body', async () => {
    const { api, requests } = signedInClient({
      [`POST ${BASE}/terms`]: () => json(STRUCTURE, 201),
      [`PUT ${BASE}/terms/${ID}`]: () => json(STRUCTURE),
      [`POST ${BASE}/terms/${ID}/activation`]: () => json(STRUCTURE),
      [`DELETE ${BASE}/terms/${ID}`]: () => json(STRUCTURE),
      [`POST ${BASE}/campuses`]: () => json(STRUCTURE, 201),
      [`PUT ${BASE}/campuses/${ID}`]: () => json(STRUCTURE),
      [`DELETE ${BASE}/campuses/${ID}`]: () => json(STRUCTURE),
      [`POST ${BASE}/grade-levels`]: () => json(STRUCTURE, 201),
      [`POST ${BASE}/grade-levels/standard`]: () => json(STRUCTURE, 201),
      [`PUT ${BASE}/grade-levels/${ID}`]: () => json(STRUCTURE),
      [`POST ${BASE}/grade-levels/${ID}/move`]: () => json(STRUCTURE),
      [`DELETE ${BASE}/grade-levels/${ID}`]: () => json(STRUCTURE),
    });
    const term = { name: '2026–2027', startsOn: '2026-09-14', endsOn: '2027-06-26' };

    await api.createTerm(term);
    await api.updateTerm(ID, term);
    await api.activateTerm(ID);
    await api.deleteTerm(ID);
    await api.createCampus('Kadıköy');
    await api.renameCampus(ID, 'Kadıköy Şubesi');
    await api.deleteCampus(ID);
    await api.createGradeLevel('LGS');
    await api.addStandardGradeLevels();
    await api.renameGradeLevel(ID, 'YKS');
    await api.moveGradeLevel(ID, 'UP');
    await api.deleteGradeLevel(ID);

    expect(await requests[0]!.json()).toEqual(term);
    expect(await requests[4]!.json()).toEqual({ name: 'Kadıköy' });
    expect(await requests[10]!.json()).toEqual({ direction: 'UP' });
    expect(requests.every((r) => r.headers.get('Authorization') === 'Bearer a1')).toBe(true);
  });

  it('lists class groups of a term and campus, and adds, changes and deletes one', async () => {
    const { api, requests } = signedInClient({
      [`GET ${BASE}/class-groups`]: () => json({ termId: ID, items: [GROUP] }),
      [`POST ${BASE}/class-groups`]: () => json(GROUP, 201),
      [`PUT ${BASE}/class-groups/${ID}`]: () => json(GROUP),
      [`DELETE ${BASE}/class-groups/${ID}`]: () => new Response(null, { status: 204 }),
    });

    expect((await api.listClassGroups({ termId: ID, campusId: ID })).items).toEqual([GROUP]);
    await api.listClassGroups({});
    await api.createClassGroup({ name: '7/A', gradeLevelId: ID });
    await api.updateClassGroup(ID, { name: '7/B', gradeLevelId: ID });
    await expect(api.deleteClassGroup(ID)).resolves.toBeUndefined();

    expect(new URL(requests[0]!.url).search).toBe(`?termId=${ID}&campusId=${ID}`);
    expect(new URL(requests[1]!.url).search).toBe('');
  });

  it('a rule refusal keeps its code; a field refusal keeps its fields', async () => {
    const { api } = signedInClient({
      [`POST ${BASE}/terms`]: () => problem(422, ERROR_CODES.termOverlap),
      [`POST ${BASE}/campuses`]: () => problem(400, ERROR_CODES.validationFailed, { name: 'SIZE' }),
    });

    await expect(api.createTerm({ name: 'Y', startsOn: '2027-06-26', endsOn: '2027-08-31' })).rejects.toEqual(
      new ApiError(422, 'ACADEMIC_TERM_OVERLAP'),
    );
    const error = await api.createCampus('x'.repeat(61)).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).fieldErrors).toEqual({ name: 'SIZE' });
  });
});
