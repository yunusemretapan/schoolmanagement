import type { InvitationLink, StaffDetail } from '@schoolos/api-client';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { useApi } from '../../api/ApiProvider';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { InvitationLinkPanel } from './InvitationLinkPanel';
import { formatExpiry } from './invitationLink';
import { PRIMARY, SECONDARY, StaffFields } from './StaffFields';
import { staffErrorKey } from './staffErrors';
import { STAFF_ROLES, staffBody, validateStaff, type StaffFieldErrors, type StaffForm } from './staffSchema';

function formOf(detail: StaffDetail): StaffForm {
  return {
    fullName: detail.fullName,
    email: detail.email ?? '',
    phone: detail.phone ?? '',
    roles: STAFF_ROLES.filter((role) => detail.roles.includes(role)),
  };
}

/**
 * Spec 0004, Req. 7–9, 11 / AC-7–AC-9: change name and roles (and, while invited, e-mail and phone), deactivate with
 * confirmation, reactivate, renew the link. A control the rules block is disabled with its reason written beside it.
 */
export function StaffDetailScreen() {
  const { staffId = '' } = useParams();
  const { t } = useTranslation();
  const api = useApi();
  const staff = useQuery<StaffDetail>({
    queryKey: ['staff', 'detail', staffId],
    queryFn: ({ signal }) => api.getStaff(staffId, signal),
    retry: false,
    refetchOnWindowFocus: false,
  });

  return (
    <main className="min-h-dvh bg-canvas p-lg font-sans text-ink">
      <section className="mx-auto flex max-w-card flex-col gap-lg rounded-lg border border-line bg-surface p-xl shadow-card">
        {staff.isPending && (
          <p role="status" aria-live="polite" className="text-md text-ink-muted">
            {t('home.loading')}
          </p>
        )}
        {staff.isError && (
          <div className="flex flex-col gap-sm">
            <p role="alert" className="text-md font-semibold text-danger">
              {t(staffErrorKey(staff.error))}
            </p>
            <button type="button" onClick={() => void staff.refetch()} className={SECONDARY}>
              {t('home.retry')}
            </button>
          </div>
        )}
        {staff.isSuccess && <StaffEditor key={staff.data.id} detail={staff.data} />}
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

function StaffEditor({ detail }: { detail: StaffDetail }) {
  const { t } = useTranslation();
  const api = useApi();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<StaffForm>(() => formOf(detail));
  const [errors, setErrors] = useState<StaffFieldErrors>({});
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<{ kind: 'saved' | 'error'; key: string } | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [link, setLink] = useState<InvitationLink | null>(null);

  const isAdmin = detail.roles.includes('ADMIN');
  const blockReason = detail.lastActiveAdmin
    ? t('staff.errors.lastAdmin')
    : detail.self
      ? t('staff.blocked.self')
      : null;
  const adminLocked = isAdmin && blockReason !== null;

  function show(updated: StaffDetail) {
    queryClient.setQueryData(['staff', 'detail', detail.id], updated);
    void queryClient.invalidateQueries({ queryKey: ['staff', 'list'] });
  }

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setMessage(null);
    try {
      await action();
    } catch (error) {
      setMessage({ kind: 'error', key: staffErrorKey(error) });
    } finally {
      setBusy(false);
    }
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const found = validateStaff(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    await run(async () => {
      const updated = await api.updateStaff(detail.id, staffBody(form));
      setMessage({ kind: 'saved', key: 'staff.saved' });
      show(updated);
    });
  }

  return (
    <>
      <header className="flex flex-col gap-xs">
        <h1 className="text-lg font-bold">{detail.fullName}</h1>
        <p className="text-sm">
          <span className="rounded-pill border border-line-strong px-sm font-medium">
            {t(`staff.status.${detail.status}`)}
          </span>
          {detail.roles.includes('GUARDIAN') && (
            <span className="ml-xs rounded-pill bg-brand-subtle px-sm font-medium text-brand-ink">
              {t('roles.GUARDIAN')}
            </span>
          )}
        </p>
      </header>

      <p role="status" aria-live="polite" className="text-sm">
        {message?.kind === 'saved' && t(message.key)}
      </p>
      {message?.kind === 'error' && (
        <p role="alert" className="rounded-md bg-sunken p-sm text-sm font-semibold text-danger">
          {t(message.key)}
        </p>
      )}

      <form noValidate onSubmit={(event) => void save(event)} className="flex flex-col gap-md">
        <StaffFields
          form={form}
          errors={errors}
          onChange={setForm}
          identifiersLocked={detail.status !== 'INVITED'}
          lockedRole={
            adminLocked && blockReason !== null ? { role: 'ADMIN', reason: blockReason } : undefined
          }
        />
        <button type="submit" disabled={busy} aria-busy={busy} className={PRIMARY}>
          {busy ? t('staff.saving') : t('staff.save')}
        </button>
      </form>

      {detail.status === 'INVITED' && (
        <div className="flex flex-col gap-sm border-t border-line pt-md">
          {detail.invitationExpiresAt !== null ? (
            <p className="text-sm text-ink-muted">
              {t('staff.linkExpires', { at: formatExpiry(detail.invitationExpiresAt) })}
            </p>
          ) : (
            <p className="text-sm text-ink-muted">{t('staff.noLink')}</p>
          )}
          {link !== null && <InvitationLinkPanel link={link} />}
          <button
            type="button"
            disabled={busy}
            onClick={() => void run(async () => setLink(await api.renewInvitation(detail.id)))}
            className={SECONDARY}
          >
            {t('staff.renewLink')}
          </button>
        </div>
      )}

      <div className="flex flex-col gap-sm border-t border-line pt-md">
        {detail.status === 'INACTIVE' ? (
          <button
            type="button"
            disabled={busy}
            onClick={() => void run(async () => show(await api.reactivateStaff(detail.id)))}
            className={SECONDARY}
          >
            {t('staff.reactivate')}
          </button>
        ) : (
          <>
            <button
              type="button"
              disabled={busy || blockReason !== null}
              aria-describedby={blockReason === null ? undefined : 'deactivate-reason'}
              onClick={() => setConfirming(true)}
              className={`${SECONDARY} text-danger`}
            >
              {t('staff.deactivate')}
            </button>
            {blockReason !== null && (
              <p id="deactivate-reason" className="text-sm text-ink-muted">
                {blockReason}
              </p>
            )}
          </>
        )}
      </div>

      <ConfirmDialog
        open={confirming}
        title={t('staff.confirmTitle')}
        message={t('staff.confirmMessage', { name: detail.fullName })}
        confirmLabel={t('staff.deactivate')}
        cancelLabel={t('staff.cancel')}
        onCancel={() => setConfirming(false)}
        onConfirm={() => {
          setConfirming(false);
          void run(async () => show(await api.deactivateStaff(detail.id)));
        }}
      />
    </>
  );
}
