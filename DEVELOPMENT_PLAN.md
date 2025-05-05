# 社内業務サポートAIプラットフォーム - 开发计划，MVP实现

## 阶段0: 项目初始化与环境配置 (已完成)

- [x] 创建Spring Boot项目
- [x] 配置Maven依赖
- [x] 设置MySQL数据库环境(Docker)
- [x] 配置基本的application.properties

## 阶段1: 数据库设计与实体类开发

### 任务1.1: 设计数据库表结构
- [x] 用户表(users)
- [x] API Token表(api_tokens)
- [x] Prompt表(prompts)

### 任务1.2: 创建实体类
- [x] 创建User实体类
- [x] 创建ApiToken实体类
- [x] 创建Prompt实体类

### 任务1.3: 配置MyBatis
- [x] 创建Mapper接口 (User)
- [x] 创建Mapper接口 (ApiToken, Prompt)
- [x] 配置MyBatis全局设置

## 阶段2: 用户认证与安全

### 任务2.1: Spring Security配置
- [x] 配置SecurityFilterChain (基础)
- [x] 实现密码加密
- [x] 配置认证管理器
- [x] 集成UserDetailsService和PasswordEncoder

### 任务2.2: JWT实现
- [x] 创建JWT工具类
- [x] 实现JWT过滤器
- [x] 配置JWT认证 (集成过滤器到SecurityConfig)

### 任务2.3: 用户服务实现
- [x] 创建UserService接口
- [x] 实现UserServiceImpl (包括注册和密码加密)
- [x] 创建UserController (更新为使用安全上下文)
- [x] 创建UserDetailsService实现 (UserDetailsServiceImpl)
- [x] 创建AuthController (登录/注册端点)
- [x] 创建认证相关DTO (LoginRequest, RegisterRequest, AuthResponse)

## 阶段3: API Token管理

### 任务3.1: 加密工具开发
- [x] 创建EncryptionUtil工具类
- [x] 实现AES加密/解密功能

### 任务3.2: Token服务实现
- [x] 创建ApiTokenService接口
- [x] 实现ApiTokenServiceImpl
- [x] 创建ApiTokenController

## 阶段4: Prompt管理

### 任务4.1: Prompt服务实现
- [x] 创建PromptService接口
- [x] 实现PromptServiceImpl
- [x] 创建PromptController

## 阶段5: 网页内容摘要功能

### 任务5.1: 网页内容获取
- [x] 创建WebContentService
- [x] 实现HTML解析(Jsoup)

### 任务5.2: AI服务集成
- [x] 配置Spring AI
- [x] 创建AiService接口
- [x] 实现OpenAI集成

### 任务5.3: 摘要控制器
- [x] 创建SummarizationController
- [x] 实现摘要请求处理
- [x] 创建请求/响应DTO

## 阶段6: 测试

### 任务6.1: 单元测试
- [x] 编写Service层测试 (PromptService)
- [x] 编写Service层测试 (ApiTokenService)
- [x] 编写Util层测试 (EncryptionUtil)
- [x] 编写Util层测试 (JwtUtil)
- [x] 编写Service层测试 (UserDetailsServiceImpl)
- [x] 编写Security组件测试 (JwtAuthFilter)
- [ ] 编写Controller层测试 (AuthController, PromptController, ApiTokenController)

### 任务6.2: 集成测试
- [ ] 编写API集成测试
    - [x] 测试 `AuthController`:
        - [x] `POST /api/auth/login` (无效凭据登录)
    - [x] 测试 `ApiTokenController`:
        - [x] `GET /api/tokens` (获取用户Token列表)
        - [x] `DELETE /api/tokens/{id}` (删除指定Token)
    - [x] 测试 `PromptController` (CRUD):
        - [x] `GET /api/prompts` (获取用户Prompt列表)
        - [x] `POST /api/prompts` (创建Prompt)
        - [x] `GET /api/prompts/{id}` (获取指定Prompt)
        - [x] `PUT /api/prompts/{id}` (更新指定Prompt)
        - [x] `DELETE /api/prompts/{id}` (删除指定Prompt)
    - [x] 测试 `SummarizationController`:
        - [x] `POST /api/summarize` (无效URL)
        - [x] `POST /api/summarize` (用户无有效API Token情况)
    - [x] 测试API安全性:
        - [x] 未认证访问受保护端点 (Tokens, Prompts, Summarize)
        - [x] 使用过期/无效JWT访问受保护端点
        - [ ] 测试CORS配置 // 推迟至前后端联调阶段
    - [x] 测试API授权:
        - [x] 跨用户访问/修改资源 (Tokens, Prompts)
- [ ] 测试端到端流程:
    - [ ] 完整业务流程 (例如: 注册 -> 登录 -> 添加Token -> 添加Prompt -> 使用Prompt进行摘要)
    - [ ] 包含错误处理的流程 (例如: 摘要时Token无效或URL无法访问)
- [ ] 性能和边界测试:
    - [ ] 测试大型网页摘要处理
    - [ ] 测试API请求限制和超时处理 (如果已实现)

## 阶段7: 前端开发 (未来计划)

### 任务7.1: 前端项目设置
- [ ] 创建Vue.js项目
- [ ] 配置路由和状态管理

### 任务7.2: 用户界面实现
- [ ] 实现登录/注册页面
- [ ] 实现Token管理页面
- [ ] 实现Prompt管理页面
- [ ] 实现网页摘要页面

## 阶段8: 部署

### 任务8.1: Docker配置
- [ ] 创建后端Dockerfile
- [ ] 创建前端Dockerfile
- [ ] 更新docker-compose.yml

### 任务8.2: 部署文档
- [ ] 编写部署指南
- [ ] 编写用户手册
