import { useEffect, useId, useRef, type KeyboardEvent } from 'react';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel: string;
  onConfirm(): void;
  onCancel(): void;
}

/**
 * A modal confirmation (spec 0004, Accessibility; plan 0004, R9): focus moves into it (to "cancel", the safe
 * choice), Tab stays inside, Esc cancels, and on close focus returns to whatever opened it. No dependency: jsdom's
 * native <dialog> lacks showModal.
 */
export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel,
  cancelLabel,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const titleId = useId();
  const messageId = useId();
  const cancelRef = useRef<HTMLButtonElement>(null);
  const confirmRef = useRef<HTMLButtonElement>(null);
  const opener = useRef<Element | null>(null);

  useEffect(() => {
    if (!open) return;
    opener.current = document.activeElement;
    cancelRef.current?.focus();
    return () => {
      const back = opener.current;
      if (back instanceof HTMLElement) back.focus();
    };
  }, [open]);

  if (!open) return null;

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') {
      event.preventDefault();
      onCancel();
      return;
    }
    if (event.key === 'Tab') {
      const first = cancelRef.current;
      const last = confirmRef.current;
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last?.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first?.focus();
      }
    }
  }

  return (
    <div className="fixed inset-[var(--sos-layout-overlay-inset)] z-[var(--sos-layer-overlay)] grid place-items-center bg-ink/[var(--sos-opacity-scrim)] p-lg">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={messageId}
        onKeyDown={onKeyDown}
        className="w-full max-w-card rounded-lg border border-line bg-surface p-xl font-sans text-ink shadow-card"
      >
        <h2 id={titleId} className="text-lg font-bold">
          {title}
        </h2>
        <p id={messageId} className="mt-sm text-md">
          {message}
        </p>
        <div className="mt-lg flex justify-end gap-sm">
          <button
            ref={cancelRef}
            type="button"
            onClick={onCancel}
            className="min-h-control rounded-md border border-line-strong px-md text-sm font-medium outline-focus focus-visible:outline-[length:var(--sos-border-thick)]"
          >
            {cancelLabel}
          </button>
          <button
            ref={confirmRef}
            type="button"
            onClick={onConfirm}
            className="min-h-control rounded-md bg-danger px-md text-sm font-semibold text-on-brand outline-focus focus-visible:outline-[length:var(--sos-border-thick)] focus-visible:outline-offset-[var(--sos-border-thick)]"
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
