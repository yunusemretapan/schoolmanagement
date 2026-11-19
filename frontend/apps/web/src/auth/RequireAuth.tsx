import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, useLocation } from 'react-router';
import { useAuth } from './AuthProvider';

export const LOGIN_PATH = '/giris';

/** Where the login screen sends the user back to after signing in (spec 0003, flow "korumalı adres"). */
export interface ReturnState {
  from?: string;
}

/**
 * Guards a screen (spec 0003, AC-11): signed out → the login screen, remembering where the user wanted to go. The
 * redirect replaces the history entry, so Back never shows a guarded screen to a signed-out user (AC-9).
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { t } = useTranslation();
  const { status, retry } = useAuth();
  const location = useLocation();

  if (status === 'restoring') {
    return (
      <main className="grid min-h-dvh place-items-center bg-canvas p-lg font-sans text-ink">
        <p role="status" aria-live="polite" className="text-md text-ink-muted">
          {t('auth.restoring')}
        </p>
      </main>
    );
  }
  if (status === 'unreachable') {
    return (
      <main className="grid min-h-dvh place-items-center bg-canvas p-lg font-sans text-ink">
        <div className="flex flex-col items-center gap-md">
          <p role="alert" className="text-md font-semibold text-danger">
            {t('auth.unreachable')}
          </p>
          <button
            type="button"
            onClick={retry}
            className="min-h-control rounded-md bg-brand px-lg py-sm font-semibold text-on-brand outline-focus focus-visible:outline-[length:var(--sos-border-thick)] focus-visible:outline-offset-[var(--sos-border-thick)]"
          >
            {t('auth.retry')}
          </button>
        </div>
      </main>
    );
  }
  if (status === 'signedOut') {
    const state: ReturnState = { from: `${location.pathname}${location.search}` };
    return <Navigate to={LOGIN_PATH} replace state={state} />;
  }
  return children;
}
