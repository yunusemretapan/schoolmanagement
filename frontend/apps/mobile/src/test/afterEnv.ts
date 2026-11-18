import { device } from './device';

// The device's colour scheme is driven by the test fake (src/test/device.ts).
jest.mock('react-native/Libraries/Utilities/useColorScheme', () => ({
  __esModule: true,
  default: jest.requireActual<typeof import('./device')>('./device').useFakeColorScheme,
}));
jest.mock('@react-native-async-storage/async-storage', () =>
  jest.requireActual('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

beforeEach(() => {
  device.reset();
});
