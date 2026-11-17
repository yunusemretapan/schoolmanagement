import { contrastRatio, deriveBrand, tokens } from '@schoolos/design-tokens';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { AppearancePanel } from '../../appearance/AppearancePanel';
import { useAppearance } from '../../appearance/AppearanceProvider';
import { BrandColorField } from './BrandColorField';

// Tailwind only emits classes it can see written out, so every scale step is listed explicitly.
const TEXT_CLASS = {
  xs: 'text-xs',
  sm: 'text-sm',
  md: 'text-md',
  lg: 'text-lg',
  xl: 'text-xl',
  xxl: 'text-xxl',
  display: 'text-display',
} as const satisfies Record<keyof typeof tokens.typography.scale, string>;

const RADIUS_CLASS = {
  sm: 'rounded-sm',
  md: 'rounded-md',
  lg: 'rounded-lg',
  pill: 'rounded-pill',
} as const satisfies Record<keyof typeof tokens.radius, string>;

/** Spec 0002 AC-9: every token family on one page, live with the appearance, motion and brand choice. */
export function DesignShowcase() {
  const { t } = useTranslation();

  return (
    <main className="min-h-dvh bg-canvas p-lg font-sans text-ink">
      <div className="mx-auto flex max-w-content flex-col gap-xl">
        <header className="flex flex-wrap items-center gap-md">
          <div className="grow">
            <h1 className="text-xxl font-bold">{t('showcase.title')}</h1>
            <p className="text-md text-ink-muted">{t('showcase.subtitle')}</p>
          </div>
          <AppearancePanel />
        </header>

        <Section title={t('showcase.brand.title')}>
          <BrandColorField />
        </Section>
        <ColorSection />
        <TypographySection />
        <ScaleSection />
        <MotionSection />
      </div>
    </main>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section
      aria-label={title}
      className="flex flex-col gap-md rounded-lg border border-line bg-surface p-lg shadow-card"
    >
      <h2 className="text-lg font-semibold">{title}</h2>
      {children}
    </section>
  );
}

function ColorSection() {
  const { t } = useTranslation();
  const { scheme, brandColor } = useAppearance();
  const s = tokens.semantic[scheme];
  const brand = deriveBrand(brandColor, scheme);
  const roles: { name: string; cssVar: string; fg: string; on: string }[] = [
    { name: 'text.primary', cssVar: '--sos-text-primary', fg: s.text.primary, on: s.bg.surface },
    { name: 'text.secondary', cssVar: '--sos-text-secondary', fg: s.text.secondary, on: s.bg.surface },
    { name: 'brand.text', cssVar: '--sos-brand-text', fg: brand.text, on: s.bg.surface },
    { name: 'brand.solid', cssVar: '--sos-brand-solid', fg: brand.onSolid, on: brand.solid },
    { name: 'status.success', cssVar: '--sos-status-success', fg: s.status.success, on: s.bg.surface },
    { name: 'status.danger', cssVar: '--sos-status-danger', fg: s.status.danger, on: s.bg.surface },
    { name: 'status.pending', cssVar: '--sos-status-pending', fg: s.status.pending, on: s.bg.surface },
    { name: 'bg.canvas', cssVar: '--sos-bg-canvas', fg: s.text.primary, on: s.bg.canvas },
    { name: 'bg.sunken', cssVar: '--sos-bg-sunken', fg: s.text.primary, on: s.bg.sunken },
    { name: 'border.control', cssVar: '--sos-border-control', fg: s.border.control, on: s.bg.surface },
  ];
  return (
    <Section title={t('showcase.colors.title')}>
      <ul className="flex flex-wrap gap-sm">
        {roles.map((role) => (
          <li
            key={role.name}
            className="flex grow basis-[var(--sos-layout-card-max-width)] items-center gap-md rounded-md bg-sunken p-sm"
          >
            <span
              aria-hidden="true"
              className="size-swatch shrink-0 rounded-md border border-line"
              style={{ background: `var(${role.cssVar})` }}
            />
            <span className="flex flex-col">
              <span className="text-sm font-semibold">{role.name}</span>
              <span className="text-xs text-ink-muted">
                {t('showcase.colors.contrast', { ratio: contrastRatio(role.fg, role.on).toFixed(2) })}
              </span>
            </span>
          </li>
        ))}
      </ul>
    </Section>
  );
}

function TypographySection() {
  const { t } = useTranslation();
  return (
    <Section title={t('showcase.typography.title')}>
      <ul className="flex flex-col gap-sm">
        {(Object.keys(TEXT_CLASS) as (keyof typeof TEXT_CLASS)[]).map((step) => (
          <li key={step} className="flex items-baseline gap-md">
            <span className="w-label-column shrink-0 whitespace-nowrap text-xs text-ink-muted">
              {t('showcase.typography.step', {
                step,
                size: tokens.typography.scale[step].size,
                lineHeight: tokens.typography.scale[step].lineHeight,
              })}
            </span>
            <span className={`${TEXT_CLASS[step]} font-semibold`}>{t('showcase.typography.sample')}</span>
          </li>
        ))}
      </ul>
    </Section>
  );
}

function ScaleSection() {
  const { t } = useTranslation();
  return (
    <Section title={t('showcase.scales.title')}>
      <div className="flex flex-col gap-sm">
        {Object.entries(tokens.spacing).map(([name, value]) => (
          <div key={name} className="flex items-center gap-md">
            <span className="w-label-column shrink-0 whitespace-nowrap text-xs text-ink-muted">
              {t('showcase.scales.spacing', { name, value })}
            </span>
            <span
              aria-hidden="true"
              className="h-sm rounded-sm bg-brand"
              style={{ width: `var(--sos-spacing-${name})` }}
            />
          </div>
        ))}
      </div>
      <div className="flex flex-wrap gap-md">
        {(Object.keys(RADIUS_CLASS) as (keyof typeof RADIUS_CLASS)[]).map((name) => (
          <div
            key={name}
            className={`grid size-swatch place-items-center border border-line-strong bg-sunken text-xs ${RADIUS_CLASS[name]}`}
          >
            {name}
          </div>
        ))}
        <div className="grid place-items-center rounded-lg bg-surface px-lg text-xs shadow-card">
          {t('showcase.scales.shadow')}
        </div>
      </div>
    </Section>
  );
}

function MotionSection() {
  const { t } = useTranslation();
  const { reducedMotion } = useAppearance();
  const [moved, setMoved] = useState(false);
  return (
    <Section title={t('showcase.motion.title')}>
      <p className="text-sm text-ink-muted">
        {reducedMotion ? t('showcase.motion.reduced') : t('showcase.motion.normal')}
      </p>
      <ul className="flex flex-wrap gap-md text-sm">
        {Object.entries(tokens.motion.durationMs).map(([name, ms]) => (
          <li key={name} className="rounded-md bg-sunken px-sm py-xs">
            {t('showcase.motion.duration', { name, ms })}
          </li>
        ))}
      </ul>
      <div className="flex items-center gap-md">
        <button
          type="button"
          onClick={() => setMoved((m) => !m)}
          className="min-h-control rounded-md bg-brand px-md text-sm font-semibold text-on-brand outline-focus focus-visible:outline-[length:var(--sos-border-thick)] focus-visible:outline-offset-[var(--sos-border-thick)]"
        >
          {t('showcase.motion.play')}
        </button>
        <div className="h-logo grow rounded-pill bg-sunken p-xs">
          <span
            data-testid="motion-dot"
            data-moved={moved}
            aria-hidden="true"
            className={`block size-spinner rounded-pill bg-brand motion-ok:transition-transform motion-ok:duration-[var(--sos-duration-slow)] motion-ok:ease-standard ${moved ? 'translate-x-[var(--sos-size-glow-blur)]' : ''}`}
          />
        </div>
      </div>
    </Section>
  );
}
