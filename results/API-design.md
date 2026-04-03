# RESTful API 接口设计文档 — 学习习惯培养积分奖励系统

**Task ID**: task-20260402-083004  
**Assigned to**: @architect-lin:matrix-local.hiclaw.io:18080  
**Project**: 学习习惯培养积分奖励系统  
**Phase**: 2 - 技术架构与数据库设计  
**Date**: 2026-04-03  
**Dependencies**: task-20260402-083003（数据库建模 - 已完成）  

---

## 一、通用约定

### 1.1 基础 URL

| 环境 | 地址 |
|------|------|
| 开发 | `http://localhost:8080/api` |
| 生产 | `https://{domain}/api` |

### 1.2 统一响应格式

```json
// 成功响应
{
  "code": 200,
  "message": "success",
  "data": { /* 业务数据 */ }
}

// 分页响应
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [ /* 数据列表 */ ],
    "total": 100,
    "page": 1,
    "size": 10
  }
}

// 错误响应
{
  "code": 4001,
  "message": "积分不足，还差 20 分",
  "data": null
}
```

### 1.3 统一请求头

| Header | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `Authorization` | String | 是（登录接口除外） | `Bearer <jwt_token>` |
| `Content-Type` | String | 是（POST/PUT） | `application/json` |

### 1.4 权限标记说明

| 标记 | 含义 |
|------|------|
| 🔓 | 无需登录 |
| 🎓 | 仅学生角色可访问 |
| 👨‍👩‍👧 | 仅家长/管理员角色可访问 |
| 🔐 | 学生和家长均可（数据按角色隔离） |

### 1.5 全局错误码定义

| 错误码 | HTTP 状态 | 说明 |
|--------|----------|------|
| 200 | 200 | 成功 |
| 400 | 400 | 请求参数错误 |
| 1001 | 400 | 参数校验失败 |
| 2001 | 401 | 未登录或 Token 过期 |
| 2002 | 401 | Token 无效或已被篡改 |
| 2003 | 403 | 无权限访问 |
| 2004 | 403 | 账号已锁定 |
| 2005 | 403 | 账号已禁用 |
| 3001 | 404 | 资源不存在 |
| 4001 | 400 | 积分不足 |
| 4002 | 400 | 任务重复提交（今日已提交） |
| 4003 | 400 | 任务已达每日上限 |
| 4004 | 400 | 图片大小超过 5MB |
| 4005 | 400 | 不支持的图片格式 |
| 4006 | 400 | 商品库存不足 |
| 4007 | 400 | 兑换申请状态不允许此操作 |
| 5000 | 500 | 服务器内部错误 |

---

## 二、认证模块

### 2.1 用户登录

```
POST /auth/login
```
权限：🔓

**请求体**：
```json
{
  "username": "string (必填, 最大30字符)",
  "password": "string (必填)"
}
```

**成功响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 1800,
    "user": {
      "id": 2,
      "username": "xue",
      "nickname": "小雨",
      "role": "student",
      "avatarUrl": "/uploads/avatars/2.jpg"
    }
  }
}
```

**错误示例**：
```json
{
  "code": 2004,
  "message": "账号已锁定，请在 2026-04-03 10:30 后重试",
  "data": null
}
```

### 2.2 刷新 Token

```
POST /auth/refresh
```
权限：🔓

**请求体**：
```json
{
  "refreshToken": "string (必填)"
}
```

**成功响应**：同登录响应中的 accessToken + refreshToken

### 2.3 修改密码

```
PUT /auth/password
```
权限：🔐

**请求体**：
```json
{
  "oldPassword": "string (必填)",
  "newPassword": "string (必填, 6-20字符)"
}
```

**成功响应**：
```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

### 2.4 获取当前用户信息

```
GET /auth/me
```
权限：🔐

**成功响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "username": "xue",
    "nickname": "小雨",
    "role": "student",
    "familyGroupId": 1,
    "avatarUrl": "/uploads/avatars/2.jpg",
    "createdAt": "2026-04-01T10:00:00"
  }
}
```

---

## 三、学生端接口 — 任务模块

### 3.1 获取今日任务列表

```
GET /tasks/today
```
权限：🎓

**响应**：
```json
{
  "code": 200,
  "data": {
    "date": "2026-04-03",
    "tasks": [
      {
        "templateId": 1,
        "name": "课前预习",
        "category": "study",
        "basePoints": 1,
        "extraPoints": 1,
        "dailyCap": 4,
        "description": "语文：标出生字词...",
        "status": 0,
        "submittedAt": null,
        "pointsEarned": null
      },
      {
        "templateId": 2,
        "name": "课后作业",
        "category": "homework",
        "basePoints": 2,
        "extraPoints": 1,
        "dailyCap": 3,
        "description": "按时完成当天作业...",
        "status": 1,
        "submittedAt": "2026-04-03T09:30:00",
        "pointsEarned": 3
      }
    ]
  }
}
```

### 3.2 提交任务打卡

```
POST /tasks/submit
```
权限：🎓

**请求体**：
```json
{
  "taskTemplateId": "int (必填)",
  "taskDate": "date (必填, 格式: YYYY-MM-DD, ≤当天)",
  "description": "string (可选, 最大500字符)",
  "attachmentUrl": "string (可选, 图片URL, 最多5张逗号分隔)"
}
```

**成功响应**：
```json
{
  "code": 200,
  "message": "任务提交成功，等待家长审核",
  "data": {
    "recordId": 42,
    "status": 0,
    "estimatedPoints": 4
  }
}
```

**错误示例 — 重复提交**：
```json
{
  "code": 4002,
  "message": "今日已提交该任务",
  "data": null
}
```

> **关键设计说明**：`points` 字段不由前端传入！前端仅提交 `taskTemplateId` 和凭证，后端根据 `task_template` 规则自动计算积分。

### 3.3 获取历史打卡记录

```
GET /tasks/records
```
权限：🎓

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | date | 否 | 开始日期 |
| endDate | date | 否 | 结束日期（默认今天） |
| status | int | 否 | 0=待审核, 1=已通过, 2=已拒绝 |
| page | int | 否 | 页码（默认1） |
| size | int | 否 | 每页数量（默认20） |

**响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 42,
        "taskTemplateId": 1,
        "taskName": "课前预习",
        "taskDate": "2026-04-03",
        "description": "完成了语文和数学预习",
        "attachmentUrl": "/uploads/task_evidence/2026/04/42_1712127600000.jpg",
        "points": 4,
        "status": 0,
        "parentComment": null,
        "createdAt": "2026-04-03T09:30:00"
      }
    ],
    "total": 25,
    "page": 1,
    "size": 20
  }
}
```

### 3.4 获取任务模板列表

```
GET /tasks/templates
```
权限：🎓

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category | string | 否 | 分类筛选 |

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "课前预习",
      "category": "study",
      "basePoints": 1,
      "extraPoints": 1,
      "extraPointsRule": "{\"subject_count\":3,\"bonus\":1}",
      "dailyCap": 4,
      "description": "语文：标出生字词...",
      "allowCustom": false,
      "isActive": true
    }
  ]
}
```

---

## 四、学生端接口 — 积分模块

### 4.1 获取积分总览

```
GET /points/overview
```
权限：🎓

**响应**：
```json
{
  "code": 200,
  "data": {
    "totalPoints": 256,
    "availablePoints": 206,
    "frozenPoints": 50,
    "todayEarned": 8,
    "thisWeekEarned": 42,
    "thisMonthEarned": 156
  }
}
```

### 4.2 获取积分流水

```
GET /points/flows
```
权限：🎓

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | date | 否 | 开始日期 |
| endDate | date | 否 | 结束日期 |
| flowType | int | 否 | 流水类型筛选 |
| page | int | 否 | 页码（默认1） |
| size | int | 否 | 每页数量（默认20） |

**响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 101,
        "amount": 4,
        "flowType": 1,
        "flowTypeText": "任务获得",
        "reason": "课前预习",
        "relatedType": "task",
        "relatedId": 42,
        "createdAt": "2026-04-03T10:00:00"
      },
      {
        "id": 100,
        "amount": -50,
        "flowType": 5,
        "flowTypeText": "兑换冻结",
        "reason": "兑换: K歌房1小时体验",
        "relatedType": "exchange",
        "relatedId": 5,
        "createdAt": "2026-04-02T15:00:00"
      }
    ],
    "total": 101,
    "page": 1,
    "size": 20
  }
}
```

---

## 五、学生端接口 — 商品兑换模块

### 5.1 获取商品列表

```
GET /products
```
权限：🔐

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category | string | 否 | 兴趣分类 |
| level | string | 否 | 等级（instant/mid/long） |
| minPoints | int | 否 | 最低积分 |
| maxPoints | int | 否 | 最高积分 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

**响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "name": "家庭 K 歌时间 15 分钟",
        "category": "singing",
        "categoryText": "唱歌",
        "pointsCost": 5,
        "stock": -1,
        "stockText": "不限",
        "description": "获得 15 分钟家庭 K 歌时间",
        "imageUrls": ["/uploads/product_images/1_1.jpg"],
        "level": "instant",
        "levelText": "即时奖励",
        "canAfford": true,
        "isActive": true
      }
    ],
    "total": 12,
    "page": 1,
    "size": 10
  }
}
```

### 5.2 获取商品详情

```
GET /products/{id}
```
权限：🔐

**成功响应**：
```json
{
  "code": 200,
  "data": {
    "id": 2,
    "name": "K 歌房 1 小时体验",
    "category": "singing",
    "categoryText": "唱歌",
    "pointsCost": 60,
    "stock": 3,
    "description": "KTV 包房 1 小时，可带 1 位朋友",
    "imageUrls": ["/uploads/product_images/2_1.jpg", "/uploads/product_images/2_2.jpg"],
    "sourceUrl": null,
    "level": "mid",
    "levelText": "中期奖励",
    "canAfford": true,
    "shortage": 0
  }
}
```

### 5.3 提交兑换申请

```
POST /exchange
```
权限：🎓

**请求体**：
```json
{
  "productId": "int (必填)",
  "quantity": "int (可选, 默认1, ≥1)"
}
```

**成功响应**：
```json
{
  "code": 200,
  "message": "兑换申请已提交，等待家长确认",
  "data": {
    "exchangeId": 5,
    "status": "pending",
    "pointsCost": 60,
    "autoConfirm": false,
    "remainingPoints": 146
  }
}
```

> **即时奖励**：如果商品 level=instant 且积分 ≤15，系统自动确认，响应中 `"status": "confirmed"` 且 `"autoConfirm": true`。

### 5.4 获取兑换记录

```
GET /exchange/records
```
权限：🎓

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | 状态筛选 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

**响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 5,
        "productName": "K 歌房 1 小时体验",
        "productImage": "/uploads/product_images/2_1.jpg",
        "pointsCost": 60,
        "status": "pending",
        "statusText": "待确认",
        "autoConfirm": false,
        "remark": null,
        "createdAt": "2026-04-03T15:00:00"
      }
    ],
    "total": 3,
    "page": 1,
    "size": 10
  }
}
```

---

## 六、家长端接口 — 任务审核

### 6.1 获取待审核任务列表

```
GET /parent/tasks/pending
```
权限：👨‍👩‍👧

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | int | 否 | 筛选某学生（多学生家庭） |
| startDate | date | 否 | 开始日期 |
| endDate | date | 否 | 结束日期 |

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 42,
      "studentId": 2,
      "studentName": "小雨",
      "taskName": "课前预习",
      "taskDate": "2026-04-03",
      "description": "完成了语文和数学预习",
      "attachmentUrl": "/uploads/task_evidence/2026/04/42_1712127600000.jpg",
      "estimatedPoints": 4,
      "submittedAt": "2026-04-03T09:30:00"
    }
  ]
}
```

### 6.2 单条审核

```
PUT /parent/tasks/{recordId}/review
```
权限：👨‍👩‍👧

**请求体**：
```json
{
  "approved": "boolean (必填, true=通过, false=拒绝)",
  "parentComment": "string (可选, 拒绝时建议填写, 最大500字符)"
}
```

**成功响应**：
```json
{
  "code": 200,
  "message": "审核通过，已发放 4 积分",
  "data": {
    "recordId": 42,
    "points": 4,
    "newStudentPoints": 260
  }
}
```

### 6.3 批量审核

```
POST /parent/tasks/batch-review
```
权限：👨‍👩‍👧

**请求体**：
```json
{
  "recordIds": "int[] (必填, 任务记录ID列表, 至少1个)",
  "approved": "boolean (必填, 全部通过/全部拒绝)",
  "parentComment": "string (可选)"
}
```

**成功响应**：
```json
{
  "code": 200,
  "message": "批量审核完成",
  "data": {
    "successCount": 5,
    "failCount": 0,
    "totalPoints": 18,
    "records": [
      {"recordId": 42, "points": 4, "status": "ok"},
      {"recordId": 43, "points": 3, "status": "ok"},
      {"recordId": 44, "points": 5, "status": "ok"},
      {"recordId": 45, "points": 2, "status": "ok"},
      {"recordId": 46, "points": 4, "status": "ok"}
    ]
  }
}
```

### 6.4 获取任务审核历史

```
GET /parent/tasks/history
```
权限：👨‍👩‍👧

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | int | 否 | 筛选某学生 |
| status | int | 否 | 0=待审, 1=通过, 2=拒绝 |
| startDate | date | 否 | 开始日期 |
| endDate | date | 否 | 结束日期 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

---

## 七、家长端接口 — 积分管理

### 7.1 获取学生积分（家长视角）

```
GET /parent/points/{studentId}
```
权限：👨‍👩‍👧

**响应**：与学生端积分总览相同结构，额外包含流水最近 10 条。

### 7.2 手动调整积分

```
POST /parent/points/adjust
```
权限：👨‍👩‍👧

**请求体**：
```json
{
  "studentId": "int (必填)",
  "amount": "int (必填, 正数=加分, 负数=扣分)",
  "reason": "string (必填, 最大255字符)"
}
```

**成功响应**：
```json
{
  "code": 200,
  "message": "积分调整成功",
  "data": {
    "flowId": 102,
    "amount": -5,
    "previousPoints": 260,
    "currentPoints": 255
  }
}
```

**错误示例 — 扣到负数**：
```json
{
  "code": 4001,
  "message": "积分不足，当前可用积分 3 分，无法扣除 5 分",
  "data": null
}
```

---

## 八、家长端接口 — 商品管理

### 8.1 新增商品

```
POST /parent/products
```
权限：👨‍👩‍👧

**请求体**：
```json
{
  "name": "string (必填, 最大100字符)",
  "category": "string (必填, singing/craft/comic/playground/other)",
  "pointsCost": "int (必填, ≥1)",
  "stock": "int (必填, -1=不限, ≥0)",
  "description": "string (可选)",
  "level": "string (必填, instant/mid/long)",
  "imageUrlList": "string[] (可选, 图片URL数组)"
}
```

**成功响应**：
```json
{
  "code": 200,
  "message": "商品上架成功",
  "data": {
    "id": 13,
    "name": "家庭 K 歌时间 15 分钟"
  }
}
```

### 8.2 修改商品

```
PUT /parent/products/{id}
```
权限：👨‍👩‍👧

**请求体**：字段同新增，所有字段可选（仅传入需修改的字段）

### 8.3 上下架商品

```
PATCH /parent/products/{id}/toggle
```
权限：👨‍👩‍👧

**请求体**：
```json
{
  "is_active": "boolean (必填, true=上架, false=下架)"
}
```

### 8.4 获取商品管理列表

```
GET /parent/products
```
权限：👨‍👩‍👧

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category | string | 否 | 分类筛选 |
| isActive | boolean | 否 | 上架状态 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

**响应**：返回所有商品（含已下架），比学生端多出 `stock`（实际数字）和 `is_active`。

---

## 九、家长端接口 — 兑换审批

### 9.1 获取待审批兑换列表

```
GET /parent/exchange/pending
```
权限：👨‍👩‍👧

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 5,
      "studentName": "小雨",
      "productName": "K 歌房 1 小时体验",
      "productImage": "/uploads/product_images/2_1.jpg",
      "pointsCost": 60,
      "autoConfirm": false,
      "createdAt": "2026-04-03T15:00:00"
    }
  ]
}
```

### 9.2 审批兑换

```
PUT /parent/exchange/{exchangeId}/approve
```
权限：👨‍👩‍👧

**请求体**：
```json
{
  "action": "string (必填, confirm=确认发货, reject=拒绝)",
  "remark": "string (可选, 拒绝时建议填写)"
}
```

**成功响应（确认）**：
```json
{
  "code": 200,
  "message": "兑换已确认，积分已扣除",
  "data": {
    "exchangeId": 5,
    "status": "confirmed",
    "studentRemainingPoints": 146
  }
}
```

### 9.3 获取兑换审批历史

```
GET /parent/exchange/history
```
权限：👨‍👩‍👧

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | int | 否 | 筛选某学生 |
| status | string | 否 | 状态筛选 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

---

## 十、家长端接口 — 统计报表

### 10.1 打卡热力图数据

```
GET /parent/stats/checkin-heatmap
```
权限：👨‍👩‍👧

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | int | 否 | 学生 ID |
| year | int | 否 | 年份（默认当年） |
| month | int | 否 | 月份（1-12，默认当月） |

**响应**：
```json
{
  "code": 200,
  "data": {
    "year": 2026,
    "month": 4,
    "days": [
      { "date": "2026-04-01", "completed": 5, "total": 7, "rate": 0.71 },
      { "date": "2026-04-02", "completed": 6, "total": 7, "rate": 0.86 },
      { "date": "2026-04-03", "completed": 2, "total": 7, "rate": 0.29 }
    ],
    "overallRate": 0.62
  }
}
```

### 10.2 积分趋势数据

```
GET /parent/stats/point-trend
```
权限：👨‍👩‍👧

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | int | 否 | 学生 ID |
| startDate | date | 否 | 开始日期 |
| endDate | date | 否 | 结束日期（默认当天） |
| granularity | string | 否 | day/week/month（默认 day） |

**响应**：
```json
{
  "code": 200,
  "data": {
    "points": [
      { "date": "2026-04-01", "earned": 12, "spent": 0, "balance": 218 },
      { "date": "2026-04-02", "earned": 8, "spent": 0, "balance": 226 },
      { "date": "2026-04-03", "earned": 4, "spent": 0, "balance": 230 }
    ],
    "summary": {
      "totalEarned": 24,
      "totalSpent": 0,
      "averageDaily": 8.0
    }
  }
}
```

### 10.3 任务完成率分布

```
GET /parent/stats/completion-rate
```
权限：👨‍👩‍👧

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | int | 否 | 学生 ID |
| period | string | 否 | week/month（默认 week） |

**响应**：
```json
{
  "code": 200,
  "data": {
    "categories": [
      { "name": "基础学习习惯", "completed": 18, "total": 21, "rate": 0.86 },
      { "name": "作业相关", "completed": 5, "total": 5, "rate": 1.0 },
      { "name": "自主阅读", "completed": 6, "total": 7, "rate": 0.86 },
      { "name": "兴趣特长", "completed": 3, "total": 7, "rate": 0.43 }
    ]
  }
}
```

### 10.4 导出报表

```
GET /parent/stats/export
```
权限：👨‍👩‍👧

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | int | 否 | 学生 ID |
| startDate | date | 是 | 开始日期 |
| endDate | date | 是 | 结束日期 |
| format | string | 否 | csv/xlsx（默认 csv） |

**响应**：直接返回文件下载
```
Content-Type: text/csv
Content-Disposition: attachment; filename=habit-report-20260401-20260403.csv
```

---

## 十一、文件上传接口

### 11.1 上传图片

```
POST /files/upload
```
权限：🔐

**请求**：`multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 图片文件 |
| type | String | 是 | `task_evidence`（任务凭证）或 `product`（商品图片） |
| recordId | int | 否 | 关联的任务记录 ID（type=task_evidence 时可选，先上传后提交打卡时使用） |

**约束**：
- 文件大小：≤ 5MB
- 格式：JPG / PNG / WEBP
- MIME 校验 + 文件头校验

**成功响应**：
```json
{
  "code": 200,
  "data": {
    "url": "/uploads/task_evidence/2026/04/42_1712127600000.jpg",
    "size": 2048000,
    "mimeType": "image/jpeg"
  }
}
```

**错误示例**：
```json
{
  "code": 4004,
  "message": "图片大小不能超过 5MB，当前文件 8.2MB",
  "data": null
}
```

---

## 十二、淘宝链接导入接口（P1，可选增强）

### 12.1 解析淘宝链接

```
POST /taobao/parse
```
权限：👨‍👩‍👧

**请求体**：
```json
{
  "url": "string (必填, 淘宝商品URL或短链接)"
}
```

**成功响应**：
```json
{
  "code": 200,
  "data": {
    "title": "2026新款夏季纯棉T恤女宽松显瘦百搭短袖上衣",
    "price": "49.00",
    "imageUrl": "https://img.alicdn.com/imgextra/i1/123456789/XXX.jpg",
    "imageList": [
      "https://img.alicdn.com/imgextra/i1/123456789/XXX1.jpg",
      "https://img.alicdn.com/imgextra/i1/123456789/XXX2.jpg"
    ],
    "itemId": "689123456789",
    "sourceUrl": "https://item.taobao.com/item.htm?id=689123456789"
  }
}
```

**失败响应（降级为手动）**：
```json
{
  "code": 5000,
  "message": "淘宝链接解析失败，请手动填写商品信息",
  "data": null
}
```

> **说明**：此功能需要额外配置淘宝 API 或第三方代理服务。解析失败时前端应提供手动填写表单降级。

---

## 十三、通知接口

### 13.1 获取通知列表

```
GET /notifications
```
权限：🔐

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| isRead | boolean | 否 | false=仅未读 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

**响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "title": "新的打卡待审核",
        "content": "小雨提交了「课前预习」的打卡，请及时审核",
        "type": "task_submitted",
        "relatedId": 42,
        "relatedType": "task_record",
        "isRead": false,
        "createdAt": "2026-04-03T09:30:00"
      }
    ],
    "total": 5,
    "page": 1,
    "size": 10
  }
}
```

### 13.2 获取未读消息数

```
GET /notifications/unread-count
```
权限：🔐

**响应**：
```json
{
  "code": 200,
  "data": {
    "count": 3
  }
}
```

### 13.3 标记为已读

```
PUT /notifications/{id}/read
```
权限：🔐

**请求体**：无

**成功响应**：
```json
{
  "code": 200,
  "data": null
}
```

### 13.4 全部标记为已读

```
PUT /notifications/read-all
```
权限：🔐

---

## 十四、操作审计日志接口

### 14.1 获取操作日志

```
GET /parent/logs
```
权限：👨‍👩‍👧

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| action | string | 否 | 操作类型筛选 |
| startDate | date | 否 | 开始日期 |
| endDate | date | 否 | 结束日期 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

---

## 十五、接口汇总清单

| 模块 | 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|------|
| **认证** |
| | POST | /api/auth/login | 🔓 | 登录 |
| | POST | /api/auth/refresh | 🔓 | 刷新 Token |
| | PUT | /api/auth/password | 🔐 | 修改密码 |
| | GET | /api/auth/me | 🔐 | 当前用户信息 |
| **学生-任务** |
| | GET | /api/tasks/today | 🎓 | 今日任务列表 |
| | POST | /api/tasks/submit | 🎓 | 提交打卡 |
| | GET | /api/tasks/records | 🎓 | 历史打卡 |
| | GET | /api/tasks/templates | 🎓 | 任务模板列表 |
| **学生-积分** |
| | GET | /api/points/overview | 🎓 | 积分总览 |
| | GET | /api/points/flows | 🎓 | 积分流水 |
| **学生-兑换** |
| | GET | /api/products | 🔐 | 商品列表 |
| | GET | /api/products/{id} | 🔐 | 商品详情 |
| | POST | /api/exchange | 🎓 | 提交兑换 |
| | GET | /api/exchange/records | 🎓 | 兑换记录 |
| **家长-审核** |
| | GET | /api/parent/tasks/pending | 👨‍👩‍👧 | 待审核列表 |
| | PUT | /api/parent/tasks/{id}/review | 👨‍👩‍👧 | 单条审核 |
| | POST | /api/parent/tasks/batch-review | 👨‍👩‍👧 | 批量审核 |
| | GET | /api/parent/tasks/history | 👨‍👩‍👧 | 审核历史 |
| **家长-积分** |
| | GET | /api/parent/points/{studentId} | 👨‍👩‍👧 | 学生积分详情 |
| | POST | /api/parent/points/adjust | 👨‍👩‍👧 | 手动调整积分 |
| **家长-商品** |
| | POST | /api/parent/products | 👨‍👩‍👧 | 新增商品 |
| | PUT | /api/parent/products/{id} | 👨‍👩‍👧 | 修改商品 |
| | PATCH | /api/parent/products/{id}/toggle | 👨‍👩‍👧 | 上下架 |
| | GET | /api/parent/products | 👨‍👩‍👧 | 商品管理列表 |
| **家长-兑换** |
| | GET | /api/parent/exchange/pending | 👨‍👩‍👧 | 待审批列表 |
| | PUT | /api/parent/exchange/{id}/approve | 👨‍👩‍👧 | 审批兑换 |
| | GET | /api/parent/exchange/history | 👨‍👩‍👧 | 兑换审批历史 |
| **家长-统计** |
| | GET | /api/parent/stats/checkin-heatmap | 👨‍👩‍👧 | 打卡热力图 |
| | GET | /api/parent/stats/point-trend | 👨‍👩‍👧 | 积分趋势 |
| | GET | /api/parent/stats/completion-rate | 👨‍👩‍👧 | 任务完成率 |
| | GET | /api/parent/stats/export | 👨‍👩‍👧 | 导出报表 |
| **文件** |
| | POST | /api/files/upload | 🔐 | 上传图片 |
| **淘宝** |
| | POST | /api/taobao/parse | 👨‍👩‍👧 | 解析淘宝链接 |
| **通知** |
| | GET | /api/notifications | 🔐 | 通知列表 |
| | GET | /api/notifications/unread-count | 🔐 | 未读数 |
| | PUT | /api/notifications/{id}/read | 🔐 | 标记已读 |
| | PUT | /api/notifications/read-all | 🔐 | 全部已读 |
| **审计日志** |
| | GET | /api/parent/logs | 👨‍👩‍👧 | 操作日志 |

**总计接口数：32 个**

---

## 十六、CORS 配置

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")  // 前端开发服务器
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## 十七、API 安全要点

| 安全措施 | 实现方式 |
|---------|---------|
| Token 认证 | JWT 拦截器，所有接口（除登录/刷新外）需 Bearer Token |
| 角色鉴权 | 拦截器校验 role 字段，数据按 student_id 隔离 |
| 积分安全 | `points` 仅后端计算，前端不可传入 |
| 防止超额兑换 | 兑换前校验 available_points >= cost（并发安全：事务 + 乐观锁） |
| SQL 注入 | MyBatis-Plus 参数化查询 |
| XSS | Vue 模板自动转义 + CSP 响应头 |
| 文件安全 | 类型白名单 + 大小限制 + 重命名 + 非可执行检查 |
| 暴力破解 | 登录失败 5 次锁定 15 分钟 |

---

*RESTful API 接口设计完成。32 个接口覆盖认证、任务、积分、兑换、商品、统计、文件上传、通知全部模块。可作为前后端并行开发的契约文档。*
