import { useId } from 'react';
import { useTranslation } from 'react-i18next';
import { STAFF_ROLES, type StaffFieldErrors, type StaffForm, type StaffRole } from './staffSchema';

export const INPUT =
  'min-h-control w-full rounded-md border border-line-strong bg-surface px-md text-md text-ink outline-focus focus-visible:outline-[length:var(--sos-border-thick)] focus-visible:outline-offset-[var(--sos-border-thick)] aria-invalid:border-danger disabled:bg-sunken disabled:text-ink-muted';

export const PRIMARY =
  'flex min-h-control items-center justify-center gap-sm rounded-md bg-brand px-lg py-sm font-semibold text-on-brand outline-focus focus-visible:outline-[length:var(--sos-border-thick)] focus-visible:outline-offset-[var(--sos-border-thick)] hover:opacity-[var(--sos-opacity-pressed)] disabled:opacity-[var(--sos-opacity-pressed)]';

export const SECONDARY =
  'min-h-control rounded-md border border-line-strong px-md text-sm font-medium outline-focus focus-visible:outline-[length:var(--sos-border-thick)] disabled:opacity-[var(--sos-opacity-pressed)]';

type TextField = 'fullName' | 'email' | 'phone';

/** Input type and autofill hint per text field. */
const TEXT_FIELDS: readonly { field: TextField; type: string; autoComplete: string; identifier: boolean }[] =
  [
    { field: 'fullName', type: 'text', autoComplete: 'name', identifier: false },
    { field: 'email', type: 'email', autoComplete: 'email', identifier: true },
    { field: 'phone', type: 'tel', autoComplete: 'tel', identifier: true },
  ];

interface StaffFieldsProps {
  form: StaffForm;
  errors: StaffFieldErrors;
  onChange(form: StaffForm): void;
  /** E-mail and phone change only while invited (spec 0004, Req. 11). */
  identifiersLocked?: boolean;
  /** A role checkbox that cannot change, with the reason shown beside it (spec 0004, Req. 9). */
  lockedRole?: { role: StaffRole; reason: string };
}

/** Name, e-mail, phone and staff roles, shared by the invite form and the detail screen (spec 0004, UX). */
export function StaffFields({
  form,
  errors,
  onChange,
  identifiersLocked = false,
  lockedRole,
}: StaffFieldsProps) {
  const { t } = useTranslation();
  const ids = {
    fullName: useId(),
    email: useId(),
    phone: useId(),
    roles: useId(),
    lockedReason: useId(),
    identifiersHint: useId(),
  };
  const errorId = (field: keyof StaffForm) => `${ids[field]}-error`;

  function toggle(role: StaffRole, checked: boolean) {
    const roles = checked ? [...form.roles, role] : form.roles.filter((r) => r !== role);
    onChange({ ...form, roles });
  }

  const describedBy = (field: keyof StaffForm, extra?: string) =>
    [errors[field] === undefined ? undefined : errorId(field), extra].filter(Boolean).join(' ') || undefined;

  const text = (field: TextField, type: string, autoComplete: string, locked: boolean) => (
    <div key={field} className="flex flex-col gap-xs">
      <label htmlFor={ids[field]} className="text-sm font-medium">
        {t(`staff.fields.${field}`)}
      </label>
      <input
        id={ids[field]}
        name={field}
        type={type}
        autoComplete={autoComplete}
        value={form[field]}
        disabled={locked}
        onChange={(event) => onChange({ ...form, [field]: event.target.value })}
        aria-invalid={errors[field] !== undefined}
        aria-describedby={describedBy(field, locked ? ids.identifiersHint : undefined)}
        className={INPUT}
      />
      {errors[field] !== undefined && (
        <p id={errorId(field)} role="alert" className="text-sm text-danger">
          {t(errors[field])}
        </p>
      )}
    </div>
  );

  return (
    <div className="flex flex-col gap-md">
      {TEXT_FIELDS.map(({ field, type, autoComplete, identifier }) =>
        text(field, type, autoComplete, identifier && identifiersLocked),
      )}
      {identifiersLocked && (
        <p id={ids.identifiersHint} className="text-sm text-ink-muted">
          {t('staff.identifiersLocked')}
        </p>
      )}
      <fieldset
        className="flex flex-col gap-xs"
        aria-invalid={errors.roles !== undefined}
        aria-describedby={describedBy('roles')}
      >
        <legend className="mb-xs text-sm font-medium">{t('staff.fields.roles')}</legend>
        {STAFF_ROLES.map((role) => {
          const locked = lockedRole?.role === role;
          const id = `${ids.roles}-${role}`;
          return (
            <div key={role} className="flex items-center gap-sm">
              <input
                id={id}
                type="checkbox"
                checked={form.roles.includes(role)}
                disabled={locked}
                aria-describedby={locked ? ids.lockedReason : undefined}
                onChange={(event) => toggle(role, event.target.checked)}
                className="accent-brand"
              />
              <label htmlFor={id} className="text-md">
                {t(`roles.${role}`)}
              </label>
            </div>
          );
        })}
        {lockedRole !== undefined && (
          <p id={ids.lockedReason} className="text-sm text-ink-muted">
            {lockedRole.reason}
          </p>
        )}
        {errors.roles !== undefined && (
          <p id={errorId('roles')} role="alert" className="text-sm text-danger">
            {t(errors.roles)}
          </p>
        )}
      </fieldset>
    </div>
  );
}
