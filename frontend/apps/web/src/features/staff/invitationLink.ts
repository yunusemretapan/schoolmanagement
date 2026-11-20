/**
 * The invitation address (plan 0004, R4): the token sits in the fragment, which browsers never send to a server,
 * put in a Referer header or write to access logs.
 */
export const INVITATION_PATH = '/davet';

export function invitationUrl(origin: string, token: string): string {
  return `${origin}${INVITATION_PATH}#${encodeURIComponent(token)}`;
}

/** The token from {@code location.hash}; empty when there is none. */
export function invitationToken(hash: string): string {
  const raw = hash.startsWith('#') ? hash.slice(1) : hash;
  try {
    return decodeURIComponent(raw);
  } catch {
    return '';
  }
}

/** "23 Kasım 2026 12:00" in the institution's time zone (docs/conventions.md, Data rules). */
export function formatExpiry(iso: string): string {
  return new Intl.DateTimeFormat('tr-TR', {
    dateStyle: 'long',
    timeStyle: 'short',
    timeZone: 'Europe/Istanbul',
  }).format(new Date(iso));
}
