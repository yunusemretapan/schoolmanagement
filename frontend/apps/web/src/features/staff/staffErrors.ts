import { ApiError, ERROR_CODES } from '@schoolos/api-client';

/** API error → i18n key of the message the staff screens show (spec 0004, UX states). */
export function staffErrorKey(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.code) {
      case ERROR_CODES.identifierTaken:
        return 'staff.errors.identifierTaken';
      case ERROR_CODES.identifierLocked:
        return 'staff.errors.identifierLocked';
      case ERROR_CODES.lastAdmin:
        return 'staff.errors.lastAdmin';
      case ERROR_CODES.selfChange:
        return 'staff.errors.selfChange';
      case ERROR_CODES.staffState:
        return 'staff.errors.staffState';
      case ERROR_CODES.notFound:
        return 'staff.errors.notFound';
      case ERROR_CODES.validationFailed:
        return 'staff.errors.invalid';
      default:
        if (error.status !== undefined && error.status < 500) return 'staff.errors.unexpected';
    }
  }
  return 'auth.errors.network';
}
