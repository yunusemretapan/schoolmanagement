import { STORAGE_KEYS } from '@schoolos/design-tokens';
import { act, render, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import { AppearanceProvider } from '../appearance/AppearanceProvider';
import { createMemoryStorage, device } from '../test/device';
import { MotionIndicator } from './MotionIndicator';

async function renderIndicator(motion?: string) {
  const storage = createMemoryStorage(motion ? { [STORAGE_KEYS.motion]: motion } : {});
  await render(
    <AppearanceProvider storage={storage} configuredBrand={null}>
      <MotionIndicator label="Yükleniyor" />
    </AppearanceProvider>,
  );
  return screen.findByRole('progressbar', { name: 'Yükleniyor' });
}

/** The ring's transform, if any: the animated style adds a rotate transform only while it turns. */
const transformOf = (element: { props: { style?: unknown } }) =>
  StyleSheet.flatten(element.props.style as never)?.transform;

describe('MotionIndicator (mobile, spec 0002 AC-2)', () => {
  it('turns while motion is on', async () => {
    const ring = await renderIndicator('normal');

    expect(transformOf(ring)).toEqual([expect.objectContaining({ rotate: expect.any(String) })]);
  });

  it('is static when motion is reduced', async () => {
    const ring = await renderIndicator('reduced');

    expect(transformOf(ring)).toBeUndefined();
  });

  it('stops when the device turns on reduce motion', async () => {
    await renderIndicator();
    await act(async () => device.setReduceMotion(true));

    expect(transformOf(screen.getByRole('progressbar', { name: 'Yükleniyor' }))).toBeUndefined();
  });
});
