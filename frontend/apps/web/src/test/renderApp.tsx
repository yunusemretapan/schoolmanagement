import type { ApiClient, CurrentUser } from '@schoolos/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import { useEffect } from 'react';
import { MemoryRouter, useLocation, useNavigate, type NavigateFunction } from 'react-router';
import { AppRoutes } from '../App';
import { ApiProvider } from '../api/ApiProvider';
import { AppearanceProvider } from '../appearance/AppearanceProvider';
import { AuthProvider } from '../auth/AuthProvider';
import { createSessionExpiry, type SessionExpiry } from '../auth/sessionExpiry';
import { createMemoryStorage } from './fakes';

export const ME: CurrentUser = {
  userId: '01900000-0000-7000-8000-000000000001',
  fullName: 'Ayşe Yılmaz',
  institutionName: 'Örnek Koleji',
  roles: ['TEACHER', 'GUARDIAN'],
};

/** A complete ApiClient whose methods a test overrides; the defaults describe a signed-out user. */
export function fakeApi(overrides: Partial<ApiClient> = {}): ApiClient {
  return {
    getSystemStatus: async () => ({ status: 'UP', version: '0.1.0' }),
    login: async () => undefined,
    restoreSession: async () => false,
    logout: async () => undefined,
    getMe: async () => ME,
    ...overrides,
  };
}

let currentPath = '';
let navigateFn: NavigateFunction | null = null;

/** Records the router's current path (and its navigate) so tests can assert where the user ended up. */
function LocationProbe() {
  const location = useLocation();
  const navigate = useNavigate();
  useEffect(() => {
    currentPath = location.pathname;
    navigateFn = navigate;
  }, [location, navigate]);
  return null;
}

export function pathname(): string {
  return currentPath;
}

/** The browser's Back button, through the router's own history. */
export async function goBack(): Promise<void> {
  await navigateFn?.(-1);
}

/** The real routes and providers, in a MemoryRouter starting at `path`. */
export function renderApp({
  api = fakeApi(),
  path = '/',
  history,
  expiry = createSessionExpiry(),
}: {
  api?: ApiClient;
  path?: string;
  history?: string[];
  expiry?: SessionExpiry;
} = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } });
  const entries = history ?? [path];
  const result = render(
    <MemoryRouter initialEntries={entries} initialIndex={entries.length - 1}>
      <AppearanceProvider storage={createMemoryStorage()} configuredBrand={null}>
        <QueryClientProvider client={queryClient}>
          <ApiProvider client={api}>
            <AuthProvider expiry={expiry}>
              <AppRoutes />
              <LocationProbe />
            </AuthProvider>
          </ApiProvider>
        </QueryClientProvider>
      </AppearanceProvider>
    </MemoryRouter>,
  );
  return { ...result, expiry, queryClient };
}
