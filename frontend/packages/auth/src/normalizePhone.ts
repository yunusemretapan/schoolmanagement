/**
 * Turkish mobile numbers to one form, `+905XXXXXXXXX` (spec 0003, Req. 2). Mirrors the backend's
 * PhoneNumberNormalizer: the server still decides, this only lets the form catch mistakes before sending.
 */
const SEPARATORS = /[\s\-()]/g;
const MOBILE = /^5\d{9}$/;

export function normalizePhone(raw: string): string | null {
  const digits = raw.trim().replace(SEPARATORS, '');
  let national: string;
  if (digits.startsWith('+90')) {
    national = digits.slice(3);
  } else if (digits.startsWith('0090')) {
    national = digits.slice(4);
  } else if (digits.startsWith('90') && digits.length === 12) {
    national = digits.slice(2);
  } else if (digits.startsWith('0') && digits.length === 11) {
    national = digits.slice(1);
  } else {
    national = digits;
  }
  return MOBILE.test(national) ? `+90${national}` : null;
}

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

/** An e-mail if it holds an `@`, otherwise a phone number — the same rule as the backend. */
export function isEmailOrPhone(raw: string): boolean {
  const value = raw.trim();
  return value.includes('@') ? EMAIL.test(value) : normalizePhone(value) !== null;
}
