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

interface PendingMove {
  id: string;
  direction: Direction;
  outcome: 'pending' | 'moved' | 'refused';
  /** The level's index in the server's answer: usually one step on, elsewhere after another administrator's change. */
  at?: number;
}

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
  const moved = useRef<PendingMove | null>(null);
  const [settled, setSettled] = useState(0);

  // Review 1, M-1: a move disables the buttons (busy) and may disable the pressed one for good (an end), so focus is
  // put back on the moved level's button — the other one when this one can no longer move. The structure arrives a
  // render after the request ends, so the move waits until the list shows the server's answer — wherever it put the
  // level (review 3, L-1: also after another administrator's change) — and is then used up, so it never takes focus
  // later (review 2, L-1). A refusal puts focus back on the pressed button (review 2, I-1).
  useEffect(() => {
    const target = moved.current;
    if (busy || target === null || list.current === null || target.outcome === 'pending') return;
    if (target.outcome === 'moved' && levels.findIndex((level) => level.id === target.id) !== target.at)
      return;
    moved.current = null;
    const button = (direction: Direction) =>
      list.current?.querySelector<HTMLButtonElement>(`[data-move="${target.id}:${direction}"]`);
    const same = button(target.direction);
    (same && !same.disabled ? same : button(target.direction === 'UP' ? 'DOWN' : 'UP'))?.focus();
  }, [busy, levels, settled]);

  const move = (id: string, direction: Direction, name: string, index: number) => {
    const pending: PendingMove = {
      id,
      direction,
      outcome: 'pending',
    };
    moved.current = pending;
    void run(
      async () => {
        const next = await api.moveGradeLevel(id, direction);
        pending.at = next.gradeLevels.findIndex((level) => level.id === id);
        apply(next);
      },
      {
        kind: 'saved',
        key: 'academic.levels.moved',
        values: { name, position: direction === 'UP' ? index : index + 2 },
      },
    ).then((refused) => {
      pending.outcome = refused === undefined ? 'moved' : 'refused';
      setSettled((n) => n + 1);
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
