# AI业务支持平台 - 后端架构图

## 项目结构架构（精确到方法）
```
AIplatJava/src/main/java/com/ding/aiplatjava/
├── config/                        # Spring Boot配置类
│   ├── MyBatisConfig.java         # MyBatis数据库配置
│   │   └── sqlSessionFactory()   # 创建SQL会话工厂
│   ├── OcrConfig.java             # OCR服务配置
│   │   ├── ocrServiceUrl()        # OCR服务URL配置
│   │   └── restTemplate()         # HTTP客户端配置
│   ├── SecurityConfig.java        # Spring Security安全配置
│   │   ├── filterChain()          # 配置安全过滤链
│   │   └── passwordEncoder()      # 密码加密器配置
│   └── VideoProcessingConfig.java # 视频处理微服务配置
│       ├── videoProcessingRestTemplate() # 视频处理服务HTTP客户端
│       ├── whisperServiceRestTemplate() # Whisper服务HTTP客户端
│       └── configureTimeouts()    # 配置超时设置
├── controller/                    # REST API控制器层
│   ├── ApiTokenController.java    # API Token管理控制器
│   │   ├── getCurrentUserTokens() # 获取当前用户所有Token
│   │   ├── createToken()          # 创建新的API Token
│   │   └── deleteToken()          # 删除指定Token
│   ├── AuthController.java        # 用户认证控制器
│   │   ├── login()                # 用户登录端点
│   │   └── register()             # 用户注册端点
│   ├── LinkProcessingController.java # 链接处理控制器
│   │   ├── analyzeLink()          # 分析链接类型和支持情况
│   │   ├── processLink()          # 提交链接处理任务
│   │   ├── getTaskDetail()        # 获取任务详情
│   │   ├── getUserTasks()         # 获取用户任务列表
│   │   ├── deleteTask()           # 删除处理任务
│   │   └── checkServiceHealth()   # 检查微服务健康状态
│   ├── OcrController.java         # OCR文档处理控制器
│   │   ├── uploadFile()           # 上传文件并开始OCR
│   │   ├── getTaskStatus()        # 获取OCR任务状态
│   │   └── getTaskResult()        # 获取OCR处理结果
│   ├── PromptController.java      # Prompt管理控制器
│   │   ├── getCurrentUserPrompts() # 获取用户Prompt列表
│   │   ├── getPromptById()        # 根据ID获取Prompt
│   │   ├── createPrompt()         # 创建新Prompt
│   │   ├── updatePrompt()         # 更新现有Prompt
│   │   └── deletePrompt()         # 删除Prompt
│   ├── SummarizationController.java # 网页摘要控制器
│   │   └── summarizeContent()     # 网页内容摘要端点
│   └── UserController.java        # 用户信息控制器
│       └── getCurrentUser()       # 获取当前登录用户信息
├── entity/                        # 数据库实体类
│   ├── ApiToken.java              # API Token实体
│   │   ├── getId()                # 获取Token ID
│   │   ├── getUserId()            # 获取用户ID
│   │   ├── getProvider()          # 获取提供商名称
│   │   └── getTokenValue()        # 获取Token值
│   ├── OcrTask.java               # OCR任务实体
│   │   ├── getTaskId()            # 获取任务ID
│   │   ├── getUserId()            # 获取用户ID
│   │   ├── getStatus()            # 获取任务状态
│   │   └── getResult()            # 获取处理结果
│   ├── Prompt.java                # Prompt实体
│   │   ├── getId()                # 获取Prompt ID
│   │   ├── getUserId()            # 获取用户ID
│   │   ├── getTitle()             # 获取Prompt标题
│   │   └── getContent()           # 获取Prompt内容
│   ├── User.java                  # 用户实体
│   │   ├── getId()                # 获取用户ID
│   │   ├── getUsername()          # 获取用户名
│   │   ├── getEmail()             # 获取邮箱
│   │   └── getPassword()          # 获取密码哈希
│   └── VideoTranscriptionTask.java # 视频转写任务实体
│       ├── getId()                # 获取任务ID
│       ├── getTaskId()            # 获取任务UUID
│       ├── getUrl()               # 获取处理URL
│       ├── getContentType()       # 获取内容类型
│       ├── getStatus()            # 获取任务状态
│       └── getResult()            # 获取处理结果
├── mapper/                        # MyBatis数据访问层
│   ├── ApiTokenMapper.java        # API Token数据访问
│   │   ├── selectById()           # 根据ID查询Token
│   │   ├── selectByUserId()       # 根据用户ID查询
│   │   ├── insert()               # 插入新Token
│   │   └── deleteByIdAndUserId()  # 删除用户Token
│   ├── OcrTaskMapper.java         # OCR任务数据访问
│   │   ├── selectByTaskId()       # 根据任务ID查询
│   │   ├── insert()               # 插入新任务
│   │   ├── updateStatus()         # 更新任务状态
│   │   ├── updateResult()         # 更新处理结果
│   │   └── updateError()          # 更新错误信息
│   ├── PromptMapper.java          # Prompt数据访问
│   │   ├── selectById()           # 根据ID查询Prompt
│   │   ├── selectByUserId()       # 根据用户ID查询
│   │   ├── insert()               # 插入新Prompt
│   │   ├── updateById()           # 更新Prompt
│   │   └── deleteById()           # 删除Prompt
│   ├── UserMapper.java            # 用户数据访问
│   │   ├── selectById()           # 根据ID查询用户
│   │   ├── selectByUsername()     # 根据用户名查询
│   │   ├── selectByEmail()        # 根据邮箱查询
│   │   └── insert()               # 插入新用户
│   └── VideoTranscriptionTaskMapper.java # 视频转写任务数据访问
│       ├── selectById()           # 根据ID查询任务
│       ├── selectByTaskId()       # 根据任务UUID查询
│       ├── selectByUserId()       # 根据用户ID查询
│       ├── insert()               # 插入新任务
│       ├── updateStatus()         # 更新任务状态
│       └── updateResult()         # 更新处理结果
├── security/                      # Spring Security安全组件
│   └── JwtAuthFilter.java         # JWT认证过滤器
│       ├── doFilterInternal()     # 执行JWT认证逻辑
│       ├── getJwtFromRequest()    # 从请求中提取JWT
│       └── validateToken()        # 验证JWT有效性
├── service/                       # 业务逻辑服务接口层
│   ├── AiService.java             # AI服务接口
│   │   ├── summarizeContent()     # 内容摘要接口
│   │   ├── summarizeText()        # 文本摘要接口
│   │   └── analyzeContent()       # 内容分析接口
│   ├── ApiTokenService.java       # API Token服务接口
│   │   ├── createToken()          # 创建Token接口
│   │   ├── getTokensByUserId()    # 获取用户Token接口
│   │   ├── getDecryptedTokenValue() # 获取解密Token值接口
│   │   ├── getDecryptedTokenValueByProvider() # 根据提供商获取Token
│   │   └── deleteToken()          # 删除Token接口
│   ├── LinkAnalysisService.java   # 链接分析服务接口
│   │   ├── analyzeUrl()           # 分析URL类型接口
│   │   ├── isVideoUrl()           # 判断是否视频链接
│   │   ├── extractWebPageTitle()  # 提取网页标题
│   │   └── getContentType()       # 获取内容类型
│   ├── LinkProcessingService.java # 链接处理服务接口
│   │   ├── analyzeLink()          # 分析链接类型和支持情况
│   │   ├── processLink()          # 处理链接接口
│   │   ├── getTaskDetail()        # 获取任务详情接口
│   │   ├── getUserTasks()         # 获取用户任务接口
│   │   ├── deleteTask()           # 删除任务接口
│   │   └── checkServiceHealth()   # 检查微服务健康状态
│   ├── VideoProcessingService.java # 视频处理服务接口
│   │   ├── processVideo()         # 处理视频获取元数据
│   │   ├── transcribeAudio()      # 转写音频
│   │   ├── processVideoComplete() # 完整视频处理流程
│   │   ├── checkVideoServiceHealth() # 检查视频服务健康状态
│   │   ├── checkWhisperServiceHealth() # 检查Whisper服务健康状态
│   │   └── getVideoServiceUrl()   # 获取视频服务URL
│   ├── OcrService.java            # OCR服务接口
│   │   ├── uploadAndProcess()     # 上传并处理文档接口
│   │   ├── processOcrTaskAsync()  # 异步处理OCR任务
│   │   ├── getTaskStatus()        # 获取任务状态接口
│   │   └── getTaskResult()        # 获取处理结果接口
│   ├── PromptService.java         # Prompt服务接口
│   │   ├── getPromptById()        # 根据ID获取Prompt
│   │   ├── getPromptsByUserId()   # 获取用户Prompt列表
│   │   ├── createPrompt()         # 创建Prompt接口
│   │   ├── updatePrompt()         # 更新Prompt接口
│   │   └── deletePrompt()         # 删除Prompt接口
│   ├── UserService.java           # 用户服务接口
│   │   ├── registerUser()         # 用户注册接口
│   │   ├── findByUsername()       # 根据用户名查找
│   │   ├── findByEmail()          # 根据邮箱查找
│   │   └── findById()             # 根据ID查找用户
│   └── WebContentService.java     # 网页内容服务接口
│       ├── fetchWebContent()      # 获取网页内容接口
│       ├── extractText()          # 提取文本内容
│       └── parseHtml()            # 解析HTML内容
├── service/impl/                  # 业务逻辑服务实现层
│   ├── AiServiceImpl.java         # AI服务实现
│   │   ├── summarizeContent()     # 调用AI进行内容摘要
│   │   ├── summarizeText()        # 调用AI进行文本摘要
│   │   ├── analyzeContent()       # 调用AI进行内容分析
│   │   └── buildPrompt()          # 构建AI提示词
│   ├── ApiTokenServiceImpl.java   # API Token服务实现
│   │   ├── createToken()          # 创建并加密存储Token
│   │   ├── getTokensByUserId()    # 获取用户Token列表
│   │   ├── getDecryptedTokenValue() # 解密获取Token值
│   │   ├── getDecryptedTokenValueByProvider() # 根据提供商获取Token
│   │   ├── deleteToken()          # 删除Token实现
│   │   └── convertToDto()         # 转换为DTO对象
│   ├── LinkAnalysisServiceImpl.java # 链接分析服务实现
│   │   ├── analyzeUrl()           # 分析URL类型实现
│   │   ├── isVideoUrl()           # 视频链接判断实现
│   │   ├── extractWebPageTitle()  # 提取网页标题实现
│   │   ├── getContentType()       # 内容类型获取实现
│   │   └── extractMetadata()      # 提取元数据信息
│   ├── LinkProcessingServiceImpl.java # 链接处理服务实现
│   │   ├── analyzeLink()          # 分析链接类型和支持情况
│   │   ├── processLink()          # 处理链接实现
│   │   ├── processVideoLink()     # 处理视频链接
│   │   ├── processWebPageLink()   # 处理网页链接
│   │   ├── getTaskDetail()        # 获取任务详情实现
│   │   ├── getUserTasks()         # 获取用户任务实现
│   │   ├── deleteTask()           # 删除任务实现
│   │   ├── checkServiceHealth()   # 检查微服务健康状态
│   │   └── convertToResponseDto() # 转换为响应DTO
│   ├── VideoProcessingServiceImpl.java # 视频处理服务实现
│   │   ├── processVideo()         # 处理视频获取元数据
│   │   ├── transcribeAudio()      # 转写音频实现
│   │   ├── processVideoComplete() # 完整视频处理流程
│   │   ├── checkVideoServiceHealth() # 检查视频服务健康状态
│   │   ├── checkWhisperServiceHealth() # 检查Whisper服务健康状态
│   │   ├── downloadWavFile()      # 下载WAV音频文件
│   │   └── getVideoServiceUrl()   # 获取视频服务URL
│   ├── OcrProcessingServiceImpl.java # OCR处理服务实现
│   │   ├── processFile()          # 文件处理实现
│   │   ├── detectFileType()       # 文件类型检测
│   │   ├── callPythonService()    # 调用Python微服务
│   │   └── handleResponse()       # 处理响应结果
│   ├── OcrServiceImpl.java        # OCR服务实现
│   │   ├── uploadAndProcess()     # 上传处理实现
│   │   ├── processOcrTaskAsync()  # 异步OCR任务处理
│   │   ├── getTaskStatus()        # 任务状态获取实现
│   │   ├── getTaskResult()        # 结果获取实现
│   │   └── saveTaskResult()       # 保存任务结果
│   ├── PromptServiceImpl.java     # Prompt服务实现
│   │   ├── getPromptById()        # 根据ID获取实现
│   │   ├── getPromptsByUserId()   # 用户Prompt列表获取
│   │   ├── createPrompt()         # 创建Prompt实现
│   │   ├── updatePrompt()         # 更新Prompt实现
│   │   ├── deletePrompt()         # 删除Prompt实现
│   │   └── validateOwnership()    # 验证所有权
│   ├── UserDetailsServiceImpl.java # Spring Security用户详情服务
│   │   ├── loadUserByUsername()   # 根据用户名加载用户
│   │   └── createUserDetails()    # 创建用户详情对象
│   ├── UserServiceImpl.java       # 用户服务实现
│   │   ├── registerUser()         # 用户注册实现
│   │   ├── findByUsername()       # 用户名查找实现
│   │   ├── findByEmail()          # 邮箱查找实现
│   │   ├── findById()             # ID查找实现
│   │   └── encodePassword()       # 密码加密处理
│   └── WebContentServiceImpl.java # 网页内容服务实现
│       ├── fetchWebContent()      # 网页内容获取实现
│       ├── extractText()          # 文本提取实现
│       ├── parseHtml()            # HTML解析实现
│       └── cleanContent()         # 内容清理处理
├── dto/                           # 数据传输对象
│   ├── LinkAnalysisResponseDto.java # 链接分析响应DTO
│   │   ├── getLinkType()          # 获取链接类型
│   │   ├── getPlatform()          # 获取平台信息
│   │   ├── getTitle()             # 获取标题
│   │   ├── getDescription()       # 获取描述
│   │   ├── getIsSupported()       # 获取是否支持
│   │   └── getMessage()           # 获取消息
│   ├── LinkProcessRequestDto.java # 链接处理请求DTO
│   │   ├── getUrl()               # 获取URL
│   │   ├── getLanguage()          # 获取语言
│   │   └── getCustomPrompt()      # 获取自定义提示词
│   ├── LinkProcessResponseDto.java # 链接处理响应DTO
│   │   ├── getTaskId()            # 获取任务ID
│   │   ├── getStatus()            # 获取状态
│   │   ├── getContentType()       # 获取内容类型
│   │   ├── getUrl()               # 获取URL
│   │   ├── getVideoTitle()        # 获取视频标题
│   │   ├── getVideoDescription()  # 获取视频描述
│   │   ├── getVideoDuration()     # 获取视频时长
│   │   ├── getSummaryText()       # 获取摘要文本
│   │   ├── getTranscriptionText() # 获取转写文本
│   │   ├── getDetailedResult()    # 获取详细结果
│   │   ├── getErrorMessage()      # 获取错误信息
│   │   ├── getCreatedAt()         # 获取创建时间
│   │   ├── getCompletedAt()       # 获取完成时间
│   │   └── getMessage()           # 获取消息
│   ├── TranscriptionResultDto.java # 转写结果DTO
│   │   ├── getLanguage()          # 获取语言
│   │   ├── getLanguageProbability() # 获取语言概率
│   │   ├── getSegments()          # 获取分段信息
│   │   ├── getFullText()          # 获取完整文本
│   │   └── getProcessingInfo()    # 获取处理信息
│   └── VideoMetadataDto.java     # 视频元数据DTO
│       ├── getVideoId()           # 获取视频ID
│       ├── getTitle()             # 获取标题
│       ├── getDescription()       # 获取描述
│       ├── getDuration()          # 获取时长
│       ├── getPlatform()          # 获取平台
│       ├── getWavDownloadUrl()    # 获取WAV下载URL
│       ├── getSuccess()           # 获取成功状态
│       └── getErrorMessage()      # 获取错误信息
└── util/                          # 工具类
    ├── EncryptionUtil.java        # 加密工具类
    │   ├── encrypt()              # AES加密方法
    │   ├── decrypt()              # AES解密方法
    │   └── generateKey()          # 生成加密密钥
    └── JwtUtil.java               # JWT工具类
        ├── generateToken()        # 生成JWT Token
        ├── validateToken()        # 验证Token有效性
        ├── getUsernameFromToken() # 从Token提取用户名
        ├── getExpirationDateFromToken() # 获取Token过期时间
        └── isTokenExpired()       # 检查Token是否过期
```

## 数据库表结构
```
Database: aiplatform              # AI平台数据库
├── users                         # 用户信息表
│   ├── id (BIGINT, PK)          # 用户主键ID
│   ├── username (VARCHAR)        # 用户名，唯一
│   ├── email (VARCHAR)           # 邮箱，唯一
│   ├── password (VARCHAR)        # 密码哈希
│   ├── created_at (DATETIME)     # 创建时间
│   └── updated_at (DATETIME)     # 更新时间
├── api_tokens                    # API Token表
│   ├── id (BIGINT, PK)          # Token主键ID
│   ├── user_id (BIGINT, FK)     # 用户外键
│   ├── provider (VARCHAR)        # 提供商名称
│   ├── token_value (VARCHAR)     # 加密的Token值
│   ├── created_at (DATETIME)     # 创建时间
│   └── updated_at (DATETIME)     # 更新时间
├── prompts                       # Prompt模板表
│   ├── id (BIGINT, PK)          # Prompt主键ID
│   ├── user_id (BIGINT, FK)     # 用户外键
│   ├── title (VARCHAR)           # Prompt标题
│   ├── content (TEXT)            # Prompt内容
│   ├── category (VARCHAR)        # 分类标签
│   ├── created_at (DATETIME)     # 创建时间
│   └── updated_at (DATETIME)     # 更新时间
├── ocr_tasks                     # OCR处理任务表
│   ├── task_id (VARCHAR, PK)    # 任务UUID主键
│   ├── user_id (BIGINT, FK)     # 用户外键
│   ├── file_name (VARCHAR)       # 原始文件名
│   ├── file_size (BIGINT)        # 文件大小字节
│   ├── status (VARCHAR)          # 任务状态
│   ├── result (TEXT)             # JSON格式结果
│   ├── error_message (TEXT)      # 错误信息
│   ├── created_at (DATETIME)     # 创建时间
│   └── completed_at (DATETIME)   # 完成时间
└── video_transcription_tasks     # 视频转写任务表
    ├── id (BIGINT, PK)          # 任务主键ID
    ├── user_id (BIGINT, FK)     # 用户外键
    ├── task_id (VARCHAR, UNIQUE) # 任务UUID，唯一
    ├── url (VARCHAR)             # 处理的URL链接
    ├── content_type (VARCHAR)    # 内容类型：WEBPAGE/VIDEO
    ├── status (VARCHAR)          # 任务状态
    ├── video_title (VARCHAR)     # 视频标题
    ├── video_description (TEXT)  # 视频描述
    ├── video_duration (INT)      # 视频时长秒数
    ├── language (VARCHAR)        # 处理语言
    ├── custom_prompt (TEXT)      # 自定义提示词
    ├── result_json (LONGTEXT)    # 完整结果JSON
    ├── transcription_text (LONGTEXT) # 转写文本
    ├── summary_text (TEXT)       # AI总结文本
    ├── created_at (DATETIME)     # 创建时间
    ├── updated_at (DATETIME)     # 更新时间
    ├── completed_at (DATETIME)   # 完成时间
    └── error_message (TEXT)      # 错误信息
```

## 微服务架构
```
Python微服务生态                  # 外部处理服务集群
├── OCR微服务 (端口: 9001)        # 文档OCR处理服务
│   ├── /process                  # 文档处理端点
│   │   ├── process_pdf()         # PDF文档处理
│   │   ├── process_image()       # 图像OCR处理
│   │   ├── process_excel()       # Excel文件处理
│   │   ├── process_word()        # Word文档处理
│   │   └── process_text_file()   # 文本文件处理
│   └── /health                   # 健康检查端点
│       └── get_status()          # 获取服务状态
├── 视频处理微服务 (端口: 9002)    # 视频处理服务
│   ├── /process-video            # 视频处理端点
│   │   ├── download_video()      # 视频下载处理
│   │   ├── extract_metadata()    # 提取视频元数据
│   │   ├── convert_audio()       # 音频格式转换
│   │   └── upload_to_storage()   # 上传到存储服务
│   └── /health                   # 健康检查端点
│       └── get_status()          # 获取服务状态
└── Whisper转写微服务 (端口: 9999) # 语音转写服务
    ├── /transcribe               # 语音转写端点
    │   ├── load_whisper_model()  # 加载Whisper模型
    │   ├── transcribe_audio()    # 语音转文字
    │   ├── segment_audio()       # 音频分段处理
    │   └── enhance_with_prompt() # 使用自定义prompt增强
    └── /health                   # 健康检查端点
        └── get_status()          # 获取服务状态
```

## 技术栈
- **后端**: Spring Boot 3.4.5 + Java 21 + MySQL 8.0 + MyBatis
- **安全**: Spring Security + JWT + AES加密
- **AI集成**: Spring AI + Gemini API
- **微服务**: Python FastAPI + Docker 