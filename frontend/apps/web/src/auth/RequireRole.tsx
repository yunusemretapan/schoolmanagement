import type { Role } from '@schoolos/api-client';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useMe } from './useMe';

/**
 * Inside RequireAuth: shows a screen only to a user holding {@code role} (spec 0004, AC-1 "yetkisiz" state). The
 * server refuses the calls anyway; this keeps the screen from appearing at all.
 */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { t } = useTranslation();
  const me = useMe();

  if (me.isPending) {
    return (
      <main className="grid min-h-dvh place-items-center bg-canvas p-lg font-sans text-ink">
        <p role="status" aria-live="polite" className="text-md text-ink-muted">
          {t('home.loading')}
        </p>
      </main>
    );
  }
  if (me.isError || !me.data.roles.includes(role)) {
    return (
      <main className="grid min-h-dvh place-items-center bg-canvas p-lg font-sans text-ink">
        <div className="flex flex-col items-center gap-md">
          <p role="alert" className="text-md font-semibold text-danger">
            {me.isError ? t('home.error') : t('staff.forbidden')}
          </p>
          <Link
            to="/"
            className="text-sm font-medium text-brand-ink underline outline-focus focus-visible:outline-[length:var(--sos-border-thick)]"
          >
            {t('staff.backHome')}
          </Link>
        </div>
      </main>
    );
  }
  return children;
}
