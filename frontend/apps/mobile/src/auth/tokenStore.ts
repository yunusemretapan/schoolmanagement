import { createMemoryTokenStore, type TokenStore } from '@schoolos/api-client';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

export const REFRESH_KEY = 'sos.refreshToken';

/**
 * Plan 0003, R2: on a phone the refresh token lives in the OS keychain/keystore (SecureStore), so the session survives
 * restarts for 30 days. Expo web is a development surface without SecureStore: memory only, sign in again after a
 * reload. The access token is memory-only everywhere.
 */
export function createDeviceTokenStore(platform: string = Platform.OS): TokenStore {
  if (platform === 'web') {
    return createMemoryTokenStore();
  }
  return {
    accessToken: null,
    loadRefreshToken: () => SecureStore.getItemAsync(REFRESH_KEY),
    saveRefreshToken: async (token) => {
      if (token === null) {
        await SecureStore.deleteItemAsync(REFRESH_KEY);
      } else {
        await SecureStore.setItemAsync(REFRESH_KEY, token);
      }
    },
  };
}
