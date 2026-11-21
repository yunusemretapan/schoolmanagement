import type { AcademicStructure, ClassGroupList, ClassGroupView } from '@schoolos/api-client';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useApi } from '../../api/ApiProvider';
import { CLASS_NAME_MAX, nameError, serverFieldErrors, type FieldErrors } from './academicSchema';
import {
  DeleteButton,
  INPUT,
  PRIMARY,
  Section,
  SectionMessage,
  SECONDARY,
  useAction,
  useHeading,
} from './academicUi';

/** The message under a field, linked to it by id. */
function FieldError({ id, messageKey }: { id: string; messageKey: string | undefined }) {
  const { t } = useTranslation();
  if (messageKey === undefined) return null;
  return (
    <p id={id} className="text-sm text-danger">
      {t(messageKey)}
    </p>
  );
}

interface GroupForm {
  name: string;
  gradeLevelId: string;
  campusId: string;
}

interface GroupFormProps {
  structure: AcademicStructure;
  initial?: GroupForm;
  /** Campus is chosen only when adding, and only with several campuses (Req. 3, 6). */
  chooseCampus: boolean;
  submitLabel: string;
  busy: boolean;
  onSubmit(form: GroupForm): Promise<unknown>;
  onCancel?(): void;
}

function GroupFields({
  structure,
  initial,
  chooseCampus,
  submitLabel,
  busy,
  onSubmit,
  onCancel,
}: GroupFormProps) {
  const { t } = useTranslation();
  const id = useId();
  const blank = { name: '', gradeLevelId: structure.gradeLevels[0]?.id ?? '', campusId: '' };
  const [form, setForm] = useState<GroupForm>(initial ?? blank);
  const [errors, setErrors] = useState<FieldErrors>({});

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const found: FieldErrors = {};
    const name = nameError(form.name, CLASS_NAME_MAX);
    if (name) found.name = name;
    if (chooseCampus && form.campusId === '') found.campusId = 'academic.errors.chooseCampus';
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    const refused = await onSubmit({ ...form, name: form.name.trim() });
    setErrors(serverFieldErrors(refused, CLASS_NAME_MAX));
    if (refused === undefined && !initial) setForm({ ...blank, gradeLevelId: form.gradeLevelId });
  }

  const describedBy = (field: string) => (errors[field] === undefined ? undefined : `${id}-${field}-error`);
  return (
    <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-wrap items-end gap-sm">
      <div className="flex grow flex-col gap-xs">
        <label htmlFor={`${id}-name`} className="text-sm font-medium">
          {t('academic.groups.fields.name')}
        </label>
        <input
          id={`${id}-name`}
          value={form.name}
          onChange={(event) => setForm({ ...form, name: event.target.value })}
          aria-invalid={errors.name !== undefined}
          aria-describedby={describedBy('name')}
          className={INPUT}
        />
        <FieldError id={`${id}-name-error`} messageKey={errors.name} />
      </div>
      <div className="flex grow flex-col gap-xs">
        <label htmlFor={`${id}-level`} className="text-sm font-medium">
          {t('academic.groups.fields.gradeLevel')}
        </label>
        <select
          id={`${id}-level`}
          value={form.gradeLevelId}
          onChange={(event) => setForm({ ...form, gradeLevelId: event.target.value })}
          className={INPUT}
        >
          {structure.gradeLevels.map((level) => (
            <option key={level.id} value={level.id}>
              {level.name}
            </option>
          ))}
        </select>
      </div>
      {chooseCampus && (
        <div className="flex grow flex-col gap-xs">
          <label htmlFor={`${id}-campus`} className="text-sm font-medium">
            {t('academic.groups.fields.campus')}
          </label>
          <select
            id={`${id}-campus`}
            value={form.campusId}
            onChange={(event) => setForm({ ...form, campusId: event.target.value })}
            aria-invalid={errors.campusId !== undefined}
            aria-describedby={describedBy('campusId')}
            className={INPUT}
          >
            <option value="">{t('academic.groups.chooseCampus')}</option>
            {structure.campuses.map((campus) => (
              <option key={campus.id} value={campus.id}>
                {campus.name}
              </option>
            ))}
          </select>
          <FieldError id={`${id}-campusId-error`} messageKey={errors.campusId} />
        </div>
      )}
      <button type="submit" disabled={busy} aria-busy={busy} className={PRIMARY}>
        {busy ? t('academic.saving') : submitLabel}
      </button>
      {onCancel && (
        <button type="button" onClick={onCancel} className={SECONDARY}>
          {t('academic.cancel')}
        </button>
      )}
    </form>
  );
}

/**
 * Spec 0005, Req. 6 / AC-6: the class groups of the chosen term (the active one at first), by level order, filtered by
 * campus when there are several. Campus and term are fixed once a class group exists.
 */
export function ClassGroupsSection({ structure }: { structure: AcademicStructure }) {
  const { t } = useTranslation();
  const api = useApi();
  const queryClient = useQueryClient();
  const heading = useHeading();
  const termSelectId = useId();
  const campusSelectId = useId();
  const { busy, message, run } = useAction();
  const [termId, setTermId] = useState('');
  const [campusId, setCampusId] = useState('');
  const [editing, setEditing] = useState<string | null>(null);
  const severalCampuses = structure.campuses.length > 1;
  const filter = severalCampuses ? campusId : '';
  const ready = structure.terms.length > 0 && structure.gradeLevels.length > 0;
  // the active term until the administrator picks another (it may appear after this section was first shown)
  const selectedTerm = termId || structure.activeTermId || '';

  const groups = useQuery<ClassGroupList>({
    queryKey: ['academic', 'classGroups', selectedTerm, filter],
    queryFn: ({ signal }) =>
      api.listClassGroups({ termId: selectedTerm || undefined, campusId: filter || undefined }, signal),
    enabled: ready,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const levelName = (id: string) => structure.gradeLevels.find((l) => l.id === id)?.name ?? '';
  const campusName = (id: string) => structure.campuses.find((c) => c.id === id)?.name ?? '';

  async function change(action: () => Promise<unknown>, key: string, name: string) {
    return run(
      async () => {
        await action();
        await queryClient.invalidateQueries({ queryKey: ['academic'] });
      },
      { kind: 'saved', key, values: { name } },
    );
  }

  if (structure.terms.length === 0 || structure.gradeLevels.length === 0) {
    return (
      <Section title={t('academic.groups.title')} headingRef={heading.ref}>
        <p className="text-md text-ink-muted">
          {t(structure.terms.length === 0 ? 'academic.empty.needTerm' : 'academic.empty.needLevel')}
        </p>
      </Section>
    );
  }

  return (
    <Section title={t('academic.groups.title')} headingRef={heading.ref}>
      <div className="flex flex-wrap gap-sm">
        <div className="flex grow flex-col gap-xs">
          <label htmlFor={termSelectId} className="text-sm font-medium">
            {t('academic.groups.term')}
          </label>
          <select
            id={termSelectId}
            value={selectedTerm}
            onChange={(event) => setTermId(event.target.value)}
            className={INPUT}
          >
            {structure.terms.map((term) => (
              <option key={term.id} value={term.id}>
                {term.active ? t('academic.groups.activeOption', { name: term.name }) : term.name}
              </option>
            ))}
          </select>
        </div>
        {severalCampuses && (
          <div className="flex grow flex-col gap-xs">
            <label htmlFor={campusSelectId} className="text-sm font-medium">
              {t('academic.groups.campusFilter')}
            </label>
            <select
              id={campusSelectId}
              value={campusId}
              onChange={(event) => setCampusId(event.target.value)}
              className={INPUT}
            >
              <option value="">{t('academic.groups.allCampuses')}</option>
              {structure.campuses.map((campus) => (
                <option key={campus.id} value={campus.id}>
                  {campus.name}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      <SectionMessage message={message} />
      <div aria-busy={groups.isFetching}>
        {groups.isPending && (
          <p role="status" aria-live="polite" className="text-md text-ink-muted">
            {t('home.loading')}
          </p>
        )}
        {groups.isError && (
          <div className="flex flex-col gap-sm">
            <p role="alert" className="text-md font-semibold text-danger">
              {t('academic.groups.loadError')}
            </p>
            <button type="button" onClick={() => void groups.refetch()} className={SECONDARY}>
              {t('home.retry')}
            </button>
          </div>
        )}
        {groups.isSuccess && groups.data.items.length === 0 && (
          <p className="text-md text-ink-muted">{t('academic.empty.groups')}</p>
        )}
        {groups.isSuccess && groups.data.items.length > 0 && (
          <ul className="flex flex-col gap-sm">
            {groups.data.items.map((group: ClassGroupView) => (
              <li key={group.id} className="flex flex-wrap items-center gap-sm border-t border-line pt-sm">
                {editing === group.id ? (
                  <GroupFields
                    structure={structure}
                    initial={{ name: group.name, gradeLevelId: group.gradeLevelId, campusId: group.campusId }}
                    chooseCampus={false}
                    submitLabel={t('academic.save')}
                    busy={busy}
                    onCancel={() => setEditing(null)}
                    onSubmit={async (form) => {
                      const refused = await change(
                        () =>
                          api.updateClassGroup(group.id, {
                            name: form.name,
                            gradeLevelId: form.gradeLevelId,
                          }),
                        'academic.saved',
                        form.name,
                      );
                      if (refused === undefined) setEditing(null);
                      return refused;
                    }}
                  />
                ) : (
                  <>
                    <p className="grow text-md">
                      <span className="font-semibold">{group.name}</span>{' '}
                      <span className="text-sm text-ink-muted">
                        {severalCampuses
                          ? t('academic.groups.levelAndCampus', {
                              level: levelName(group.gradeLevelId),
                              campus: campusName(group.campusId),
                            })
                          : levelName(group.gradeLevelId)}
                      </span>
                    </p>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => setEditing(group.id)}
                      className={SECONDARY}
                    >
                      {t('academic.edit')}
                    </button>
                    <DeleteButton
                      name={group.name}
                      blockedReason={null}
                      busy={busy}
                      onDelete={() =>
                        change(() => api.deleteClassGroup(group.id), 'academic.deleted', group.name)
                      }
                      afterDelete={heading.focus}
                    />
                  </>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      <h3 className="text-md font-semibold">{t('academic.groups.add')}</h3>
      <GroupFields
        structure={structure}
        chooseCampus={severalCampuses}
        submitLabel={t('academic.groups.add')}
        busy={busy}
        onSubmit={(form) =>
          change(
            () =>
              api.createClassGroup({
                name: form.name,
                gradeLevelId: form.gradeLevelId,
                ...(severalCampuses ? { campusId: form.campusId } : {}),
                ...(selectedTerm ? { termId: selectedTerm } : {}),
              }),
            'academic.added',
            form.name,
          )
        }
      />
    </Section>
  );
}
