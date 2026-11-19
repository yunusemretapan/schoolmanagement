import { z } from 'zod';
import { isEmailOrPhone } from './normalizePhone';

/**
 * Login form rules shared by web and mobile (spec 0003, AC-3 and AC-10): checked before anything is sent.
 * Messages are i18n keys; each app renders them from its tr-TR file.
 */
export const LOGIN_ERRORS = {
  required: 'auth.errors.required',
  invalidIdentifier: 'auth.errors.invalidIdentifier',
} as const;

export const loginSchema = z.object({
  identifier: z
    .string()
    .trim()
    .min(1, LOGIN_ERRORS.required)
    .refine((value) => value.length === 0 || isEmailOrPhone(value), LOGIN_ERRORS.invalidIdentifier),
  password: z.string().min(1, LOGIN_ERRORS.required),
});

export type LoginForm = z.infer<typeof loginSchema>;

export type LoginFieldErrors = Partial<Record<keyof LoginForm, string>>;

/** Field → i18n key of its first problem; empty when the form may be sent. */
export function validateLogin(input: LoginForm): LoginFieldErrors {
  const result = loginSchema.safeParse(input);
  if (result.success) {
    return {};
  }
  const errors: LoginFieldErrors = {};
  for (const issue of result.error.issues) {
    const field = issue.path[0];
    if ((field === 'identifier' || field === 'password') && errors[field] === undefined) {
      errors[field] = issue.message;
    }
  }
  return errors;
}
