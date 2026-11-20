import { ApiError, ERROR_CODES, type InvitationInfo } from '@schoolos/api-client';
import { useQuery } from '@tanstack/react-query';
import { useId, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router';
import { useApi } from '../../api/ApiProvider';
import { LOGIN_PATH, type ReturnState } from '../../auth/RequireAuth';
import { invitationToken } from '../staff/invitationLink';
import { INPUT, PRIMARY, SECONDARY } from '../staff/StaffFields';

export const MIN_PASSWORD_LENGTH = 10;

type PasswordErrors = { password?: string; repeat?: string };
type FormError = 'invalid' | 'rateLimited' | 'network';

function formErrorOf(error: unknown): FormError {
  if (error instanceof ApiError) {
    if (error.status === 404) return 'invalid';
    if (error.code === ERROR_CODES.rateLimited) return 'rateLimited';
  }
  return 'network';
}

const FORM_ERROR_KEY: Record<FormError, string> = {
  invalid: 'invitation.invalid',
  rateLimited: 'invitation.errors.rateLimited',
  network: 'auth.errors.network',
};

/**
 * Spec 0004, Req. 5, 6 / AC-5, AC-6: open the link (token in the address fragment), see whom it invites, set the
 * password twice. States: loading, valid, invalid (one message for every reason), field errors (nothing sent),
 * submitting, too many tries, network error, success → the sign-in screen with the identifier filled in.
 */
export function AcceptInvitationScreen() {
  const { t } = useTranslation();
  const api = useApi();
  const navigate = useNavigate();
  const location = useLocation();
  const token = invitationToken(location.hash);
  const ids = { password: useId(), repeat: useId(), passwordError: useId(), repeatError: useId() };

  const info = useQuery<InvitationInfo>({
    queryKey: ['invitation', token],
    queryFn: ({ signal }) => api.getInvitation(token, signal),
    enabled: token !== '',
    retry: false,
    refetchOnWindowFocus: false,
    gcTime: 0,
  });
  const [password, setPassword] = useState('');
  const [repeat, setRepeat] = useState('');
  const [errors, setErrors] = useState<PasswordErrors>({});
  const [formError, setFormError] = useState<FormError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    const found: PasswordErrors = {};
    if (password.length < MIN_PASSWORD_LENGTH) found.password = 'invitation.errors.tooShort';
    else if (repeat !== password) found.repeat = 'invitation.errors.mismatch';
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      const accepted = await api.acceptInvitation(token, password);
      const state: ReturnState = { identifier: accepted.identifier, invitationAccepted: true };
      void navigate(LOGIN_PATH, { replace: true, state });
    } catch (error) {
      setFormError(formErrorOf(error));
      setSubmitting(false);
    }
  }

  const loadError = token === '' ? 'invalid' : info.isError ? formErrorOf(info.error) : null;
  const invalid = loadError === 'invalid' || formError === 'invalid';

  return (
    <main className="grid min-h-dvh place-items-center bg-canvas p-lg font-sans text-ink">
      <section className="w-full max-w-card rounded-lg border border-line bg-surface p-xl shadow-card">
        <h1 className="mb-md text-lg font-bold">{t('invitation.title')}</h1>

        {token !== '' && info.isPending && (
          <p role="status" aria-live="polite" className="text-md text-ink-muted">
            {t('home.loading')}
          </p>
        )}
        {invalid && (
          <p role="alert" className="text-md font-semibold text-danger">
            {t('invitation.invalid')}
          </p>
        )}
        {!invalid && loadError !== null && (
          <div className="flex flex-col gap-sm">
            <p role="alert" className="text-md font-semibold text-danger">
              {t(FORM_ERROR_KEY[loadError])}
            </p>
            <button type="button" onClick={() => void info.refetch()} className={SECONDARY}>
              {t('home.retry')}
            </button>
          </div>
        )}

        {!invalid && info.isSuccess && (
          <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-col gap-md">
            <p className="text-md">
              {t('invitation.invited', { name: info.data.fullName, institution: info.data.institutionName })}
            </p>
            {formError !== null && (
              <p role="alert" className="rounded-md bg-sunken p-sm text-sm font-semibold text-danger">
                {t(FORM_ERROR_KEY[formError])}
              </p>
            )}
            <div className="flex flex-col gap-xs">
              <label htmlFor={ids.password} className="text-sm font-medium">
                {t('invitation.password')}
              </label>
              <input
                id={ids.password}
                type="password"
                autoComplete="new-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                aria-invalid={errors.password !== undefined}
                aria-describedby={errors.password === undefined ? undefined : ids.passwordError}
                className={INPUT}
              />
              {errors.password !== undefined && (
                <p id={ids.passwordError} role="alert" className="text-sm text-danger">
                  {t(errors.password)}
                </p>
              )}
            </div>
            <div className="flex flex-col gap-xs">
              <label htmlFor={ids.repeat} className="text-sm font-medium">
                {t('invitation.passwordRepeat')}
              </label>
              <input
                id={ids.repeat}
                type="password"
                autoComplete="new-password"
                value={repeat}
                onChange={(event) => setRepeat(event.target.value)}
                aria-invalid={errors.repeat !== undefined}
                aria-describedby={errors.repeat === undefined ? undefined : ids.repeatError}
                className={INPUT}
              />
              {errors.repeat !== undefined && (
                <p id={ids.repeatError} role="alert" className="text-sm text-danger">
                  {t(errors.repeat)}
                </p>
              )}
            </div>
            <button type="submit" disabled={submitting} aria-busy={submitting} className={PRIMARY}>
              {submitting ? t('invitation.submitting') : t('invitation.submit')}
            </button>
          </form>
        )}
      </section>
    </main>
  );
}
