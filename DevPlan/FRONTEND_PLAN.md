# 社内業務サポートAIプラットフォーム - 前端开发计划 (正式版)

## MVP 完成状况总结

前端 MVP 开发阶段已实现以下核心功能：
- 项目初始化 (Vite, Vue3, TypeScript) 及核心库集成 (Pinia, Vue Router, Naive UI, Axios)。
- 完整的用户认证流程 (登录、注册、登出、路由守卫、状态持久化)。
- API Token 管理功能 (列表、创建、删除)。
- Prompt 管理功能 (列表、创建、编辑、删除)。
- 网页内容摘要功能，支持用户选择 API Token。
- OCR文档处理功能，包括文件上传、处理选项配置、状态显示和结果展示。
- API 服务层、状态管理模块化以及基础的全局错误处理。

主要的 MVP 收尾工作包括：解决已知的 TypeScript 类型检查问题 (已通过创建单独的类型定义文件解决部分问题) 和 Naive UI 全局消息提示的渲染 bug，以及完成代码审查、构建测试。

## 当前未解决的问题

在OCR前端功能实现过程中，我们遇到了以下尚未解决的问题：

1. **类型导入错误**：
   ```
   SyntaxError: The requested module '/src/services/ocrService.ts?t=1747740415669' does not provide an export named 'OcrTaskResponse' (at ocrStore.ts:2:22)
   ```

   尽管我们已经创建了单独的类型定义文件`types/ocr.ts`并修改了导入路径，但仍然存在类型导入错误。这可能是由于Vite的热更新机制或TypeScript编译配置导致的。

2. **解决方案尝试**：
   - 创建了单独的类型定义文件，将所有OCR相关的类型定义放在一个地方
   - 修改了导入方式，使用`import type`语法
   - 修改了错误处理代码，确保类型安全
   - 直接使用`messageService`而不是`messageStore`，减少依赖

3. **下一步解决方案**：
   - 检查TypeScript配置，确保类型定义正确加载
   - 考虑使用更简单的类型定义，减少复杂性
   - 可能需要重新构建项目，清除缓存
   - 考虑使用JavaScript而不是TypeScript实现部分功能，绕过类型检查问题

---

## 正式版开发规划

### 已完成模块：OCR文档处理与交互界面

**总体方向：**
为用户提供流畅的文档上传体验，清晰地展示后端OCR服务处理后的文档内容，并逐步构建支持更高级文档交互（如内容选择、高亮、未来可能的文档问答）的用户界面。

**已实现的功能：**

1.  **核心页面与组件：**
    *   **已实现 `OcrPage.vue`：** 作为OCR功能主界面，提供完整的文档上传、处理和结果展示功能。
    *   **文件上传组件：** 使用Naive UI的`n-upload`和`n-upload-dragger`组件，支持拖放和文件选择，并提供文件格式限制。
    *   **处理选项配置：** 提供OCR处理选项的配置界面，包括：
        *   使用PyPDF2提取文本
        *   使用Docling进行OCR
        *   使用Gemini进行内容分析
        *   强制OCR处理（即使PDF包含文本）
        *   语言选择（自动检测、中文、英文、日文、韩文、中英混合）
    *   **处理状态与反馈：** 使用Naive UI的`n-spin`和`messageService`，清晰展示上传进度、后端处理状态（等待、处理中、成功、失败）及错误信息。
    *   **结果展示组件：** 使用Naive UI的`n-tabs`、`n-tab-pane`、`n-scrollbar`和`n-data-table`组件，以多标签页的形式展示OCR处理结果，包括：
        *   文本内容标签页：显示提取的文本内容
        *   表格内容标签页：使用`n-data-table`展示提取的表格数据
        *   内容分析标签页：显示Gemini生成的内容分析结果
        *   原始JSON标签页：显示后端返回的完整JSON数据

2.  **API服务层 (`src/services/ocrService.ts`)：**
    *   已实现OCR服务API的封装，包括：
        *   `uploadFile`：上传文件并处理
        *   `getTaskStatus`：获取任务状态
        *   `getTaskResult`：获取任务结果
        *   `getUserTasks`：获取用户的所有OCR任务
    *   定义了OCR相关的类型，包括：
        *   `OcrTaskStatus`：OCR任务状态枚举
        *   `OcrUploadRequest`：OCR上传请求参数接口
        *   `OcrTaskResponse`：OCR任务响应接口

3.  **状态管理 (`src/stores/ocrStore.ts` - Pinia)：**
    *   已实现OCR状态管理，包括：
        *   管理文件上传状态
        *   管理OCR任务状态
        *   处理异步轮询
        *   管理后端返回的结果数据
    *   实现了以下Actions：
        *   `uploadFile`：上传文件并处理
        *   `getTaskStatus`：获取任务状态
        *   `getTaskResult`：获取任务结果
        *   `getUserTasks`：获取用户的所有OCR任务
        *   `startPolling`：开始轮询任务状态
        *   `stopPolling`：停止轮询任务状态
        *   `reset`：重置状态

4.  **类型定义 (`src/types/ocr.ts`)：**
    *   为了解决循环依赖和导入问题，创建了单独的类型定义文件，包括：
        *   `OcrTaskStatus`：OCR任务状态枚举
        *   `OcrUploadRequest`：OCR上传请求参数接口
        *   `OcrTaskResponse`：OCR任务响应接口

5.  **数据处理与渲染逻辑：**
    *   实现了对后端返回的JSON数据的解析和渲染，包括：
        *   文本内容的展示
        *   表格数据的展示
        *   内容分析结果的展示
    *   实现了简单的格式化处理，将Markdown格式的内容转换为HTML进行展示

**下一步计划：**

1.  **性能优化：**
    *   优化大型文档的渲染性能
    *   实现更高效的表格数据处理

2.  **用户体验增强：**
    *   实现文本选择与复制功能
    *   提供下载和分享功能，方便用户保存和分享OCR结果
    *   实现多文档管理界面，支持历史记录查询

3.  **功能增强：**
    *   支持更多文档格式的预览
    *   实现更高级的文档交互功能
    *   实现实时进度反馈机制

4.  **(远期) 文档问答界面集成：**
    *   配合后端文档问答功能的实现，设计和开发相应的用户问答交互界面