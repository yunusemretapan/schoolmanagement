/**
 * Raw colour scales. Screens never use these directly: they use the semantic roles in tokens.ts,
 * which pick from here per appearance (light / dark).
 */
export const palette = {
  white: '#FFFFFF',
  black: '#000000',
  neutral: {
    50: '#F8FAFC',
    100: '#F1F5F9',
    200: '#E2E8F0',
    300: '#CBD5E1',
    400: '#94A3B8',
    500: '#64748B',
    600: '#475569',
    700: '#334155',
    800: '#1E293B',
    900: '#0F172A',
    950: '#020617',
  },
  indigo: {
    50: '#EEF2FF',
    100: '#E0E7FF',
    200: '#C7D2FE',
    300: '#A5B4FC',
    400: '#818CF8',
    500: '#6366F1',
    600: '#4F46E5',
    700: '#4338CA',
    800: '#3730A3',
    900: '#312E81',
  },
  green: {
    300: '#6EE7B7',
    400: '#34D399',
    700: '#047857',
    800: '#065F46',
  },
  red: {
    300: '#FCA5A5',
    400: '#F87171',
    700: '#B91C1C',
    800: '#991B1B',
  },
  amber: {
    300: '#FCD34D',
    400: '#FBBF24',
    700: '#B45309',
    800: '#92400E',
  },
} as const;
