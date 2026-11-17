import type { ApiClient } from '@schoolos/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiProvider } from './api/ApiProvider';
import { StatusScreen } from './features/system/StatusScreen';
import './i18n';

export function App({ api }: { api: ApiClient }) {
  const [queryClient] = useState(() => new QueryClient());
  return (
    <QueryClientProvider client={queryClient}>
      <ApiProvider client={api}>
        <StatusScreen />
      </ApiProvider>
    </QueryClientProvider>
  );
}
