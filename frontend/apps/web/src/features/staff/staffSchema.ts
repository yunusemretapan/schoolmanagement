import type { Role } from '@schoolos/api-client';
import { isEmailOrPhone, normalizePhone } from '@schoolos/auth';

/** Roles staff management assigns (spec 0004, Req. 4); Veli comes with student records. */
export const STAFF_ROLES = ['ADMIN', 'TEACHER', 'ACCOUNTANT'] as const satisfies readonly Role[];
export type StaffRole = (typeof STAFF_ROLES)[number];

export interface StaffForm {
  fullName: string;
  email: string;
  phone: string;
  roles: StaffRole[];
}

export type StaffFieldErrors = Partial<Record<keyof StaffForm, string>>;

/** Field → i18n key of its first problem; empty when the form may be sent (spec 0004, AC-4: nothing sent). */
export function validateStaff(form: StaffForm): StaffFieldErrors {
  const errors: StaffFieldErrors = {};
  const email = form.email.trim();
  const phone = form.phone.trim();
  if (form.fullName.trim().length === 0) {
    errors.fullName = 'staff.errors.required';
  }
  if (email.length === 0 && phone.length === 0) {
    errors.email = 'staff.errors.emailOrPhone';
  } else {
    if (email.length > 0 && !(email.includes('@') && isEmailOrPhone(email))) {
      errors.email = 'staff.errors.invalidEmail';
    }
    if (phone.length > 0 && normalizePhone(phone) === null) {
      errors.phone = 'staff.errors.invalidPhone';
    }
  }
  if (form.roles.length === 0) {
    errors.roles = 'staff.errors.roleRequired';
  }
  return errors;
}

/** The request body: blanks become null, roles in a stable order. */
export function staffBody(form: StaffForm) {
  return {
    fullName: form.fullName.trim(),
    email: form.email.trim() === '' ? null : form.email.trim(),
    phone: form.phone.trim() === '' ? null : form.phone.trim(),
    roles: STAFF_ROLES.filter((role) => form.roles.includes(role)),
  };
}
