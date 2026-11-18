import appJson from '../app.json';
import packageJson from '../package.json';

/**
 * Spec 0002 AC-1 (review 1, H-1): in a native build useColorScheme() only follows the device when the app
 * declares "automatic"; "light" locks it, and the test fake (src/test/device.ts) cannot see that.
 * Android also needs expo-system-ui to apply the setting.
 */
describe('app config (mobile)', () => {
  it('lets the device choose light or dark', () => {
    expect(appJson.expo.userInterfaceStyle).toBe('automatic');
    expect(packageJson.dependencies).toHaveProperty('expo-system-ui');
  });
});
