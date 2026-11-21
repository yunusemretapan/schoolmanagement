import type { AcademicStructure, TermBody, TermView } from '@schoolos/api-client';
import { useId, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useApi } from '../../api/ApiProvider';
import {
  EARLIEST,
  formatDay,
  LATEST,
  serverFieldErrors,
  termErrors,
  type FieldErrors,
} from './academicSchema';
import {
  DeleteButton,
  INPUT,
  PRIMARY,
  Section,
  SectionMessage,
  SECONDARY,
  useAction,
  useHeading,
} from './academicUi';

const FIELDS: readonly { field: 'name' | 'startsOn' | 'endsOn'; type: string }[] = [
  { field: 'name', type: 'text' },
  { field: 'startsOn', type: 'date' },
  { field: 'endsOn', type: 'date' },
];

interface TermFormProps {
  initial?: TermBody;
  submitLabel: string;
  busy: boolean;
  onSubmit(body: TermBody): Promise<unknown>;
  onCancel?(): void;
}

/** Name, first and last day (spec 0005, Req. 4): checked before sending; what was typed stays on an error. */
function TermForm({ initial, submitLabel, busy, onSubmit, onCancel }: TermFormProps) {
  const { t } = useTranslation();
  const id = useId();
  const [form, setForm] = useState<TermBody>(initial ?? { name: '', startsOn: '', endsOn: '' });
  const [errors, setErrors] = useState<FieldErrors>({});

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const found = termErrors(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    const refused = await onSubmit({ ...form, name: form.name.trim() });
    setErrors(serverFieldErrors(refused));
    if (refused === undefined && !initial) setForm({ name: '', startsOn: '', endsOn: '' });
  }

  return (
    <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-wrap items-end gap-sm">
      {FIELDS.map(({ field, type }) => (
        <div key={field} className="flex grow flex-col gap-xs">
          <label htmlFor={`${id}-${field}`} className="text-sm font-medium">
            {t(`academic.terms.fields.${field}`)}
          </label>
          <input
            id={`${id}-${field}`}
            type={type}
            min={type === 'date' ? EARLIEST : undefined}
            max={type === 'date' ? LATEST : undefined}
            value={form[field]}
            onChange={(event) => setForm({ ...form, [field]: event.target.value })}
            aria-invalid={errors[field] !== undefined}
            aria-describedby={errors[field] === undefined ? undefined : `${id}-${field}-error`}
            className={INPUT}
          />
          {errors[field] !== undefined && (
            <p id={`${id}-${field}-error`} className="text-sm text-danger">
              {t(errors[field])}
            </p>
          )}
        </div>
      ))}
      <button type="submit" disabled={busy} aria-busy={busy} className={PRIMARY}>
        {busy ? t('academic.saving') : submitLabel}
      </button>
      {onCancel && (
        <button type="button" onClick={onCancel} className={SECONDARY}>
          {t('academic.cancel')}
        </button>
      )}
    </form>
  );
}

function blockedReason(term: TermView): string | null {
  if (term.active) return 'academic.blocked.activeTerm';
  if (term.inUse) return 'academic.blocked.inUse';
  return null;
}

/** Spec 0005, Req. 4 / AC-4: terms newest first; the active one says so in words; switch, edit, delete. */
export function TermsSection({
  structure,
  apply,
}: {
  structure: AcademicStructure;
  apply(next: AcademicStructure): void;
}) {
  const { t } = useTranslation();
  const api = useApi();
  const { busy, message, run } = useAction();
  const heading = useHeading();
  const [editing, setEditing] = useState<string | null>(null);

  const save = (call: () => Promise<AcademicStructure>, key: string, name: string) =>
    run(async () => apply(await call()), { kind: 'saved', key, values: { name } });

  return (
    <Section title={t('academic.terms.title')} headingRef={heading.ref}>
      <SectionMessage message={message} />
      {structure.terms.length === 0 ? (
        <p className="text-md text-ink-muted">{t('academic.empty.terms')}</p>
      ) : (
        <ul className="flex flex-col gap-sm">
          {structure.terms.map((term) => (
            <li key={term.id} className="flex flex-col gap-sm border-t border-line pt-sm">
              {editing === term.id ? (
                <TermForm
                  initial={{ name: term.name, startsOn: term.startsOn, endsOn: term.endsOn }}
                  submitLabel={t('academic.save')}
                  busy={busy}
                  onCancel={() => setEditing(null)}
                  onSubmit={async (body) => {
                    const refused = await save(
                      () => api.updateTerm(term.id, body),
                      'academic.saved',
                      body.name,
                    );
                    if (refused === undefined) setEditing(null);
                    return refused;
                  }}
                />
              ) : (
                <div className="flex flex-wrap items-center gap-sm">
                  <p className="grow text-md">
                    <span className="font-semibold">{term.name}</span>{' '}
                    <span className="text-sm text-ink-muted">
                      {t('academic.terms.range', {
                        from: formatDay(term.startsOn),
                        to: formatDay(term.endsOn),
                      })}
                    </span>
                  </p>
                  {term.active ? (
                    <span className="rounded-pill bg-brand-subtle px-sm text-sm font-medium text-brand-ink">
                      {t('academic.terms.active')}
                    </span>
                  ) : (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() =>
                        void save(() => api.activateTerm(term.id), 'academic.terms.activated', term.name)
                      }
                      className={SECONDARY}
                    >
                      {t('academic.terms.makeActive')}
                    </button>
                  )}
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => setEditing(term.id)}
                    className={SECONDARY}
                  >
                    {t('academic.edit')}
                  </button>
                  <DeleteButton
                    name={term.name}
                    blockedReason={blockedReason(term)}
                    busy={busy}
                    onDelete={() => save(() => api.deleteTerm(term.id), 'academic.deleted', term.name)}
                    afterDelete={heading.focus}
                  />
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      <h3 className="text-md font-semibold">{t('academic.terms.add')}</h3>
      <TermForm
        submitLabel={t('academic.terms.add')}
        busy={busy}
        onSubmit={(body) => save(() => api.createTerm(body), 'academic.added', body.name)}
      />
    </Section>
  );
}
