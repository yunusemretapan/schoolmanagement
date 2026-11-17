import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import trTR from './tr-TR.json';

export const DEFAULT_LOCALE = 'tr-TR';

void i18n.use(initReactI18next).init({
  resources: { [DEFAULT_LOCALE]: { translation: trTR } },
  lng: DEFAULT_LOCALE,
  fallbackLng: DEFAULT_LOCALE,
  interpolation: { escapeValue: false },
  initAsync: false,
});

export { i18n };
