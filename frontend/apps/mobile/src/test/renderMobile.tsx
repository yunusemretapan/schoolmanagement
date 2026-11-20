import type { ApiClient, CurrentUser } from '@schoolos/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react-native';
import { ApiProvider } from '../api/ApiProvider';
import { AppearanceProvider } from '../appearance/AppearanceProvider';
import { AuthProvider } from '../auth/AuthProvider';
import { createSessionExpiry, type SessionExpiry } from '../auth/sessionExpiry';
import { Screens } from '../Screens';
import { createMemoryStorage } from './device';

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
    listStaff: notStubbed,
    getStaff: notStubbed,
    inviteStaff: notStubbed,
    updateStaff: notStubbed,
    renewInvitation: notStubbed,
    deactivateStaff: notStubbed,
    reactivateStaff: notStubbed,
    getInvitation: notStubbed,
    acceptInvitation: notStubbed,
    ...overrides,
  };
}

/** Staff and invitation calls (spec 0004) fail loudly unless a test stubs them. */
async function notStubbed(): Promise<never> {
  throw new Error('not stubbed in this test');
}

/** The real screen switch with every provider, as App.tsx builds it. */
export async function renderMobile({
  api = fakeApi(),
  expiry = createSessionExpiry(),
}: { api?: ApiClient; expiry?: SessionExpiry } = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } });
  await render(
    <QueryClientProvider client={client}>
      <ApiProvider client={api}>
        <AppearanceProvider storage={createMemoryStorage()} configuredBrand={null}>
          <AuthProvider expiry={expiry}>
            <Screens />
          </AuthProvider>
        </AppearanceProvider>
      </ApiProvider>
    </QueryClientProvider>,
  );
  return { expiry, client };
}
