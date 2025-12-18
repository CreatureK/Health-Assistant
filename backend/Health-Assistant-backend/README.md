# 健康助手后端服务

基于 Spring Boot + MyBatis + JWT 的健康助手后端项目。

## 技术栈

- **框架**: Spring Boot 2.7.18
- **ORM**: MyBatis 3.5.x
- **数据库**: MySQL 8.0
- **认证**: JWT (jjwt 0.11.5)
- **API文档**: Swagger/OpenAPI 3.0 (springdoc-openapi-ui 1.7.0)
- **构建工具**: Maven
- **Java版本**: 17

## 项目结构

```
src/main/java/org/health/
├── HealthAssistantApplication.java    # 启动类
├── config/                            # 配置类
│   ├── GlobalExceptionHandler.java    # 全局异常处理
│   ├── MyBatisConfig.java             # MyBatis配置
│   ├── SwaggerConfig.java             # Swagger配置
│   └── WebConfig.java                 # Web配置（拦截器、跨域）
├── common/                            # 通用组件
│   ├── Result.java                    # 统一响应格式
│   ├── ResultCode.java                # 错误码枚举
│   ├── JwtUtil.java                   # JWT工具类
│   ├── UserContext.java               # 用户上下文
│   └── JsonTypeHandler.java           # JSON类型处理器
├── interceptor/                       # 拦截器
│   └── AuthInterceptor.java           # JWT认证拦截器
├── controller/                        # 控制器层
│   ├── auth/
│   │   └── AuthController.java       # 认证接口
│   ├── med/
│   │   ├── MedPlanController.java    # 用药计划接口
│   │   ├── MedTodayController.java   # 今日用药接口
│   │   ├── MedRecordController.java  # 用药记录接口
│   │   └── DrugController.java       # 药品库接口
│   └── wechat/
│       └── SubscribeController.java   # 订阅消息接口
├── service/                           # 服务层
│   ├── auth/
│   │   └── AuthService.java          # 认证服务
│   ├── med/
│   │   ├── MedPlanService.java       # 用药计划服务
│   │   ├── MedTodayService.java      # 今日用药服务
│   │   ├── MedRecordService.java     # 用药记录服务
│   │   └── DrugService.java          # 药品库服务
│   └── wechat/
│       └── SubscribeService.java      # 订阅消息服务
├── mapper/                            # 数据访问层
│   ├── UserMapper.java               # 用户Mapper
│   ├── med/
│   │   ├── MedPlanMapper.java        # 用药计划Mapper
│   │   ├── PlanTimesMapper.java      # 计划时间点Mapper
│   │   ├── PlanRepeatDaysMapper.java # 计划重复天数Mapper
│   │   ├── MedRecordMapper.java      # 用药记录Mapper
│   │   └── DrugCatalogMapper.java    # 药品库Mapper
│   └── wechat/
│       └── SubscribeGrantMapper.java  # 订阅授权Mapper
└── entity/                            # 实体类
    ├── User.java                     # 用户实体
    ├── med/
    │   ├── MedPlan.java              # 用药计划实体
    │   ├── PlanTimes.java            # 计划时间点实体
    │   ├── PlanRepeatDays.java       # 计划重复天数实体
    │   ├── MedRecord.java            # 用药记录实体
    │   └── DrugCatalog.java         # 药品库实体
    └── wechat/
        └── SubscribeGrant.java        # 订阅授权实体
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE health_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行SQL脚本：
```bash
mysql -u root -p health_assistant < health.sql
```

3. 修改数据库连接配置（`src/main/resources/application-dev.yml`）：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/health_assistant?...
    username: your_username
    password: your_password
```

### 3. 运行项目

```bash
# 使用Maven运行
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/Health-Assistant-backend-1.0-SNAPSHOT.jar
```

### 4. 访问接口

- 基础路径: `http://localhost:8080/api/v1`
- **认证接口**:
  - `GET /auth/captcha` - 获取验证码
  - `POST /auth/login` - 登录
  - `POST /auth/register` - 注册
- **用药计划接口**:
  - `GET /med/plans` - 获取计划列表
  - `POST /med/plans` - 创建计划
  - `GET /med/plans/:id` - 获取计划详情
  - `PUT /med/plans/:id` - 更新计划
  - `DELETE /med/plans/:id` - 删除计划
  - `POST /med/plans/:id/remind` - 更新提醒开关
- **今日用药接口**:
  - `GET /med/today` - 获取当天点位
  - `POST /med/today/ensure` - 确保生成点位
- **用药记录接口**:
  - `GET /med/records` - 查询记录列表
  - `POST /med/records/:recordId/mark` - 标记已服用/未服
  - `POST /med/records/:recordId/adjust` - 补记/更正
- **药品库接口**:
  - `GET /med/drugs` - 药品搜索列表
  - `GET /med/drugs/:id` - 药品详情
- **订阅消息接口**:
  - `GET /wechat/subscribe/config` - 获取订阅模板配置
  - `POST /wechat/subscribe/report` - 上报授权结果

## Swagger API文档

项目已集成 Swagger/OpenAPI 3.0，可以通过以下地址访问API文档：

### Swagger UI界面
- **访问地址**: `http://localhost:8080/api/v1/swagger-ui.html`
- **功能**: 可视化API文档，支持在线测试接口

### OpenAPI JSON文档
- **访问地址**: `http://localhost:8080/api/v1/v3/api-docs`
- **功能**: 获取OpenAPI 3.0规范的JSON文档

### 使用说明

1. **查看API文档**：
   - 启动项目后，访问 `http://localhost:8080/api/v1/swagger-ui.html`
   - 可以看到所有已配置的API接口

2. **测试接口**：
   - 在Swagger UI中点击接口展开详情
   - 点击"Try it out"按钮
   - 填写请求参数
   - 点击"Execute"执行请求
   - 查看响应结果

3. **JWT认证**：
   - 需要认证的接口，点击右上角的"Authorize"按钮
   - 输入JWT Token（格式：`Bearer {token}`）
   - 或者直接输入Token值，系统会自动添加Bearer前缀
   - 点击"Authorize"完成认证
   - 之后所有请求都会自动携带Token

4. **接口分组**：
   - 接口按Controller分组显示
   - 当前包含以下模块：
     - 认证管理
     - 用药计划管理
     - 今日用药管理
     - 用药记录管理
     - 药品库管理
     - 微信订阅消息管理

## 接口规范

### 统一响应格式

```json
{
  "code": 0,
  "msg": "ok",
  "data": {}
}
```

### 错误码

| 错误码 | 含义 |
|--------|------|
| 0 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录/Token失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器错误 |

### 认证方式

除以下接口外，所有接口都需要在请求头中携带JWT Token：

```
Authorization: Bearer <token>
```

**无需认证的接口**：
- `GET /auth/captcha` - 获取验证码
- `POST /auth/login` - 登录
- `POST /auth/register` - 注册

## 配置说明

### application.yml

- `server.port`: 服务端口（默认8080）
- `server.servlet.context-path`: 上下文路径（/api/v1）
- `jwt.secret`: JWT密钥（生产环境请修改）
- `jwt.expiration`: Token过期时间（毫秒）
- `mybatis.mapper-locations`: MyBatis XML映射文件位置（支持子目录）
- `wechat.subscribe.template-ids`: 微信订阅消息模板ID列表（逗号分隔）

### application-dev.yml

开发环境数据库连接配置。

## 开发说明

### 密码加密

当前使用MD5加密（简化处理），生产环境建议使用BCrypt：

```java
// 推荐使用Spring Security的BCryptPasswordEncoder
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String encodedPassword = encoder.encode(password);
```

### 验证码功能

验证码接口当前返回占位数据，需要实现：
- 图片验证码生成
- 验证码存储（Redis推荐）
- 验证码校验

## 已实现模块

### 第一阶段：核心功能模块（MVP）

1. **认证模块** (`/auth/*`) ✅
   - 用户登录、注册
   - 验证码获取（占位实现）

2. **用药计划模块** (`/med/plans/*`) ✅
   - 创建、查询、更新、删除用药计划
   - 支持每日/每周重复类型
   - 提醒开关管理
   - 更新计划时自动重建未来点位记录

3. **今日用药模块** (`/med/today/*`) ✅
   - 获取当天用药点位
   - 自动生成点位记录（按计划规则）

4. **用药记录模块** (`/med/records/*`) ✅
   - 查询用药记录列表（支持多条件过滤）
   - 标记已服用/未服
   - 补记/更正功能

5. **药品库模块** (`/med/drugs/*`) ✅
   - 药品搜索（支持关键词和全文检索）
   - 药品详情查询

6. **微信订阅消息模块** (`/wechat/subscribe/*`) ✅
   - 获取订阅模板配置
   - 上报授权结果

## 后续开发

### 第二阶段：其他功能模块

1. **健康日记模块** (`/health/diary/*`)
   - 创建/更新健康日记
   - 查询日记列表
   - 趋势数据统计

2. **AI对话模块** (`/ai/*`)
   - 发送消息
   - 会话管理
   - 消息历史

3. **紧急联系人模块** (`/profile/emergency-contacts/*`)
   - 获取/更新紧急联系人

4. **健康文章模块** (`/articles/*`)
   - 文章列表
   - 文章详情

5. **家属绑定模块** (`/family/*`)
   - 生成家庭码
   - 家属绑定
   - 家属侧聚合数据

## 技术实现说明

### JSON字段处理

项目使用自定义的 `JsonTypeHandler` 处理数据库中的JSON字段（如药品库的tags、commonNames等）。该处理器基于Jackson实现，支持List和Map类型的自动转换。

### 点位记录生成逻辑

- 创建计划时不会自动生成点位记录
- 调用 `/med/today` 或 `/med/today/ensure` 时会自动生成当天的点位记录
- 更新计划时，会删除 `today+1` 之后的所有点位记录，然后重新生成
- 点位记录的唯一性由 `plan_id + date + time` 保证

### 计划重复类型

- **daily**: 每日重复，无需设置 `repeatDays`
- **weekly**: 每周特定几天重复，需要设置 `repeatDays`（0=周日，1=周一，...，6=周六）

### 软删除机制

用药计划使用软删除（`deleted_at` 字段），删除后数据仍保留在数据库中，便于数据恢复和审计。

## 注意事项

1. 生产环境请修改JWT密钥
2. 密码加密建议使用BCrypt（当前使用MD5，仅用于开发）
3. 验证码功能需要完整实现（当前返回占位数据）
4. 数据库连接池配置可根据实际情况调整
5. 日志级别生产环境建议调整为INFO
6. JSON字段需要MySQL 5.7+支持
7. 全文检索功能需要MySQL全文索引支持（FULLTEXT）
8. 微信订阅消息的定时发送功能需要单独实现（使用Spring的@Scheduled注解）

