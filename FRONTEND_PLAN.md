# 社内業務サポートAIプラットフォーム (内部业务支持AI平台) - 前端规划 (MVP)

## 1. 项目概述

- **项目名称:** 社内業務サポートAIプラットフォーム (内部业务支持AI平台)
- **核心理念:** 构建一个基于Web的平台，利用AI能力，为用户提供处理公开信息的辅助工具，提高个人工作效率。平台不处理任何公司内部或客户的敏感数据。用户需要提供自己的AI服务API Token来驱动AI功能。
- **部署模式:** Web SaaS

## 2. MVP (最小可行产品)

### 2.1 MVP 核心功能模块

1.  **用户认证与管理 (User Auth & Management):**
    *   **功能:** 用户注册、登录、登出。
    *   **前端页面/视图:**
        *   注册页面 (`/register`)
        *   登录页面 (`/login`)
        *   (导航栏中应有登出按钮)
    *   **后端API依赖:**
        *   `POST /api/auth/register`
        *   `POST /api/auth/login`

2.  **API Token 安全管理 (Secure API Token Management):**
    *   **功能:** 用户在设置页面输入、更新、删除自己的AI API Token。
    *   **前端页面/视图:**
        *   API Token管理页面 (`/settings/tokens` 或类似路径)
            *   Token列表展示 (提供商，部分掩码的Token值，创建/更新日期)
            *   新建Token表单/模态框 (输入提供商，Token值)
            *   (编辑Token功能 - MVP中可简化为删除后新建)
            *   删除Token确认
    *   **后端API依赖:**
        *   `GET /api/tokens` (获取列表)
        *   `POST /api/tokens` (创建)
        *   `DELETE /api/tokens/{id}` (删除)

3.  **Prompt 管理 (Prompt Management - CRUD):**
    *   **功能:** 用户可以创建、查看、编辑、删除自己常用的Prompt。
    *   **前端页面/视图:**
        *   Prompt管理页面 (`/prompts`)
            *   Prompt列表展示 (标题，分类，部分内容预览)
            *   新建Prompt表单/页面 (`/prompts/new`)
            *   编辑Prompt表单/页面 (`/prompts/{id}/edit`)
            *   查看单个Prompt详情页 (`/prompts/{id}` - 可选，列表项点击直接编辑也可)
            *   删除Prompt确认
    *   **后端API依赖:**
        *   `GET /api/prompts` (获取列表)
        *   `POST /api/prompts` (创建)
        *   `GET /api/prompts/{id}` (获取单个，用于编辑或查看详情)
        *   `PUT /api/prompts/{id}` (更新)
        *   `DELETE /api/prompts/{id}` (删除)

4.  **网页内容摘要 (Web Article Summarization):**
    *   **功能:** 用户输入一个公开网页的URL，后端获取网页内容，调用用户配置的AI Token对应的AI服务进行内容摘要和要点提取，并在前端展示结果。
    *   **前端页面/视图:**
        *   内容摘要页面 (`/summarize` 或作为仪表盘主功能)
            *   URL输入框
            *   (可选) AI Token选择下拉框 (如果用户有多个Token，或者允许针对不同任务使用不同Token)
            *   "摘要"按钮
            *   加载状态指示
            *   摘要结果展示区域
    *   **后端API依赖:**
        *   `POST /api/summarize` (请求体含URL和AI Provider标识)
        *   (前端可能需要先调用 `GET /api/tokens` 来填充Token选择下拉框)

### 2.2 MVP 预计使用技术栈 (前端相关)

*   **语言:** TypeScript
*   **框架:** Vue.js (推荐Vue 3 + Vite)
*   **UI框架/库:** Naive UI
*   **状态管理:** Pinia (Vue 3推荐)
*   **路由:** Vue Router
*   **HTTP客户端:** Axios 或 Fetch API
*   **构建/版本:** npm/yarn, Git

## 3. 页面/视图初步规划 (MVP)

*   `/login`: 登录页
*   `/register`: 注册页
*   `/dashboard` (或 `/summarize`): 主页/摘要功能页 (受保护路由)
*   `/tokens`: API Token管理页 (受保护路由)
*   `/prompts`: Prompt管理页 (受保护路由)
*   `/prompts/new`: 新建Prompt页 (受保护路由)
*   `/prompts/:id/edit`: 编辑Prompt页 (受保护路由)
*   (可能需要的) 用户设置/个人资料页 (MVP阶段可简化或合并到Token管理等)

## 4. 通用组件初步设想 (MVP)

*   导航栏 (显示Logo/项目名，导航链接，用户信息/登出按钮)
*   按钮 (主要按钮，次要按钮，危险按钮等)
*   输入框 (文本，密码，文本域)
*   模态框 (用于确认操作，或简单表单)
*   列表/表格 (用于展示Tokens, Prompts)
*   加载指示器 (Spinner, 骨架屏)
*   通知/提示条 (操作成功/失败提示)

## 5. API集成要点

*   所有受保护的API请求都需要在请求头中添加 `Authorization: Bearer <JWT>`。
*   统一的API错误处理机制：
    *   **401 Unauthorized (认证失败):** JWT过期、无效或未提供。后端返回JSON响应 (含 `timestamp`, `status=401`, `error="Unauthorized"`, `message`, `path`)。前端应捕获此错误，清除本地认证信息，并重定向到登录页。
    *   **403 Forbidden (授权失败):** 用户已认证但无权访问特定资源或执行操作。后端返回JSON响应 (含 `timestamp`, `status=403`, `error="Forbidden"`, `message`, `path`)。前端应向用户显示权限不足的提示。
    *   **404 Not Found (资源不存在):** 请求的资源未找到 (例如，错误的ID)。后端返回JSON响应 (含 `timestamp`, `status=404`, `error="Not Found"`, `message`, `path`)。前端应显示资源未找到的提示。
    *   **400 Bad Request / 422 Unprocessable Entity (请求错误/校验失败):**
        *   一般性请求错误 (如 `SummarizationController` 中因用户未配置某 `provider` 的Token，或Token配置无效/损坏导致无法使用时): 后端 `POST /api/summarize` 返回400，响应体为 `SummarizationResponseDto` (`success=false`, `message="具体原因..."`)。前端应直接向用户显示此 `message`。
        *   请求体参数校验失败 (如注册时用户名格式不正确): 后端返回400，响应体为JSON (含 `timestamp`, `status=400`, `error="Validation Error"`, `errors: [{field, rejectedValue, defaultMessage}]`)。前端应解析 `errors` 数组，在对应表单字段旁显示 `defaultMessage`。
    *   **5xx 服务器错误 (服务器内部错误):** 后端返回JSON响应 (含 `timestamp`, `status=500`, `error="Internal Server Error"`, `message="通用错误提示..."`)。前端应显示一个通用的服务错误提示，并建议用户稍后重试。

## 6. MVP 不包含的功能 (后续迭代考虑)

*   AI辅助Prompt生成/逻辑分析
*   音视频摘要
*   自然语言任务管理及日历集成
*   想法支援
*   OneNote集成
*   复杂的自动化数据分析流程
*   自建工作流引擎
*   (前端方面) 复杂的用户个性化设置、主题切换、国际化等。

## 7. 前端开发准备与最佳实践建议

### 7.1 开发环境与工具
*   **Node.js & 包管理器:** 确保安装最新稳定版的 Node.js 及 npm 或 yarn。
*   **IDE/编辑器:** 推荐使用 VS Code，并安装 Vue 3 推荐的插件 (如 Volar)。
*   **浏览器开发工具:** 安装 Vue Devtools 浏览器扩展以便调试。

### 7.2 代码风格与规范
*   **Linter & Formatter:** 推荐集成 ESLint 和 Prettier 以统一代码风格并检查潜在错误。
*   **Vue风格指南:** 可参考 Vue 官方风格指南进行编码。

### 7.3 API Client/Service 层封装
*   **模块化API调用:** 建议将所有后端API请求封装在专门的 `services` 或 `api` 模块中，而不是直接在组件中调用。
    *   **优点:** 代码清晰、易维护、方便统一处理请求头 (如JWT) 和响应。

### 7.4 环境变量配置
*   **配置文件:** 使用 `.env` 文件 (如 `.env.development`, `.env.production`) 管理环境变量 (例如 `VITE_API_BASE_URL`)，避免硬编码。
    *   Vite 对此提供良好支持。

---
*本文档的这一部分旨在提供前端开发启动前的一些准备建议。*

---
*这是一个初步规划，随着开发的进行会不断细化和调整。* 