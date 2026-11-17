import { createApiClient } from '@schoolos/api-client';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import './index.css';
import { applyVariables, scaleVariables, schemeVariables } from './theme';

// Step 1 of spec 0002: light scheme only; the appearance provider (step 3) takes over scheme and motion.
applyVariables(document.documentElement, { ...scaleVariables(), ...schemeVariables('light') });
document.documentElement.dataset.motion = 'on';

const root = document.getElementById('root');
if (root === null) {
  throw new Error('#root missing in index.html');
}

createRoot(root).render(
  <StrictMode>
    <App api={createApiClient(import.meta.env.VITE_API_BASE_URL ?? window.location.origin)} />
  </StrictMode>,
);
