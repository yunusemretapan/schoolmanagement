import { tokens } from '@schoolos/design-tokens';
import { render, screen, userEvent } from '@testing-library/react-native';
import { AccessibilityInfo, Text } from 'react-native';
import { AppearanceProvider, useAppearance } from '../../appearance/AppearanceProvider';
import { createMemoryStorage } from '../../test/device';
import { BrandColorField } from './BrandColorField';

function Brand() {
  return <Text testID="brand">{useAppearance().theme.colors.brand}</Text>;
}

const brandSolid = () => screen.getByTestId('brand').props.children as string;
const ERROR = 'Geçerli bir renk girin (ör. #1E40AF)';

async function renderField(configuredBrand: string | null = null) {
  const storage = createMemoryStorage();
  await render(
    <AppearanceProvider storage={storage} configuredBrand={configuredBrand}>
      <BrandColorField />
      <Brand />
    </AppearanceProvider>,
  );
  const input = await screen.findByLabelText('Kurum rengi (#RRGGBB)');
  return { storage, input };
}

describe('BrandColorField (mobile, spec 0002 AC-5, AC-6)', () => {
  it('starts empty, on the default brand, without an error', async () => {
    const { input } = await renderField();

    expect(input).toHaveDisplayValue('');
    expect(screen.queryByTestId('brand-error')).not.toBeOnTheScreen();
    expect(input.props.accessibilityHint).toBeUndefined();
    expect(brandSolid()).toBe(tokens.semantic.light.brand.solid);
  });

  it('a valid colour previews at once and is not saved', async () => {
    const user = userEvent.setup();
    const { input, storage } = await renderField();

    await user.type(input, '#e11d48');

    expect(brandSolid()).toBe('#E11D48');
    expect(screen.queryByTestId('brand-error')).not.toBeOnTheScreen();
    expect(storage.values.size).toBe(0);
  });

  it('an invalid colour keeps the default, shows the error and announces it', async () => {
    const user = userEvent.setup();
    const announce = jest
      .spyOn(AccessibilityInfo, 'announceForAccessibility')
      .mockImplementation(() => undefined);
    announce.mockClear();
    try {
      const { input } = await renderField();

      await user.type(input, '#12345');

      const error = screen.getByTestId('brand-error');
      expect(error).toHaveTextContent(ERROR);
      expect(error).toHaveProp('role', 'alert');
      expect(error).toHaveProp('accessibilityLiveRegion', 'assertive');
      expect(input).toHaveProp('accessibilityHint', ERROR);
      expect(announce).toHaveBeenCalledWith(ERROR);
      expect(brandSolid()).toBe(tokens.semantic.light.brand.solid);
    } finally {
      announce.mockRestore();
    }
  });

  it('"Varsayılana dön" clears the field and returns to the configured brand', async () => {
    const user = userEvent.setup();
    const { input } = await renderField('#1E3A8A');
    await user.type(input, '#E11D48');
    expect(brandSolid()).toBe('#E11D48');

    await user.press(screen.getByRole('button', { name: 'Varsayılana dön' }));

    expect(input).toHaveDisplayValue('');
    expect(brandSolid()).toBe('#1E3A8A');
  });
});
