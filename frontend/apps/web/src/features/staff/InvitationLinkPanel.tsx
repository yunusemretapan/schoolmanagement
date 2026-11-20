import type { InvitationLink } from '@schoolos/api-client';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { formatExpiry, invitationUrl } from './invitationLink';
import { PRIMARY } from './StaffFields';

/**
 * A fresh invitation link, shown once (spec 0004, Req. 5): the address, "Bağlantıyı kopyala" with an announced
 * result, and when it stops working.
 */
export function InvitationLinkPanel({ link }: { link: InvitationLink }) {
  const { t } = useTranslation();
  const [copy, setCopy] = useState<'idle' | 'copied' | 'failed'>('idle');
  const url = invitationUrl(window.location.origin, link.token);

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(url);
      setCopy('copied');
    } catch {
      setCopy('failed');
    }
  }

  return (
    <div className="flex flex-col gap-sm rounded-md bg-sunken p-md">
      <p className="text-md font-semibold">{t('staff.linkReady')}</p>
      <p className="break-all text-sm" data-testid="invitation-url">
        {url}
      </p>
      <p className="text-sm text-ink-muted">{t('staff.linkExpires', { at: formatExpiry(link.expiresAt) })}</p>
      <button type="button" onClick={() => void copyLink()} className={PRIMARY}>
        {t('staff.copyLink')}
      </button>
      <p role="status" aria-live="polite" className="text-sm">
        {copy === 'copied' && t('staff.copied')}
        {copy === 'failed' && t('staff.copyFailed')}
      </p>
    </div>
  );
}
