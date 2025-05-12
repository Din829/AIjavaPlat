# AI 业务支持平台 - 前端项目架构文档

## 1. 项目概述

本项目的前端部分旨在为"AI 业务支持平台"提供一个现代化、响应式且用户友好的Web界面。用户通过此界面与后端服务进行交互，以使用平台提供的各项AI辅助功能，如用户认证、API Token管理、Prompt管理以及网页内容摘要等。前端强调组件化开发、清晰的状态管理和高效的API交互。

## 2. 技术栈

-   **语言**: TypeScript
-   **核心框架**: Vue.js 3 (使用 `<script setup>` 组合式 API)
-   **构建工具**: Vite
-   **UI组件库**: Naive UI
-   **路由管理**: Vue Router
-   **状态管理**: Pinia
-   **HTTP客户端**: Axios (已实现，用于API服务层)
-   **代码规范与格式化**: ESLint, Prettier

## 3. 项目结构 (`my-ai-platform-frontend/src/`)

```
src/
├── assets/            # 静态资源 (图片、字体等，会被Vite处理)
├── components/        # 可复用的全局UI组件
├── layouts/           # 页面布局组件
│   └── AppLayout.vue  # 主应用布局，包含导航菜单、页眉和页脚
├── router/            # 路由配置
│   └── index.ts       # 定义所有路由规则，包括认证保护
├── services/          # API服务封装
│   ├── apiClient.ts   # Axios实例配置，包含拦截器和错误处理
│   ├── authService.ts # 认证相关API (登录、注册)
│   ├── messageService.ts # 全局消息提示服务
│   └── tokenService.ts   # API Token管理服务
├── stores/            # Pinia状态管理模块
│   ├── authStore.ts   # 用户认证状态管理
│   └── tokenStore.ts  # API Token状态管理
├── utils/             # 工具函数
├── views/             # 页面级组件
│   ├── DashboardPage.vue # 仪表盘页面
│   ├── LoginPage.vue     # 登录页面
│   ├── RegisterPage.vue  # 注册页面
│   └── TokensPage.vue    # API Token管理页面
├── App.vue            # Vue应用的根组件
├── main.ts            # 应用主入口文件 (初始化Vue, Pinia, Router, Naive UI等)
├── style.css          # 全局CSS样式文件
└── vite-env.d.ts      # Vite相关的环境变量和类型声明
```

## 4. 核心模块与概念

### 4.1. 路由管理 (`vue-router`)
-   **配置**: `src/router/index.ts` 集中管理所有页面路由。
-   **特性**:
    -   使用HTML5 history模式。
    -   页面组件懒加载 (`() => import(...)`) 以优化初始加载性能。
    -   通过路由元信息 (`meta`字段) 实现如页面访问权限控制。
    -   支持编程式导航和 `<router-link>` 组件。

### 4.2. 状态管理 (`pinia`)
-   **配置**: `src/stores/` 目录下按功能模块组织各个store (例如 `authStore.ts`)。
-   **特性**:
    -   集中管理应用的全局状态，如用户认证信息、Token、用户信息等。
    -   提供 `state`, `getters`, `actions` 来定义和操作状态。
    -   类型安全，与TypeScript良好集成。
    -   支持Devtools进行状态调试。
    -   通过 `localStorage` 等方式实现状态持久化 (例如认证Token)，并包含在应用加载时恢复用户信息的逻辑。

### 4.3. UI组件系统
-   **UI库**: Naive UI 提供了一套丰富、美观且可定制的Vue 3组件。
    -   全局注册在 `src/main.ts`。
    -   Provider组件 (`NMessageProvider`, `NDialogProvider`等) 在 `src/App.vue` 中包裹根应用，以支持全局API调用。
-   **自定义组件**:
    -   **页面组件 (`src/views/`)**: 对应各个路由，是具体页面的容器和逻辑单元。
    -   **布局组件 (`src/layouts/`)**: 定义应用的整体页面结构，如包含页眉、页脚、侧边栏的布局。
    -   **可复用组件 (`src/components/`)**: 存放应用中通用的、可被多处复用的UI片段。
-   **样式**:
    -   全局样式在 `src/style.css` 中定义。
    -   组件内部使用 `<style scoped>` 来编写局部作用域的CSS，避免样式冲突。

### 4.4. API服务层 (`axios`)
-   **配置**: `src/services/` 目录下按后端资源或功能模块组织API服务文件。
-   **实现**:
    -   `apiClient.ts`: 全局Axios实例，配置了基础URL、请求/响应拦截器，实现了JWT Token自动附加和统一错误处理。
    -   `authService.ts`: 封装认证相关API (登录、注册)，提供类型安全的接口。
    -   `tokenService.ts`: 封装API Token管理相关API，提供Token值掩码处理等辅助功能。
    -   `messageService.ts`: 全局消息提示服务，集成Naive UI的消息API。
-   **特点**:
    -   所有服务都提供清晰、类型安全的函数供上层 (如Pinia stores或组件) 调用。
    -   统一的错误处理机制，包括HTTP状态码处理和用户友好的错误提示。
    -   详细的日志记录，便于调试和问题排查。

### 4.5. 构建与开发 (`vite`)
-   **配置文件**: `vite.config.ts` 用于配置Vite的行为，如插件 (Vue插件)、开发服务器、构建选项等。
-   **开发服务器**: `npm run dev` 启动，提供快速的模块热重载 (HMR)。
-   **生产构建**: `npm run build` 将应用打包为静态资源，用于部署。

## 5. 关键文件及其作用

-   **`index.html`**: 单页应用的HTML入口骨架，Vue应用挂载点。
-   **`src/main.ts`**: 应用的启动入口，初始化Vue实例、插件 (Router, Pinia, Naive UI) 和全局样式。
-   **`src/App.vue`**: Vue应用的根组件，承载全局布局和`<router-view>`。
-   **`src/router/index.ts`**: 定义所有路由规则，包括认证保护和路由元信息。
-   **`src/layouts/AppLayout.vue`**: 主应用布局，包含导航菜单、页眉和页脚。
-   **`src/stores/authStore.ts`**: 管理用户认证相关的状态，包括登录、注册和登出逻辑。
-   **`src/stores/tokenStore.ts`**: 管理API Token相关的状态，包括获取、创建和删除Token。
-   **`src/services/apiClient.ts`**: 全局Axios实例，配置了拦截器和错误处理。
-   **`src/services/authService.ts`**: 封装认证相关的API请求。
-   **`src/services/tokenService.ts`**: 封装API Token管理相关的API请求。
-   **`src/services/messageService.ts`**: 全局消息提示服务。
-   **`src/views/LoginPage.vue`**: 用户登录页面。
-   **`src/views/RegisterPage.vue`**: 用户注册页面。
-   **`src/views/DashboardPage.vue`**: 仪表盘页面。
-   **`src/views/TokensPage.vue`**: API Token管理页面。
-   **`eslint.config.js`**: ESLint配置文件，用于代码规范检查。
-   **`.prettierrc.json`**: Prettier配置文件，用于代码格式化。
-   **`tsconfig.json` / `tsconfig.app.json` / `tsconfig.node.json`**: TypeScript配置文件。
-   **`package.json`**: 项目依赖和脚本配置。

## 6. 开发流程与规则 (遵循 `FRONTEND_PLAN.md` 中的开发规则)

1.  **组件化开发**: 将UI拆分为可管理的、可复用的组件。
2.  **状态集中管理**: 全局或模块共享的状态通过Pinia进行管理。
3.  **服务层分离**: API请求通过专门的服务层进行封装。
4.  **代码规范**: 遵循ESLint和Prettier配置，确保代码质量和风格一致性。
5.  **中文注释**: 为关键逻辑、功能和复杂语法添加清晰的中文注释。
6.  **迭代计划**: 按照 `FRONTEND_PLAN.md` 中的阶段逐步推进。
7.  **先分析后编码**: 对需求和实现方案进行思考后再开始编码。
8.  **细致严谨**: 注重代码细节和可维护性。

## 7. 当前实现状态

截至目前，前端项目已完成以下功能模块：

1. **基础架构**:
   - 项目初始化与配置
   - 路由系统
   - 状态管理
   - API服务层

2. **用户认证**:
   - 登录功能
   - 注册功能
   - JWT Token管理
   - 认证状态持久化

3. **API Token管理**:
   - Token列表展示
   - Token创建
   - Token删除
   - Token值掩码处理

4. **UI组件**:
   - 应用布局 (包含导航菜单)
   - 登录/注册表单
   - Token管理界面
   - 全局消息提示

5. **安全与错误处理**:
   - 路由认证保护
   - API请求错误处理
   - 表单验证
   - 详细的日志记录

后续开发计划包括Prompt管理、网页内容摘要等功能，详见 `FRONTEND_PLAN.md`。

---
本文档将随着开发的进行而更新。