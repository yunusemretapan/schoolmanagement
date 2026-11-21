import { ApiError, ERROR_CODES } from '@schoolos/api-client';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { renderApp } from '../../test/renderApp';
import { asAdmin, EIGHTH, EMPTY, id, MERKEZ, PATH, SEVENTH, structure } from './academicFixtures';

/** Spec 0005, Req. 5 / AC-5: the ordered list, its ends, the standard list. */

const SIXTH = id(19);
const NAMES: Record<string, string> = { [SIXTH]: '6. sınıf', [SEVENTH]: '7. sınıf', [EIGHTH]: '8. sınıf' };

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

  it('after a move focus stays on the moved level, on its other button once it reaches an end', async () => {
    const user = userEvent.setup();
    const three = (order: string[]) =>
      structure({
        gradeLevels: order.map((id, i) => ({ id, name: NAMES[id]!, position: i + 1, inUse: false })),
      });
    const moveGradeLevel = vi
      .fn()
      .mockResolvedValueOnce(three([SIXTH, EIGHTH, SEVENTH]))
      .mockResolvedValueOnce(three([EIGHTH, SIXTH, SEVENTH]));
    renderApp({
      api: asAdmin({
        getAcademicStructure: async () => three([SIXTH, SEVENTH, EIGHTH]),
        moveGradeLevel,
      }),
      path: PATH,
    });
    await screen.findByRole('heading', { level: 2, name: 'Sınıf seviyeleri' });

    await user.click(levels().getByRole('button', { name: '8. sınıf seviyesini yukarı taşı' }));
    await levels().findByText('8. sınıf 2. sıraya taşındı');
    await vi.waitFor(() =>
      expect(levels().getByRole('button', { name: '8. sınıf seviyesini yukarı taşı' })).toHaveFocus(),
    );

    await user.keyboard('{Enter}');
    await levels().findByText('8. sınıf 1. sıraya taşındı');
    await vi.waitFor(() =>
      expect(levels().getByRole('button', { name: '8. sınıf seviyesini aşağı taşı' })).toHaveFocus(),
    );
  });

  it('a move answered with another order focuses the level at once, never later (review 2–3, L-1)', async () => {
    const user = userEvent.setup();
    const three = (order: string[]) =>
      structure({
        gradeLevels: order.map((id, i) => ({ id, name: NAMES[id]!, position: i + 1, inUse: false })),
      });
    let resolve: (value: ReturnType<typeof three>) => void = () => {};
    const answer = new Promise<ReturnType<typeof three>>((done) => {
      resolve = done;
    });
    renderApp({
      api: asAdmin({
        getAcademicStructure: async () => three([SIXTH, SEVENTH, EIGHTH]),
        // meanwhile another administrator changed the list: 8. sınıf is not where this move would put it
        moveGradeLevel: () => answer,
        deleteGradeLevel: async () => three([SEVENTH, EIGHTH]),
      }),
      path: PATH,
    });
    await screen.findByRole('heading', { level: 2, name: 'Sınıf seviyeleri' });
    await user.click(levels().getByRole('button', { name: '8. sınıf seviyesini yukarı taşı' }));
    // a browser drops focus from the button disabled while busy; jsdom does not, so the test does
    (document.activeElement as HTMLElement).blur();
    resolve(three([SIXTH, SEVENTH, EIGHTH]));
    await levels().findByText('8. sınıf 2. sıraya taşındı');
    // review 3, L-1: focus still lands on the level, where the server put it
    await vi.waitFor(() =>
      expect(levels().getByRole('button', { name: '8. sınıf seviyesini yukarı taşı' })).toHaveFocus(),
    );

    // deleting 6. sınıf brings 8. sınıf to the index that move expected; focus belongs to the heading
    await user.click(levels().getAllByRole('button', { name: 'Sil' })[0]!);
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Sil' }));
    await levels().findByText('6. sınıf silindi');
    const heading = screen.getByRole('heading', { level: 2, name: 'Sınıf seviyeleri' });
    await vi.waitFor(() => expect(heading).toHaveFocus());
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(heading).toHaveFocus();
  });

  it('a refused move keeps focus on the pressed button and says why (review 2, I-1)', async () => {
    const user = userEvent.setup();
    let refuse: () => void = () => {};
    const answer = new Promise<never>((_, fail) => {
      refuse = () => fail(new ApiError(422, ERROR_CODES.cannotMove));
    });
    renderApp({ api: asAdmin({ moveGradeLevel: () => answer }), path: PATH });
    await screen.findByRole('heading', { level: 2, name: 'Sınıf seviyeleri' });
    const pressed = levels().getByRole('button', { name: '8. sınıf seviyesini yukarı taşı' });

    await user.click(pressed);
    // a browser drops focus from the button disabled while busy; jsdom does not, so the test does
    pressed.blur();
    refuse();

    expect(await levels().findByRole('alert')).toBeInTheDocument();
    await vi.waitFor(() => expect(pressed).toHaveFocus());
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
