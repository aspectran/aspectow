-- User accounts
create table if not exists asc_user (
    user_id bigserial not null,
    username varchar(50) not null unique,
    password varchar(100) not null,
    nickname varchar(50),
    email varchar(100),
    status varchar(10) default 'NORMAL' not null, -- NORMAL, LOCKED, EXPIRED
    last_login_at timestamp,
    created_at timestamp default current_timestamp not null,
    updated_at timestamp default current_timestamp not null,
    primary key (user_id)
);

comment on table asc_user is 'User accounts';

-- Roles
create table if not exists asc_role (
    role_id bigserial not null,
    role_name varchar(50) not null unique,
    description varchar(200),
    primary key (role_id)
);

comment on table asc_role is 'Roles';

-- User-Role mapping
create table if not exists asc_user_role (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id),
    foreign key (user_id) references asc_user(user_id) on delete cascade,
    foreign key (role_id) references asc_role(role_id) on delete cascade
);

comment on table asc_user_role is 'User-Role mapping';

-- Permissions
create table if not exists asc_permission (
    perm_id bigserial not null,
    perm_code varchar(50) not null unique,
    description varchar(200),
    primary key (perm_id)
);

comment on table asc_permission is 'Permissions';

-- Role-Permission mapping
create table if not exists asc_role_permission (
    role_id bigint not null,
    perm_id bigint not null,
    primary key (role_id, perm_id),
    foreign key (role_id) references asc_role(role_id) on delete cascade,
    foreign key (perm_id) references asc_permission(perm_id) on delete cascade
);

comment on table asc_role_permission is 'Role-Permission mapping';

-- Login History
create table if not exists asc_login_history (
    history_id bigserial not null,
    username varchar(50) not null,
    login_at timestamp default current_timestamp not null,
    ip_address varchar(45),
    user_agent varchar(500),
    success_yn char(1) default 'Y' not null,
    primary key (history_id)
);

comment on table asc_login_history is 'Login History';

-- Vault (Encrypted Tokens)
create table if not exists asc_vault (
    vault_id bigserial not null,
    label varchar(100) not null,
    token_type varchar(20) default 'SIMPLE' not null, -- SIMPLE, PERSISTENT, TIME_LIMITED
    encrypted_value varchar(500) not null,
    description varchar(500),
    valid_until timestamp,
    created_at timestamp default current_timestamp not null,
    updated_at timestamp default current_timestamp not null,
    primary key (vault_id)
);

comment on table asc_vault is 'Vault (Encrypted Tokens)';

-- Initial data
insert into asc_role (role_name, description) values ('SUPER_ADMIN', 'Super administrator with full access') on conflict (role_name) do nothing;
insert into asc_role (role_name, description) values ('ADMIN', 'Administrator with limited management access') on conflict (role_name) do nothing;
insert into asc_role (role_name, description) values ('VIEWER', 'User with read-only access') on conflict (role_name) do nothing;
insert into asc_role (role_name, description) values ('DEMO', 'Demo user with simulation access') on conflict (role_name) do nothing;

insert into asc_permission (perm_code, description) values ('MONITOR_VIEW', 'Access to monitoring dashboard') on conflict (perm_code) do nothing;
insert into asc_permission (perm_code, description) values ('MONITOR_CONTROL', 'Control monitoring settings') on conflict (perm_code) do nothing;
insert into asc_permission (perm_code, description) values ('USER_MANAGE', 'Manage users and roles') on conflict (perm_code) do nothing;
insert into asc_permission (perm_code, description) values ('NODE_MANAGE', 'Manage and restart cluster nodes') on conflict (perm_code) do nothing;
insert into asc_permission (perm_code, description) values ('COMMAND_EXECUTE', 'Execute remote commands') on conflict (perm_code) do nothing;

-- Map permissions to SUPER_ADMIN
insert into asc_role_permission (role_id, perm_id) select 1, perm_id from asc_permission on conflict (role_id, perm_id) do nothing;
-- Map permissions to ADMIN
insert into asc_role_permission (role_id, perm_id) select 2, perm_id from asc_permission where perm_code in ('MONITOR_VIEW', 'COMMAND_EXECUTE', 'NODE_MANAGE') on conflict (role_id, perm_id) do nothing;
-- Map permissions to VIEWER
insert into asc_role_permission (role_id, perm_id) select 3, perm_id from asc_permission where perm_code = 'MONITOR_VIEW' on conflict (role_id, perm_id) do nothing;
-- Map permissions to DEMO
insert into asc_role_permission (role_id, perm_id) select 4, perm_id from asc_permission on conflict (role_id, perm_id) do nothing;

-- Initial Super Admin user (password: admin123)
insert into asc_user (username, password, nickname, email) values ('admin', 'admin123', 'Super Admin', 'admin@aspectow.com') on conflict (username) do nothing;
insert into asc_user_role (user_id, role_id) values (1, 1) on conflict (user_id, role_id) do nothing;
