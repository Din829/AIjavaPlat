# AI业务支持平台 - 前端架构图

## 项目结构架构（精确到方法）
```
my-ai-platform-frontend/src/
├── components/                    # 可复用UI组件
│   ├── LinkProcessingHistory.vue # 链接处理历史记录组件
│   │   ├── handleRefresh()        # 刷新任务列表
│   │   ├── handleTaskSelect()     # 选择任务查看详情
│   │   ├── handleTaskDelete()     # 删除指定任务
│   │   ├── clearCompleted()       # 批量清理已完成任务
│   │   ├── formatTime()           # 格式化时间显示
│   │   ├── handlePageChange()     # 处理分页变更
│   │   └── handlePageSizeChange() # 处理页面大小变更
│   ├── LinkProcessingProgress.vue # 链接处理进度显示组件
│   │   ├── getStatusTagType()     # 获取状态标签类型
│   │   ├── getStatusText()        # 获取状态文本
│   │   ├── getStepsStatus()       # 获取步骤状态
│   │   ├── getProgressStatus()    # 获取进度状态
│   │   ├── getStepDescription()   # 获取步骤描述
│   │   ├── formatTime()           # 格式化时间
│   │   ├── formatDuration()       # 格式化时长
│   │   └── calculateDuration()    # 计算处理时长
│   ├── LinkProcessingResult.vue   # 链接处理结果展示组件
│   │   ├── copyResult()           # 复制处理结果
│   │   ├── copyTranscription()    # 复制转写文本
│   │   ├── copyRawData()          # 复制原始JSON数据
│   │   ├── downloadResult()       # 下载结果为Markdown文件
│   │   ├── exportSegments()       # 导出转写分段数据
│   │   ├── openOriginalLink()     # 打开原始链接
│   │   ├── formatTime()           # 格式化时间戳
│   │   ├── formatDuration()       # 格式化视频时长
│   │   └── calculateDuration()    # 计算处理时长
│   └── RichTextDisplay.vue       # 富文本显示组件，支持图像内嵌
│       ├── parsedContent          # 计算属性：解析富文本内容和图像标记
│       └── handleImageError()     # 处理图像加载错误
├── layouts/                      # 页面布局组件
│   └── AppLayout.vue             # 主应用布局，包含导航和页脚
│       ├── handleLogout()        # 处理用户登出逻辑
│       └── checkAuthStatus()     # 检查用户认证状态
├── router/                       # 路由配置
│   └── index.ts                  # Vue Router路由定义和守卫
│       ├── beforeEach()          # 全局路由前置守卫
│       └── createRouter()        # 创建路由实例
├── services/                     # API服务层
│   ├── apiClient.ts              # Axios全局配置和拦截器
│   │   ├── requestInterceptor()  # 请求拦截器，自动添加JWT
│   │   └── responseInterceptor() # 响应拦截器，统一错误处理
│   ├── authService.ts            # 用户认证相关API
│   │   ├── loginUser()           # 用户登录接口
│   │   ├── registerUser()        # 用户注册接口
│   │   └── fetchCurrentUser()    # 获取当前用户信息
│   ├── linkProcessingService.ts  # 链接处理服务API
│   │   ├── analyzeLink()         # 分析链接类型和支持情况
│   │   ├── createTask()          # 创建链接处理任务
│   │   ├── getTaskDetail()       # 获取任务详情
│   │   ├── getTaskList()         # 获取用户任务列表
│   │   ├── deleteTask()          # 删除任务
│   │   ├── pollTaskStatus()      # 轮询任务状态直到完成
│   │   └── checkServiceHealth()  # 检查微服务健康状态
│   ├── messageService.ts         # 消息通知服务API
│   │   ├── showSuccess()         # 显示成功消息
│   │   ├── showError()           # 显示错误消息
│   │   ├── showWarning()         # 显示警告消息
│   │   └── showInfo()            # 显示信息消息
│   ├── ocrService.ts             # OCR文档处理API
│   │   ├── uploadFile()          # 上传文件并处理
│   │   ├── getTaskStatus()       # 获取OCR任务状态
│   │   ├── getTaskResult()       # 获取OCR处理结果
│   │   └── getUserTasks()        # 获取用户所有OCR任务
│   ├── promptService.ts          # Prompt管理API
│   │   ├── getPrompts()          # 获取用户Prompt列表
│   │   ├── getPromptById()       # 根据ID获取Prompt
│   │   ├── createPrompt()        # 创建新Prompt
│   │   ├── updatePrompt()        # 更新Prompt
│   │   └── deletePrompt()        # 删除Prompt
│   ├── tokenService.ts           # API Token管理API
│   │   ├── getTokens()           # 获取用户Token列表
│   │   ├── createToken()         # 创建新Token
│   │   ├── deleteToken()         # 删除Token
│   │   └── maskTokenValue()      # Token值掩码处理
│   └── summarizationService.ts   # 网页摘要API
│       └── summarizeUrl()        # 网页URL摘要处理
├── stores/                       # Pinia状态管理
│   ├── authStore.ts              # 用户认证状态管理
│   │   ├── login()               # 登录并更新状态
│   │   ├── register()            # 注册并更新状态
│   │   ├── logout()              # 登出并清除状态
│   │   ├── getCurrentUser()      # 获取并更新用户信息
│   │   └── checkAuthStatus()     # 检查认证状态
│   ├── linkProcessingStore.ts    # 链接处理状态管理
│   │   ├── analyzeLink()         # 分析链接类型
│   │   ├── processLink()         # 创建并开始处理任务
│   │   ├── loadTaskList()        # 加载任务列表
│   │   ├── getTaskDetail()       # 获取任务详情
│   │   ├── deleteTask()          # 删除任务
│   │   ├── checkServiceHealth()  # 检查服务健康状态
│   │   ├── clearCurrentProcessing() # 清空当前处理状态
│   │   ├── clearLinkAnalysis()   # 清空链接分析结果
│   │   ├── updateProgress()      # 更新处理进度
│   │   └── updateProgressFromTask() # 根据任务状态更新进度
│   ├── messageStore.ts           # 消息状态管理
│   │   ├── addMessage()          # 添加消息到队列
│   │   ├── removeMessage()       # 移除指定消息
│   │   ├── clearMessages()       # 清空所有消息
│   │   └── showNotification()    # 显示通知消息
│   ├── ocrStore.ts               # OCR功能状态管理
│   │   ├── uploadFile()          # 上传文件并更新状态
│   │   ├── getTaskStatus()       # 获取任务状态并更新
│   │   ├── getTaskResult()       # 获取结果并更新状态
│   │   ├── getUserTasks()        # 获取用户任务列表
│   │   ├── startPolling()        # 开始轮询任务状态
│   │   ├── stopPolling()         # 停止轮询
│   │   └── reset()               # 重置OCR状态
│   ├── promptStore.ts            # Prompt管理状态
│   │   ├── fetchPrompts()        # 获取并缓存Prompt列表
│   │   ├── createPrompt()        # 创建Prompt并更新列表
│   │   ├── updatePrompt()        # 更新Prompt并刷新
│   │   └── deletePrompt()        # 删除Prompt并更新列表
│   ├── tokenStore.ts             # Token管理状态
│   │   ├── fetchTokens()         # 获取并缓存Token列表
│   │   ├── createToken()         # 创建Token并更新列表
│   │   └── deleteToken()         # 删除Token并更新列表
│   └── summarizationStore.ts     # 网页摘要状态管理
│       ├── summarizeUrl()        # 执行摘要并更新结果
│       ├── setLoading()          # 设置加载状态
│       └── setError()            # 设置错误状态
├── types/                        # TypeScript类型定义
│   ├── linkProcessing.ts         # 链接处理相关类型定义
│   │   ├── LinkType              # 链接类型枚举（VIDEO/WEBPAGE）
│   │   ├── TaskStatus            # 任务状态枚举
│   │   ├── LinkProcessRequest    # 链接处理请求接口
│   │   ├── LinkAnalysisResponse  # 链接分析响应接口
│   │   ├── TaskCreateResponse    # 任务创建响应接口
│   │   ├── TranscriptionSegment  # 转写分段信息接口
│   │   ├── TranscriptionResult   # 转写结果接口
│   │   ├── VideoMetadata         # 视频元数据接口
│   │   ├── TaskDetailResponse    # 任务详情响应接口
│   │   ├── TaskListResponse      # 任务列表响应接口
│   │   ├── ProcessingProgress    # 处理进度信息接口
│   │   ├── LinkProcessingState   # 链接处理状态接口
│   │   ├── LanguageOption        # 语言选项接口
│   │   ├── SUPPORTED_PLATFORMS   # 支持的视频平台常量
│   │   ├── LANGUAGE_OPTIONS      # 语言选项列表常量
│   │   └── PROCESSING_STEPS      # 处理步骤定义常量
│   └── ocr.ts                    # OCR相关类型定义
│       ├── OcrTaskStatus         # OCR任务状态枚举
│       ├── OcrUploadRequest      # OCR上传请求接口
│       └── OcrTaskResponse       # OCR任务响应接口
└── views/                        # 页面级组件
    ├── DashboardPage.vue         # 仪表盘主页面
    │   └── onMounted()           # 页面加载时初始化
    ├── LinkProcessingPage.vue    # 智能链接处理页面
    │   ├── handleUrlBlur()       # 处理URL输入失焦事件
    │   ├── getLinkAnalysisTitle() # 获取链接分析标题
    │   ├── handleSubmit()        # 处理表单提交
    │   ├── handleClear()         # 清空表单和状态
    │   ├── handleTaskSelected()  # 处理任务选择事件
    │   ├── loadTaskList()        # 加载任务列表
    │   └── checkServiceHealth()  # 检查服务健康状态
    ├── LoginPage.vue             # 用户登录页面
    │   ├── handleLogin()         # 处理登录表单提交
    │   └── validateForm()        # 验证登录表单
    ├── OcrPage.vue               # OCR文档处理页面
    │   ├── handleFileUpload()    # 处理文件上传
    │   ├── handleProcessingOptions() # 处理OCR选项配置
    │   ├── startProcessing()     # 开始OCR处理
    │   └── displayResults()      # 显示处理结果
    ├── PromptsPage.vue           # Prompt管理页面
    │   ├── fetchPrompts()        # 获取Prompt列表
    │   ├── handleCreate()        # 处理创建Prompt
    │   ├── handleEdit()          # 处理编辑Prompt
    │   └── handleDelete()        # 处理删除Prompt
    ├── RegisterPage.vue          # 用户注册页面
    │   ├── handleRegister()      # 处理注册表单提交
    │   └── validateForm()        # 验证注册表单
    ├── SummarizationPage.vue     # 网页摘要页面
    │   ├── handleSummarize()     # 处理摘要请求
    │   └── selectToken()         # 选择API Token
    └── TokensPage.vue            # Token管理页面
        ├── fetchTokens()         # 获取Token列表
        ├── handleCreate()        # 处理创建Token
        └── handleDelete()        # 处理删除Token
```

## 技术栈
- **框架**: Vue.js 3 + TypeScript + Vite
- **UI库**: Naive UI
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP客户端**: Axios