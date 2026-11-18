import { palette } from './palette';

/**
 * The single source of the visual language for web and mobile (spec 0001 AC-7, spec 0002).
 * Screens use semantic roles (`semantic.light` / `semantic.dark`) and the scales below; never a raw value.
 */

/** #RRGGBB + alpha → rgba(), so derived colours (shadows, tints) stay tied to their base colour. */
export function withAlpha(hex: string, alpha: number): string {
  const value = parseInt(hex.slice(1), 16);
  return `rgba(${(value >> 16) & 0xff}, ${(value >> 8) & 0xff}, ${value & 0xff}, ${alpha})`;
}

const { neutral, indigo, green, red, amber, white } = palette;

const light = {
  bg: {
    canvas: neutral[50],
    surface: white,
    sunken: neutral[100],
  },
  border: {
    /** Decorative separators (cards); not a UI-control boundary, so no contrast requirement. */
    subtle: neutral[200],
    /** Boundaries a user must perceive (inputs, controls): at least 3:1 (WCAG 1.4.11). */
    control: neutral[500],
  },
  text: {
    primary: neutral[900],
    secondary: neutral[600],
  },
  brand: {
    solid: indigo[700],
    onSolid: white,
    text: indigo[700],
    subtle: indigo[50],
  },
  status: {
    success: green[700],
    danger: red[700],
    pending: amber[700],
  },
  focus: indigo[600],
  shadow: neutral[900],
} as const;

const dark = {
  bg: {
    canvas: neutral[950],
    surface: neutral[900],
    sunken: neutral[800],
  },
  border: {
    subtle: neutral[800],
    control: neutral[400],
  },
  text: {
    primary: neutral[50],
    secondary: neutral[400],
  },
  brand: {
    solid: indigo[600],
    onSolid: white,
    text: indigo[300],
    subtle: indigo[900],
  },
  status: {
    success: green[400],
    danger: red[400],
    pending: amber[400],
  },
  focus: indigo[300],
  shadow: palette.black,
} as const;

export const tokens = {
  palette,
  semantic: { light, dark },
  typography: {
    fontFamily: {
      sans: 'Inter',
      /** Web stack while the bundled Inter loads. */
      sansStack: '"Inter Variable", Inter, "Segoe UI", system-ui, sans-serif',
    },
    /** size / lineHeight in px (web) and dp (mobile). */
    scale: {
      xs: { size: 12, lineHeight: 16 },
      sm: { size: 14, lineHeight: 20 },
      md: { size: 16, lineHeight: 24 },
      lg: { size: 20, lineHeight: 28 },
      xl: { size: 24, lineHeight: 32 },
      xxl: { size: 30, lineHeight: 38 },
      display: { size: 36, lineHeight: 44 },
    },
    weight: {
      regular: '400',
      medium: '500',
      semibold: '600',
      bold: '700',
    },
  },
  spacing: {
    xxs: 2,
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
    xxl: 48,
  },
  size: {
    dot: 12,
    iconSm: 16,
    spinner: 20,
    iconMd: 24,
    logo: 40,
    /** Minimum touch / click target. */
    control: 44,
    swatch: 56,
    glow: 576,
    glowBlur: 64,
  },
  layout: {
    cardMaxWidth: 420,
    contentMaxWidth: 1120,
    glowOffset: 160,
    /** Fixed label column in definition-style rows (showcase scales). */
    labelColumn: 120,
    /** How far the showcase motion example travels. */
    motionDemoTravel: 64,
  },
  /** Stacking order (z-index). */
  layer: {
    overlay: 50,
  },
  radius: {
    sm: 8,
    md: 12,
    lg: 20,
    pill: 999,
  },
  borderWidth: {
    hairline: 1,
    thick: 2,
  },
  opacity: {
    glow: 0.15,
    pulse: 0.6,
    pressed: 0.85,
    /** Dims the screen behind a modal panel (mobile). */
    scrim: 0.4,
  },
  elevation: {
    light: { card: `0 24px 64px -24px ${withAlpha(light.shadow, 0.25)}` },
    dark: { card: `0 24px 64px -24px ${withAlpha(dark.shadow, 0.6)}` },
  },
  motion: {
    durationMs: {
      fast: 150,
      normal: 250,
      slow: 400,
      /** One full turn of a spinner / pulse loop. */
      loop: 1000,
    },
    /** cubic-bezier control points. */
    easing: {
      standard: [0.2, 0, 0, 1],
      enter: [0, 0, 0, 1],
      exit: [0.3, 0, 1, 1],
      /** Constant speed: busy loops (spinner) only. */
      linear: [0, 0, 1, 1],
    },
    pressScale: 0.98,
  },
} as const;

export type Tokens = typeof tokens;
export type ColorScheme = keyof Tokens['semantic'];
export type SemanticColors = Tokens['semantic'][ColorScheme];
