import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { renderApp } from '../../test/renderApp';
import {
  asAdmin,
  groups,
  KADIKOY,
  MERKEZ,
  NEXT_TERM,
  PATH,
  SEVENTH,
  structure,
  TERM,
} from './academicFixtures';

/** Spec 0005, Req. 3, 6 / AC-3, AC-6: class groups of a term; the campus appears only with several campuses. */

const section = () => within(screen.getByRole('heading', { level: 2, name: 'Sınıflar' }).closest('section')!);

const twoCampuses = () =>
  structure({
    campuses: [
      { id: MERKEZ, name: 'Merkez', inUse: true },
      { id: KADIKOY, name: 'Kadıköy', inUse: false },
    ],
  });

describe('ClassGroupsSection (web)', () => {
  it('with one campus neither the filter nor the form asks for a campus', async () => {
    renderApp({ api: asAdmin(), path: PATH });

    expect(await screen.findByText('7/A')).toBeInTheDocument();
    expect(section().queryByLabelText('Şube')).toBeNull();
    expect(section().getByLabelText('Dönem')).toHaveDisplayValue('2026–2027 (aktif)');
  });

  it('with a second campus the filter and the form ask for one', async () => {
    const user = userEvent.setup();
    const listClassGroups = vi.fn(async () => groups());
    const createClassGroup = vi.fn(async () => groups().items[0]!);
    renderApp({
      api: asAdmin({ getAcademicStructure: async () => twoCampuses(), listClassGroups, createClassGroup }),
      path: PATH,
    });
    await screen.findByText('7/A');

    expect(section().getAllByLabelText('Şube')).toHaveLength(2);
    await user.type(section().getByLabelText('Sınıf adı'), '7/B');
    await user.click(section().getByRole('button', { name: 'Sınıf ekle' }));
    expect(section().getAllByText('Şube seçin').length).toBeGreaterThan(1);
    expect(createClassGroup).not.toHaveBeenCalled();

    await user.selectOptions(section().getAllByLabelText('Şube')[0]!, KADIKOY);
    expect(listClassGroups).toHaveBeenLastCalledWith({ termId: TERM, campusId: KADIKOY }, expect.anything());
  });

  it('adding sends the name, level and the shown term; the list reloads', async () => {
    const user = userEvent.setup();
    const createClassGroup = vi.fn(async () => groups(['7/B']).items[0]!);
    const listClassGroups = vi.fn(async () => groups());
    renderApp({ api: asAdmin({ createClassGroup, listClassGroups }), path: PATH });
    await screen.findByText('7/A');

    await user.selectOptions(section().getByLabelText('Dönem'), NEXT_TERM);
    await user.type(section().getByLabelText('Sınıf adı'), ' 7/B ');
    await user.click(section().getByRole('button', { name: 'Sınıf ekle' }));

    expect(createClassGroup).toHaveBeenCalledWith({ name: '7/B', gradeLevelId: SEVENTH, termId: NEXT_TERM });
    expect(await section().findByText('7/B eklendi')).toBeInTheDocument();
    expect(listClassGroups).toHaveBeenCalledWith(
      { termId: NEXT_TERM, campusId: undefined },
      expect.anything(),
    );
  });

  it('a class name over 20 characters is refused before sending', async () => {
    const user = userEvent.setup();
    const createClassGroup = vi.fn(async () => groups().items[0]!);
    renderApp({ api: asAdmin({ createClassGroup }), path: PATH });
    await screen.findByText('7/A');

    await user.type(section().getByLabelText('Sınıf adı'), 'x'.repeat(21));
    await user.click(section().getByRole('button', { name: 'Sınıf ekle' }));

    expect(section().getByText('En çok 20 karakter')).toBeInTheDocument();
    expect(createClassGroup).not.toHaveBeenCalled();
  });

  it('a term without class groups says so', async () => {
    renderApp({ api: asAdmin({ listClassGroups: async () => groups([]) }), path: PATH });

    expect(await screen.findByText('Bu dönemde henüz sınıf yok')).toBeInTheDocument();
  });

  it('without grade levels the section asks for them first', async () => {
    renderApp({
      api: asAdmin({ getAcademicStructure: async () => structure({ gradeLevels: [] }) }),
      path: PATH,
    });

    expect(await screen.findByText('Önce sınıf seviyesi ekleyin')).toBeInTheDocument();
  });
});
