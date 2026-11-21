import { ApiError, ERROR_CODES } from '@schoolos/api-client';

/** Spec 0005, Req. 4, 7: the same limits the server applies, checked before anything is sent. */
export const NAME_MAX = 60;
export const CLASS_NAME_MAX = 20;
export const TERM_MAX_DAYS = 731;
export const EARLIEST = '2000-01-01';
export const LATEST = '2100-12-31';

export type TermField = 'name' | 'startsOn' | 'endsOn';
/** Field → i18n key of its message. */
export type FieldErrors = Partial<Record<string, string>>;

/** The message key for a name, measured after trimming; undefined when valid. */
export function nameError(value: string, max: number): string | undefined {
  const name = value.trim();
  if (name.length === 0) return 'academic.errors.required';
  if (name.length > max)
    return max === CLASS_NAME_MAX ? 'academic.errors.tooLong20' : 'academic.errors.tooLong60';
  return undefined;
}

/** Days from start to end, both included; dates are ISO calendar days without a time zone. */
export function daysInclusive(startsOn: string, endsOn: string): number {
  const day = 24 * 60 * 60 * 1000;
  return Math.round((Date.parse(`${endsOn}T00:00:00Z`) - Date.parse(`${startsOn}T00:00:00Z`)) / day) + 1;
}

export function termErrors(form: { name: string; startsOn: string; endsOn: string }): FieldErrors {
  const errors: FieldErrors = {};
  const name = nameError(form.name, NAME_MAX);
  if (name) errors.name = name;
  for (const field of ['startsOn', 'endsOn'] as const) {
    const value = form[field];
    if (value === '') errors[field] = 'academic.errors.required';
    else if (value < EARLIEST || value > LATEST) errors[field] = 'academic.errors.dateRange';
  }
  if (errors.startsOn === undefined && errors.endsOn === undefined) {
    if (form.endsOn <= form.startsOn) errors.endsOn = 'academic.errors.endBeforeStart';
    else if (daysInclusive(form.startsOn, form.endsOn) > TERM_MAX_DAYS)
      errors.endsOn = 'academic.errors.termTooLong';
  }
  return errors;
}

const FIELD_CODES: Record<string, string> = {
  NOT_BLANK: 'academic.errors.required',
  NOT_NULL: 'academic.errors.required',
  END_NOT_AFTER_START: 'academic.errors.endBeforeStart',
  TOO_LONG: 'academic.errors.termTooLong',
  OUT_OF_RANGE: 'academic.errors.dateRange',
};

/** Field errors the server sent (VALIDATION_FAILED), as message keys. */
export function serverFieldErrors(error: unknown, nameMax = NAME_MAX): FieldErrors {
  if (!(error instanceof ApiError) || error.code !== ERROR_CODES.validationFailed) return {};
  const errors: FieldErrors = {};
  for (const [field, code] of Object.entries(error.fieldErrors)) {
    errors[field] =
      code === 'SIZE'
        ? nameMax === CLASS_NAME_MAX
          ? 'academic.errors.tooLong20'
          : 'academic.errors.tooLong60'
        : (FIELD_CODES[code] ?? 'academic.errors.invalid');
  }
  return errors;
}

/** API error → i18n key of the message the academic screen shows (spec 0005, UX states). */
export function academicErrorKey(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.code) {
      case ERROR_CODES.academicNameTaken:
        return 'academic.errors.nameTaken';
      case ERROR_CODES.termOverlap:
        return 'academic.errors.termOverlap';
      case ERROR_CODES.lastCampus:
        return 'academic.blocked.lastCampus';
      case ERROR_CODES.activeTerm:
        return 'academic.blocked.activeTerm';
      case ERROR_CODES.inUse:
        return 'academic.blocked.inUse';
      case ERROR_CODES.gradeLevelLimit:
        return 'academic.errors.levelLimit';
      case ERROR_CODES.gradeLevelsNotEmpty:
        return 'academic.errors.levelsNotEmpty';
      case ERROR_CODES.cannotMove:
        return 'academic.errors.cannotMove';
      case ERROR_CODES.noTerm:
        return 'academic.empty.needTerm';
      case ERROR_CODES.notFound:
        return 'academic.errors.notFound';
      case ERROR_CODES.validationFailed:
        return 'academic.errors.invalid';
      default:
        if (error.status !== undefined && error.status < 500) return 'academic.errors.unexpected';
    }
  }
  return 'auth.errors.network';
}

/** "14 Eylül 2026" for an ISO calendar day (no time zone shift). */
export function formatDay(iso: string): string {
  return new Intl.DateTimeFormat('tr-TR', { dateStyle: 'long', timeZone: 'UTC' }).format(
    new Date(`${iso}T00:00:00Z`),
  );
}
