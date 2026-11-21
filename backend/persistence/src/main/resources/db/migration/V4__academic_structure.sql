-- Spec 0005 (akademik yapı): campuses, terms, grade levels, class groups and one academic_settings row per
-- institution (the active term; and the row every write locks first: plan 0005, R3). All tenant-bound with RLS.
-- Names are stored as typed (trimmed); uniqueness compares the folded name (NameFold, plan R4) — the same
-- expression the application's JPQL uses.
-- A child names its parent together with the tenant (composite foreign keys), so the database itself refuses a class
-- group pointing at another institution's campus, term or grade level (BR-1, AC-2).

create table campuses (
    id          uuid        primary key,
    tenant_id   uuid        not null references tenants (id),
    name        text        not null check (length(name) between 1 and 60 and name = btrim(name)),
    created_at  timestamptz not null,
    unique (tenant_id, id)
);
create unique index campuses_name_uq
    on campuses (tenant_id, lower(replace(replace(replace(name, 'İ', 'i'), 'I', 'i'), 'ı', 'i')));

create table terms (
    id          uuid        primary key,
    tenant_id   uuid        not null references tenants (id),
    name        text        not null check (length(name) between 1 and 60 and name = btrim(name)),
    starts_on   date        not null,
    ends_on     date        not null,
    created_at  timestamptz not null,
    unique (tenant_id, id),
    constraint terms_dates_valid check (
        ends_on > starts_on
        and starts_on >= date '2000-01-01'
        and ends_on <= date '2100-12-31'
        and ends_on - starts_on + 1 <= 731)
);
create unique index terms_name_uq
    on terms (tenant_id, lower(replace(replace(replace(name, 'İ', 'i'), 'I', 'i'), 'ı', 'i')));
create index terms_tenant_dates_idx on terms (tenant_id, starts_on);

create table grade_levels (
    id          uuid        primary key,
    tenant_id   uuid        not null references tenants (id),
    name        text        not null check (length(name) between 1 and 60 and name = btrim(name)),
    position    integer     not null check (position between 1 and 50),
    created_at  timestamptz not null,
    unique (tenant_id, id),
    constraint grade_levels_position_uq unique (tenant_id, position) deferrable initially deferred
);
create unique index grade_levels_name_uq
    on grade_levels (tenant_id, lower(replace(replace(replace(name, 'İ', 'i'), 'I', 'i'), 'ı', 'i')));

create table class_groups (
    id              uuid        primary key,
    tenant_id       uuid        not null references tenants (id),
    campus_id       uuid        not null,
    term_id         uuid        not null,
    grade_level_id  uuid        not null,
    name            text        not null check (length(name) between 1 and 20 and name = btrim(name)),
    created_at      timestamptz not null,
    foreign key (tenant_id, campus_id) references campuses (tenant_id, id) on delete restrict,
    foreign key (tenant_id, term_id) references terms (tenant_id, id) on delete restrict,
    foreign key (tenant_id, grade_level_id) references grade_levels (tenant_id, id) on delete restrict
);
create unique index class_groups_name_uq
    on class_groups (tenant_id, campus_id, term_id,
                     lower(replace(replace(replace(name, 'İ', 'i'), 'I', 'i'), 'ı', 'i')));
create index class_groups_term_idx on class_groups (tenant_id, term_id);
create index class_groups_campus_idx on class_groups (tenant_id, campus_id);
create index class_groups_grade_level_idx on class_groups (tenant_id, grade_level_id);

create table academic_settings (
    id              uuid primary key,
    tenant_id       uuid not null unique references tenants (id),
    active_term_id  uuid,
    foreign key (tenant_id, active_term_id) references terms (tenant_id, id) on delete restrict
);

-- Every institution opened before this spec gets its settings row and one campus named after it (Req. 3): the
-- first 60 characters of the institution name. tenants has FORCE RLS and no tenant is bound while migrating, so the
-- owner lifts the force for this transaction only; DDL is transactional in PostgreSQL.
alter table tenants no force row level security;
insert into academic_settings (id, tenant_id, active_term_id)
    select uuidv7(), t.id, null from tenants t;
insert into campuses (id, tenant_id, name, created_at)
    select uuidv7(), t.id, btrim(left(btrim(t.name), 60)), now() from tenants t;
alter table tenants force row level security;

alter table campuses enable row level security;
alter table campuses force row level security;
create policy tenant_isolation on campuses
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

alter table terms enable row level security;
alter table terms force row level security;
create policy tenant_isolation on terms
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

alter table grade_levels enable row level security;
alter table grade_levels force row level security;
create policy tenant_isolation on grade_levels
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

alter table class_groups enable row level security;
alter table class_groups force row level security;
create policy tenant_isolation on class_groups
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

alter table academic_settings enable row level security;
alter table academic_settings force row level security;
create policy tenant_isolation on academic_settings
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
