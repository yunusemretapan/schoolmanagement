import type { AcademicStructure } from '@schoolos/api-client';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useApi } from '../../api/ApiProvider';
import { NAME_MAX } from './academicSchema';
import {
  DeleteButton,
  NameForm,
  PRIMARY,
  Section,
  SectionMessage,
  SECONDARY,
  useAction,
  useHeading,
} from './academicUi';

export const MAX_GRADE_LEVELS = 50;

type Direction = 'UP' | 'DOWN';

/**
 * Spec 0005, Req. 5 / AC-5: the institution's ordered levels; the standard list while there are none; move up/down
 * (the ends cannot move further) with the new place announced.
 */
export function GradeLevelsSection({
  structure,
  apply,
}: {
  structure: AcademicStructure;
  apply(next: AcademicStructure): void;
}) {
  const { t } = useTranslation();
  const api = useApi();
  const { busy, message, run } = useAction();
  const heading = useHeading();
  const [editing, setEditing] = useState<string | null>(null);
  const levels = structure.gradeLevels;
  const list = useRef<HTMLOListElement>(null);
  const moved = useRef<{ id: string; direction: Direction; to: number } | null>(null);

  // Review 1, M-1: a move disables the buttons (busy) and may disable the pressed one for good (an end), so focus is
  // put back on the moved level's button — the other one when this one can no longer move. It waits until the list
  // shows the level at its new place (the structure arrives a render after the request ends).
  useEffect(() => {
    const target = moved.current;
    if (busy || target === null || list.current === null) return;
    if (levels.findIndex((level) => level.id === target.id) !== target.to) return;
    const button = (direction: Direction) =>
      list.current?.querySelector<HTMLButtonElement>(`[data-move="${target.id}:${direction}"]`);
    const same = button(target.direction);
    (same && !same.disabled ? same : button(target.direction === 'UP' ? 'DOWN' : 'UP'))?.focus();
    moved.current = null;
  }, [busy, levels]);

  const move = (id: string, direction: Direction, name: string, index: number) => {
    moved.current = { id, direction, to: direction === 'UP' ? index - 1 : index + 1 };
    void run(async () => apply(await api.moveGradeLevel(id, direction)), {
      kind: 'saved',
      key: 'academic.levels.moved',
      values: { name, position: direction === 'UP' ? index : index + 2 },
    }).then((refused) => {
      if (refused !== undefined) moved.current = null;
    });
  };

  const save = (call: () => Promise<AcademicStructure>, key: string, name: string) =>
    run(async () => apply(await call()), { kind: 'saved', key, values: { name } });

  return (
    <Section title={t('academic.levels.title')} headingRef={heading.ref}>
      <SectionMessage message={message} />
      {levels.length === 0 ? (
        <div className="flex flex-wrap items-center gap-sm">
          <p className="grow text-md text-ink-muted">{t('academic.empty.levels')}</p>
          <button
            type="button"
            disabled={busy}
            onClick={() =>
              void run(async () => apply(await api.addStandardGradeLevels()), {
                kind: 'saved',
                key: 'academic.levels.standardAdded',
              })
            }
            className={PRIMARY}
          >
            {t('academic.levels.addStandard')}
          </button>
        </div>
      ) : (
        <ol ref={list} className="flex flex-col gap-sm">
          {levels.map((level, index) => (
            <li key={level.id} className="flex flex-wrap items-center gap-sm border-t border-line pt-sm">
              {editing === level.id ? (
                <NameForm
                  label={t('academic.levels.name')}
                  initial={level.name}
                  max={NAME_MAX}
                  submitLabel={t('academic.save')}
                  busy={busy}
                  onCancel={() => setEditing(null)}
                  onSubmit={async (name) => {
                    const refused = await save(
                      () => api.renameGradeLevel(level.id, name),
                      'academic.saved',
                      name,
                    );
                    if (refused === undefined) setEditing(null);
                    return refused;
                  }}
                />
              ) : (
                <>
                  <p className="grow text-md font-semibold">{level.name}</p>
                  <button
                    type="button"
                    disabled={busy || index === 0}
                    data-move={`${level.id}:UP`}
                    aria-label={t('academic.levels.moveUpNamed', { name: level.name })}
                    onClick={() => move(level.id, 'UP', level.name, index)}
                    className={SECONDARY}
                  >
                    {t('academic.levels.moveUp')}
                  </button>
                  <button
                    type="button"
                    disabled={busy || index === levels.length - 1}
                    data-move={`${level.id}:DOWN`}
                    aria-label={t('academic.levels.moveDownNamed', { name: level.name })}
                    onClick={() => move(level.id, 'DOWN', level.name, index)}
                    className={SECONDARY}
                  >
                    {t('academic.levels.moveDown')}
                  </button>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => setEditing(level.id)}
                    className={SECONDARY}
                  >
                    {t('academic.edit')}
                  </button>
                  <DeleteButton
                    name={level.name}
                    blockedReason={level.inUse ? 'academic.blocked.inUse' : null}
                    busy={busy}
                    onDelete={() =>
                      save(() => api.deleteGradeLevel(level.id), 'academic.deleted', level.name)
                    }
                    afterDelete={heading.focus}
                  />
                </>
              )}
            </li>
          ))}
        </ol>
      )}
      {levels.length < MAX_GRADE_LEVELS ? (
        <NameForm
          label={t('academic.levels.addLabel')}
          max={NAME_MAX}
          submitLabel={t('academic.levels.add')}
          busy={busy}
          onSubmit={(name) => save(() => api.createGradeLevel(name), 'academic.added', name)}
        />
      ) : (
        <p className="text-sm text-ink-muted">{t('academic.errors.levelLimit')}</p>
      )}
    </Section>
  );
}
