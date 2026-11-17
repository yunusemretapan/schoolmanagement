import { createApiClient } from '@schoolos/api-client';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import './index.css';
import { applyTheme } from './theme';

applyTheme(document.documentElement);

const root = document.getElementById('root');
if (root === null) {
  throw new Error('#root missing in index.html');
}

createRoot(root).render(
  <StrictMode>
    <App api={createApiClient(import.meta.env.VITE_API_BASE_URL ?? window.location.origin)} />
  </StrictMode>,
);
