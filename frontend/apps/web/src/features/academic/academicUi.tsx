import { useId, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { INPUT, PRIMARY, SECONDARY } from '../staff/StaffFields';
import { academicErrorKey, nameError, serverFieldErrors } from './academicSchema';

export { INPUT, PRIMARY, SECONDARY };

export const DANGER =
  'min-h-control rounded-md border border-line-strong px-md text-sm font-medium text-danger outline-focus focus-visible:outline-[length:var(--sos-border-thick)] disabled:opacity-[var(--sos-opacity-pressed)]';

export interface Message {
  kind: 'saved' | 'error';
  key: string;
  values?: Record<string, string | number>;
}

/**
 * One request at a time per section (no double submit): busy while it runs, then a saved message (announced) or the
 * error's message (spec 0005, UX states).
 */
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<Message | null>(null);

  async function run(action: () => Promise<void>, saved?: Message): Promise<unknown> {
    if (busy) return 'busy';
    setBusy(true);
    setMessage(null);
    try {
      await action();
      if (saved) setMessage(saved);
      return undefined;
    } catch (error) {
      setMessage({ kind: 'error', key: academicErrorKey(error) });
      return error;
    } finally {
      setBusy(false);
    }
  }

  return { busy, message, run, setMessage };
}

export function SectionMessage({ message }: { message: Message | null }) {
  const { t } = useTranslation();
  return (
    <>
      <p role="status" aria-live="polite" className="text-sm">
        {message?.kind === 'saved' && t(message.key, message.values ?? {})}
      </p>
      {message?.kind === 'error' && (
        <p role="alert" className="rounded-md bg-sunken p-sm text-sm font-semibold text-danger">
          {t(message.key, message.values ?? {})}
        </p>
      )}
    </>
  );
}

interface NameFormProps {
  label: string;
  initial?: string;
  max: number;
  submitLabel: string;
  busy: boolean;
  /** Sends the trimmed name; resolves to the error if the server refused it (the input keeps what was typed). */
  onSubmit(name: string): Promise<unknown>;
  onCancel?(): void;
}

/** A labelled name field with its own check before sending (spec 0005, Req. 7 / AC-7). */
export function NameForm({ label, initial = '', max, submitLabel, busy, onSubmit, onCancel }: NameFormProps) {
  const { t } = useTranslation();
  const id = useId();
  const [value, setValue] = useState(initial);
  const [error, setError] = useState<string | undefined>();

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const found = nameError(value, max);
    setError(found);
    if (found) return;
    const refused = await onSubmit(value.trim());
    setError(serverFieldErrors(refused, max).name);
    if (refused === undefined && !initial) setValue('');
  }

  return (
    <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-wrap items-end gap-sm">
      <div className="flex grow flex-col gap-xs">
        <label htmlFor={id} className="text-sm font-medium">
          {label}
        </label>
        <input
          id={id}
          value={value}
          onChange={(event) => setValue(event.target.value)}
          aria-invalid={error !== undefined}
          aria-describedby={error === undefined ? undefined : `${id}-error`}
          className={INPUT}
        />
        {error !== undefined && (
          <p id={`${id}-error`} className="text-sm text-danger">
            {t(error)}
          </p>
        )}
      </div>
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

interface DeleteButtonProps {
  name: string;
  /** i18n key of why the record cannot be deleted, or null. */
  blockedReason: string | null;
  busy: boolean;
  onDelete(): Promise<unknown>;
  /** Where focus goes once the record (and so this button) is gone (spec 0004 review 1, L-3). */
  afterDelete(): void;
}

/** "Sil" with a confirmation; a blocked delete is disabled with its reason written beside it (spec 0005, Req. 9). */
export function DeleteButton({ name, blockedReason, busy, onDelete, afterDelete }: DeleteButtonProps) {
  const { t } = useTranslation();
  const reasonId = useId();
  const [confirming, setConfirming] = useState(false);

  return (
    <>
      <button
        type="button"
        disabled={busy || blockedReason !== null}
        aria-describedby={blockedReason === null ? undefined : reasonId}
        onClick={() => setConfirming(true)}
        className={DANGER}
      >
        {t('academic.delete')}
      </button>
      {blockedReason !== null && (
        <span id={reasonId} className="text-sm text-ink-muted">
          {t(blockedReason)}
        </span>
      )}
      <ConfirmDialog
        open={confirming}
        title={t('academic.confirmTitle')}
        message={t('academic.confirmMessage', { name })}
        confirmLabel={t('academic.delete')}
        cancelLabel={t('academic.cancel')}
        onCancel={() => setConfirming(false)}
        onConfirm={() => {
          setConfirming(false);
          void onDelete().then((refused) => {
            if (refused === undefined) afterDelete();
          });
        }}
      />
    </>
  );
}

/** A section with a focusable heading, so focus has somewhere meaningful to land after a delete. */
export function Section({
  title,
  children,
  headingRef,
}: {
  title: string;
  children: ReactNode;
  headingRef: React.RefObject<HTMLHeadingElement | null>;
}) {
  return (
    <section className="flex flex-col gap-md rounded-lg border border-line bg-surface p-lg shadow-card">
      <h2 ref={headingRef} tabIndex={-1} className="text-lg font-bold outline-focus">
        {title}
      </h2>
      {children}
    </section>
  );
}

export function useHeading() {
  const ref = useRef<HTMLHeadingElement>(null);
  return { ref, focus: () => ref.current?.focus() };
}
