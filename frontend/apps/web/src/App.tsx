import type { ApiClient } from '@schoolos/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiProvider } from './api/ApiProvider';
import { AppearanceProvider } from './appearance/AppearanceProvider';
import { DesignShowcase } from './features/design/DesignShowcase';
import { StatusScreen } from './features/system/StatusScreen';
import './i18n';

/** Two screens only: a pathname switch is enough until spec 0003 decides on a router (plan 0002, R6). */
export function App({ api, path = window.location.pathname }: { api: ApiClient; path?: string }) {
  const [queryClient] = useState(() => new QueryClient());
  return (
    <AppearanceProvider>
      <QueryClientProvider client={queryClient}>
        <ApiProvider client={api}>
          {path.startsWith('/tasarim') ? <DesignShowcase /> : <StatusScreen />}
        </ApiProvider>
      </QueryClientProvider>
    </AppearanceProvider>
  );
}
