import { describe, expect, it } from 'vitest';
import { isEmailOrPhone, normalizePhone } from './normalizePhone';

/** Spec 0003, AC-3 (client side): the same cases as the backend's PhoneNumberNormalizerTest. */
describe('normalizePhone', () => {
  it.each(['05321234567', '5321234567', '+90 532 123 45 67', '(0532) 123-45-67', '00905321234567'])(
    '%s is +905321234567',
    (raw) => {
      expect(normalizePhone(raw)).toBe('+905321234567');
    },
  );

  it.each(['532123456', '53212345678', '0532123456a', 'abcdefghij', '02121234567', ''])(
    '%s is not a mobile number',
    (raw) => {
      expect(normalizePhone(raw)).toBeNull();
    },
  );
});

describe('isEmailOrPhone', () => {
  it('accepts e-mails and mobile numbers, nothing else', () => {
    expect(isEmailOrPhone('Kurucu@Ornek.test')).toBe(true);
    expect(isEmailOrPhone('0532 123 45 67')).toBe(true);
    expect(isEmailOrPhone('kurucu@')).toBe(false);
    expect(isEmailOrPhone('532123456')).toBe(false);
  });
});
