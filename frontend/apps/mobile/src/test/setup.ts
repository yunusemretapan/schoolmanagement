import '../i18n';

// docs/testing.md, Determinism: fixed time zone. Reanimated runs its web implementation under Jest
// (jest.resolver in package.json); src/test/afterEnv.ts fakes the device and AsyncStorage.
process.env.TZ = 'Europe/Istanbul';
