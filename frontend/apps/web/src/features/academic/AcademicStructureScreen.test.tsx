import { ApiError, ERROR_CODES, type AcademicStructure } from '@schoolos/api-client';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { fakeApi, renderApp } from '../../test/renderApp';
import { asAdmin, EIGHTH, EMPTY, NEXT_TERM, PATH, structure, TERM } from './academicFixtures';

/** Spec 0005, AC-1 (no permission), AC-4 (terms), AC-8 (delete), AC-9 (screen states) on the web. */

const section = (name: string) => screen.getByRole('heading', { level: 2, name }).closest('section')!;

describe('AcademicStructureScreen (web)', () => {
  it('a teacher who opens the address sees the no-permission state and nothing is loaded', async () => {
    const getAcademicStructure = vi.fn(async () => structure());
    renderApp({ api: fakeApi({ restoreSession: async () => true, getAcademicStructure }), path: PATH });

    expect(await screen.findByRole('alert')).toHaveTextContent('Bu sayfayı görüntüleme yetkiniz yok');
    expect(getAcademicStructure).not.toHaveBeenCalled();
  });

  it('shows loading first, then the four sections with the active term said in words', async () => {
    let resolve: (s: AcademicStructure) => void = () => undefined;
    const getAcademicStructure = vi.fn(() => new Promise<AcademicStructure>((r) => (resolve = r)));
    renderApp({ api: asAdmin({ getAcademicStructure }), path: PATH });

    await vi.waitFor(() => expect(getAcademicStructure).toHaveBeenCalled());
    expect(screen.getByText('Yükleniyor')).toBeInTheDocument();
    resolve(structure());

    expect(await screen.findByRole('heading', { level: 2, name: 'Dönemler' })).toBeInTheDocument();
    for (const name of ['Şubeler', 'Sınıf seviyeleri', 'Sınıflar']) {
      expect(screen.getByRole('heading', { level: 2, name })).toBeInTheDocument();
    }
    const terms = within(section('Dönemler'));
    expect(terms.getByText('Aktif dönem')).toBeInTheDocument();
    expect(terms.getByText('14 Eylül 2026 – 26 Haziran 2027')).toBeInTheDocument();
  });

  it('a failed load shows the error and "Tekrar dene" loads again', async () => {
    const user = userEvent.setup();
    let fail = true;
    const getAcademicStructure = vi.fn(async () => {
      if (fail) throw new ApiError(undefined, undefined);
      return structure();
    });
    renderApp({ api: asAdmin({ getAcademicStructure }), path: PATH });

    expect(await screen.findByRole('alert')).toHaveTextContent('Akademik yapı yüklenemedi');
    fail = false;
    await user.click(screen.getByRole('button', { name: 'Tekrar dene' }));

    expect(await screen.findByRole('heading', { level: 2, name: 'Dönemler' })).toBeInTheDocument();
  });

  it('an empty institution gets guidance in every section', async () => {
    renderApp({ api: asAdmin({ getAcademicStructure: async () => EMPTY }), path: PATH });

    expect(await screen.findByText('Henüz dönem yok')).toBeInTheDocument();
    expect(screen.getByText('Henüz sınıf seviyesi yok')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Standart listeyi ekle' })).toBeInTheDocument();
    expect(within(section('Sınıflar')).getByText('Önce bir dönem ekleyin')).toBeInTheDocument();
  });

  it('a term is checked before sending: required, end after start, at most 731 days', async () => {
    const user = userEvent.setup();
    const createTerm = vi.fn(async () => structure());
    renderApp({ api: asAdmin({ createTerm }), path: PATH });
    await screen.findByRole('heading', { level: 2, name: 'Dönemler' });
    const terms = within(section('Dönemler'));

    await user.click(terms.getByRole('button', { name: 'Dönem ekle' }));
    expect(terms.getAllByText('Bu alan zorunlu')).toHaveLength(3);

    await user.type(terms.getByLabelText('Dönem adı'), 'Uzun');
    await user.type(terms.getByLabelText('Başlangıç tarihi'), '2030-01-01');
    await user.type(terms.getByLabelText('Bitiş tarihi'), '2032-01-02');
    await user.click(terms.getByRole('button', { name: 'Dönem ekle' }));
    expect(terms.getByText('Bir dönem en çok 731 gün sürebilir')).toBeInTheDocument();

    await user.clear(terms.getByLabelText('Bitiş tarihi'));
    await user.type(terms.getByLabelText('Bitiş tarihi'), '2030-01-01');
    await user.click(terms.getByRole('button', { name: 'Dönem ekle' }));
    expect(terms.getByText('Bitiş tarihi başlangıçtan sonra olmalı')).toBeInTheDocument();
    expect(createTerm).not.toHaveBeenCalled();
  });

  it('an overlapping term is refused by the server with its reason; what was typed stays', async () => {
    const user = userEvent.setup();
    renderApp({
      api: asAdmin({
        createTerm: async () => {
          throw new ApiError(422, ERROR_CODES.termOverlap);
        },
      }),
      path: PATH,
    });
    await screen.findByRole('heading', { level: 2, name: 'Dönemler' });
    const terms = within(section('Dönemler'));

    await user.type(terms.getByLabelText('Dönem adı'), 'Yaz');
    await user.type(terms.getByLabelText('Başlangıç tarihi'), '2027-06-26');
    await user.type(terms.getByLabelText('Bitiş tarihi'), '2027-08-31');
    await user.click(terms.getByRole('button', { name: 'Dönem ekle' }));

    expect(await terms.findByRole('alert')).toHaveTextContent('Bu tarihler başka bir dönemle çakışıyor');
    expect(terms.getByLabelText('Dönem adı')).toHaveValue('Yaz');
  });

  it('another term becomes active and the change is announced', async () => {
    const user = userEvent.setup();
    const activateTerm = vi.fn(async () =>
      structure({
        activeTermId: NEXT_TERM,
        terms: structure().terms.map((t) => ({ ...t, active: t.id === NEXT_TERM })),
      }),
    );
    renderApp({ api: asAdmin({ activateTerm }), path: PATH });

    await user.click(await screen.findByRole('button', { name: 'Aktif dönem yap' }));

    expect(activateTerm).toHaveBeenCalledWith(NEXT_TERM);
    expect(await screen.findByText('2027–2028 aktif dönem oldu')).toBeInTheDocument();
  });

  it('the active term and records with class groups cannot be deleted, with the reason written', async () => {
    renderApp({ api: asAdmin(), path: PATH });
    await screen.findByRole('heading', { level: 2, name: 'Dönemler' });

    const deletes = within(section('Dönemler')).getAllByRole('button', { name: 'Sil' });
    expect(deletes[1]).toBeDisabled();
    expect(deletes[1]).toHaveAccessibleDescription('Aktif dönem silinemez');
    expect(deletes[0]).toBeEnabled();
    const campus = within(section('Şubeler')).getByRole('button', { name: 'Sil' });
    expect(campus).toHaveAccessibleDescription('Kurumun son şubesi silinemez');
    const levels = within(section('Sınıf seviyeleri')).getAllByRole('button', { name: 'Sil' });
    expect(levels[0]).toHaveAccessibleDescription('Bu kayda bağlı sınıflar var');
    expect(levels[1]).toBeEnabled();
  });

  it('deleting asks first; cancelling changes nothing; confirming deletes and focus goes to the section', async () => {
    const user = userEvent.setup();
    const deleteGradeLevel = vi.fn(async () =>
      structure({ gradeLevels: structure().gradeLevels.filter((l) => l.id !== EIGHTH) }),
    );
    renderApp({ api: asAdmin({ deleteGradeLevel }), path: PATH });
    await screen.findByRole('heading', { level: 2, name: 'Dönemler' });
    const trigger = within(section('Sınıf seviyeleri')).getAllByRole('button', { name: 'Sil' })[1]!;

    await user.click(trigger);
    expect(screen.getByRole('dialog', { name: 'Kaydı sil' })).toHaveAccessibleDescription(
      '8. sınıf silinsin mi?',
    );
    await user.click(screen.getByRole('button', { name: 'Vazgeç' }));
    expect(deleteGradeLevel).not.toHaveBeenCalled();
    expect(trigger).toHaveFocus();

    await user.click(trigger);
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Sil' }));

    expect(deleteGradeLevel).toHaveBeenCalledWith(EIGHTH);
    expect(await screen.findByText('8. sınıf silindi')).toBeInTheDocument();
    await vi.waitFor(() =>
      expect(screen.getByRole('heading', { level: 2, name: 'Sınıf seviyeleri' })).toHaveFocus(),
    );
  });

  it('a network error keeps what was typed and says the server is unreachable', async () => {
    const user = userEvent.setup();
    renderApp({
      api: asAdmin({
        createCampus: async () => {
          throw new ApiError(undefined, undefined);
        },
      }),
      path: PATH,
    });
    await screen.findByRole('heading', { level: 2, name: 'Şubeler' });
    const campuses = within(section('Şubeler'));

    await user.type(campuses.getByLabelText('Yeni şube adı'), 'Kadıköy');
    await user.click(campuses.getByRole('button', { name: 'Şube ekle' }));

    expect(await campuses.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor, tekrar deneyin');
    expect(campuses.getByLabelText('Yeni şube adı')).toHaveValue('Kadıköy');
  });

  it('while saving the button is disabled (no double submit), then the result is announced', async () => {
    const user = userEvent.setup();
    let resolve: (s: AcademicStructure) => void = () => undefined;
    const createCampus = vi.fn(() => new Promise<AcademicStructure>((r) => (resolve = r)));
    renderApp({ api: asAdmin({ createCampus }), path: PATH });
    await screen.findByRole('heading', { level: 2, name: 'Şubeler' });
    const campuses = within(section('Şubeler'));

    await user.type(campuses.getByLabelText('Yeni şube adı'), 'Kadıköy');
    await user.click(campuses.getByRole('button', { name: 'Şube ekle' }));
    const saving = campuses.getByRole('button', { name: 'Kaydediliyor…' });
    expect(saving).toBeDisabled();
    await user.click(saving);
    expect(createCampus).toHaveBeenCalledTimes(1);

    resolve(structure({ campuses: [...structure().campuses, { id: 'k', name: 'Kadıköy', inUse: false }] }));
    expect(await campuses.findByText('Kadıköy eklendi')).toBeInTheDocument();
    expect(TERM).toBeTruthy();
  });

  it('a taken name is refused by the server with its reason', async () => {
    const user = userEvent.setup();
    renderApp({
      api: asAdmin({
        createCampus: async () => {
          throw new ApiError(422, ERROR_CODES.academicNameTaken);
        },
      }),
      path: PATH,
    });
    await screen.findByRole('heading', { level: 2, name: 'Şubeler' });
    const campuses = within(section('Şubeler'));

    await user.type(campuses.getByLabelText('Yeni şube adı'), 'merkez');
    await user.click(campuses.getByRole('button', { name: 'Şube ekle' }));

    expect(await campuses.findByRole('alert')).toHaveTextContent('Bu ad zaten kullanılıyor');
  });
});
