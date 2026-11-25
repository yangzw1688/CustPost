CREATE TABLE IF NOT EXISTS admin_permission (
    phone VARCHAR(11) NOT NULL PRIMARY KEY,
    super_admin BOOLEAN NOT NULL DEFAULT FALSE,
    policy_transfer BOOLEAN NOT NULL,
    ui_management BOOLEAN NOT NULL,
    dashboard_access BOOLEAN,
    case_access BOOLEAN,
    policy_access BOOLEAN,
    project_access BOOLEAN,
    platform_access BOOLEAN,
    user_access BOOLEAN,
    password_hash VARCHAR(128) NOT NULL
);
