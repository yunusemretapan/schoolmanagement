import { useTranslation } from 'react-i18next';
import { useSystemStatus, type StatusView } from './useSystemStatus';

const DOT: Record<StatusView['kind'], string> = {
  checking: 'bg-pending',
  running: 'bg-success',
  unreachable: 'bg-danger',
};

export function StatusScreen() {
  const { t } = useTranslation();
  const { view, retry } = useSystemStatus();

  return (
    <main className="relative grid min-h-dvh place-items-center overflow-hidden bg-canvas p-lg font-sans text-ink">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -top-glow-offset left-1/2 size-glow -translate-x-1/2 rounded-pill bg-brand-glow blur-glow"
      />
      <section className="relative w-full max-w-card rounded-lg border border-line bg-surface p-xl shadow-card">
        <header className="mb-xl flex items-center gap-sm">
          <span className="grid size-logo place-items-center rounded-md bg-brand text-lg font-bold text-on-brand">
            {t('app.name').charAt(0)}
          </span>
          <div>
            <p className="text-md font-bold">{t('app.name')}</p>
            <p className="text-sm text-ink-muted">{t('app.tagline')}</p>
          </div>
        </header>

        <div role="status" aria-live="polite" className="flex items-center gap-md">
          <span className="relative flex size-dot">
            {view.kind === 'checking' && (
              <span
                data-pulse
                aria-hidden="true"
                className={`absolute inline-flex size-full rounded-pill opacity-[var(--sos-opacity-pulse)] motion-ok:animate-ping ${DOT[view.kind]}`}
              />
            )}
            <span className={`relative inline-flex size-dot rounded-pill ${DOT[view.kind]}`} />
          </span>
          <div>
            {view.kind === 'checking' && (
              <>
                <p className="text-lg font-semibold">{t('status.checking')}</p>
                {/* Visible spinner ring; with reduced motion it stays a static ring with a brand-coloured arc. */}
                <span
                  role="progressbar"
                  aria-label={t('status.loading')}
                  className="mt-xs block size-spinner rounded-pill border-[length:var(--sos-border-thick)] border-line border-t-brand motion-ok:animate-spin"
                />
              </>
            )}
            {view.kind === 'running' && (
              <>
                <p className="text-lg font-semibold text-success">{t('status.running')}</p>
                <p className="text-sm text-ink-muted">{t('status.version', { version: view.version })}</p>
              </>
            )}
            {view.kind === 'unreachable' && (
              <p className="text-lg font-semibold text-danger">{t('status.unreachable')}</p>
            )}
          </div>
        </div>

        {view.kind === 'unreachable' && (
          <button
            type="button"
            onClick={retry}
            className="mt-lg min-h-control w-full rounded-md bg-brand px-lg py-sm font-semibold text-on-brand outline-focus focus-visible:outline-[length:var(--sos-border-thick)] focus-visible:outline-offset-[var(--sos-border-thick)] hover:opacity-[var(--sos-opacity-pressed)] motion-ok:transition-transform motion-ok:duration-[var(--sos-duration-fast)] motion-ok:ease-standard active:scale-[var(--sos-press-scale)]"
          >
            {t('status.retry')}
          </button>
        )}
      </section>
    </main>
  );
}
