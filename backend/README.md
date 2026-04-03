# 🌟 HabitSpark Backend

> 后端服务 — Spring Boot 3 + MyBatis-Plus + SQLite

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.x |
| 语言 | Java | 17+ |
| 数据库 | SQLite | 3.45+ |
| ORM | MyBatis-Plus | 3.x |
| 认证 | Spring Security + JWT | - |
| 缓存 | Ehcache | - |
| 文件上传 | Spring MultipartFile | - |
| 日志 | SLF4J + Logback | - |

## 项目结构（规划中）

```
backend/
├── pom.xml
├── src/main/java/com/habitspark/
│   ├── HabitSparkApplication.java    # 启动类
│   ├── config/                       # 配置类
│   ├── controller/                   # REST 控制器
│   │   ├── AuthController.java       # 认证接口
│   │   ├── TaskController.java       # 任务管理
│   │   ├── StudentController.java    # 学生端接口
│   │   ├── ParentController.java     # 家长端接口
│   │   └── ProductController.java    # 商品管理
│   ├── service/                      # 业务逻辑
│   │   ├── AuthService.java
│   │   ├── TaskService.java
│   │   ├── PointService.java
│   │   └── ProductService.java
│   ├── dao/                          # 数据访问
│   │   └── mapper/                   # MyBatis Mapper
│   ├── entity/                       # 实体类
│   ├── dto/                          # 传输对象
│   └── interceptor/                  # 拦截器
├── src/main/resources/
│   ├── application.yml               # 配置文件
│   └── mapper/                       # MyBatis XML
└── data/
    └── habitspark.db                 # SQLite 数据库
```

## 快速开始

```bash
# 编译
./mvnw clean package -DskipTests
# 运行
java -jar target/habitspark.jar # 或
./mvnw spring-boot:run
```

## API 文档
- 接口设计文档: [参见 docs/](../docs/)

## 开发规范
- 遵循 RESTful API 设计规范
- 使用统一响应格式: `{ "code": 200, "data": {...}, "message": "..." }` 
- 所有积分操作必须通过后端计算，不接受前端传入积分值
- 图片上传限制: 单张 < 5MB, JPG/PNG
