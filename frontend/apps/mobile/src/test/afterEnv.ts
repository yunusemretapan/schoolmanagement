import { device } from './device';

// The device's colour scheme is driven by the test fake (src/test/device.ts).
jest.mock('react-native/Libraries/Utilities/useColorScheme', () => ({
  __esModule: true,
  default: jest.requireActual<typeof import('./device')>('./device').useFakeColorScheme,
}));
jest.mock('@react-native-async-storage/async-storage', () =>
  jest.requireActual('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

// SecureStore has no native module under Jest: an in-memory keychain (src/auth/tokenStore.ts).
jest.mock('expo-secure-store', () => {
  const items = new Map<string, string>();
  return {
    getItemAsync: jest.fn(async (key: string) => items.get(key) ?? null),
    setItemAsync: jest.fn(async (key: string, value: string) => void items.set(key, value)),
    deleteItemAsync: jest.fn(async (key: string) => void items.delete(key)),
    __reset: () => items.clear(),
  };
});

beforeEach(() => {
  device.reset();
});
