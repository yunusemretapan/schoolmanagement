import type { AcademicStructure, ClassGroupList, CurrentUser } from '@schoolos/api-client';
import { fakeApi, ME } from '../../test/renderApp';

/** Test data for the academic structure screen (spec 0005). */
export const ADMIN: CurrentUser = { ...ME, roles: ['ADMIN'] };

export const id = (n: number) => `01900000-0000-7000-8000-${String(n).padStart(12, '0')}`;

export const TERM = id(1);
export const NEXT_TERM = id(2);
export const MERKEZ = id(10);
export const KADIKOY = id(11);
export const SEVENTH = id(20);
export const EIGHTH = id(21);

export function structure(overrides: Partial<AcademicStructure> = {}): AcademicStructure {
  return {
    activeTermId: TERM,
    terms: [
      {
        id: NEXT_TERM,
        name: '2027–2028',
        startsOn: '2027-09-13',
        endsOn: '2028-06-24',
        active: false,
        inUse: false,
      },
      {
        id: TERM,
        name: '2026–2027',
        startsOn: '2026-09-14',
        endsOn: '2027-06-26',
        active: true,
        inUse: true,
      },
    ],
    campuses: [{ id: MERKEZ, name: 'Merkez', inUse: true }],
    gradeLevels: [
      { id: SEVENTH, name: '7. sınıf', position: 1, inUse: true },
      { id: EIGHTH, name: '8. sınıf', position: 2, inUse: false },
    ],
    ...overrides,
  };
}

export const EMPTY: AcademicStructure = {
  activeTermId: null,
  terms: [],
  campuses: [{ id: MERKEZ, name: 'Örnek Koleji', inUse: false }],
  gradeLevels: [],
};

export function groups(names: string[] = ['7/A']): ClassGroupList {
  return {
    termId: TERM,
    items: names.map((name, i) => ({
      id: id(100 + i),
      name,
      campusId: MERKEZ,
      termId: TERM,
      gradeLevelId: SEVENTH,
    })),
  };
}

export const asAdmin = (overrides: Parameters<typeof fakeApi>[0] = {}) =>
  fakeApi({
    restoreSession: async () => true,
    getMe: async () => ADMIN,
    getAcademicStructure: async () => structure(),
    listClassGroups: async () => groups(),
    ...overrides,
  });

export const PATH = '/akademik-yapi';
