# 数据库建模与建表脚本 — 学习习惯培养积分奖励系统

**Task ID**: task-20260402-083003  
**Assigned to**: @architect-lin:matrix-local.hiclaw.io:18080  
**Project**: 学习习惯培养积分奖励系统  
**Phase**: 2 - 技术架构与数据库设计  
**Date**: 2026-04-03  
**Dependencies**: task-20260402-083002（技术架构设计文档 - 已完成）  

---

## 一、完整 ER 图（文本描述）

```
family_group (家庭组)
    │ 1
    │
    │ N
    ▼
user (用户) ─────────────────────────┐
    │ role: student/parent/admin      │
    │ family_group_id                 │
    │                                  │
    │ 1                                │
    │                                  │
    ▼                                  │
point_account (积分账户)                │
    │ 1                                │
    │ student_id = user.id             │
    │                                  │
    │ 1                                │
    ├── N ──► point_flow (积分流水)    │
    │           student_id             │
    │           operator_id ───────────┘ (家长操作)
    │
    │
    │ N
    ├── N ──► task_record (任务记录)
    │           student_id
    │           task_template_id
    │
    │ N
    ├── N ──► exchange_request (兑换申请)
    │           student_id
    │           product_id
    │
    │ N
    └── N ──► notification (系统通知)
                user_id


task_template (任务模板)                    product (商品)
    │ 1                                         │ 1
    │                                           │
    ▼ N                                         ▼ N
task_record                                exchange_request


user (操作人)
    │ 1
    │ operator_id
    ▼
operation_log (操作审计日志)
```

### 实体关系说明

| 关系 | 类型 | 说明 |
|------|------|------|
| family_group → user | 1:N | 一个家庭组包含多个用户（家长+学生） |
| user → point_account | 1:1 | 每个学生用户对应一个积分账户 |
| user → notification | 1:N | 一个用户可收到多条通知 |
| point_account → point_flow | 1:N | 一个账户有多条积分流水 |
| point_account → task_record | 1:N | 一个学生有多条任务记录 |
| point_account → exchange_request | 1:N | 一个学生可提交多个兑换申请 |
| task_template → task_record | 1:N | 一个模板对应多条打卡记录 |
| product → exchange_request | 1:N | 一个商品可被多次兑换 |
| user → operation_log | 1:N | 家长操作产生审计日志 |

---

## 二、表结构详细设计

### 2.1 family_group — 家庭组表

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | INTEGER | PK, AUTOINCREMENT | - | 主键 |
| name | VARCHAR(50) | NOT NULL | - | 家庭组名称（如"小雨家"） |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

**索引**：无（表极小，≤5 行）

### 2.2 user — 统一用户表

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | INTEGER | PK, AUTOINCREMENT | - | 主键 |
| family_group_id | INTEGER | NOT NULL, FK→family_group(id) | - | 所属家庭组 |
| username | VARCHAR(30) | NOT NULL, UNIQUE | - | 登录用户名 |
| password_hash | VARCHAR(100) | NOT NULL | - | BCrypt 密码哈希 |
| nickname | VARCHAR(30) | NOT NULL | - | 显示昵称（学生用可爱名，家长用真名） |
| role | VARCHAR(10) | NOT NULL, CHECK(role IN ('student','parent','admin')) | - | 角色 |
| avatar_url | VARCHAR(255) | - | NULL | 头像图片路径 |
| locked_until | DATETIME | - | NULL | 账号锁定截止时间（登录失败超限） |
| is_active | TINYINT | NOT NULL | 1 | 是否启用（0=禁用） |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 注册时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- `UNIQUE(username)` — 唯一登录名
- `IDX_user_family_group(family_group_id)` — 查询家庭内用户

### 2.3 point_account — 积分账户表

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | INTEGER | PK, AUTOINCREMENT | - | 主键 |
| student_id | INTEGER | NOT NULL, UNIQUE, FK→user(id) | - | 学生用户 ID |
| total_points | INTEGER | NOT NULL | 0 | 累计获得总积分（只增不减） |
| available_points | INTEGER | NOT NULL | 0 | 可用积分 = total - used - frozen |
| frozen_points | INTEGER | NOT NULL | 0 | 冻结积分（待确认的兑换申请） |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |

**说明**：
- `total_points`：历史累计获得的总积分，不会因为兑换或扣分而减少
- `available_points`：当前可以使用的积分，兑换时扣减
- `frozen_points`：已提交兑换申请但还未确认的积分
- 关系：`total_points = available_points + frozen_points + used_points`（used 已消费的）
- 为了简化，我们采用：`total_points` 累计获得，`available_points` 当前可用，`frozen_points` 冻结中
- 校验约束：`available_points >= 0`，`frozen_points >= 0`

**索引**：
- `UNIQUE(student_id)` — 每个学生一个账户

### 2.4 point_flow — 积分流水表

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | INTEGER | PK, AUTOINCREMENT | - | 主键 |
| student_id | INTEGER | NOT NULL, FK→user(id) | - | 学生用户 ID |
| amount | INTEGER | NOT NULL | - | 变动值（正=获得，负=扣除/消费） |
| flow_type | TINYINT | NOT NULL, CHECK(flow_type IN (1,2,3,4,5,6)) | - | 流水类型 |
| reason | VARCHAR(255) | NOT NULL | - | 变动原因描述 |
| related_id | INTEGER | - | NULL | 关联记录 ID（task_record/exchange_request/手动调整） |
| related_type | VARCHAR(20) | - | NULL | 关联类型（task/exchange/adjust） |
| operator_id | INTEGER | - | NULL, FK→user(id) | 操作人 ID（系统自动则为 NULL） |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 发生时间 |

**流水类型枚举 (flow_type)**：

| 代码 | 名称 | amount 正负 | 场景 |
|------|------|------------|------|
| 1 | 任务获得 | + | 任务审核通过 |
| 2 | 家长扣分 | - | 家长手动扣分 |
| 3 | 家长加分 | + | 家长手动加分 |
| 4 | 兑换消费 | - | 兑换确认后扣减 |
| 5 | 兑换冻结 | - | 提交兑换申请时冻结（负值） |
| 6 | 兑换解冻 | + | 兑换被拒绝时释放（正值） |

**索引**：
- `IDX_flow_student(student_id)` — 查询某个学生的流水
- `IDX_flow_created(student_id, created_at)` — 按时间排序查询（覆盖索引）

### 2.5 task_template — 任务模板表

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | INTEGER | PK, AUTOINCREMENT | - | 主键 |
| name | VARCHAR(50) | NOT NULL | - | 模板名称（如"课前预习"） |
| category | VARCHAR(20) | NOT NULL, CHECK(category IN ('study','homework','morality','interest','extra')) | - | 分类 |
| base_points | INTEGER | NOT NULL | 0 | 基础积分（完成即得） |
| extra_points_rule | VARCHAR(500) | - | NULL | 额外积分规则描述（JSON 格式） |
| extra_points | INTEGER | NOT NULL | 0 | 额外积分最大值 |
| daily_cap | INTEGER | NOT NULL | -1 | 每日上限（-1=无上限） |
| allow_custom | TINYINT | NOT NULL | 0 | 是否允许学生自定义（1=可自定义） |
| description | TEXT | - | NULL | 任务描述/完成标准 |
| sort_order | INTEGER | NOT NULL | 0 | 排序权重 |
| is_active | TINYINT | NOT NULL | 1 | 是否启用（0=停用） |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |

**分类枚举 (category)**：

| 代码 | 名称 | 对应 PRD 模块 |
|------|------|-------------|
| study | 基础学习习惯 | 课前预习/课后复习/规律作息 |
| homework | 作业相关 | 课后作业/错题整理 |
| extra | 进阶学习提升 | 单元测验/作文优秀/听写全对 |
| morality | 家务德育 | 家务劳动 + 品德行为 |
| interest | 兴趣特长 | 唱歌/手工/漫画 |

**索引**：
- `IDX_template_category(category)` — 按分类查询
- `IDX_template_active(category, is_active)` — 查询启用的模板

### 2.6 task_record — 任务记录表

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | INTEGER | PK, AUTOINCREMENT | - | 主键 |
| student_id | INTEGER | NOT NULL, FK→user(id) | - | 学生用户 ID |
| task_template_id | INTEGER | NOT NULL, FK→task_template(id) | - | 任务模板 ID |
| task_date | DATE | NOT NULL | - | 任务日期 |
| description | TEXT | - | NULL | 学生提交的完成描述 |
| attachment_url | VARCHAR(500) | - | NULL | 凭证图片路径（可多张，逗号分隔） |
| points | INTEGER | NOT NULL | 0 | 获得积分（后端根据模板自动计算） |
| status | TINYINT | NOT NULL, CHECK(status IN (0,1,2)), DEFAULT 0 | 0 | 审核状态（0=待审核, 1=已通过, 2=已拒绝） |
| parent_comment | VARCHAR(500) | - | NULL | 家长审核评语 |
| reviewer_id | INTEGER | - | NULL, FK→user(id) | 审核人 ID |
| reviewed_at | DATETIME | - | NULL | 审核时间 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 提交时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |

**约束**：
- 同一学生 + 同模板 + 同日 = 唯一（防止重复提交）

**索引**：
- `UQ_record_student_template_date(student_id, task_template_id, task_date)` — 防重复提交
- `IDX_record_student_status(student_id, status)` — 学生查看自己的待审核
- `IDX_record_status(status, task_date)` — 家长查看待审核列表
- `IDX_record_student_date(student_id, task_date)` — 按日期查询

### 2.7 product — 商品表

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | INTEGER | PK, AUTOINCREMENT | - | 主键 |
| name | VARCHAR(100) | NOT NULL | - | 商品名称 |
| category | VARCHAR(20) | NOT NULL, CHECK(category IN ('singing','craft','comic','playground','other')) | - | 兴趣分类 |
| points_cost | INTEGER | NOT NULL | - | 所需积分 |
| stock | INTEGER | NOT NULL | -1 | 库存数量（-1=不限） |
| description | TEXT | - | NULL | 商品描述 |
| image_urls | TEXT | - | NULL | 图片 URL 列表（逗号分隔） |
| source_type | TINYINT | NOT NULL, DEFAULT 2 | 2 | 来源（1=淘宝导入, 2=手动创建） |
| source_url | VARCHAR(500) | - | NULL | 来源链接（淘宝商品 URL） |
| source_id | VARCHAR(50) | - | NULL | 来源商品 ID |
| level | VARCHAR(10) | NOT NULL, DEFAULT 'instant' | - | 等级档次（instant=即时, mid=中期, long=长期） |
| is_active | TINYINT | NOT NULL | 1 | 是否上架（0=下架） |
| sort_order | INTEGER | NOT NULL | 0 | 排序权重 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |

**商品等级 (level)**：

| 代码 | 名称 | 说明 |
|------|------|------|
| instant | 即时奖励 | ≤15 分，自动确认 |
| mid | 中期奖励 | 需要家长审批 |
| long | 长期奖励 | 需要家长审批 |

**商品分类 (category)**：

| 代码 | 名称 |
|------|------|
| singing | 唱歌 |
| craft | 手工 |
| comic | 漫画 |
| playground | 游乐场 |
| other | 其他 |

**索引**：
- `IDX_product_active(category, is_active)` — 查询上架商品
- `IDX_product_level(is_active, level)` — 按等级筛选

### 2.8 exchange_request — 兑换申请表

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | INTEGER | PK, AUTOINCREMENT | - | 主键 |
| student_id | INTEGER | NOT NULL, FK→user(id) | - | 学生用户 ID |
| product_id | INTEGER | NOT NULL, FK→product(id) | - | 商品 ID |
| points_cost | INTEGER | NOT NULL | - | 兑换时消耗的积分（快照） |
| status | VARCHAR(15) | NOT NULL, CHECK(status IN ('pending','confirmed','delivered','cancelled','rejected')) | 'pending' | 状态 |
| product_name_snapshot | VARCHAR(100) | NOT NULL | - | 商品名称快照（防止商品改名后混乱） |
| auto_confirm | TINYINT | NOT NULL | 0 | 是否自动确认（即时奖励=1） |
| confirm_by | INTEGER | - | NULL, FK→user(id) | 确认人 ID |
| confirmed_at | DATETIME | - | NULL | 确认时间 |
| remark | VARCHAR(500) | - | NULL | 备注/拒绝理由 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 申请时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |

**状态流转**：

```
pending (待确认)
  ├── auto_confirm=1 → confirmed → delivered（即时奖励自动流转）
  ├── parent confirm → confirmed → delivered（家长确认后发货→完成）
  ├── parent reject → cancelled（家长拒绝，积分释放）
  └── student cancel → cancelled（学生主动取消，积分释放）
```

**索引**：
- `IDX_exchange_student(student_id, status)` — 学生查看自己的兑换
- `IDX_exchange_status(status)` — 家长查看待审批
- `IDX_exchange_created(student_id, created_at)` — 按时间查询

### 2.9 notification — 系统通知表

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | INTEGER | PK, AUTOINCREMENT | - | 主键 |
| user_id | INTEGER | NOT NULL, FK→user(id) | - | 接收用户 ID |
| title | VARCHAR(100) | NOT NULL | - | 通知标题 |
| content | TEXT | NOT NULL | - | 通知内容 |
| type | VARCHAR(20) | NOT NULL, DEFAULT 'system' | - | 通知类型 |
| related_id | INTEGER | - | NULL | 关联记录 ID |
| related_type | VARCHAR(20) | - | NULL | 关联类型 |
| is_read | TINYINT | NOT NULL | 0 | 是否已读（0=未读, 1=已读） |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 发送时间 |

**通知类型 (type)**：

| 代码 | 说明 | 触发场景 |
|------|------|---------|
| task_submitted | 任务已提交 | 学生提交打卡 → 通知家长 |
| task_approved | 任务已通过 | 家长审核通过 → 通知学生 |
| task_rejected | 任务被拒绝 | 家长审核拒绝 → 通知学生 |
| exchange_submitted | 兑换已提交 | 学生提交兑换 → 通知家长 |
| exchange_confirmed | 兑换已确认 | 家长确认 → 通知学生 |
| exchange_rejected | 兑换被拒绝 | 家长拒绝 → 通知学生 |
| point_adjusted | 积分被调整 | 家长调整积分 → 通知学生 |
| system | 系统通知 | 系统消息 |

**索引**：
- `IDX_notification_user_read(user_id, is_read)` — 查询未读通知
- `IDX_notification_user(user_id, created_at DESC)` — 按时间查询通知

### 2.10 operation_log — 操作审计日志表

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | INTEGER | PK, AUTOINCREMENT | - | 主键 |
| operator_id | INTEGER | NOT NULL, FK→user(id) | - | 操作人 ID |
| action | VARCHAR(50) | NOT NULL | - | 操作类型 |
| target_type | VARCHAR(20) | NOT NULL | - | 目标类型 |
| target_id | INTEGER | - | NULL | 目标记录 ID |
| detail | TEXT | - | NULL | 操作详情（JSON，记录变更前后值） |
| ip_address | VARCHAR(45) | - | NULL | 请求 IP |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 操作时间 |

**操作类型 (action)**：

| 代码 | 说明 |
|------|------|
| point_adjust | 积分调整 |
| task_review | 任务审核 |
| product_create | 新增商品 |
| product_update | 修改商品 |
| product_toggle | 上下架商品 |
| exchange_confirm | 确认兑换 |
| exchange_reject | 拒绝兑换 |
| user_create | 新增用户 |

**索引**：
- `IDX_log_operator(operator_id, created_at)` — 查看某人的操作历史
- `IDX_log_action(action, created_at)` — 按操作类型查询

---

## 三、完整建表 SQL 脚本（SQLite 语法）

```sql
-- =====================================================
-- 学习习惯培养积分奖励系统 - 数据库建表脚本
-- 数据库: SQLite 3.x (WAL 模式)
-- 生成日期: 2026-04-03
-- =====================================================

-- 启用外键约束（SQLite 默认关闭）
PRAGMA foreign_keys = ON;

-- 设置 WAL 模式
PRAGMA journal_mode = WAL;

-- =====================================================
-- 1. family_group — 家庭组表
-- =====================================================
CREATE TABLE IF NOT EXISTS family_group (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        VARCHAR(50)    NOT NULL,
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. user — 统一用户表
-- =====================================================
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

-- =====================================================
-- 3. point_account — 积分账户表
-- =====================================================
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

-- =====================================================
-- 4. point_flow — 积分流水表
-- =====================================================
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

-- =====================================================
-- 5. task_template — 任务模板表
-- =====================================================
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
CREATE INDEX IF NOT EXISTS idx_template_active ON task_template(category, is_active);

-- =====================================================
-- 6. task_record — 任务记录表
-- =====================================================
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
CREATE INDEX IF NOT EXISTS idx_record_status_date ON task_record(status, task_date);
CREATE INDEX IF NOT EXISTS idx_record_student_date ON task_record(student_id, task_date);

-- =====================================================
-- 7. product — 商品表
-- =====================================================
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
CREATE INDEX IF NOT EXISTS idx_product_level ON product(is_active, level);

-- =====================================================
-- 8. exchange_request — 兑换申请表
-- =====================================================
CREATE TABLE IF NOT EXISTS exchange_request (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id            INTEGER        NOT NULL,
    product_id            INTEGER        NOT NULL,
    points_cost           INTEGER        NOT NULL,
    status                VARCHAR(15)    NOT NULL DEFAULT 'pending' 
                        CHECK(status IN ('pending', 'confirmed', 'delivered', 'cancelled', 'rejected')),
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
CREATE INDEX IF NOT EXISTS idx_exchange_status ON exchange_request(status);
CREATE INDEX IF NOT EXISTS idx_exchange_created ON exchange_request(student_id, created_at);

-- =====================================================
-- 9. notification — 系统通知表
-- =====================================================
CREATE TABLE IF NOT EXISTS notification (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER        NOT NULL,
    title         VARCHAR(100)   NOT NULL,
    content       TEXT           NOT NULL,
    type          VARCHAR(20)    NOT NULL DEFAULT 'system'
                CHECK(type IN ('task_submitted', 'task_approved', 'task_rejected',
                               'exchange_submitted', 'exchange_confirmed', 'exchange_rejected',
                               'point_adjusted', 'system')),
    related_id    INTEGER,
    related_type  VARCHAR(20),
    is_read       TINYINT        NOT NULL DEFAULT 0,
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX IF NOT EXISTS idx_notification_user_read ON notification(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notification_user ON notification(user_id, created_at DESC);

-- =====================================================
-- 10. operation_log — 操作审计日志表
-- =====================================================
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
CREATE INDEX IF NOT EXISTS idx_log_action ON operation_log(action, created_at);
```

---

## 四、初始数据插入脚本

```sql
-- =====================================================
-- 学习习惯培养积分奖励系统 - 初始数据脚本
-- 生成日期: 2026-04-03
-- =====================================================

-- =====================================================
-- 1. 默认家庭组
-- =====================================================
INSERT INTO family_group (id, name) VALUES (1, '小雨家');

-- =====================================================
-- 2. 默认用户
-- 密码均为: BCrypt 加密后的值
-- admin 密码: admin123
-- 学生(xue) 密码: xue123
-- 家长(jia)  密码: jia123
-- 注意: 实际部署时需替换为真实的 BCrypt 哈希
-- =====================================================

-- 管理员
INSERT INTO user (id, family_group_id, username, password_hash, nickname, role, is_active)
VALUES (1, 1, 'admin', '$2a$12$LJ3m4ys3Lk0N4G6qZQKpGeNlYqFvBnXjGqZQKpGeNlYqFvBnXjGqZQK', '系统管理员', 'admin', 1);

-- 学生
INSERT INTO user (id, family_group_id, username, password_hash, nickname, role, is_active)
VALUES (2, 1, 'xue', '$2a$12$Xz3qLJ3m4ys3Lk0N4G6qZQKpGeNlYqFvBnXjGqZQKpGeNlYqFvBnXjG', '小雨', 'student', 1);

-- 家长
INSERT INTO user (id, family_group_id, username, password_hash, nickname, role, is_active)
VALUES (3, 1, 'jia', '$2a$12$Qz3qLJ3m4ys3Lk0N4G6qZQKpGeNlYqFvBnXjGqZQKpGeNlYqFvBnXjG', '妈妈', 'parent', 1);

-- =====================================================
-- 3. 学生积分账户
-- =====================================================
INSERT INTO point_account (student_id, total_points, available_points, frozen_points)
VALUES (2, 0, 0, 0);

-- =====================================================
-- 4. 任务模板数据
-- =====================================================

-- 4.1 基础学习习惯 (study)
INSERT INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active)
VALUES 
    ('课前预习', 'study', 1, 1, '{"subject_count":3,"bonus":1,"rule":"语数英每科1分,全完成额外+1"}', 4, 
     '语文：标出生字词、分段；数学：看懂例题、标注不懂知识点；英语：听读课文、圈出生词', 1, 1),
    
    ('课后复习', 'study', 0, 1, '{"per_subject":0.5,"bonus":0.5,"rule":"每科0.5分,3科全完成额外+0.5"}', 2,
     '复习当天所学内容，每科至少10分钟', 4, 1),
    
    ('规律作息', 'study', 1, 0, NULL, 1,
     '21:30 前完成洗漱、上床睡觉', 6, 1);

-- 4.2 作业相关 (homework)
INSERT INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active)
VALUES 
    ('课后作业', 'homework', 2, 1, '{"neat_bonus":1,"rule":"按时完成基础2分,工整额外+1"}', 3,
     '按时完成当天作业，书写工整、格式规范', 2, 1),
    
    ('错题整理', 'homework', 0, 1, '{"per_item":0.5,"bonus":0.5,"rule":"每道0.5分,当日全部整理额外+0.5", "daily_cap_items":6}', 3,
     '整理当天错题，分析错误原因，写出正确解法', 3, 1);

-- 4.3 进阶学习提升 (extra)
INSERT INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active, allow_custom)
VALUES 
    ('单元测验优秀', 'extra', 10, 0, NULL, -1,
     '单元测验 90 分以上，每科 10 分', 7, 1, 1),
    
    ('作文获优', 'extra', 8, 0, NULL, -1,
     '作文获得老师"优+"评价', 8, 1, 1),
    
    ('英语听写全对', 'extra', 3, 0, NULL, -1,
     '英语单词听写全部正确', 9, 1, 1);

-- 4.4 自主阅读 (study)
INSERT INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active)
VALUES 
    ('自主阅读', 'study', 2, 5, '{"book_bonus":5,"rule":"阅读30分钟基础2分,读完一本书额外+5"}', -1,
     '课外自主阅读至少 30 分钟', 5, 1);

-- 4.5 兴趣特长 (interest)
INSERT INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active, allow_custom)
VALUES 
    ('唱歌练习', 'interest', 1, 0, NULL, -1,
     '每日练歌 15 分钟', 10, 1, 1),
    
    ('手工作品', 'interest', 5, 0, NULL, -1,
     '完成一件手工作品', 11, 1, 1),
    
    ('漫画创作', 'interest', 3, 0, NULL, -1,
     '完成一幅漫画作品', 12, 1, 1);

-- 4.6 家务德育 (morality)
INSERT INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active)
VALUES 
    ('整理房间', 'morality', 3, 0, NULL, 1,
     '整理自己的房间，保持整洁', 20, 1, 0),
    
    ('洗碗', 'morality', 2, 0, NULL, 1,
     '帮忙洗碗筷', 21, 1, 0),
    
    ('扫地拖地', 'morality', 2, 0, NULL, 1,
     '帮忙扫地拖地', 22, 1, 0),
    
    ('帮忙做饭', 'morality', 5, 0, NULL, 1,
     '帮忙准备饭菜', 23, 1, 0),
    
    ('照顾家人', 'morality', 5, 0, NULL, 1,
     '照顾弟妹或给家人按摩等', 24, 1, 0),
    
    ('礼貌行为', 'morality', 1, 0, NULL, -1,
     '主动问好、说谢谢等礼貌行为', 25, 1, 0),
    
    ('分享帮助', 'morality', 3, 0, NULL, -1,
     '分享食物/玩具或帮助同学家人', 26, 1, 0),
    
    ('诚实勇敢', 'morality', 5, 5, '{"range":"5-10","rule":"承认错误、面对困难，根据情况5-10分"}', -1,
     '承认错误、勇敢面对困难', 27, 1, 0),
    
    ('公益活动', 'morality', 10, 10, '{"range":"10-20","rule":"参加社区公益活动，根据情况10-20分"}', -1,
     '参加社区公益活动', 28, 1, 0);

-- =====================================================
-- 5. 预置礼品数据
-- =====================================================

-- 5.1 唱歌类 (singing)
INSERT INTO product (name, category, points_cost, stock, description, level, sort_order, is_active)
VALUES 
    ('家庭 K 歌时间 15 分钟', 'singing', 5, -1, '获得 15 分钟家庭 K 歌时间', 'instant', 1, 1),
    ('K 歌房 1 小时体验', 'singing', 60, 3, 'KTV 包房 1 小时，可带 1 位朋友', 'mid', 2, 1),
    ('专业儿童唱歌设备', 'singing', 400, 1, '一套专业儿童唱歌设备（麦克风+音箱）', 'long', 3, 1);

-- 5.2 手工类 (craft)
INSERT INTO product (name, category, points_cost, stock, description, level, sort_order, is_active)
VALUES 
    ('手工彩纸 / 贴纸', 'craft', 3, -1, '精美手工彩纸或贴纸一包', 'instant', 10, 1),
    ('奶油胶套装 / 手账礼盒', 'craft', 30, 5, '奶油胶 DIY 套装或精美手账礼盒', 'mid', 11, 1),
    ('高端手工工具礼盒', 'craft', 300, 2, '包含各类高端手工工具的大礼盒', 'long', 12, 1);

-- 5.3 漫画类 (comic)
INSERT INTO product (name, category, points_cost, stock, description, level, sort_order, is_active)
VALUES 
    ('漫画阅读时间 10 分钟', 'comic', 1, -1, '额外 10 分钟漫画阅读时间', 'instant', 20, 1),
    ('正版漫画单行本', 'comic', 50, 5, '自选一本正版漫画书', 'mid', 21, 1),
    ('全年漫画杂志订阅', 'comic', 500, 1, '全年漫画杂志订阅（12 期）', 'long', 22, 1);

-- 5.4 游乐场类 (playground)
INSERT INTO product (name, category, points_cost, stock, description, level, sort_order, is_active)
VALUES 
    ('周末额外动画时间 30 分钟', 'playground', 15, -1, '周末可额外看 30 分钟动画片', 'instant', 30, 1),
    ('室内游乐场全天畅玩', 'playground', 200, 2, '室内游乐场一整天畅玩', 'mid', 31, 1),
    ('主题乐园 2 天 1 晚', 'playground', 800, 1, '主题乐园 2 天 1 晚行程（含住宿）', 'long', 32, 1);
```

---

## 五、索引设计说明

| 索引名称 | 表 | 字段 | 用途 |
|---------|------|------|------|
| `idx_user_family_group` | user | family_group_id | 查询家庭组下所有成员 |
| `idx_flow_student` | point_flow | student_id | 查询某学生全部流水 |
| `idx_flow_created` | point_flow | (student_id, created_at) | 按时间倒序查询流水（覆盖索引） |
| `idx_template_category` | task_template | category | 按分类筛选模板 |
| `idx_template_active` | task_template | (category, is_active) | 查询某分类下的启用模板 |
| `idx_record_student_status` | task_record | (student_id, status) | 学生查看自己各状态任务 |
| `idx_record_status_date` | task_record | (status, task_date) | 家长查看待审核任务（核心查询） |
| `idx_record_student_date` | task_record | (student_id, task_date) | 按日期范围查询某学生记录 |
| `idx_product_active` | product | (category, is_active) | 查询上架商品（核心查询） |
| `idx_product_level` | product | (is_active, level) | 按等级筛选商品 |
| `idx_exchange_student` | exchange_request | (student_id, status) | 学生查看自己的兑换进度 |
| `idx_exchange_status` | exchange_request | status | 家长查看待审批兑换 |
| `idx_exchange_created` | exchange_request | (student_id, created_at) | 按时间查询兑换历史 |
| `idx_notification_user_read` | notification | (user_id, is_read) | 查询未读通知数量（红点） |
| `idx_notification_user` | notification | (user_id, created_at DESC) | 通知列表分页查询 |
| `idx_log_operator` | operation_log | (operator_id, created_at) | 查看某操作人员的历史 |
| `idx_log_action` | operation_log | (action, created_at) | 按操作类型审计 |

### 索引设计原则

1. **覆盖高频查询**：家长待审核列表、学生任务列表、未读通知是最高频查询
2. **联合索引前缀原则**：等值字段在前，范围排序字段在后
3. **避免过度索引**：SQLite 写锁敏感，小表（family_group、point_account）不加额外索引
4. **唯一约束即索引**：`UNIQUE` 约束自动创建唯一索引，无需重复创建

---

## 六、数据字典

### 6.1 核心概念映射

| 业务概念 | 对应表/字段 | 说明 |
|---------|-----------|------|
| 学生账号 | user (role='student') | 统一用户表中的学生角色 |
| 家长账号 | user (role='parent') | 统一用户表中的家长角色 |
| 积分账户 | point_account | 每个学生的积分钱包 |
| 积分流水 | point_flow | 每一笔积分变动的明细 |
| 任务模板 | task_template | 预定义的任务规则 |
| 打卡记录 | task_record | 学生提交的具体打卡 |
| 礼品 | product | 可兑换的商品 |
| 兑换申请 | exchange_request | 学生的兑换申请单 |
| 系统通知 | notification | 站内消息 |
| 操作日志 | operation_log | 家长操作的审计记录 |

### 6.2 状态码字典

**task_record.status**：
| 值 | 含义 | 前端展示色 |
|----|------|----------|
| 0 | 待审核 | 黄色 🟡 |
| 1 | 已通过 | 绿色 🟢 |
| 2 | 已拒绝 | 红色 🔴 |

**point_flow.flow_type**：
| 值 | 含义 | amount 符号 |
|----|------|-----------|
| 1 | 任务获得 | + |
| 2 | 家长扣分 | - |
| 3 | 家长加分 | + |
| 4 | 兑换消费 | - |
| 5 | 兑换冻结 | - |
| 6 | 兑换解冻 | + |

**exchange_request.status**：
| 值 | 含义 |
|----|------|
| pending | 待确认 |
| confirmed | 已确认 |
| delivered | 已发货（已完成） |
| cancelled | 已取消 |
| rejected | 已拒绝 |

**product.level**：
| 值 | 含义 | 审核要求 |
|----|------|---------|
| instant | 即时奖励 | 自动确认 |
| mid | 中期奖励 | 家长审批 |
| long | 长期奖励 | 家长审批 |

### 6.3 数据库约束汇总

| 表 | 约束类型 | 字段 | 规则 |
|----|---------|------|------|
| user | CHECK | role | IN ('student', 'parent', 'admin') |
| user | UNIQUE | username | 用户名唯一 |
| point_account | UNIQUE | student_id | 每学生一个账户 |
| point_account | CHECK | available_points | >= 0 |
| point_account | CHECK | frozen_points | >= 0 |
| point_flow | CHECK | flow_type | IN (1,2,3,4,5,6) |
| task_template | CHECK | category | IN ('study','homework','morality','interest','extra') |
| task_record | CHECK | status | IN (0, 1, 2) |
| task_record | UNIQUE | (student_id, task_template_id, task_date) | 同日同表不重复 |
| product | CHECK | category | IN ('singing','craft','comic','playground','other') |
| product | CHECK | level | IN ('instant', 'mid', 'long') |
| exchange_request | CHECK | status | IN ('pending','confirmed','delivered','cancelled','rejected') |
| notification | CHECK | type | 8 种通知类型 |

---

## 七、SQLite 特殊注意事项

### 7.1 外键约束启用

SQLite 默认不启用外键约束，必须在每次连接时执行：
```sql
PRAGMA foreign_keys = ON;
```
在 Spring Boot 中通过数据源初始化脚本确保：
```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data/habit.db?foreign_keys=ON&journal_mode=WAL&busy_timeout=5000
```

### 7.2 AUTOINCREMENT vs 隐式 ROWID

- 本表使用 `INTEGER PRIMARY KEY AUTOINCREMENT` 显式自增
- `AUTOINCREMENT` 确保不重用已删除的 ID，虽然性能略低，但避免 ID 混淆（适合审计场景）

### 7.3 日期时间处理

- 使用 `DATETIME` 类型存储，SQLite 以 TEXT 格式存储（`YYYY-MM-DD HH:MM:SS`）
- Spring Boot / MyBatis-Plus 自动处理 DATE/DATETIME 类型映射
- 查询时可使用 SQLite 内置函数：`date()`, `datetime()`, `strftime()`

### 7.4 布尔值处理

- SQLite 无原生 BOOLEAN 类型，使用 `TINYINT` (0/1)
- MyBatis-Plus 可使用 `@TableLogic` 处理逻辑删除标志

---

*数据库建模完成，建表脚本就绪，可进入 API 接口设计阶段。*
