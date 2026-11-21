import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { renderApp } from '../../test/renderApp';
import { asAdmin, EIGHTH, EMPTY, MERKEZ, PATH, SEVENTH, structure } from './academicFixtures';

/** Spec 0005, Req. 5 / AC-5: the ordered list, its ends, the standard list. */

const levels = () =>
  within(screen.getByRole('heading', { level: 2, name: 'Sınıf seviyeleri' }).closest('section')!);

describe('GradeLevelsSection (web)', () => {
  it('the first level cannot move up and the last cannot move down', async () => {
    renderApp({ api: asAdmin(), path: PATH });
    await screen.findByRole('heading', { level: 2, name: 'Sınıf seviyeleri' });

    expect(levels().getByRole('button', { name: '7. sınıf seviyesini yukarı taşı' })).toBeDisabled();
    expect(levels().getByRole('button', { name: '7. sınıf seviyesini aşağı taşı' })).toBeEnabled();
    expect(levels().getByRole('button', { name: '8. sınıf seviyesini aşağı taşı' })).toBeDisabled();
    expect(levels().getByRole('button', { name: '8. sınıf seviyesini yukarı taşı' })).toBeEnabled();
  });

  it('moving reorders the list and announces the new place', async () => {
    const user = userEvent.setup();
    const moveGradeLevel = vi.fn(async () =>
      structure({
        gradeLevels: [
          { id: EIGHTH, name: '8. sınıf', position: 1, inUse: false },
          { id: SEVENTH, name: '7. sınıf', position: 2, inUse: true },
        ],
      }),
    );
    renderApp({ api: asAdmin({ moveGradeLevel }), path: PATH });
    await screen.findByRole('heading', { level: 2, name: 'Sınıf seviyeleri' });

    await user.click(levels().getByRole('button', { name: '8. sınıf seviyesini yukarı taşı' }));

    expect(moveGradeLevel).toHaveBeenCalledWith(EIGHTH, 'UP');
    expect(await levels().findByText('8. sınıf 1. sıraya taşındı')).toBeInTheDocument();
    const items = levels().getAllByRole('listitem');
    expect(items[0]).toHaveTextContent('8. sınıf');
    expect(items[1]).toHaveTextContent('7. sınıf');
  });

  it('with no levels the standard list is offered and adds fourteen; afterwards it is not offered', async () => {
    const user = userEvent.setup();
    const names = ['Anaokulu', ...Array.from({ length: 12 }, (_, i) => `${i + 1}. sınıf`), 'Hazırlık'];
    const addStandardGradeLevels = vi.fn(async () => ({
      ...EMPTY,
      gradeLevels: names.map((name, i) => ({ id: `l${i}`, name, position: i + 1, inUse: false })),
    }));
    renderApp({
      api: asAdmin({ getAcademicStructure: async () => EMPTY, addStandardGradeLevels }),
      path: PATH,
    });

    await user.click(await screen.findByRole('button', { name: 'Standart listeyi ekle' }));

    expect(await levels().findByText('Standart liste eklendi')).toBeInTheDocument();
    expect(levels().getAllByRole('listitem')).toHaveLength(14);
    expect(levels().getAllByRole('listitem')[13]).toHaveTextContent('Hazırlık');
    expect(screen.queryByRole('button', { name: 'Standart listeyi ekle' })).toBeNull();
    expect(MERKEZ).toBeTruthy();
  });

  it('a name over 60 characters is refused before sending', async () => {
    const user = userEvent.setup();
    const createGradeLevel = vi.fn(async () => structure());
    renderApp({ api: asAdmin({ createGradeLevel }), path: PATH });
    await screen.findByRole('heading', { level: 2, name: 'Sınıf seviyeleri' });

    await user.type(levels().getByLabelText('Yeni seviye adı'), 'x'.repeat(61));
    await user.click(levels().getByRole('button', { name: 'Seviye ekle' }));

    expect(levels().getByText('En çok 60 karakter')).toBeInTheDocument();
    expect(levels().getByLabelText('Yeni seviye adı')).toHaveAccessibleDescription('En çok 60 karakter');
    expect(createGradeLevel).not.toHaveBeenCalled();
  });
});
