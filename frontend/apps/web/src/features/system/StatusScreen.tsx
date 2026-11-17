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
        className="pointer-events-none absolute -top-40 left-1/2 h-[36rem] w-[36rem] -translate-x-1/2 rounded-full bg-brand/15 blur-3xl"
      />
      <section className="relative w-full max-w-card rounded-lg border border-line bg-raised p-xl shadow-card">
        <header className="mb-xl flex items-center gap-sm">
          <span className="grid size-10 place-items-center rounded-md bg-brand text-lg font-bold text-on-brand">
            {t('app.name').charAt(0)}
          </span>
          <div>
            <p className="text-md font-bold leading-tight">{t('app.name')}</p>
            <p className="text-sm text-ink-muted">{t('app.tagline')}</p>
          </div>
        </header>

        <div role="status" aria-live="polite" className="flex items-center gap-md">
          <span className="relative flex size-3">
            {view.kind === 'checking' && (
              <span
                data-pulse
                aria-hidden="true"
                className={`absolute inline-flex size-full rounded-full opacity-60 motion-safe:animate-ping ${DOT[view.kind]}`}
              />
            )}
            <span className={`relative inline-flex size-3 rounded-full ${DOT[view.kind]}`} />
          </span>
          <div>
            {view.kind === 'checking' && (
              <>
                <p className="text-lg font-semibold">{t('status.checking')}</p>
                {/* Visible spinner ring; with reduced motion it stays a static ring with a brand-coloured arc. */}
                <span
                  role="progressbar"
                  aria-label={t('status.loading')}
                  className="mt-xs block size-5 rounded-full border-2 border-line border-t-brand motion-safe:animate-spin"
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
            className="mt-lg w-full rounded-md bg-brand px-lg py-sm font-semibold text-on-brand transition-transform duration-fast hover:brightness-110 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand active:scale-[0.98]"
          >
            {t('status.retry')}
          </button>
        )}
      </section>
    </main>
  );
}
