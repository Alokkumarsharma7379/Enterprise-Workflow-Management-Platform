CREATE TABLE users (
    id uuid PRIMARY KEY,
    email varchar(254) NOT NULL UNIQUE CHECK (email = lower(trim(email))),
    display_name varchar(100) NOT NULL,
    password_hash varchar(255) NOT NULL,
    created_at timestamptz NOT NULL
);
CREATE TABLE organizations (
    id uuid PRIMARY KEY,
    name varchar(120) NOT NULL,
    created_by uuid NOT NULL REFERENCES users(id),
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE TABLE organization_members (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES users(id),
    role varchar(20) NOT NULL CHECK (role IN ('OWNER','ADMIN','MANAGER','MEMBER')),
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE','REMOVED')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (organization_id, user_id)
);
CREATE UNIQUE INDEX one_active_owner ON organization_members(organization_id) WHERE role = 'OWNER' AND status = 'ACTIVE';
CREATE INDEX memberships_user ON organization_members(user_id, status, organization_id);
CREATE TABLE projects (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name varchar(120) NOT NULL,
    project_key varchar(10) NOT NULL CHECK (project_key ~ '^[A-Z][A-Z0-9]{1,9}$'),
    description text,
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE','ARCHIVED')),
    created_by uuid NOT NULL REFERENCES users(id),
    next_task_number bigint NOT NULL DEFAULT 1 CHECK (next_task_number > 0),
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (organization_id, project_key)
);
CREATE TABLE tasks (
    id uuid PRIMARY KEY,
    project_id uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    task_number bigint NOT NULL CHECK (task_number > 0),
    title varchar(200) NOT NULL,
    description text,
    status varchar(20) NOT NULL CHECK (status IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE','CANCELLED')),
    priority varchar(20) NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    reporter_id uuid NOT NULL REFERENCES users(id),
    assignee_id uuid REFERENCES users(id),
    due_date date,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (project_id, task_number),
    UNIQUE (id, project_id)
);
CREATE INDEX tasks_project_created ON tasks(project_id, created_at, id);
CREATE INDEX tasks_project_status ON tasks(project_id, status, created_at, id);
CREATE INDEX tasks_project_assignee ON tasks(project_id, assignee_id);
CREATE TABLE comments (
    id uuid PRIMARY KEY,
    task_id uuid NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    author_id uuid NOT NULL REFERENCES users(id),
    body text NOT NULL CHECK (length(body) BETWEEN 1 AND 10000),
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE INDEX comments_task ON comments(task_id, created_at, id);
CREATE TABLE labels (
    id uuid PRIMARY KEY,
    project_id uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name varchar(50) NOT NULL,
    color varchar(7) NOT NULL CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    UNIQUE (project_id, name),
    UNIQUE (id, project_id)
);
CREATE TABLE task_labels (
    task_id uuid NOT NULL,
    label_id uuid NOT NULL,
    project_id uuid NOT NULL,
    PRIMARY KEY (task_id, label_id),
    FOREIGN KEY (task_id, project_id) REFERENCES tasks(id, project_id) ON DELETE CASCADE,
    FOREIGN KEY (label_id, project_id) REFERENCES labels(id, project_id) ON DELETE CASCADE
);
CREATE INDEX task_labels_label ON task_labels(label_id, task_id);
CREATE TABLE activity_logs (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id uuid REFERENCES projects(id) ON DELETE SET NULL,
    actor_id uuid NOT NULL REFERENCES users(id),
    action_type varchar(50) NOT NULL,
    entity_type varchar(30) NOT NULL,
    entity_id uuid NOT NULL,
    old_value text,
    new_value text,
    created_at timestamptz NOT NULL
);
CREATE INDEX activity_entity ON activity_logs(entity_type, entity_id, created_at, id);
CREATE INDEX activity_project ON activity_logs(project_id, created_at, id);
CREATE INDEX activity_organization ON activity_logs(organization_id, created_at, id);
CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    family_id uuid NOT NULL,
    token_hash varchar(64) NOT NULL UNIQUE,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    revoked_at timestamptz
);
CREATE INDEX refresh_family ON refresh_tokens(family_id);
