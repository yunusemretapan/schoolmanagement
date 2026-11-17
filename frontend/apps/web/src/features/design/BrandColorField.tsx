import { parseHexColor } from '@schoolos/design-tokens';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAppearance } from '../../appearance/AppearanceProvider';

/**
 * Spec 0002 AC-5, AC-6: preview an institution colour. A valid #RRGGBB applies at once; an invalid entry keeps
 * the configured / default brand and says why; empty means "default" without an error. Never persisted.
 */
export function BrandColorField() {
  const { t } = useTranslation();
  const { setBrandPreview } = useAppearance();
  const [value, setValue] = useState('');
  const inputId = useId();
  const errorId = useId();
  const invalid = value.trim() !== '' && parseHexColor(value) === null;

  const onChange = (next: string) => {
    setValue(next);
    setBrandPreview(parseHexColor(next));
  };

  return (
    <div className="flex flex-col gap-xs">
      <label htmlFor={inputId} className="text-sm font-semibold text-ink">
        {t('showcase.brand.label')}
      </label>
      <div className="flex flex-wrap items-center gap-sm">
        <input
          id={inputId}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={t('showcase.brand.placeholder')}
          aria-invalid={invalid}
          aria-describedby={invalid ? errorId : undefined}
          spellCheck={false}
          autoComplete="off"
          className="min-h-control grow rounded-md border border-line-strong bg-surface px-md font-medium text-ink outline-focus aria-[invalid=true]:border-danger focus-visible:outline-[length:var(--sos-border-thick)]"
        />
        <button
          type="button"
          onClick={() => onChange('')}
          className="min-h-control rounded-md bg-sunken px-md text-sm font-medium text-ink outline-focus focus-visible:outline-[length:var(--sos-border-thick)]"
        >
          {t('showcase.brand.reset')}
        </button>
      </div>
      {invalid && (
        <p id={errorId} role="alert" className="text-sm font-medium text-danger">
          {t('showcase.brand.invalid')}
        </p>
      )}
    </div>
  );
}
