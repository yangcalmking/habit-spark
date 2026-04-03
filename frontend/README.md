# 🌟 HabitSpark Frontend

> 前端界面 — Vue 3 + Vite + Pinia

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 (Composition API) | 3.x |
| 构建 | Vite | 5.x |
| 状态管理 | Pinia | - |
| 路由 | Vue Router | 4.x |
| UI 组件(管理端) | Element Plus | 2.x |
| UI 组件(学生端) | 自定义卡通组件 | 自研 |
| CSS | SCSS | - |
| HTTP 客户端 | Axios | - |

## 项目结构（规划中）

```
frontend/
├── package.json
├── vite.config.js
├── index.html
├── src/
│   ├── main.js                       # 入口
│   ├── App.vue                       # 根组件
│   ├── router/                       # 路由配置
│   │   ├── index.js
│   │   ├── student.js                # 学生端路由
│   │   └── parent.js                 # 家长端路由
│   ├── stores/                       # Pinia 状态
│   ├── views/
│   │   ├── student/                  # 学生端页面
│   │   │   ├── Home.vue              # 首页(今日任务)
│   │   │   ├── TaskCheckin.vue       # 任务打卡
│   │   │   ├── Points.vue            # 积分查询
│   │   │   └── Exchange.vue          # 礼品兑换
│   │   └── parent/                   # 家长端页面
│   │       ├── Dashboard.vue         # 数据看板
│   │       ├── Review.vue            # 审核管理
│   │       ├── PointAdjust.vue       # 积分调整
│   │       └── ProductManage.vue     # 商品管理
│   ├── components/                   # 公共组件
│   └── assets/                       # 资源(图片等)
```

## 快速开始

```bash
# 安装依赖
npm install
# 开发模式
npm run dev
# 构建
npm run build
```

## UI 设计规范

### 学生端（卡通风格）
- 主色调: 粉色(#FF69B4)、紫色(#9B59B6)、蓝色(#87CEEB)
- 字体: 大字体、>= 16px
- 圆角: 12-20px
- 动效: 弹跳、缩放等可爱动画

### 家长端（简洁管理）
- 主色调: 清新简洁
- 组件: Element Plus
- 功能分区清晰，操作路径短
BACKENDREADME

echo "✅ frontend/README.md"