/**
 * The single source of the visual language for web and mobile (spec 0001, AC-7).
 * Apps never hard-code a colour, size or spacing; they derive their theme from these values.
 * Brand look and dark mode arrive with "Tasarım sistemi v1".
 */
const INK = '#0F172A';

/** #RRGGBB + alpha → rgba(), so derived colours (shadows) stay tied to their base token. */
export function withAlpha(hex: string, alpha: number): string {
  const value = parseInt(hex.slice(1), 16);
  return `rgba(${(value >> 16) & 0xff}, ${(value >> 8) & 0xff}, ${value & 0xff}, ${alpha})`;
}

export const tokens = {
  color: {
    brand: {
      primary: '#4338CA',
      primaryContrast: '#FFFFFF',
      accent: '#0EA5E9',
    },
    surface: {
      canvas: '#F5F7FB',
      raised: '#FFFFFF',
      border: '#E2E8F0',
    },
    text: {
      primary: INK,
      secondary: '#475569',
    },
    status: {
      success: '#047857',
      danger: '#B91C1C',
      pending: '#B45309',
    },
  },
  typography: {
    fontFamily: {
      sans: 'Inter, "Segoe UI", system-ui, sans-serif',
    },
    size: {
      sm: 14,
      md: 16,
      lg: 20,
      xl: 28,
    },
    weight: {
      regular: '400',
      semibold: '600',
      bold: '700',
    },
  },
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
    xxl: 48,
  },
  radius: {
    md: 12,
    lg: 20,
    pill: 999,
  },
  layout: {
    cardMaxWidth: 420,
  },
  elevation: {
    // A CSS box-shadow string; React Native accepts the same syntax (boxShadow).
    card: `0 24px 64px -24px ${withAlpha(INK, 0.25)}`,
  },
  motion: {
    durationMs: {
      fast: 150,
      normal: 250,
    },
  },
} as const;

export type Tokens = typeof tokens;
