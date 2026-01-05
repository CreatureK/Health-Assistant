# Health-Assistant

Health-Assistant 是一套基于 **Spring Boot + uni-app** 的健康助手系统，面向长期用药或慢病人群，提供 **用药计划管理、每日用药打卡、用药记录、药品库查询、健康文章、AI 对话（Dify）** 等功能，并规划集成 **微信订阅** 能力。

---

## 功能概览

### 已实现功能（MVP）
- 用户认证（注册 / 登录 / JWT 鉴权）
- 用药计划管理（创建 / 编辑 / 删除）
- 今日用药点位生成
- 用药记录（打卡、补记、更正、历史查询）
- 药品库（搜索、详情）
- 健康文章（分类、详情、浏览统计）
- AI 对话（Dify）

### 规划功能
- 健康日记
- 紧急联系人
- 家属绑定与家庭数据聚合
- 微信订阅

---

## 技术栈

### 后端
- Java 17
- Spring Boot 2.7.18
- MyBatis 3.5.x
- MySQL 8.0
- JWT（jjwt 0.11.5）
- Swagger / OpenAPI 3.0（springdoc-openapi-ui 1.7.0）
- Maven

### 前端
- uni-app
- Vue
- 运行环境：微信小程序
- 开发工具：HBuilderX

---

## 项目结构

Health-Assistant
├── backend/                 后端 Spring Boot 项目
├── frontend/                前端 uni-app 项目
├── docs/                    项目文档
├── DIFY_docs/               Dify 对接说明
├── health.sql               数据库表结构
├── insert.sql               初始化数据
└── README.md

---

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- HBuilderX（最新版）

---

### 数据库初始化

创建数据库：

CREATE DATABASE health_assistant
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

执行 SQL（按顺序）：

mysql -u root -p health_assistant < health.sql
mysql -u root -p health_assistant < insert.sql

---

### 后端启动

配置文件路径：
backend/src/main/resources/application-dev.yml

启动方式：

mvn spring-boot:run

或：

mvn clean package
java -jar target/Health-Assistant-backend-1.0-SNAPSHOT.jar

---

### 前端启动

- 使用 HBuilderX 打开 frontend 目录
- 运行到 微信小程序（mp-weixin）

接口地址配置：
frontend/common/api.js

约定：
- BASE_URL 必须包含 /api/v1
- API 仅定义资源路径（如 /auth/login）

---

## 接口规范

统一响应格式：

{
  "code": 0,
  "msg": "ok",
  "data": {}
}

JWT 请求头：
Authorization: Bearer <token>

---

## 文档索引

- 接口文档：docs/接口.docx
- 详细需求：docs/详细需求.docx
- 成员分工：docs/成员分工.docx
- 技术报告：docs/02_健康用药管理小程序_技术报告.docx

---

## 注意事项

- 前端需确保不存在 Git 冲突标记
- BASE_URL 必须包含 /api/v1
- 生产环境请修改 JWT 密钥
- 当前密码加密使用 MD5，仅用于开发环境
- 验证码功能为占位实现
- MySQL 需支持 JSON 与 FULLTEXT

---

## License

本项目仅用于学习与课程设计，未授权商业使用。
