# 社内業務サポートAIプラットフォーム (内部业务支持AI平台) - 项目架构文档

## 项目概述

本项目旨在构建一个基于Web的平台，利用AI能力，为用户提供处理公开信息的辅助工具，提高个人工作效率。平台不处理任何公司内部或客户的敏感数据。用户需要提供自己的AI服务API Token来驱动AI功能。

## 部署模式

Web SaaS (Software as a Service)

## 技术栈

- **后端**:
  - 语言: Java 21
  - 框架: Spring Boot 3.4.5
  - 数据库: MySQL 8.0
  - ORM: MyBatis-Plus 3.5.7
  - 安全: Spring Security, JWT
  - AI集成: Spring AI (OpenAI)
  - 构建工具: Maven

- **前端** (计划中):
  - 语言: TypeScript
  - 框架: Vue.js
  - 构建工具: npm/yarn

- **部署**:
  - 容器化: Docker
  - Web服务器: Nginx (计划中)

## 项目结构

```
AIplatJava/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ding/aiplatjava/
│   │   │       ├── config/           # 配置类
│   │   │       ├── controller/       # 控制器
│   │   │       ├── dto/              # 数据传输对象
│   │   │       ├── entity/           # 实体类
│   │   │       ├── exception/        # 异常处理
│   │   │       ├── mapper/           # MyBatis映射接口
│   │   │       ├── security/         # 安全相关
│   │   │       ├── service/          # 服务层
│   │   │       │   └── impl/         # 服务实现
│   │   │       ├── util/             # 工具类
│   │   │       └── AIplatJavaApplication.java  # 主应用类
│   │   └── resources/
│   │       ├── static/               # 静态资源
│   │       ├── templates/            # 模板
│   │       ├── mapper/               # MyBatis XML映射文件
│   │       └── application.properties # 应用配置
│   └── test/                         # 测试代码
└── docker-compose.yml                # Docker配置
```

## 核心功能模块

### 1. 用户认证与管理 (User Auth & Management)

**职责**:
- 用户注册、登录、登出
- 用户信息管理
- 权限控制

**主要组件**:
- `entity.User`: 用户实体类
- `mapper.UserMapper`: 用户数据访问接口
- `service.UserService`: 用户服务接口
- `service.impl.UserServiceImpl`: 用户服务实现
- `controller.UserController`: 用户相关API
- `security.*`: Spring Security配置和JWT实现

### 2. API Token 安全管理 (Secure API Token Management)

**职责**:
- 安全存储用户的AI服务API Token
- 提供Token的加密和解密功能
- 管理Token的生命周期

**主要组件**:
- `entity.ApiToken`: API Token实体类
- `mapper.ApiTokenMapper`: Token数据访问接口
- `service.ApiTokenService`: Token服务接口
- `service.impl.ApiTokenServiceImpl`: Token服务实现
- `controller.ApiTokenController`: Token相关API
- `util.EncryptionUtil`: 加密工具类

### 3. Prompt 管理 (Prompt Management)

**职责**:
- 提供Prompt的CRUD操作
- 管理用户的Prompt集合

**主要组件**:
- `entity.Prompt`: Prompt实体类
- `mapper.PromptMapper`: Prompt数据访问接口
- `service.PromptService`: Prompt服务接口
- `service.impl.PromptServiceImpl`: Prompt服务实现
- `controller.PromptController`: Prompt相关API

### 4. 网页内容摘要 (Web Article Summarization)

**职责**:
- 获取和解析网页内容
- 调用AI服务进行内容摘要
- 返回处理结果

**主要组件**:
- `service.WebContentService`: 网页内容服务接口
- `service.impl.WebContentServiceImpl`: 网页内容服务实现
- `service.AiService`: AI服务接口
- `service.impl.AiServiceImpl`: AI服务实现
- `controller.SummarizationController`: 摘要相关API
- `dto.SummarizationRequest`: 摘要请求DTO
- `dto.SummarizationResponse`: 摘要响应DTO

## 数据库设计 (初步)

### 用户表 (users)
- id: bigint (PK)
- username: varchar(50)
- email: varchar(100)
- password: varchar(255) (加密存储)
- created_at: datetime
- updated_at: datetime

### API Token表 (api_tokens)
- id: bigint (PK)
- user_id: bigint (FK -> users.id)
- provider: varchar(50) (如 "openai")
- token_value: varchar(255) (加密存储)
- created_at: datetime
- updated_at: datetime

### Prompt表 (prompts)
- id: bigint (PK)
- user_id: bigint (FK -> users.id)
- title: varchar(100)
- content: text
- category: varchar(50)
- created_at: datetime
- updated_at: datetime

## 安全考虑

1. 所有API Token使用强加密算法存储
2. 使用HTTPS保护所有通信
3. 实现适当的输入验证和清理
4. 使用JWT进行无状态认证
5. 实施适当的访问控制和权限检查

## 开发路线图

### 阶段1: 基础设施和核心功能
- 设置项目结构和依赖
- 实现数据库模型和迁移
- 开发用户认证系统
- 实现API Token管理

### 阶段2: 核心业务功能
- 开发Prompt管理功能
- 实现网页内容获取和解析
- 集成AI服务
- 开发网页摘要功能

### 阶段3: 前端开发和集成
- 设计和实现用户界面
- 集成前后端
- 实现响应式设计

### 阶段4: 测试、优化和部署
- 编写单元测试和集成测试
- 性能优化
- 部署配置
- 用户文档

## 注意事项

- 本项目不处理任何公司内部或客户的敏感数据
- 用户需要提供自己的AI服务API Token
- 所有功能应遵循最小权限原则
- 代码应遵循良好的编码实践和设计模式
