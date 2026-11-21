import type { AcademicStructure } from '@schoolos/api-client';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useApi } from '../../api/ApiProvider';
import { CampusesSection } from './CampusesSection';
import { ClassGroupsSection } from './ClassGroupsSection';
import { GradeLevelsSection } from './GradeLevelsSection';
import { SECONDARY } from './academicUi';
import { TermsSection } from './TermsSection';

export const ACADEMIC_PATH = '/akademik-yapi';
const STRUCTURE_KEY = ['academic', 'structure'] as const;

const LINK =
  'font-medium text-brand-ink underline outline-focus focus-visible:outline-[length:var(--sos-border-thick)]';

/**
 * Spec 0005: the institution's academic structure on one page — terms, campuses, grade levels, class groups (plan
 * R7). States: loading, ready, error with retry; each section has its own empty guidance.
 */
export function AcademicStructureScreen() {
  const { t } = useTranslation();
  const api = useApi();
  const queryClient = useQueryClient();
  const structure = useQuery<AcademicStructure>({
    queryKey: STRUCTURE_KEY,
    queryFn: ({ signal }) => api.getAcademicStructure(signal),
    retry: false,
    refetchOnWindowFocus: false,
  });

  /** Every structure change answers with the whole structure: show it, and reload the class groups. */
  function apply(next: AcademicStructure) {
    queryClient.setQueryData(STRUCTURE_KEY, next);
    void queryClient.invalidateQueries({ queryKey: ['academic', 'classGroups'] });
  }

  return (
    <main className="min-h-dvh bg-canvas p-lg font-sans text-ink">
      <div className="mx-auto flex max-w-content flex-col gap-lg">
        <h1 className="text-xl font-bold">{t('academic.title')}</h1>

        {structure.isPending && (
          <p role="status" aria-live="polite" className="text-md text-ink-muted">
            {t('home.loading')}
          </p>
        )}
        {structure.isError && (
          <div className="flex flex-col gap-sm rounded-lg border border-line bg-surface p-lg">
            <p role="alert" className="text-md font-semibold text-danger">
              {t('academic.loadError')}
            </p>
            <button type="button" onClick={() => void structure.refetch()} className={SECONDARY}>
              {t('home.retry')}
            </button>
          </div>
        )}
        {structure.isSuccess && (
          <>
            <TermsSection structure={structure.data} apply={apply} />
            <CampusesSection structure={structure.data} apply={apply} />
            <GradeLevelsSection structure={structure.data} apply={apply} />
            <ClassGroupsSection structure={structure.data} />
          </>
        )}

        <Link to="/" className={`${LINK} text-sm`}>
          {t('staff.backHome')}
        </Link>
      </div>
    </main>
  );
}
