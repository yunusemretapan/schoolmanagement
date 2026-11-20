import type { StaffPage } from '@schoolos/api-client';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useApi } from '../../api/ApiProvider';
import { INPUT, SECONDARY } from './StaffFields';

const LINK =
  'font-medium text-brand-ink underline outline-focus focus-visible:outline-[length:var(--sos-border-thick)]';

/**
 * Spec 0004, Req. 3 / AC-3: the institution's staff with roles and status, searched by name, e-mail or phone,
 * 50 per page. States: loading, ready, no match, error (with retry).
 */
export function StaffListScreen() {
  const { t } = useTranslation();
  const api = useApi();
  const searchId = useId();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const staff = useQuery<StaffPage>({
    queryKey: ['staff', 'list', q.trim(), page],
    queryFn: ({ signal }) => api.listStaff({ q: q.trim(), page }, signal),
    placeholderData: keepPreviousData,
    retry: false,
    refetchOnWindowFocus: false,
  });
  const pages = staff.data ? Math.max(1, Math.ceil(staff.data.total / staff.data.pageSize)) : 1;

  return (
    <main className="min-h-dvh bg-canvas p-lg font-sans text-ink">
      <div className="mx-auto flex max-w-content flex-col gap-lg">
        <header className="flex flex-wrap items-center gap-sm">
          <h1 className="grow text-xl font-bold">{t('staff.title')}</h1>
          <Link
            to="/personel/davet"
            className={`${SECONDARY} grid place-items-center bg-brand text-on-brand`}
          >
            {t('staff.invite')}
          </Link>
        </header>

        <div className="flex flex-col gap-xs">
          <label htmlFor={searchId} className="text-sm font-medium">
            {t('staff.search')}
          </label>
          <input
            id={searchId}
            type="search"
            value={q}
            onChange={(event) => {
              setQ(event.target.value);
              setPage(0);
            }}
            className={INPUT}
          />
        </div>

        <section
          className="rounded-lg border border-line bg-surface p-lg shadow-card"
          aria-busy={staff.isFetching}
        >
          {staff.isPending && (
            <p role="status" aria-live="polite" className="text-md text-ink-muted">
              {t('home.loading')}
            </p>
          )}
          {staff.isError && (
            <div className="flex flex-col gap-sm">
              <p role="alert" className="text-md font-semibold text-danger">
                {t('staff.listError')}
              </p>
              <button type="button" onClick={() => void staff.refetch()} className={SECONDARY}>
                {t('home.retry')}
              </button>
            </div>
          )}
          {staff.isSuccess && staff.data.items.length === 0 && (
            <p role="status" className="text-md text-ink-muted">
              {t('staff.noMatch')}
            </p>
          )}
          {staff.isSuccess && staff.data.items.length > 0 && (
            <table className="w-full text-left text-md">
              <thead className="text-sm text-ink-muted">
                <tr>
                  <th scope="col" className="py-xs">
                    {t('staff.fields.fullName')}
                  </th>
                  <th scope="col" className="py-xs">
                    {t('staff.contact')}
                  </th>
                  <th scope="col" className="py-xs">
                    {t('staff.fields.roles')}
                  </th>
                  <th scope="col" className="py-xs">
                    {t('staff.statusLabel')}
                  </th>
                </tr>
              </thead>
              <tbody>
                {staff.data.items.map((person) => (
                  <tr key={person.id} className="border-t border-line">
                    <td className="py-sm">
                      <Link to={`/personel/${person.id}`} className={LINK}>
                        {person.fullName}
                      </Link>
                    </td>
                    <td className="py-sm">{[person.email, person.phone].filter(Boolean).join(' · ')}</td>
                    <td className="py-sm">
                      <ul className="flex flex-wrap gap-xs">
                        {person.roles.map((role) => (
                          <li
                            key={role}
                            className="rounded-pill bg-brand-subtle px-sm text-sm font-medium text-brand-ink"
                          >
                            {t(`roles.${role}`)}
                          </li>
                        ))}
                      </ul>
                    </td>
                    <td className="py-sm">
                      <span className="rounded-pill border border-line-strong px-sm text-sm font-medium">
                        {t(`staff.status.${person.status}`)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        {staff.isSuccess && pages > 1 && (
          <nav aria-label={t('staff.pagination')} className="flex items-center justify-between gap-sm">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className={SECONDARY}
            >
              {t('staff.previous')}
            </button>
            <p className="text-sm text-ink-muted">{t('staff.pageOf', { page: page + 1, pages })}</p>
            <button
              type="button"
              disabled={page + 1 >= pages}
              onClick={() => setPage((p) => p + 1)}
              className={SECONDARY}
            >
              {t('staff.next')}
            </button>
          </nav>
        )}

        <Link to="/" className={`${LINK} text-sm`}>
          {t('staff.backHome')}
        </Link>
      </div>
    </main>
  );
}
