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
  it('rejects hostile long input quickly (review 1, H-2)', () => {
    const start = performance.now();
    expect(isEmailOrPhone(`a@${'a.'.repeat(40_000)}@`)).toBe(false);
    expect(isEmailOrPhone(`${'a'.repeat(250)}@b.co`)).toBe(false);
    expect(normalizePhone('5'.repeat(40_000))).toBeNull();
    expect(performance.now() - start).toBeLessThan(500);
  });

  it('domain labels must be non-empty', () => {
    expect(isEmailOrPhone('veli@ornek..test')).toBe(false);
    expect(isEmailOrPhone('veli@okul.k12.tr')).toBe(true);
  });

  it('accepts e-mails and mobile numbers, nothing else', () => {
    expect(isEmailOrPhone('Kurucu@Ornek.test')).toBe(true);
    expect(isEmailOrPhone('0532 123 45 67')).toBe(true);
    expect(isEmailOrPhone('kurucu@')).toBe(false);
    expect(isEmailOrPhone('532123456')).toBe(false);
  });
});
