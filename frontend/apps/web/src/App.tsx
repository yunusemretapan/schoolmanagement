import type { ApiClient } from '@schoolos/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { Navigate, Route, Routes } from 'react-router';
import { ApiProvider } from './api/ApiProvider';
import { AppearanceProvider } from './appearance/AppearanceProvider';
import { AuthProvider } from './auth/AuthProvider';
import { LOGIN_PATH, RequireAuth } from './auth/RequireAuth';
import type { SessionExpiry } from './auth/sessionExpiry';
import { LoginScreen } from './features/auth/LoginScreen';
import { DesignShowcase } from './features/design/DesignShowcase';
import { HomeScreen } from './features/home/HomeScreen';
import { StatusScreen } from './features/system/StatusScreen';
import './i18n';

/**
 * Routes (spec 0003; router decision ADR 0004). The caller supplies the router: BrowserRouter in main.tsx,
 * MemoryRouter in tests. Only the login and status screens are open; everything else is behind RequireAuth.
 */
export function App({ api, expiry }: { api: ApiClient; expiry: SessionExpiry }) {
  const [queryClient] = useState(() => new QueryClient());
  return (
    <AppearanceProvider>
      <QueryClientProvider client={queryClient}>
        <ApiProvider client={api}>
          <AuthProvider expiry={expiry}>
            <AppRoutes />
          </AuthProvider>
        </ApiProvider>
      </QueryClientProvider>
    </AppearanceProvider>
  );
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path={LOGIN_PATH} element={<LoginScreen />} />
      <Route path="/durum" element={<StatusScreen />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <HomeScreen />
          </RequireAuth>
        }
      />
      <Route
        path="/tasarim"
        element={
          <RequireAuth>
            <DesignShowcase />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
