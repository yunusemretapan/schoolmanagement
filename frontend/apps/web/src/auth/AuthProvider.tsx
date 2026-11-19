import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useApi } from '../api/ApiProvider';
import type { SessionExpiry } from './sessionExpiry';

export type AuthStatus = 'restoring' | 'signedOut' | 'signedIn';

interface AuthContextValue {
  status: AuthStatus;
  /** True after the session ended on its own; the login screen shows "Oturumunuz sona erdi". */
  expired: boolean;
  signIn(identifier: string, password: string): Promise<void>;
  signOut(): Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * Spec 0003, Req. 6–7: on start, an existing session (the refresh cookie) opens the app signed in; otherwise the
 * login screen. Signing out or a session that ends forgets every cached answer of the previous user.
 */
export function AuthProvider({ expiry, children }: { expiry: SessionExpiry; children: ReactNode }) {
  const api = useApi();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<AuthStatus>('restoring');
  const [expired, setExpired] = useState(false);

  useEffect(() => {
    let active = true;
    void api.restoreSession().then((restored) => {
      if (active) setStatus(restored ? 'signedIn' : 'signedOut');
    });
    return () => {
      active = false;
    };
  }, [api]);

  useEffect(
    () =>
      expiry.subscribe(() => {
        queryClient.clear();
        setExpired(true);
        setStatus('signedOut');
      }),
    [expiry, queryClient],
  );

  const signIn = useCallback(
    async (identifier: string, password: string) => {
      await api.login(identifier, password);
      setExpired(false);
      setStatus('signedIn');
    },
    [api],
  );

  const signOut = useCallback(async () => {
    await api.logout();
    queryClient.clear();
    setExpired(false);
    setStatus('signedOut');
  }, [api, queryClient]);

  const value = useMemo(() => ({ status, expired, signIn, signOut }), [status, expired, signIn, signOut]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (value === null) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return value;
}
