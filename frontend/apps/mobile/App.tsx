import { createApiClient } from '@schoolos/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import { ApiProvider } from './src/api/ApiProvider';
import { StatusScreen } from './src/features/system/StatusScreen';
import './src/i18n';

const api = createApiClient(process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080');

export default function App() {
  const [queryClient] = useState(() => new QueryClient());
  return (
    <QueryClientProvider client={queryClient}>
      <ApiProvider client={api}>
        <StatusScreen />
        <StatusBar style="dark" />
      </ApiProvider>
    </QueryClientProvider>
  );
}
