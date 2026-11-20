import type { InvitationLink } from '@schoolos/api-client';
import { useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useApi } from '../../api/ApiProvider';
import { InvitationLinkPanel } from './InvitationLinkPanel';
import { PRIMARY, StaffFields } from './StaffFields';
import { staffErrorKey } from './staffErrors';
import { staffBody, validateStaff, type StaffFieldErrors, type StaffForm } from './staffSchema';

const EMPTY: StaffForm = { fullName: '', email: '', phone: '', roles: [] };

/**
 * Spec 0004, Req. 4, 5 / AC-4: the invite form. States: empty, field errors (nothing sent), submitting (no double
 * submit), identifier unavailable, network error (input kept), success (the link).
 */
export function InviteStaffScreen() {
  const { t } = useTranslation();
  const api = useApi();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<StaffForm>(EMPTY);
  const [errors, setErrors] = useState<StaffFieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [link, setLink] = useState<InvitationLink | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    const found = validateStaff(form);
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      setLink(await api.inviteStaff(staffBody(form)));
      void queryClient.invalidateQueries({ queryKey: ['staff'] });
    } catch (error) {
      setFormError(staffErrorKey(error));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="min-h-dvh bg-canvas p-lg font-sans text-ink">
      <section className="mx-auto flex max-w-card flex-col gap-lg rounded-lg border border-line bg-surface p-xl shadow-card">
        <h1 className="text-lg font-bold">{t('staff.inviteTitle')}</h1>

        {link !== null ? (
          <InvitationLinkPanel link={link} />
        ) : (
          <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-col gap-md">
            {formError !== null && (
              <p role="alert" className="rounded-md bg-sunken p-sm text-sm font-semibold text-danger">
                {t(formError)}
              </p>
            )}
            <StaffFields form={form} errors={errors} onChange={setForm} />
            <button type="submit" disabled={submitting} aria-busy={submitting} className={PRIMARY}>
              {submitting ? t('staff.inviting') : t('staff.inviteSubmit')}
            </button>
          </form>
        )}

        <Link
          to="/personel"
          className="text-sm font-medium text-brand-ink underline outline-focus focus-visible:outline-[length:var(--sos-border-thick)]"
        >
          {t('staff.backToList')}
        </Link>
      </section>
    </main>
  );
}
