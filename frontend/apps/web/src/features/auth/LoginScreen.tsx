import { ApiError, ERROR_CODES } from '@schoolos/api-client';
import { validateLogin, type LoginFieldErrors } from '@schoolos/auth';
import { useId, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../../auth/AuthProvider';
import type { ReturnState } from '../../auth/RequireAuth';

type FormError = 'invalidCredentials' | 'rateLimited' | 'network';

function formErrorOf(error: unknown): FormError {
  if (error instanceof ApiError) {
    if (error.code === ERROR_CODES.invalidCredentials) return 'invalidCredentials';
    if (error.code === ERROR_CODES.rateLimited) return 'rateLimited';
    if (error.status === 401) return 'invalidCredentials';
  }
  return 'network';
}

const INPUT =
  'min-h-control w-full rounded-md border border-line-strong bg-surface px-md text-md text-ink outline-focus focus-visible:outline-[length:var(--sos-border-thick)] focus-visible:outline-offset-[var(--sos-border-thick)] aria-invalid:border-danger';

/**
 * Spec 0003 login screen: empty, field errors (nothing sent), submitting (no double submit), refused, network error
 * (incl. 10 s timeout). After any error the identifier stays and the password is cleared. Keyboard: identifier →
 * password → sign in; Enter submits.
 */
export function LoginScreen() {
  const { t } = useTranslation();
  const { status, expired, signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as ReturnState | null)?.from ?? '/';
  const ids = {
    identifier: useId(),
    password: useId(),
    show: useId(),
    identifierError: useId(),
    passwordError: useId(),
  };

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<LoginFieldErrors>({});
  const [formError, setFormError] = useState<FormError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (status === 'signedIn' && !submitting) {
    return <Navigate to={from} replace />;
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    const errors = validateLogin({ identifier, password });
    setFieldErrors(errors);
    setFormError(null);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      await signIn(identifier.trim(), password);
      void navigate(from, { replace: true });
    } catch (error) {
      setFormError(formErrorOf(error));
      setPassword('');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="relative grid min-h-dvh place-items-center overflow-hidden bg-canvas p-lg font-sans text-ink">
      <section className="relative w-full max-w-card rounded-lg border border-line bg-surface p-xl shadow-card">
        <header className="mb-lg flex items-center gap-sm">
          <span className="grid size-logo place-items-center rounded-md bg-brand text-lg font-bold text-on-brand">
            {t('app.name').charAt(0)}
          </span>
          <div className="grow">
            <h1 className="text-lg font-bold">{t('auth.title')}</h1>
            <p className="text-sm text-ink-muted">{t('auth.subtitle')}</p>
          </div>
        </header>

        {expired && formError === null && (
          <p role="status" className="mb-md rounded-md bg-sunken p-sm text-sm text-ink">
            {t('auth.sessionExpired')}
          </p>
        )}
        {formError !== null && (
          <p role="alert" className="mb-md rounded-md bg-sunken p-sm text-sm font-semibold text-danger">
            {t(`auth.errors.${formError}`)}
          </p>
        )}

        <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-col gap-md">
          <div className="flex flex-col gap-xs">
            <label htmlFor={ids.identifier} className="text-sm font-medium">
              {t('auth.identifier')}
            </label>
            <input
              id={ids.identifier}
              name="identifier"
              type="text"
              inputMode="email"
              autoComplete="username"
              value={identifier}
              onChange={(event) => setIdentifier(event.target.value)}
              aria-invalid={fieldErrors.identifier !== undefined}
              aria-describedby={fieldErrors.identifier === undefined ? undefined : ids.identifierError}
              className={INPUT}
            />
            {fieldErrors.identifier !== undefined && (
              <p id={ids.identifierError} role="alert" className="text-sm text-danger">
                {t(fieldErrors.identifier)}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-xs">
            <label htmlFor={ids.password} className="text-sm font-medium">
              {t('auth.password')}
            </label>
            <input
              id={ids.password}
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              aria-invalid={fieldErrors.password !== undefined}
              aria-describedby={fieldErrors.password === undefined ? undefined : ids.passwordError}
              className={INPUT}
            />
            {fieldErrors.password !== undefined && (
              <p id={ids.passwordError} role="alert" className="text-sm text-danger">
                {t(fieldErrors.password)}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={submitting}
            aria-busy={submitting}
            className="mt-sm flex min-h-control w-full items-center justify-center gap-sm rounded-md bg-brand px-lg py-sm font-semibold text-on-brand outline-focus focus-visible:outline-[length:var(--sos-border-thick)] focus-visible:outline-offset-[var(--sos-border-thick)] hover:opacity-[var(--sos-opacity-pressed)] disabled:opacity-[var(--sos-opacity-pressed)]"
          >
            {submitting && (
              <span
                aria-hidden="true"
                className="block size-spinner rounded-pill border-[length:var(--sos-border-thick)] border-on-brand border-t-transparent motion-ok:animate-spin"
              />
            )}
            {submitting ? t('auth.submitting') : t('auth.submit')}
          </button>

          {/* After the submit button in the DOM: the spec's Tab order is identifier → password → sign in. */}
          <div className="flex items-center gap-sm">
            <input
              id={ids.show}
              type="checkbox"
              checked={showPassword}
              onChange={(event) => setShowPassword(event.target.checked)}
              className="accent-brand"
            />
            <label htmlFor={ids.show} className="text-sm text-ink-muted">
              {t('auth.showPassword')}
            </label>
          </div>
        </form>

        <Link
          to="/durum"
          className="mt-lg block text-center text-sm font-medium text-brand-ink underline outline-focus focus-visible:outline-[length:var(--sos-border-thick)]"
        >
          {t('auth.statusLink')}
        </Link>
      </section>
    </main>
  );
}
