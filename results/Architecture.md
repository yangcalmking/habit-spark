# 技术架构设计文档 — 学习习惯培养积分奖励系统

**Task ID**: task-20260402-083002  
**Assigned to**: @architect-lin:matrix-local.hiclaw.io:18080  
**Project**: 学习习惯培养积分奖励系统  
**Phase**: 2 - 技术架构与数据库设计  
**Date**: 2026-04-03  

---

## 一、系统整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                         用 户 层                              │
│  ┌──────────────────┐              ┌──────────────────┐      │
│  │   学生端浏览器     │              │   家长端浏览器     │      │
│  │  Vue 3 SPA       │              │  Vue 3 SPA       │      │
│  │  卡通风格 UI       │              │  简洁管理后台       │      │
│  └────────┬─────────┘              └────────┬─────────┘      │
│           │                                  │                │
└───────────┼──────────────────────────────────┼────────────────┘
            │ HTTPS / JSON                     │
            ▼                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                        应 用 层                              │
│                     Spring Boot 3                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                  Controller 层                        │   │
│  │  AuthController / TaskController / PointController   │   │
│  │  ProductController / ExchangeController / FileCtrl   │   │
│  │  StatsController / TaobaoController                  │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                  Service 层                           │   │
│  │  AuthService / TaskService / PointService            │   │
│  │  ProductService / ExchangeService / FileService      │   │
│  │  StatsService / PointRuleEngine (积分计算引擎)        │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                   DAO 层 (MyBatis-Plus)               │   │
│  │  UserMapper / FamilyMapper / TaskTemplateMapper      │   │
│  │  TaskRecordMapper / PointFlowMapper / ProductMapper  │   │
│  │  ExchangeMapper / NotificationMapper                 │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Interceptor / Filter 层                  │   │
│  │  JWTAuthInterceptor / GlobalExceptionHandler         │   │
│  │  RateLimitFilter / CorsFilter                        │   │
│  └──────────────────────┬───────────────────────────────┘   │
└─────────────────────────┼───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                         数 据 层                              │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │   SQLite 数据库    │  │  本地文件存储       │                 │
│  │  habit.db        │  │  ./uploads/       │                 │
│  │  (WAL 模式)       │  │  task_evidence/   │                 │
│  │                  │  │  product_images/  │                 │
│  └──────────────────┘  └──────────────────┘                 │
└─────────────────────────────────────────────────────────────┘
```

### 架构模式：B/S 架构（Browser/Server）

> **注**：PRD 原文标注为"CS 架构"，PM 评审 P0-1 已确认实际为 B/S 架构。Spring Boot + Vue 3 是典型的 Web 应用，通过浏览器访问，非传统 C/S 客户端。

### 分层职责

| 层级 | 职责 | 技术实现 |
|------|------|---------|
| 用户层 | 浏览器访问，学生/家长两套 UI | Vue 3 SPA，路由按角色分流 |
| 控制层 | 接收 HTTP 请求，参数校验，路由分发 | Spring MVC @RestController |
| 业务层 | 业务逻辑、积分计算引擎、事务管理 | Spring @Service + @Transactional |
| 数据层 | 数据持久化、查询构建 | MyBatis-Plus Mapper |
| 存储层 | 关系数据 + 文件存储 | SQLite 3 + 本地文件系统 |

---

## 二、技术选型详细说明

### 2.1 后端技术栈

| 技术 | 版本 | 选型理由 |
|------|------|---------|
| **Java** | 17+ | LTS 版本，Spring Boot 3 最低要求，生态成熟 |
| **Spring Boot** | 3.2+ | 约定优于配置，内嵌 Tomcat，快速启动，生态完善 |
| **MyBatis-Plus** | 3.5+ | 简化 CRUD，代码生成器，分页插件，适合 SQLite |
| **SQLite** | 3.45+ | 零配置数据库，单文件部署，家庭场景并发低（≤10），备份简单（直接拷贝 .db 文件） |
| **JJWT** | 0.12+ | JWT Token 签发/验证，轻量级，API 简洁 |
| **Ehcache 3** | 3.10+ | 本地缓存，缓存任务模板/积分规则等热点配置数据，减少 DB 查询 |
| **Hibernate Validator** | 8.0+ | Bean Validation 参数校验，JSR-380 标准 |
| **Spring Security Crypto** | 6.x | BCrypt 密码加密，无需完整 Spring Security（降低复杂度） |
| **SLF4J + Logback** | 内置 | 日志框架，Spring Boot 默认集成 |
| **HikariCP** | 内置 | 连接池，SQLite 建议 5-10 连接 |

### 2.2 前端技术栈

| 技术 | 版本 | 选型理由 |
|------|------|---------|
| **Vue 3** | 3.4+ | Composition API，响应式性能好，生态活跃 |
| **Vite** | 5.x | 极速 HMR，构建速度快，开发体验佳 |
| **Vue Router** | 4.x | 官方路由，支持路由守卫（角色权限控制） |
| **Pinia** | 2.x | 官方状态管理，轻量，TypeScript 友好 |
| **Element Plus** | 2.x | 家长端 UI 组件库，成熟稳定的管理后台组件 |
| **自定义卡通组件** | - | 学生端自行开发卡通风格组件，配色以粉色/紫色/蓝色为主 |
| **Axios** | 1.6+ | HTTP 客户端，拦截器统一处理 Token 和错误 |
| **SCSS** | - | CSS 预处理器，支持变量和嵌套，方便主题管理 |
| **driver.js** | 1.x | 新手指引，轻量级，支持高亮引导步骤 |
| **echarts** | 5.x | 家长端统计图表（热力图、折线图、饼图） |

### 2.3 开发工具

| 工具 | 用途 |
|------|------|
| Maven | 后端依赖管理和构建 |
| Node.js + npm | 前端依赖管理和构建 |
| Git | 版本控制 |
| IntelliJ IDEA / VS Code | IDE |

---

## 三、前后端项目结构

### 3.1 后端项目结构

```
habit-tracker-backend/
├── pom.xml
├── src/main/java/com/habit/
│   ├── HabitTrackerApplication.java          # 启动类
│   ├── config/                                # 配置类
│   │   ├── WebConfig.java                    # Web 配置（CORS、拦截器注册）
│   │   ├── SQLiteConfig.java                 # SQLite 数据源配置
│   │   ├── MyBatisPlusConfig.java            # MyBatis-Plus 配置（分页插件）
│   │   ├── JwtConfig.java                    # JWT 密钥和参数配置
│   │   └── CacheConfig.java                  # Ehcache 配置
│   ├── interceptor/                           # 拦截器
│   │   └── JwtAuthInterceptor.java           # JWT 认证 + 角色权限校验
│   ├── exception/                             # 异常处理
│   │   ├── GlobalExceptionHandler.java       # 全局异常统一响应
│   │   ├── BusinessException.java            # 业务异常（积分不足、任务重复等）
│   │   └── ErrorCode.java                    # 错误码枚举
│   ├── common/                                # 公共模块
│   │   ├── Result.java                       # 统一响应包装
│   │   ├── PageResult.java                   # 分页响应
│   │   └── Constants.java                    # 常量定义
│   ├── controller/                            # 控制层
│   │   ├── AuthController.java               # 认证（登录/注册/Token刷新）
│   │   ├── TaskController.java               # 任务（模板/打卡/审核）
│   │   ├── PointController.java              # 积分（查询/流水/调整）
│   │   ├── ProductController.java            # 商品（CRUD）
│   │   ├── ExchangeController.java           # 兑换申请/审批
│   │   ├── FileController.java               # 文件上传
│   │   ├── StatsController.java              # 统计报表
│   │   ├── NotificationController.java       # 系统内通知
│   │   └── TaobaoController.java             # 淘宝链接解析（P1）
│   ├── service/                               # 业务层
│   │   ├── AuthService.java
│   │   ├── TaskService.java
│   │   ├── TaskReviewService.java            # 审核业务（逐条+批量）
│   │   ├── PointService.java                 # 积分账户管理
│   │   ├── PointRuleEngine.java              # 积分计算引擎（核心）
│   │   ├── ProductService.java
│   │   ├── ExchangeService.java              # 兑换流程（冻结→确认→扣减）
│   │   ├── FileService.java                  # 文件上传处理
│   │   ├── StatsService.java
│   │   └── NotificationService.java
│   ├── dao/                                   # 数据访问层
│   │   ├── entity/                           # 实体类
│   │   │   ├── User.java
│   │   │   ├── FamilyGroup.java
│   │   │   ├── TaskTemplate.java
│   │   │   ├── TaskRecord.java
│   │   │   ├── PointAccount.java
│   │   │   ├── PointFlow.java
│   │   │   ├── Product.java
│   │   │   ├── ExchangeRequest.java
│   │   │   ├── Notification.java
│   │   │   └── OperationLog.java
│   │   └── mapper/                           # MyBatis-Plus Mapper 接口
│   │       ├── UserMapper.java
│   │       ├── FamilyGroupMapper.java
│   │       ├── TaskTemplateMapper.java
│   │       ├── TaskRecordMapper.java
│   │       ├── PointAccountMapper.java
│   │       ├── PointFlowMapper.java
│   │       ├── ProductMapper.java
│   │       ├── ExchangeRequestMapper.java
│   │       ├── NotificationMapper.java
│   │       └── OperationLogMapper.java
│   └── util/                                  # 工具类
│       ├── JwtUtil.java
│       ├── FileUtil.java
│       └── DateUtil.java
├── src/main/resources/
│   ├── application.yml                        # 应用配置
│   ├── application-prod.yml                   # 生产环境配置
│   ├── mapper/                                # MyBatis XML（复杂查询）
│   ├── db/
│   │   └── schema.sql                         # 建表脚本
│   │   └── data.sql                           # 初始数据
│   └── ehcache.xml                            # Ehcache 配置
└── src/test/java/com/habit/                   # 单元测试
```

### 3.2 前端项目结构

```
habit-tracker-frontend/
├── package.json
├── vite.config.js
├── index.html
├── public/
│   └── favicon.ico
├── src/
│   ├── main.js                                # 入口
│   ├── App.vue                                # 根组件
│   ├── api/                                   # API 请求封装
│   │   ├── request.js                         # Axios 实例 + 拦截器
│   │   ├── auth.js
│   │   ├── task.js
│   │   ├── point.js
│   │   ├── product.js
│   │   ├── exchange.js
│   │   ├── file.js
│   │   └── stats.js
│   ├── router/
│   │   └── index.js                           # 路由配置 + 角色守卫
│   ├── stores/                                # Pinia 状态管理
│   │   ├── user.js                            # 用户信息 + Token
│   │   ├── task.js                            # 任务状态
│   │   ├── point.js                           # 积分状态
│   │   └── notification.js                    # 通知状态
│   ├── views/                                 # 页面视图
│   │   ├── Login.vue                          # 登录页（学生/家长共用）
│   │   ├── student/                           # 学生端
│   │   │   ├── Home.vue                       # 首页（积分总览 + 今日任务）
│   │   │   ├── TaskList.vue                   # 任务列表
│   │   │   ├── TaskSubmit.vue                 # 打卡提交
│   │   │   ├── PointDetail.vue                # 积分明细
│   │   │   ├── ProductMall.vue                # 兑换商城
│   │   │   ├── ProductDetail.vue              # 礼品详情
│   │   │   ├── ExchangeHistory.vue            # 兑换记录
│   │   │   ├── Wishlist.vue                   # 心愿单
│   │   │   └── My.vue                         # 个人中心
│   │   └── parent/                            # 家长端
│   │       ├── Home.vue                       # 首页（数据概览）
│   │       ├── ReviewList.vue                 # 待审核列表
│   │       ├── TaskHistory.vue                # 打卡历史
│   │       ├── PointManager.vue               # 积分管理（调整）
│   │       ├── ProductManage.vue              # 商品管理
│   │       ├── ExchangeManage.vue             # 兑换审批
│   │       ├── Statistics.vue                 # 统计报表
│   │       └── Settings.vue                   # 系统设置
│   ├── components/                            # 公共组件
│   │   ├── common/
│   │   │   ├── NavBar.vue                     # 底部导航栏（学生端）
│   │   │   ├── SideMenu.vue                   # 侧边菜单（家长端）
│   │   │   ├── PointBadge.vue                 # 积分数字徽章
│   │   │   └── LoadingSpinner.vue
│   │   ├── student/                           # 学生端专属组件
│   │   │   ├── CuteCard.vue                   # 卡通卡片
│   │   │   ├── TaskCard.vue                   # 任务卡片
│   │   │   ├── PointAnimation.vue             # 积分飞入动画
│   │   │   ├── StarRating.vue                 # 星级评价
│   │   │   └── ProgressBar.vue                # 心愿单进度条
│   │   └── parent/                            # 家长端专属组件
│   │       ├── ReviewCard.vue                 # 审核卡片
│   │       ├── BatchActionBar.vue             # 批量操作栏
│   │       └── StatChart.vue                  # 统计图表封装
│   ├── styles/                                # 全局样式
│   │   ├── variables.scss                     # 主题变量（颜色/字体/尺寸）
│   │   ├── student-theme.scss                 # 学生端主题
│   │   ├── parent-theme.scss                  # 家长端主题
│   │   └── common.scss
│   ├── utils/                                 # 工具函数
│   │   ├── auth.js                            # Token 存储/读取
│   │   ├── format.js                          # 格式化（日期/积分/金额）
│   │   └── validate.js                        # 表单校验
│   └── assets/                                # 静态资源
│       ├── images/                            # 图标、装饰图
│       └── fonts/
├── .env.development                           # 开发环境变量
└── .env.production                            # 生产环境变量
```

---

## 四、关键技术决策

### 决策 1：SQLite 并发处理方案（写入锁问题）

**背景**：SQLite 采用文件级锁，写操作会阻塞其他写操作。

**方案对比**：

| 方案 | 优点 | 缺点 | 适用性 |
|------|------|------|--------|
| **WAL 模式**（选定） | 读写不互斥，1 写多读，性能好 | 需要额外 -wal/-shm 文件 | ✅ 家庭场景完美匹配 |
| 连接池串行化 | 实现简单 | 性能差，并发写入排队严重 | ❌ |
| 升级到 MySQL/PostgreSQL | 并发能力强 | 增加部署复杂度，不符合轻量目标 | ❌ 过度设计 |

**决策**：启用 SQLite WAL 模式（Write-Ahead Logging）

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:sqlite:./data/habit.db?journal_mode=WAL&busy_timeout=5000
    hikari:
      maximum-pool-size: 8        # SQLite 建议 5-10
      minimum-idle: 2
      connection-timeout: 10000   # 10 秒超时
```

**配套措施**：
- 设置 `busy_timeout=5000`（5 秒重试）
- 所有写操作加短事务，避免长事务持有写锁
- 批量审核使用事务包裹，一次性提交
- 积分计算引擎在后端执行，确保计算和写入在同一事务

### 决策 2：图片文件存储方案

**背景**：学生上传任务完成凭证图片，家长上传商品图片。

**方案对比**：

| 方案 | 优点 | 缺点 | 适用性 |
|------|------|------|--------|
| **本地文件存储**（选定） | 零成本，部署简单，直接备份 | 不适合大规模/分布式 | ✅ 家庭/单机场景 |
| 对象存储（OSS/S3） | 高可用，CDN 加速 | 额外费用，配置复杂 | ❌ 过度设计 |
| Base64 存数据库 | 简单，无需额外方案 | 数据库膨胀，性能差 | ❌ |

**决策**：本地文件系统存储

```
项目根目录/
├── data/habit.db                    # 数据库
├── uploads/                         # 上传根目录
│   ├── task_evidence/               # 任务凭证
│   │   ├── 2026/
│   │   │   └── 04/
│   │   │       └── {record_id}_{timestamp}.jpg
│   │   └── ...
│   └── product_images/              # 商品图片
│       └── {product_id}_{index}.jpg
└── backup/                          # 数据库备份
```

**约束**：
- 单张图片最大 5MB（前端校验 + 后端校验双重保障）
- 仅支持 JPG/PNG/WEBP 格式
- 文件名使用 `{record_id}_{timestamp}.ext` 避免冲突
- 上传后压缩处理（质量 80%，最大宽度 1920px）
- 数据库仅存储相对路径：`/uploads/task_evidence/2026/04/123_1712102400000.jpg`

### 决策 3：积分计算后移（防止前端恶意提交）

**背景**：PRD 原文中 `task_record.points` 由前端提交，存在伪造风险。

**方案对比**：

| 方案 | 安全性 | 灵活性 | 适用性 |
|------|--------|--------|--------|
| **后端根据模板自动计算**（选定） | 极高，前端无法篡改 | 规则集中管理 | ✅ PM 评审 P0-3 要求 |
| 前端计算 + 后端校验 | 中等 | 分散 | ❌ 校验逻辑重复 |
| 完全信任前端 | 无 | - | ❌ |

**决策**：积分计算引擎（PointRuleEngine）在后端统一处理

```java
// 流程示意
TaskSubmitRequest (前端仅提交)
  ├── taskTemplateId    // 选择哪个任务模板
  ├── description       // 文字描述
  ├── attachmentUrl     // 凭证图片（可选）
  └── taskDate          // 任务日期

↓ PointRuleEngine.calcPoints(templateId, submitData)

TaskRecord (后端创建)
  ├── points = 自动计算  // 从模板规则计算，不含前端传入
  ├── status = PENDING   // 待审核
  └── ...
```

**规则引擎设计**：
- 基础积分 + 额外奖励 + 每日上限，全部存储在 `task_template` 表
- 引擎从模板读取规则，计算理论最高积分
- 审核通过后，按模板规则写入积分流水

### 决策 4：礼品冻结积分机制

**背景**：兑换时积分如何扣减？PM 评审 P0-4 建议冻结机制。

**方案设计**：

```
积分账户模型：
  total_points = available_points + frozen_points

兑换流程：
  1. 学生提交兑换申请
     → 校验 available_points >= cost
     → available_points -= cost
     → frozen_points += cost
     → 创建 exchange_request (status=PENDING)

  2a. 家长确认发货
      → frozen_points -= cost
      → total_points -= cost
      → exchange_request.status = COMPLETED

  2b. 家长拒绝
      → frozen_points -= cost  
      → available_points += cost    // 释放回可用
      → exchange_request.status = CANCELLED

  2c. 即时奖励（≤15 分）
      → 系统自动确认
      → frozen_points -= cost
      → total_points -= cost
      → exchange_request.status = COMPLETED
```

### 决策 5：前端路由权限控制

**方案**：Vue Router 全局导航守卫 + 角色标识

```javascript
// 路由元信息
{
  path: '/student/home',
  component: () => import('@/views/student/Home.vue'),
  meta: { requiresAuth: true, role: 'student' }
}

// 导航守卫
router.beforeEach((to, from, next) => {
  const user = useUserStore()
  if (to.meta.requiresAuth && !user.token) {
    return next('/login')
  }
  if (to.meta.role && user.role !== to.meta.role) {
    return next(`/${user.role}/home`) // 角色不匹配，重定向
  }
  next()
})
```

### 决策 6：淘宝链接导入降级方案

**背景**：淘宝 API 需企业资质，PM 评审 P1-3 建议降级方案。

**决策**：
- MVP 阶段：**手动录入**是唯一必需功能
- 淘宝链接解析作为可选增强（P1），不影响 MVP 上线
- 实现方案：正则提取商品 ID → 通过第三方 API 或爬虫抓取（需额外配置）
- 失败时自动降级为手动填写表单

---

## 五、安全设计

### 5.1 认证机制

| 项目 | 方案 |
|------|------|
| 密码存储 | BCrypt 加密（加盐哈希，不可逆） |
| 认证方式 | JWT Token（请求头 `Authorization: Bearer <token>`） |
| Token 有效期 | Access Token 30 分钟 |
| Token 续期 | 登录时同时发放 Refresh Token（7 天有效期） |
| 登录保护 | 连续 5 次失败锁定账号 15 分钟 |
| 会话管理 | 30 分钟无操作自动过期 |

### 5.2 授权机制（RBAC）

| 角色 | 权限范围 |
|------|---------|
| `student` | 查看自己的任务/积分/商品，提交打卡，申请兑换 |
| `parent` | 审核打卡，管理商品，调整积分，查看统计，审批兑换 |
| `admin` | 所有家长权限 + 用户管理 + 系统配置 |

**接口级权限控制**：
- JWT Auth Interceptor 解析 Token 中的 role
- Controller 层通过 `@PreAuthorize` 或手动校验角色
- 数据级权限：学生只能访问 own 数据（WHERE student_id = ?），家长可访问同家庭组所有数据

### 5.3 数据安全

| 项目 | 方案 |
|------|------|
| 传输加密 | 生产环境强制 HTTPS（开发环境 HTTP） |
| 密码加密 | BCrypt（cost factor 12） |
| SQL 注入防护 | MyBatis-Plus 参数化查询 |
| XSS 防护 | 前端 Vue 自动转义 + CSP 头 |
| CSRF 防护 | SPA + JWT 方案天然不受 CSRF 影响 |
| 文件上传安全 | 文件类型白名单、大小限制、重命名存储、非可执行文件检查 |
| 操作审计 | 家长端的积分调整、审核操作均记录到 `operation_log` 表 |

### 5.4 请求限流

| 接口 | 限流策略 |
|------|---------|
| 登录接口 | 同一 IP 每分钟 ≤ 10 次 |
| 文件上传 | 同一用户每分钟 ≤ 5 次 |
| 其他接口 | 不限流（家庭场景低并发） |

---

## 六、部署拓扑图

```
┌─────────────────────────────────────────┐
│             部署环境（单机）               │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │         Nginx（可选）             │    │
│  │  :80 → :5173 (开发)              │    │
│  │  :80 → dist (生产)               │    │
│  └────────────┬────────────────────┘    │
│               │                          │
│  ┌────────────┴────────────────────┐    │
│  │     Spring Boot 应用              │    │
│  │     java -jar habit-tracker.jar   │    │
│  │     :8080                         │    │
│  │                                  │    │
│  │  ┌──────────┐  ┌──────────────┐  │    │
│  │  │ SQLite   │  │  uploads/    │  │    │
│  │  │ habit.db │  │  backup/     │  │    │
│  │  │ (WAL)    │  │              │  │    │
│  │  └──────────┘  └──────────────┘  │    │
│  └──────────────────────────────────┘    │
│                                         │
│  系统要求:                                │
│  - Java 17+                             │
│  - 512MB+ RAM                           │
│  - 200MB+ 磁盘                          │
│  - Windows/macOS/Linux                  │
└─────────────────────────────────────────┘

开发环境:
  前端: localhost:5173 (Vite Dev Server)
  后端: localhost:8080 (Spring Boot)
  跨域: Vite proxy / CORS 配置

生产环境:
  前端: Nginx 托管 dist 静态文件 :80
  后端: Systemd 管理 Spring Boot :8080
  Nginx: proxy_pass /api → localhost:8080
```

### 部署步骤概要

1. 后端：`mvn clean package` → `java -jar habit-tracker.jar`
2. 前端：`npm run build` → 部署 `dist` 到 Nginx 或直接由 Spring Boot 托管
3. 数据库：首次启动自动执行 `schema.sql` 和 `data.sql`
4. 初始账号：`admin/admin123`（管理员）、`xue/xue123`（学生）、`jia/jia123`（家长）

---

## 七、备份与恢复

### 7.1 数据库备份

| 方式 | 频率 | 实现 |
|------|------|------|
| 自动备份 | 每日凌晨 2:00 | Spring `@Scheduled` + SQLite Online Backup API |
| 手动备份 | 家长端触发 | 调用备份接口 |
| 备份文件 | `./backup/YYYYMMDD_HHmmss.db` | 保留最近 30 天 |

### 7.2 文件备份

- `uploads/` 目录与数据库同步备份
- 备份策略：tar 打包 `data/` + `uploads/` → `backup/habit-full-YYYYMMDD.tar.gz`

---

## 八、PM 评审修订落实清单

| PM 评审项 | 状态 | 落实方式 |
|-----------|------|---------|
| **P0-1** CS 架构 → B/S 架构 | ✅ 已修正 | 本文档统一使用 B/S 架构描述 |
| **P0-2** 缺少 user/family_group 表 | ✅ 已纳入 | 新增 `user` 统一用户表 + `family_group` 家庭组表 |
| **P0-3** 任务模板分离 + 积分后移 | ✅ 已纳入 | 新增 `task_template` 表，积分由 PointRuleEngine 后端计算 |
| **P0-4** 兑换流程 + 冻结积分 | ✅ 已纳入 | 新增 `exchange_request` 表，实现冻结积分机制 |
| **P0-5** 图片存储方案 | ✅ 已纳入 | 本地文件存储，5MB 限制，JPG/PNG/WEBP |
| **P1-1** 家务德育模块 | ✅ 已纳入 | 复用 task_template + task_record，通过 category 区分 |
| **P1-2** 统计可视化 | ✅ 已纳入 | 日历热力图、积分趋势折线图、任务完成率饼图 |
| **P1-3** 淘宝链接降级 | ✅ 已纳入 | 手动录入为 MVP 必需，淘宝解析为可选增强 |
| **P1-4** 消息通知 | ✅ 已纳入 | 新增 notification 表，系统内红点通知 |
| **P1-5** 种子数据 | ✅ 已纳入 | 提供完整 init SQL 脚本 |
| **P2-6** 数据模型补充字段 | ✅ 已纳入 | parent_comment、category、is_active 等字段 |

---

*文档完成，架构设计就绪，可进入数据库建模阶段。*
