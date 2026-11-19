/**
 * The API client reports "the session is over" here (AuthOptions.onSessionExpired); AuthProvider listens and sends
 * the user to the login screen with the "session ended" notice (spec 0003, AC-8).
 */
type Listener = () => void;

export interface SessionExpiry {
  notify(): void;
  subscribe(listener: Listener): () => void;
}

export function createSessionExpiry(): SessionExpiry {
  const listeners = new Set<Listener>();
  return {
    notify: () => listeners.forEach((listener) => listener()),
    subscribe(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };
}
