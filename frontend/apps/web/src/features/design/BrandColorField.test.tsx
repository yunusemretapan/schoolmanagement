import { tokens } from '@schoolos/design-tokens';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { AppearanceProvider } from '../../appearance/AppearanceProvider';
import { createFakeMedia, createMemoryStorage, DARK_QUERY, REDUCE_QUERY } from '../../test/fakes';
import { BrandColorField } from './BrandColorField';

const brandSolid = () => document.documentElement.style.getPropertyValue('--sos-brand-solid');

beforeEach(() => {
  createFakeMedia({ [DARK_QUERY]: false, [REDUCE_QUERY]: true }).install();
});

function renderField(configuredBrand: string | null = null) {
  const storage = createMemoryStorage();
  render(
    <AppearanceProvider storage={storage} configuredBrand={configuredBrand}>
      <BrandColorField />
    </AppearanceProvider>,
  );
  return { storage, input: screen.getByLabelText('Kurum rengi (#RRGGBB)') };
}

describe('BrandColorField (web, spec 0002 AC-5, AC-6)', () => {
  it('starts empty, on the default brand, without an error', () => {
    const { input } = renderField();
    expect(input).toHaveValue('');
    expect(input).not.toHaveAttribute('aria-invalid', 'true');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(brandSolid()).toBe(tokens.semantic.light.brand.solid);
  });

  it('a valid colour previews at once and is not saved', async () => {
    const user = userEvent.setup();
    const { input, storage } = renderField();

    await user.type(input, '#e11d48');

    expect(brandSolid()).toBe('#E11D48');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(storage.length).toBe(0);
  });

  it('an invalid colour keeps the default, shows the error and announces it', async () => {
    const user = userEvent.setup();
    const { input } = renderField();

    await user.type(input, '#12345');

    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Geçerli bir renk girin (ör. #1E40AF)');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Geçerli bir renk girin (ör. #1E40AF)');
    expect(brandSolid()).toBe(tokens.semantic.light.brand.solid);
  });

  it('"Varsayılana dön" clears the field and returns to the configured brand', async () => {
    const user = userEvent.setup();
    const { input } = renderField('#1E3A8A');
    await user.type(input, '#E11D48');
    expect(brandSolid()).toBe('#E11D48');

    await user.click(screen.getByRole('button', { name: 'Varsayılana dön' }));

    expect(input).toHaveValue('');
    expect(brandSolid()).toBe('#1E3A8A');
  });
});
