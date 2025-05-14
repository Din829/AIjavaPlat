# 前端开发规则

在进行前端开发时，请遵循以下规则：

1.  **中文注释**: 为关键的代码逻辑、功能说明以及复杂的语法实现添加清晰的中文注释。
2.  **阶段性更新**: 在完成本计划（`FRONTEND_PLAN.md`）中定义的每一个阶段后，及时更新本文档以反映最新进度。
3.  **先分析再编码**: 在开始具体编码工作前，进行充分的需求分析和技术方案思考。
4.  **细致严谨**:在开发过程中保持细心和严谨的态度，注重代码质量和细节。

---

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

## 已知问题与临时解决方案

### 1. `LoginPage.vue` 中的 TypeScript 类型检查问题

*   **问题描述:**
    *   在 `LoginPage.vue` 文件中，IDE (通过 Volar/Vue Language Server) 报告了以下 TypeScript 相关错误和警告：
        *   `LoginCredentials` 类型虽然在类型注解中使用，但 TypeScript 可能无法正确识别这种使用方式并报告为未使用。
        *   底层的 Vue 语言服务错误，如 `找不到名称 __VLS_intrinsicElements` 和 `找不到名称 __VLS_asFunctionalComponent`。
    *   这些问题在尝试了调整依赖版本、清理 `node_modules`、检查 `tsconfig.json` 和 `vite-env.d.ts` 配置后仍然存在。

*   **临时解决方案:**
    *   为了不阻塞开发进度，暂时在 `my-ai-platform-frontend/src/views/LoginPage.vue` 文件的 `<script setup lang="ts">` 标签内部顶部添加了 `// @ts-nocheck` 注释。
    *   这个指令会禁用该文件的 TypeScript 类型检查，从而消除了 IDE 中的相关错误和警告提示。

*   **影响与注意事项:**
    *   **类型安全缺失:** `LoginPage.vue` 中的后续开发和修改将失去 TypeScript 的静态类型检查保护，增加了引入潜在 bug 的风险。
    *   **维护成本:** 需要更依赖手动测试和代码审查来确保该文件的质量。
    *   **根本原因未解决:** 底层的 `__VLS_` 相关错误源头尚未完全定位，可能与 Volar 版本、特定组件组合或环境配置有关。

*   **未来计划:**
    *   在后续开发过程中，如果时间允许，将尝试进一步排查这些类型问题的根本原因。
    *   关注 Volar、Vue、TypeScript 和 Naive UI 的更新，看新版本是否解决了此类底层问题。
    *   一旦根本原因解决或有更合适的修复方案，应移除 `LoginPage.vue` 中的 `// @ts-nocheck` 注释，以恢复完整的类型检查。

## 前端开发迭代计划 (MVP)

以下计划旨在逐步构建前端功能，保持灵活性并允许持续测试。

**阶段 1: 项目基础与核心结构 (Project Foundation & Core Structure)**

*   **目标:** 初始化项目，集成核心库，并建立基本的前端架构。
*   **主要任务:**
    1.  **项目初始化:** 使用 Vite 创建 Vue 3 + TypeScript 项目。 (已完成)
    2.  **依赖安装:** 集成 Vue Router, Pinia, Axios, Naive UI。 (已完成)
    3.  **基础配置:**
        *   设置目录结构 (`router`, `stores`, `services`, `views`, `components`, `layouts`)。 (已完成)
        *   集成并配置 Naive UI, Pinia, Vue Router。 (已完成)
        *   配置 ESLint 和 Prettier。 (已完成)
    4.  **环境变量:** 设置 `.env` 文件 (如 `VITE_API_BASE_URL`)。 (已完成)

**阶段 2: 布局、路由与认证基础 (Layout, Routing & Auth Basics)**

*   **目标:** 创建应用主布局，设置基础路由，并开始用户认证模块。
*   **主要任务:**
    1.  **主布局 (`AppLayout.vue`):** 包含导航栏和内容区域 (`<router-view>`)。 (初始结构已完成)
    2.  **基础路由:** 定义登录 (`/login`)、注册 (`/register`) 和仪表盘 (`/dashboard`) 占位路由。 (已完成)
    3.  **认证状态管理 (Pinia - `authStore.ts`):** 管理用户、Token、认证状态。 (初始结构已完成)
    4.  **认证服务 (`authService.ts`):** 封装登录/注册API调用。 (初始结构已完成)

**阶段 3: 用户认证流程 (User Authentication Flow)** ✅

*   **目标:** 完成用户登录、注册功能及路由保护。
*   **主要任务:**
    1.  **登录页面 (`LoginPage.vue`) 与登录逻辑: UI完成；核心登录逻辑已移入 `authStore` action。** ✅
    2.  **注册页面 (`RegisterPage.vue`): 初始UI和调用 `authStore` 的注册逻辑已完成。** ✅
    3.  **登出功能: 已在 AppLayout 中实现，包括条件UI和调用 `authStore.logout()`。** ✅
    4.  **路由守卫 (`router/index.ts`): 已添加全局前置守卫，实现基于认证状态的页面访问控制。** ✅
    5.  **状态恢复: 已实现应用加载时检查持久化的Token，并重新获取用户信息以恢复完整登录状态。** ✅

**阶段 4: API客户端与全局错误处理 (API Client & Global Error Handling)** ✅

*   **目标:** 建立统一的API请求客户端并实现全局错误响应处理。
*   **主要任务:**
    1.  **Axios实例 (`apiClient.ts`):** ✅
        *   配置 `baseURL`。 ✅
        *   请求拦截器：自动添加 JWT。 ✅
        *   响应拦截器：根据 `FRONTEND_PLAN.md` 处理通用API错误 (如 401, 403, 5xx)。 ✅
    2.  **消息服务 (`messageService.ts`):** ✅
        *   创建全局消息提示服务。
        *   集成Naive UI的消息API。
    3.  **更新认证服务 (`authService.ts`):** ✅
        *   使用 `apiClient` 替换模拟实现。
        *   完善错误处理。

**阶段 5: API Token 管理 (API Token Management)** ✅

*   **目标:** 实现用户对自己AI API Token的CRUD操作。
*   **主要任务:**
    1.  **Token状态管理 (Pinia - `tokenStore.ts`)** ✅
        * 实现Token列表的状态管理
        * 提供获取、创建、删除Token的方法
        * 管理加载状态和错误处理
    2.  **Token服务 (`tokenService.ts`)** ✅
        * 封装与后端API的交互
        * 定义Token相关的接口和类型
        * 提供Token值掩码处理等辅助功能
    3.  **Token管理页面 (`/tokens`):** ✅
        *   列表展示 (Naive UI Table)
        *   新建表单/模态框 (Naive UI Modal/Form)
        *   删除确认 (Naive UI Modal)
    4.  **路由配置:** ✅
        *   添加Token管理页面路由
        *   设置为需要认证的路由
    5.  **导航菜单:** ✅
        *   在AppLayout中添加导航菜单
        *   添加Token管理页面的链接

**阶段 6: Prompt 管理 (Prompt Management)** ✅

*   **目标:** 实现用户对常用Prompt的CRUD操作。
*   **主要任务:**
    1.  **Prompt状态管理 (Pinia - `promptStore.ts`)**
    2.  **Prompt服务 (`promptService.ts`)**
    3.  **Prompt列表页 (`/prompts`)**
    4.  **新建Prompt页 (`/prompts/new`)**
    5.  **编辑Prompt页 (`/prompts/:id/edit`)**

**阶段 7: 网页内容摘要 (Web Article Summarization)**

*   **目标:** 实现核心的网页摘要功能。
*   **主要任务:**
    1.  **摘要状态管理 (Pinia - `summarizeStore.ts`)**
    2.  **摘要服务 (`summarizeService.ts`)**
    3.  **摘要页面 (`/summarize` 或集成到Dashboard):**
        *   URL输入。
        *   (可选) Token选择。
        *   结果展示与加载状态。
        *   处理特定错误 (如用户Token无效)。

**阶段 8: UI完善与组件化 (UI Polish & Componentization)**

*   **目标:** 优化整体用户体验，提炼可复用组件。
*   **主要任务:**
    1.  **通用组件:** 根据需要创建或优化 (如确认模态框、页面标题等)。
    2.  **导航栏完善:** 添加实际链接，根据认证状态显示不同内容。
    3.  **样式统一:** 确保视觉一致性。

**阶段 9: 测试与收尾 (Testing & Final Touches)**

*   **目标:** 确保功能完整性和稳定性。
*   **主要任务:**
    1.  **功能测试:** 全面测试所有用户流程和边缘情况。
    2.  **代码审查与重构:** 提升代码质量。