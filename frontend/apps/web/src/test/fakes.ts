/**
 * Test doubles for browser APIs jsdom lacks (plan 0002, R4): a matchMedia whose answers a test can flip,
 * and an in-memory Storage.
 */
type Listener = () => void;

export interface FakeMedia {
  set(query: string, matches: boolean): void;
  install(): void;
}

export function createFakeMedia(initial: Record<string, boolean> = {}): FakeMedia {
  const state = new Map<string, boolean>(Object.entries(initial));
  const listeners = new Map<string, Set<Listener>>();
  const listFor = (query: string): MediaQueryList =>
    ({
      get matches() {
        return state.get(query) ?? false;
      },
      media: query,
      onchange: null,
      addEventListener: (_type: string, listener: Listener) => {
        if (!listeners.has(query)) listeners.set(query, new Set());
        listeners.get(query)?.add(listener);
      },
      removeEventListener: (_type: string, listener: Listener) => listeners.get(query)?.delete(listener),
      addListener: () => undefined,
      removeListener: () => undefined,
      dispatchEvent: () => true,
    }) as unknown as MediaQueryList;
  return {
    set(query, matches) {
      state.set(query, matches);
      listeners.get(query)?.forEach((listener) => listener());
    },
    install() {
      window.matchMedia = listFor;
    },
  };
}

export function createMemoryStorage(initial: Record<string, string> = {}): Storage {
  const data = new Map<string, string>(Object.entries(initial));
  return {
    get length() {
      return data.size;
    },
    clear: () => data.clear(),
    getItem: (key) => data.get(key) ?? null,
    key: (index) => [...data.keys()][index] ?? null,
    removeItem: (key) => void data.delete(key),
    setItem: (key, value) => void data.set(key, String(value)),
  };
}

export const DARK_QUERY = '(prefers-color-scheme: dark)';
export const REDUCE_QUERY = '(prefers-reduced-motion: reduce)';
