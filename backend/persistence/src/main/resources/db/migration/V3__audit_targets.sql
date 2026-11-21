-- Spec 0005, plan R1: the audit log is shared by every module, and its target is no longer only a user.
-- A record names its target by type and id (and, for non-personal data such as the academic structure, by name).
-- The table stays append-only: existing rows are not updated (the trigger would refuse it); adding columns and
-- dropping NOT NULL are DDL and fire no row trigger. Existing rows are user targets: target_type defaults to USER
-- and their target_user_id is already set.

alter table audit_events add column target_type text not null default 'USER';
alter table audit_events alter column target_type drop default;
alter table audit_events add column target_id uuid;
alter table audit_events add column target_name text;
alter table audit_events alter column target_user_id drop not null;
alter table audit_events
    add constraint audit_events_target_known check (
        (target_type = 'USER' and target_user_id is not null)
        or (target_type <> 'USER' and target_id is not null and target_user_id is null));
