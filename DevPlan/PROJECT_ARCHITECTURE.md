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
  - ORM: MyBatis 3.0.3
  - 安全: Spring Security, JWT
  - AI集成: Spring AI (OpenAI)
  - 构建工具: Maven
  - 其他: Jsoup (HTML解析), Spring Validation (数据校验)

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
│   │   │   └── com/ding/aiplatjava/      # Java源代码根目录
│   │   │       ├── config/           # Spring Boot 配置类 (如MyBatis, Security等)
│   │   │       ├── controller/       # Spring MVC 控制器 (处理HTTP请求)
│   │   │       ├── dto/              # 数据传输对象 (用于API请求/响应)
│   │   │       ├── entity/           # 数据库实体类 (映射数据库表)
│   │   │       ├── exception/        # 自定义异常类及全局异常处理器
│   │   │       ├── mapper/           # MyBatis Mapper 接口
│   │   │       ├── security/         # Spring Security 相关实现 (JWT过滤器等)
│   │   │       ├── service/          # 业务逻辑服务层接口
│   │   │       │   └── impl/         # 业务逻辑服务层实现 (含UserDetailsService)
│   │   │       ├── util/             # 通用工具类 (含JwtUtil)
│   │   │       └── AIplatJavaApplication.java  # Spring Boot 主应用启动类
│   │   └── resources/                # 资源文件目录
│   │       ├── db/                   # 数据库脚本 (如 schema.sql)
│   │       ├── mapper/               # MyBatis XML 映射文件
│   │       │   ├── PromptMapper.xml
│   │       │   ├── ApiTokenMapper.xml
│   │       │   └── OcrTaskMapper.xml
│   │       ├── static/               # 静态资源 (如HTML, CSS, JS) (计划中)
│   │       ├── templates/            # 服务端模板 (如Thymeleaf) (计划中)
│   │       └── application.properties # Spring Boot 应用配置文件
│   └── test/                         # 测试代码根目录
│       └── java/
│           └── com/ding/aiplatjava/      # 测试代码包结构
│               ├── controller/       # 控制器测试 (待添加)
│               ├── security/         # Security组件测试
│               │   └── JwtAuthFilterTest.java
│               ├── service/
│               │   └── impl/         # 服务实现测试
│               │       ├── PromptServiceImplTest.java
│               │       ├── ApiTokenServiceImplTest.java
│               │       └── UserDetailsServiceImplTest.java
│               ├── util/             # 工具类测试
│               │   ├── EncryptionUtilTest.java
│               │   └── JwtUtilTest.java
│               └── AIplatJavaApplicationTests.java # Spring Boot 上下文加载测试
├── pom.xml                           # Maven项目配置文件
├── docker-compose.yml                # Docker Compose 配置文件
├── PROJECT_ARCHITECTURE.md           # 项目架构文档 (本文档)
├── DEVELOPMENT_PLAN.md               # 开发计划
└── PROGRESS_SUMMARY.md               # 进度总结
```

## 核心功能模块

### 1. 用户认证与管理 (User Auth & Management)

**职责**:
- 用户注册、登录、登出
- 用户信息管理
- 权限控制 (通过JWT)

**主要组件**:
- `entity.User`: 用户实体类
- `mapper.UserMapper`: 用户数据访问接口
- `service.UserService`: 用户服务接口 (含 `registerUser`)
- `service.impl.UserServiceImpl`: 用户服务实现 (含密码加密)
- `service.impl.UserDetailsServiceImpl`: Spring Security 用户详情服务实现
- `controller.UserController`: 用户信息查询API (待移除或改造)
- `controller.AuthController`: 认证API (登录`/api/auth/login`, 注册`/api/auth/register`)
- `dto.UserDto`: 用户数据传输对象
- `dto.UserRegistrationDto`: 用户注册数据传输对象
- `dto.LoginRequestDto`: 登录请求DTO
- `dto.RegisterRequestDto`: 注册请求DTO
- `dto.AuthResponseDto`: 认证响应DTO (含JWT)
- `config.SecurityConfig`: Spring Security核心配置
- `security.JwtAuthFilter`: JWT认证过滤器
- `util.JwtUtil`: JWT生成与验证工具类

### 2. API Token 安全管理 (Secure API Token Management)

**职责**:
- 安全存储用户的AI服务API Token
- 提供Token的加密和解密功能
- 管理Token的生命周期

**主要组件**:
- `entity.ApiToken`: API Token实体类 (使用 Lombok)
- `mapper.ApiTokenMapper`: Token数据访问接口
    - `ApiToken selectById(Long id)`: 根据ID查询 ApiToken。
    - `List<ApiToken> selectByUserId(Long userId)`: 根据用户ID查询该用户的所有 ApiTokens。
    - `int insert(ApiToken apiToken)`: 插入新的 ApiToken。
    - `int deleteByIdAndUserId(Long id, Long userId)`: 根据 Token ID 和用户 ID 删除 ApiToken。
- `service.ApiTokenService`: Token服务接口
    - `ApiToken createToken(ApiToken apiToken, Long userId)`: 创建新的 API Token (加密)。
    - `List<ApiToken> getTokensByUserId(Long userId)`: 获取指定用户的所有 API Tokens (加密状态)。
    - `String getDecryptedTokenValue(Long tokenId, Long userId)`: 根据 Token ID 获取解密后的 Token 值 (校验权限)。
    - `boolean deleteToken(Long tokenId, Long userId)`: 删除指定 ID 的 API Token (校验权限)。
- `service.impl.ApiTokenServiceImpl`: Token服务实现 (实现 `ApiTokenService` 接口)
- `controller.ApiTokenController`: Token相关API
    - `ResponseEntity<List<ApiTokenDto>> getCurrentUserTokens()`: 获取当前用户的所有 API Tokens (仅含安全信息)。
    - `ResponseEntity<ApiTokenDto> createToken(ApiTokenDto tokenDto)`: 为当前用户创建新的 API Token。
    - `ResponseEntity<Void> deleteToken(Long id)`: 删除指定 ID 的 API Token。
    - `(private) ApiTokenDto convertToDto(ApiToken apiToken)`: 内部转换方法。
- `dto.ApiTokenDto`: API Token 数据传输对象 (用于API交互, 不含Token值)。
- `util.EncryptionUtil`: 加密工具类
    - `String encrypt(String plainText)`: AES加密。
    - `String decrypt(String encryptedText)`: AES解密。

### 3. Prompt 管理 (Prompt Management)

**职责**:
- 提供Prompt的CRUD操作
- 管理用户的Prompt集合

**主要组件**:
- `entity.Prompt`: Prompt实体类 (使用 Lombok)
- `mapper.PromptMapper`: Prompt数据访问接口
    - `Prompt selectById(Long id)`: 根据ID查询Prompt
    - `List<Prompt> selectByUserId(Long userId)`: 根据用户ID查询该用户的所有Prompt
    - `int insert(Prompt prompt)`: 插入新的Prompt
    - `int updateById(Prompt prompt)`: 根据ID更新Prompt
    - `int deleteById(Long id, Long userId)`: 根据ID删除Prompt (会校验用户ID)
- `service.PromptService`: Prompt服务接口
    - `Prompt getPromptById(Long id, Long userId)`: 根据ID获取Prompt (校验用户)
    - `List<Prompt> getPromptsByUserId(Long userId)`: 获取指定用户的所有Prompt
    - `Prompt createPrompt(Prompt prompt, Long userId)`: 创建新的Prompt
    - `Prompt updatePrompt(Long id, Prompt prompt, Long userId)`: 更新现有的Prompt (校验用户)
    - `boolean deletePrompt(Long id, Long userId)`: 删除Prompt (校验用户)
- `service.impl.PromptServiceImpl`: Prompt服务实现 (实现 `PromptService` 接口)
- `controller.PromptController`: Prompt相关API
    - `ResponseEntity<List<Prompt>> getCurrentUserPrompts()`: 获取当前用户的所有Prompts
    - `ResponseEntity<Prompt> getPromptById(Long id)`: 根据ID获取单个Prompt
    - `ResponseEntity<Prompt> createPrompt(Prompt prompt)`: 创建新的Prompt
    - `ResponseEntity<Prompt> updatePrompt(Long id, Prompt promptDetails)`: 更新现有的Prompt
    - `ResponseEntity<Void> deletePrompt(Long id)`: 删除Prompt

### 4. 网页内容摘要 (Web Article Summarization)

**职责**:
- 获取和解析网页内容
- 调用AI服务进行内容摘要
- 返回处理结果

**主要组件**:
- `service.WebContentService`: 网页内容服务接口
- `service.impl.WebContentServiceImpl`: 网页内容服务实现 (使用 Jsoup)
- `service.AiService`: AI服务接口
- `service.impl.AiServiceImpl`: AI服务实现 (使用 Spring AI ChatModel)
- `controller.SummarizationController`: 摘要相关API (处理 `/api/summarize` POST 请求)
- `dto.SummarizationRequestDto`: 摘要请求DTO (包含 URL)
- `dto.SummarizationResponseDto`: 摘要响应DTO (包含摘要结果)

### 5. OCR文档处理 (OCR Document Processing)

**职责**:
- 接收用户上传的文档
- 异步处理OCR任务
- 与Python OCR微服务通信
- 管理OCR任务状态和结果
- 返回处理结果

**主要组件**:
- `entity.OcrTask`: OCR任务实体类
- `mapper.OcrTaskMapper`: OCR任务数据访问接口
  - `OcrTask selectByTaskId(String taskId)`: 根据任务ID查询OCR任务
  - `int insert(OcrTask ocrTask)`: 插入新的OCR任务
  - `int updateStatus(String taskId, String status)`: 更新OCR任务状态
  - `int updateResult(String taskId, String result, LocalDateTime completedAt)`: 更新OCR任务结果
  - `int updateError(String taskId, String errorMessage)`: 更新OCR任务错误信息
- `service.OcrService`: OCR服务接口
  - `OcrResponseDto uploadAndProcess(MultipartFile file, OcrUploadRequestDto requestDto, Long userId)`: 上传并处理文档
  - `CompletableFuture<OcrResponseDto> processOcrTaskAsync(String taskId, String filePath, Long userId, OcrUploadRequestDto requestDto)`: 异步处理OCR任务
  - `OcrResponseDto getTaskStatus(String taskId)`: 获取OCR任务状态
  - `OcrResponseDto getTaskResult(String taskId)`: 获取OCR任务结果
- `service.impl.OcrServiceImpl`: OCR服务实现
- `service.OcrProcessingService`: OCR处理服务接口
  - `CompletableFuture<Map<String, Object>> processFile(Path filePath, Map<String, Object> options)`: 处理文件
- `service.impl.OcrProcessingServiceImpl`: OCR处理服务实现 (与Python微服务通信)
- `controller.OcrController`: OCR相关API
  - `ResponseEntity<OcrResponseDto> uploadFile(MultipartFile file, OcrUploadRequestDto requestDto)`: 上传文件并处理
  - `ResponseEntity<OcrResponseDto> getTaskStatus(String taskId)`: 获取任务状态
  - `ResponseEntity<OcrResponseDto> getTaskResult(String taskId)`: 获取任务结果
- `dto.OcrUploadRequestDto`: OCR上传请求DTO
- `dto.OcrResponseDto`: OCR响应DTO
- `dto.OcrTaskStatusDto`: OCR任务状态DTO

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

### OCR任务表 (ocr_tasks)
- task_id: varchar(36) (PK, UUID)
- user_id: bigint (FK -> users.id)
- file_name: varchar(255)
- file_size: bigint
- status: varchar(20) (如 "PENDING", "PROCESSING", "COMPLETED", "FAILED")
- result: text (JSON格式，存储OCR处理结果)
- error_message: text (存储错误信息，如果有)
- created_at: datetime
- completed_at: datetime

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
