/**
 * The institution's brand colour for this build (spec 0002 Requirement 11): VITE_BRAND_COLOR, e.g. "#1E40AF".
 * Read from the institution's settings once the institution module exists. An invalid value is passed on
 * unchanged — deriveBrand falls back to the default brand (AC-6).
 */
export function configuredBrandColor(): string | null {
  const value: unknown = import.meta.env.VITE_BRAND_COLOR;
  return typeof value === 'string' && value.trim() !== '' ? value : null;
}
