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
│   └── WebConfig.java                 # Web配置（拦截器、跨域）
├── common/                            # 通用组件
│   ├── Result.java                    # 统一响应格式
│   ├── ResultCode.java                # 错误码枚举
│   ├── JwtUtil.java                   # JWT工具类
│   └── UserContext.java               # 用户上下文
├── interceptor/                       # 拦截器
│   └── AuthInterceptor.java           # JWT认证拦截器
├── controller/                        # 控制器层
│   └── auth/
│       └── AuthController.java       # 认证接口
├── service/                           # 服务层
│   └── auth/
│       └── AuthService.java          # 认证服务
├── mapper/                            # 数据访问层
│   └── UserMapper.java               # 用户Mapper
└── entity/                            # 实体类
    └── User.java                     # 用户实体
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
- 登录接口: `POST /auth/login`
- 注册接口: `POST /auth/register`
- 验证码接口: `GET /auth/captcha`

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
   - 当前有"认证管理"模块

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

## 后续开发

根据接口文档，需要实现以下模块：

1. **用药管理模块** (`/med/*`)
   - 用药计划管理
   - 今日用药打卡
   - 用药记录查询

2. **订阅消息模块** (`/wechat/subscribe/*`)
   - 订阅模板配置
   - 授权结果上报

3. **药品库模块** (`/med/drugs/*`)
   - 药品搜索
   - 药品详情

4. **第二阶段模块**
   - 健康日记
   - AI对话
   - 紧急联系人
   - 健康文章
   - 家属绑定

## 注意事项

1. 生产环境请修改JWT密钥
2. 密码加密建议使用BCrypt
3. 验证码功能需要完整实现
4. 数据库连接池配置可根据实际情况调整
5. 日志级别生产环境建议调整为INFO

