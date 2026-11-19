import type { CurrentUser } from '@schoolos/api-client';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { useApi } from '../../api/ApiProvider';
import { AppearancePanel } from '../../appearance/AppearancePanel';
import { useAuth } from '../../auth/AuthProvider';
import { LOGIN_PATH } from '../../auth/RequireAuth';

const LINK =
  'block rounded-md border border-line px-md py-sm text-md font-medium text-ink outline-focus hover:bg-sunken focus-visible:outline-[length:var(--sos-border-thick)]';

/** Spec 0003, Req. 10: who is signed in, for which institution, with which roles; status, showcase and sign-out. */
export function HomeScreen() {
  const { t } = useTranslation();
  const api = useApi();
  const { signOut } = useAuth();
  const navigate = useNavigate();
  const [signingOut, setSigningOut] = useState(false);
  const me = useQuery<CurrentUser>({
    queryKey: ['me'],
    queryFn: ({ signal }) => api.getMe(signal),
    retry: false,
    refetchOnWindowFocus: false,
  });

  async function logout() {
    setSigningOut(true);
    await signOut();
    void navigate(LOGIN_PATH, { replace: true });
  }

  return (
    <main className="min-h-dvh bg-canvas p-lg font-sans text-ink">
      <div className="mx-auto flex max-w-card flex-col gap-lg">
        <header className="flex items-center gap-sm">
          <h1 className="grow text-xl font-bold">{t('home.title')}</h1>
          <AppearancePanel />
        </header>

        <section
          className="rounded-lg border border-line bg-surface p-xl shadow-card"
          aria-busy={me.isPending}
        >
          {me.isPending && (
            <p role="status" aria-live="polite" className="text-md text-ink-muted">
              {t('home.loading')}
            </p>
          )}
          {me.isError && (
            <div className="flex flex-col gap-sm">
              <p role="alert" className="text-md font-semibold text-danger">
                {t('home.error')}
              </p>
              <button
                type="button"
                onClick={() => void me.refetch()}
                className="min-h-control rounded-md border border-line-strong px-md text-sm font-medium outline-focus focus-visible:outline-[length:var(--sos-border-thick)]"
              >
                {t('home.retry')}
              </button>
            </div>
          )}
          {me.isSuccess && (
            <div className="flex flex-col gap-md">
              <p className="text-lg font-semibold">{t('home.greeting', { name: me.data.fullName })}</p>
              <dl className="grid gap-xs">
                <dt className="text-sm text-ink-muted">{t('home.institution')}</dt>
                <dd className="text-md">{me.data.institutionName}</dd>
                <dt className="text-sm text-ink-muted">{t('home.roles')}</dt>
                <dd>
                  <ul className="flex flex-wrap gap-xs">
                    {me.data.roles.map((role) => (
                      <li
                        key={role}
                        className="rounded-pill bg-brand-subtle px-sm text-sm font-medium text-brand-ink"
                      >
                        {t(`roles.${role}`)}
                      </li>
                    ))}
                  </ul>
                </dd>
              </dl>
            </div>
          )}
        </section>

        <nav className="flex flex-col gap-sm">
          <Link to="/durum" className={LINK}>
            {t('home.status')}
          </Link>
          <Link to="/tasarim" className={LINK}>
            {t('home.showcase')}
          </Link>
        </nav>

        <button
          type="button"
          onClick={() => void logout()}
          disabled={signingOut}
          className="min-h-control w-full rounded-md bg-brand px-lg py-sm font-semibold text-on-brand outline-focus focus-visible:outline-[length:var(--sos-border-thick)] focus-visible:outline-offset-[var(--sos-border-thick)] hover:opacity-[var(--sos-opacity-pressed)]"
        >
          {t('home.logout')}
        </button>
      </div>
    </main>
  );
}
