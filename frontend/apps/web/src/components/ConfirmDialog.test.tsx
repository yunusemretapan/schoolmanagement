import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { ConfirmDialog } from './ConfirmDialog';

/** Spec 0004, Accessibility: focus moves in and stays, Esc cancels, focus returns (plan 0004, R9). */

function Harness({ onConfirm = () => undefined }: { onConfirm?: () => void }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Aç
      </button>
      <ConfirmDialog
        open={open}
        title="Başlık"
        message="Mesaj"
        confirmLabel="Evet"
        cancelLabel="Vazgeç"
        onConfirm={() => {
          onConfirm();
          setOpen(false);
        }}
        onCancel={() => setOpen(false)}
      />
    </>
  );
}

describe('ConfirmDialog', () => {
  it('is a labelled modal dialog that focuses the safe choice', async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(screen.getByRole('button', { name: 'Aç' }));

    const dialog = screen.getByRole('dialog', { name: 'Başlık' });
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveAccessibleDescription('Mesaj');
    expect(screen.getByRole('button', { name: 'Vazgeç' })).toHaveFocus();
  });

  it('keeps Tab inside, both ways', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Aç' }));

    await user.tab();
    expect(screen.getByRole('button', { name: 'Evet' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Vazgeç' })).toHaveFocus();
    await user.tab({ shift: true });
    expect(screen.getByRole('button', { name: 'Evet' })).toHaveFocus();
  });

  it('Esc closes without confirming and focus returns to the opener', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(<Harness onConfirm={onConfirm} />);
    const opener = screen.getByRole('button', { name: 'Aç' });
    await user.click(opener);

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(onConfirm).not.toHaveBeenCalled();
    expect(opener).toHaveFocus();
  });

  it('confirming calls back once', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(<Harness onConfirm={onConfirm} />);
    await user.click(screen.getByRole('button', { name: 'Aç' }));

    await user.click(screen.getByRole('button', { name: 'Evet' }));

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });
});
