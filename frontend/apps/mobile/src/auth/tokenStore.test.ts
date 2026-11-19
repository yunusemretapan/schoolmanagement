import * as SecureStore from 'expo-secure-store';
import { createDeviceTokenStore, REFRESH_KEY } from './tokenStore';

/** Plan 0003, R2: the refresh token lives in SecureStore on a phone, in memory only on Expo web. */
describe('createDeviceTokenStore', () => {
  it('keeps the refresh token in SecureStore on a phone and deletes it on sign-out', async () => {
    const store = createDeviceTokenStore('ios');

    await store.saveRefreshToken('r1');
    expect(SecureStore.setItemAsync).toHaveBeenCalledWith(REFRESH_KEY, 'r1');
    await expect(store.loadRefreshToken()).resolves.toBe('r1');

    await store.saveRefreshToken(null);
    expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith(REFRESH_KEY);
    await expect(store.loadRefreshToken()).resolves.toBeNull();
  });

  it('never touches SecureStore on Expo web', async () => {
    jest.mocked(SecureStore.setItemAsync).mockClear();
    const store = createDeviceTokenStore('web');

    await store.saveRefreshToken('r1');

    await expect(store.loadRefreshToken()).resolves.toBe('r1');
    expect(SecureStore.setItemAsync).not.toHaveBeenCalled();
  });

  it('starts without an access token', () => {
    expect(createDeviceTokenStore('android').accessToken).toBeNull();
  });
});
