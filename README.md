# JavaAIplat - 用户管理系统基础框架

## 项目概述

JavaAIplat 是一个基于 Spring Boot 和 MyBatis 构建的用户管理系统基础框架。该项目实现了用户的基本管理功能，包括用户注册、登录、信息管理等核心功能，为企业内部系统提供了坚实的基础架构。

## 技术栈

- **后端**: Spring Boot 3.4.5
- **数据库**: MySQL
- **ORM框架**: MyBatis
- **安全框架**: Spring Security
- **构建工具**: Maven
- **JDK版本**: Java 21

## 核心功能

1. **用户认证与管理**
   - 用户注册和登录
   - 用户信息的增删改查
   - 基于角色的权限控制

## 项目结构

```
src/main/java/com/ding/aiplatjava/
├── config/          # 配置类
├── controller/      # 控制器层
├── dto/             # 数据传输对象
├── entity/          # 实体类
├── exception/       # 异常处理
├── mapper/          # MyBatis映射接口
├── security/        # 安全相关
├── service/         # 服务层接口和实现
│   └── impl/
└── util/            # 工具类
```

## 快速开始

### 环境要求

- JDK 21
- Maven 3.8+
- MySQL 8.0+

### 构建与运行

1. 克隆项目
   ```bash
   git clone https://github.com/yourusername/javaAIplat.git
   cd javaAIplat
   ```

2. 配置数据库
   - 在 `application.properties` 中修改数据库连接信息

3. 构建并运行
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

4. 访问应用
   - 应用将运行在 http://localhost:8080

## 未来计划

- 实现前端页面，提供完整的用户界面
- 强化安全性，实现JWT认证
- 添加更多业务功能模块

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

该项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。 