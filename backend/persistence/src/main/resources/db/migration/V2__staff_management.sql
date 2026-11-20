-- Spec 0004 — Kullanıcı ve rol yönetimi. Account status, invitations and the append-only audit log.
--
-- invitations is global like sessions (plan 0003, R1): a link is opened before anyone is signed in, so the tenant
-- is only known from the invitation itself. Only the identity feature's handlers read it.
-- audit_events is tenant-bound (RLS) and append-only: a trigger refuses UPDATE, DELETE and TRUNCATE for every
-- role, the table owner included (plan 0004, R5).

alter table login_identities
    add column status text not null default 'ACTIVE'
        constraint login_identities_status_known check (status in ('INVITED', 'ACTIVE', 'INACTIVE'));
alter table login_identities alter column status drop default;
alter table login_identities alter column password_hash drop not null;
alter table login_identities
    add constraint login_identities_active_has_password check (status <> 'ACTIVE' or password_hash is not null);

create index login_identities_tenant_idx on login_identities (tenant_id);

create table invitations (
    id          uuid primary key,
    user_id     uuid        not null references users (id),
    tenant_id   uuid        not null references tenants (id),
    token_hash  text        not null,
    created_at  timestamptz not null,
    expires_at  timestamptz not null,
    used_at     timestamptz,
    revoked_at  timestamptz
);

create index invitations_user_idx on invitations (user_id);

create table audit_events (
    id              uuid primary key,
    tenant_id       uuid        not null references tenants (id),
    actor_user_id   uuid        not null references users (id),
    target_user_id  uuid        not null references users (id),
    action          text        not null,
    detail          text,
    occurred_at     timestamptz not null
);

create index audit_events_tenant_idx on audit_events (tenant_id, occurred_at);

alter table audit_events enable row level security;
alter table audit_events force row level security;
create policy tenant_isolation on audit_events
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

create function audit_events_append_only() returns trigger
    language plpgsql as
$$
begin
    raise exception 'audit_events is append-only (spec 0004, Req. 10)';
end;
$$;

create trigger audit_events_no_change
    before update or delete on audit_events
    for each row execute function audit_events_append_only();

create trigger audit_events_no_truncate
    before truncate on audit_events
    for each statement execute function audit_events_append_only();
