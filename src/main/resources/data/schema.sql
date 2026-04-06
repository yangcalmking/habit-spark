-- 学习习惯培养积分奖励系统 - 数据库建表脚本
-- 数据库: SQLite 3.x (WAL 模式)
-- 来源: task-20260402-083003 (architect-lin)

PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;

CREATE TABLE IF NOT EXISTS family_group (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        VARCHAR(50)    NOT NULL,
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    family_group_id INTEGER        NOT NULL,
    username        VARCHAR(30)    NOT NULL,
    password_hash   VARCHAR(100)   NOT NULL,
    nickname        VARCHAR(30)    NOT NULL,
    role            VARCHAR(10)    NOT NULL CHECK(role IN ('student', 'parent', 'admin')),
    avatar_url      VARCHAR(255),
    locked_until    DATETIME,
    is_active       TINYINT        NOT NULL DEFAULT 1,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_group_id) REFERENCES family_group(id),
    UNIQUE(username)
);
CREATE INDEX IF NOT EXISTS idx_user_family_group ON user(family_group_id);

CREATE TABLE IF NOT EXISTS point_account (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id        INTEGER        NOT NULL,
    total_points      INTEGER        NOT NULL DEFAULT 0,
    available_points  INTEGER        NOT NULL DEFAULT 0,
    frozen_points     INTEGER        NOT NULL DEFAULT 0,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES user(id),
    UNIQUE(student_id),
    CHECK (available_points >= 0),
    CHECK (frozen_points >= 0)
);

CREATE TABLE IF NOT EXISTS point_flow (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id    INTEGER        NOT NULL,
    amount        INTEGER        NOT NULL,
    flow_type     TINYINT        NOT NULL CHECK(flow_type IN (1, 2, 3, 4, 5, 6)),
    reason        VARCHAR(255)   NOT NULL,
    related_id    INTEGER,
    related_type  VARCHAR(20)    CHECK(related_type IS NULL OR related_type IN ('task', 'exchange', 'adjust')),
    operator_id   INTEGER,
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES user(id),
    FOREIGN KEY (operator_id) REFERENCES user(id)
);
CREATE INDEX IF NOT EXISTS idx_flow_student ON point_flow(student_id);
CREATE INDEX IF NOT EXISTS idx_flow_created ON point_flow(student_id, created_at);

CREATE TABLE IF NOT EXISTS task_template (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    name              VARCHAR(50)    NOT NULL,
    category          VARCHAR(20)    NOT NULL CHECK(category IN ('study', 'homework', 'morality', 'interest', 'extra')),
    base_points       INTEGER        NOT NULL DEFAULT 0,
    extra_points_rule VARCHAR(500),
    extra_points      INTEGER        NOT NULL DEFAULT 0,
    daily_cap         INTEGER        NOT NULL DEFAULT -1,
    allow_custom      TINYINT        NOT NULL DEFAULT 0,
    description       TEXT,
    sort_order        INTEGER        NOT NULL DEFAULT 0,
    is_active         TINYINT        NOT NULL DEFAULT 1,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_template_category ON task_template(category);

CREATE TABLE IF NOT EXISTS task_record (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id        INTEGER        NOT NULL,
    task_template_id  INTEGER        NOT NULL,
    task_date         DATE           NOT NULL,
    description       TEXT,
    attachment_url    VARCHAR(500),
    points            INTEGER        NOT NULL DEFAULT 0,
    status            TINYINT        NOT NULL DEFAULT 0 CHECK(status IN (0, 1, 2)),
    parent_comment    VARCHAR(500),
    reviewer_id       INTEGER,
    reviewed_at       DATETIME,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES user(id),
    FOREIGN KEY (task_template_id) REFERENCES task_template(id),
    FOREIGN KEY (reviewer_id) REFERENCES user(id),
    UNIQUE(student_id, task_template_id, task_date)
);
CREATE INDEX IF NOT EXISTS idx_record_student_status ON task_record(student_id, status);

CREATE TABLE IF NOT EXISTS product (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            VARCHAR(100)   NOT NULL,
    category        VARCHAR(20)    NOT NULL CHECK(category IN ('singing', 'craft', 'comic', 'playground', 'other')),
    points_cost     INTEGER        NOT NULL,
    stock           INTEGER        NOT NULL DEFAULT -1,
    description     TEXT,
    image_urls      TEXT,
    source_type     TINYINT        NOT NULL DEFAULT 2 CHECK(source_type IN (1, 2)),
    source_url      VARCHAR(500),
    source_id       VARCHAR(50),
    level           VARCHAR(10)    NOT NULL DEFAULT 'instant' CHECK(level IN ('instant', 'mid', 'long')),
    is_active       TINYINT        NOT NULL DEFAULT 1,
    sort_order      INTEGER        NOT NULL DEFAULT 0,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_product_active ON product(category, is_active);

CREATE TABLE IF NOT EXISTS exchange_request (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id            INTEGER        NOT NULL,
    product_id            INTEGER        NOT NULL,
    points_cost           INTEGER        NOT NULL,
    status                VARCHAR(15)    NOT NULL DEFAULT 'pending' CHECK(status IN ('pending', 'confirmed', 'delivered', 'cancelled', 'rejected')),
    product_name_snapshot VARCHAR(100)   NOT NULL,
    auto_confirm          TINYINT        NOT NULL DEFAULT 0,
    confirm_by            INTEGER,
    confirmed_at          DATETIME,
    remark                VARCHAR(500),
    created_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES user(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (confirm_by) REFERENCES user(id)
);
CREATE INDEX IF NOT EXISTS idx_exchange_student ON exchange_request(student_id, status);

CREATE TABLE IF NOT EXISTS notification (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER        NOT NULL,
    title         VARCHAR(100)   NOT NULL,
    content       TEXT           NOT NULL,
    type          VARCHAR(20)    NOT NULL DEFAULT 'system',
    related_id    INTEGER,
    related_type  VARCHAR(20),
    is_read       TINYINT        NOT NULL DEFAULT 0,
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id)
);
CREATE INDEX IF NOT EXISTS idx_notification_user_read ON notification(user_id, is_read);

CREATE TABLE IF NOT EXISTS operation_log (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id   INTEGER        NOT NULL,
    action        VARCHAR(50)    NOT NULL,
    target_type   VARCHAR(20)    NOT NULL,
    target_id     INTEGER,
    detail        TEXT,
    ip_address    VARCHAR(45),
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (operator_id) REFERENCES user(id)
);
CREATE INDEX IF NOT EXISTS idx_log_operator ON operation_log(operator_id, created_at);
