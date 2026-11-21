import type { AcademicStructure, CampusView } from '@schoolos/api-client';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useApi } from '../../api/ApiProvider';
import { NAME_MAX } from './academicSchema';
import {
  DeleteButton,
  NameForm,
  Section,
  SectionMessage,
  SECONDARY,
  useAction,
  useHeading,
} from './academicUi';

function blockedReason(campus: CampusView, count: number): string | null {
  if (count <= 1) return 'academic.blocked.lastCampus';
  if (campus.inUse) return 'academic.blocked.inUse';
  return null;
}

/** Spec 0005, Req. 3 / AC-3: every institution has at least one campus; add, rename, delete an unused one. */
export function CampusesSection({
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

  const save = (call: () => Promise<AcademicStructure>, key: string, name: string) =>
    run(async () => apply(await call()), { kind: 'saved', key, values: { name } });

  return (
    <Section title={t('academic.campuses.title')} headingRef={heading.ref}>
      <SectionMessage message={message} />
      <ul className="flex flex-col gap-sm">
        {structure.campuses.map((campus) => (
          <li key={campus.id} className="flex flex-wrap items-center gap-sm border-t border-line pt-sm">
            {editing === campus.id ? (
              <NameForm
                label={t('academic.campuses.name')}
                initial={campus.name}
                max={NAME_MAX}
                submitLabel={t('academic.save')}
                busy={busy}
                onCancel={() => setEditing(null)}
                onSubmit={async (name) => {
                  const refused = await save(() => api.renameCampus(campus.id, name), 'academic.saved', name);
                  if (refused === undefined) setEditing(null);
                  return refused;
                }}
              />
            ) : (
              <>
                <p className="grow text-md font-semibold">{campus.name}</p>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => setEditing(campus.id)}
                  className={SECONDARY}
                >
                  {t('academic.edit')}
                </button>
                <DeleteButton
                  name={campus.name}
                  blockedReason={blockedReason(campus, structure.campuses.length)}
                  busy={busy}
                  onDelete={() => save(() => api.deleteCampus(campus.id), 'academic.deleted', campus.name)}
                  afterDelete={heading.focus}
                />
              </>
            )}
          </li>
        ))}
      </ul>
      <NameForm
        label={t('academic.campuses.addLabel')}
        max={NAME_MAX}
        submitLabel={t('academic.campuses.add')}
        busy={busy}
        onSubmit={(name) => save(() => api.createCampus(name), 'academic.added', name)}
      />
    </Section>
  );
}
