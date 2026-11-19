import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useApi } from '../api/ApiProvider';
import type { SessionExpiry } from './sessionExpiry';

/** unreachable: the stored session could not be checked (offline); it is kept for retry (review 1, L-2). */
export type AuthStatus = 'restoring' | 'unreachable' | 'signedOut' | 'signedIn';

interface AuthContextValue {
  status: AuthStatus;
  /** True after the session ended on its own; the login screen shows "Oturumunuz sona erdi". */
  expired: boolean;
  signIn(identifier: string, password: string): Promise<void>;
  signOut(): Promise<void>;
  /** Checks the stored session again after "unreachable". */
  retry(): void;
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
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    api.restoreSession().then(
      (restored) => {
        if (active) setStatus(restored ? 'signedIn' : 'signedOut');
      },
      () => {
        if (active) setStatus('unreachable');
      },
    );
    return () => {
      active = false;
    };
  }, [api, attempt]);

  const retry = useCallback(() => {
    setStatus('restoring');
    setAttempt((n) => n + 1);
  }, []);

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

  const value = useMemo(
    () => ({ status, expired, signIn, signOut, retry }),
    [status, expired, signIn, signOut, retry],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (value === null) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return value;
}
