-- Spec 0003 — Giriş ve oturum. Identity tables and tenant isolation (ADR 0002, BR-1).
--
-- Tenant-bound tables (tenants, users) carry Row-Level Security keyed on the transaction setting
-- app.tenant_id, set by TransactionBehavior from the signed-in user's token (never from the client).
-- FORCE makes the policies apply to the table owner too; a superuser still bypasses RLS, so the
-- application must connect as a non-superuser role (backend/compose.yaml).
--
-- login_identities and sessions are deliberately global (plan 0003, R1): at login the tenant is not
-- known yet, and the identifier is unique platform-wide (spec Req. 3). Only the identity feature's
-- handlers read them.

create table tenants (
    id          uuid primary key,
    name        text        not null,
    created_at  timestamptz not null
);

create table users (
    id          uuid primary key,
    tenant_id   uuid        not null references tenants (id),
    full_name   text        not null,
    roles       text[]      not null,
    created_at  timestamptz not null,
    constraint users_roles_not_empty check (cardinality(roles) > 0),
    constraint users_roles_known check (roles <@ array['ADMIN', 'TEACHER', 'GUARDIAN', 'ACCOUNTANT']::text[])
);

create index users_tenant_idx on users (tenant_id);

create table login_identities (
    user_id          uuid primary key references users (id),
    tenant_id        uuid        not null references tenants (id),
    email            text unique,
    phone            text unique,
    password_hash    text        not null,
    failed_attempts  integer     not null default 0,
    locked_until     timestamptz,
    constraint login_identities_has_identifier check (email is not null or phone is not null)
);

create table sessions (
    id            uuid primary key,
    user_id       uuid        not null references users (id),
    tenant_id     uuid        not null references tenants (id),
    client_type   text        not null check (client_type in ('WEB', 'MOBILE')),
    token_hash    text        not null,
    created_at    timestamptz not null,
    last_used_at  timestamptz not null,
    revoked_at    timestamptz
);

create index sessions_user_idx on sessions (user_id);

-- Row-Level Security: no tenant bound → no rows.
alter table tenants enable row level security;
alter table tenants force row level security;
create policy tenant_isolation on tenants
    using (id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (id = nullif(current_setting('app.tenant_id', true), '')::uuid);

alter table users enable row level security;
alter table users force row level security;
create policy tenant_isolation on users
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
