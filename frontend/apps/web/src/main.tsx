import '@fontsource-variable/inter';
import { createApiClient, createMemoryTokenStore } from '@schoolos/api-client';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import { App } from './App';
import { createSessionExpiry } from './auth/sessionExpiry';
import './index.css';

const root = document.getElementById('root');
if (root === null) {
  throw new Error('#root missing in index.html');
}

// Web: the access token stays in memory, the refresh token in the HttpOnly cookie (plan 0003, R2).
const expiry = createSessionExpiry();
const api = createApiClient(import.meta.env.VITE_API_BASE_URL || window.location.origin, globalThis.fetch, {
  clientType: 'WEB',
  tokens: createMemoryTokenStore(),
  onSessionExpired: expiry.notify,
});

createRoot(root).render(
  <StrictMode>
    <BrowserRouter>
      <App api={api} expiry={expiry} />
    </BrowserRouter>
  </StrictMode>,
);
