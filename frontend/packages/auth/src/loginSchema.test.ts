import { describe, expect, it } from 'vitest';
import { LOGIN_ERRORS, validateLogin } from './loginSchema';

/** Spec 0003, AC-3 and AC-10: problems are found before anything is sent. */
describe('validateLogin', () => {
  it('requires both fields', () => {
    expect(validateLogin({ identifier: '  ', password: '' })).toEqual({
      identifier: LOGIN_ERRORS.required,
      password: LOGIN_ERRORS.required,
    });
  });

  it.each(['532123456', '0532abc4567', 'kurucu@', 'a b@c.d'])('rejects %s as an identifier', (identifier) => {
    expect(validateLogin({ identifier, password: 'x' })).toEqual({
      identifier: LOGIN_ERRORS.invalidIdentifier,
    });
  });

  it.each(['veli@ornek.test', '05321234567', '+90 532 123 45 67'])('accepts %s', (identifier) => {
    expect(validateLogin({ identifier, password: 'x' })).toEqual({});
  });
});
