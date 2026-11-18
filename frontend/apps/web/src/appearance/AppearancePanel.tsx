import {
  APPEARANCE_OPTIONS,
  MOTION_OPTIONS,
  tokens,
  type AppearancePreference,
  type MotionPreference,
} from '@schoolos/design-tokens';
import { Popover, RadioGroup } from 'radix-ui';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';
import { useAppearance } from './AppearanceProvider';

/**
 * Spec 0002 AC-1, AC-2, AC-10: the "Görünüm" button and its panel. Radix gives the keyboard contract:
 * focus moves into the panel on open and back to the button on close, Esc / outside click close it,
 * arrow keys move within a radio group, Tab moves between groups.
 */
export function AppearancePanel() {
  const { t } = useTranslation();
  const { appearance, setAppearance, motion, setMotion } = useAppearance();

  return (
    <Popover.Root>
      <Popover.Trigger className="min-h-control rounded-md border border-line-strong bg-surface px-md text-sm font-medium text-ink outline-focus hover:bg-sunken focus-visible:outline-[length:var(--sos-border-thick)] focus-visible:outline-offset-[var(--sos-border-thick)]">
        {t('appearance.open')}
      </Popover.Trigger>
      <Popover.Portal>
        <Popover.Content
          align="end"
          sideOffset={tokens.spacing.sm}
          aria-label={t('appearance.title')}
          className="z-[var(--sos-layer-overlay)] w-dvw max-w-card rounded-lg border border-line bg-surface p-lg font-sans text-ink shadow-card outline-none motion-ok:transition-opacity motion-ok:duration-[var(--sos-duration-fast)] motion-ok:ease-enter"
        >
          <div className="flex flex-col gap-lg">
            <OptionGroup<AppearancePreference>
              label={t('appearance.scheme.label')}
              value={appearance}
              options={APPEARANCE_OPTIONS}
              optionLabel={(option) => t(`appearance.scheme.${option}`)}
              onChange={setAppearance}
            />
            <OptionGroup<MotionPreference>
              label={t('appearance.motion.label')}
              value={motion}
              options={MOTION_OPTIONS}
              optionLabel={(option) => t(`appearance.motion.${option}`)}
              onChange={setMotion}
            />
          </div>
          <Popover.Close className="mt-lg min-h-control w-full rounded-md bg-sunken px-md text-sm font-medium text-ink outline-focus focus-visible:outline-[length:var(--sos-border-thick)]">
            {t('appearance.close')}
          </Popover.Close>
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  );
}

function OptionGroup<T extends string>({
  label,
  value,
  options,
  optionLabel,
  onChange,
}: {
  label: string;
  value: T;
  options: readonly T[];
  optionLabel: (option: T) => string;
  onChange: (value: T) => void;
}) {
  const labelId = useId();
  return (
    <div className="flex flex-col gap-sm">
      <p id={labelId} className="text-sm font-semibold text-ink-muted">
        {label}
      </p>
      <RadioGroup.Root
        aria-labelledby={labelId}
        value={value}
        onValueChange={(next) => onChange(next as T)}
        className="grid grid-cols-3 gap-xs rounded-md bg-sunken p-xxs"
      >
        {options.map((option) => (
          <RadioGroup.Item
            key={option}
            value={option}
            className="min-h-control rounded-sm px-sm text-sm font-medium text-ink-muted outline-focus data-[state=checked]:bg-surface data-[state=checked]:text-ink data-[state=checked]:shadow-card focus-visible:outline-[length:var(--sos-border-thick)] motion-ok:transition-colors motion-ok:duration-[var(--sos-duration-fast)]"
          >
            {optionLabel(option)}
          </RadioGroup.Item>
        ))}
      </RadioGroup.Root>
    </div>
  );
}
