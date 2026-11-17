import '../i18n';

// docs/testing.md, Determinism: fixed time zone; nothing in this app animates on a timer under test.
process.env.TZ = 'Europe/Istanbul';
