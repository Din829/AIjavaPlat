# 社内業務サポートAIプラットフォーム - 开发规范

## 代码注释规范

1. **所有代码必须添加中文注释**
   - 类和接口需要详细的类级注释，说明其用途和职责
   - 方法需要方法级注释，说明其功能、参数和返回值
   - 复杂的字段需要字段级注释
   - 注释应简洁易懂，避免冗余

2. **复杂逻辑必须详细解释**
   - 对于复杂的业务逻辑，需要分步骤添加注释
   - 对于不直观的算法或处理流程，需要解释其原理和目的
   - 对于特殊情况的处理，需要说明原因

3. **注释格式**
   - 类和方法使用JavaDoc风格的注释（`/** ... */`）
   - 行内注释使用单行注释（`// ...`）
   - 临时代码或TODO项使用明确的标记（`// TODO: ...`）

## 代码修改规范

1. **遵循项目架构**
   - 修改代码前，先参考`PROJECT_ARCHITECTURE.md`文件
   - 确保新代码符合既定的架构设计和分层原则
   - 不随意改变既定的设计模式和代码结构

2. **避免过度发散**
   - 修改应当聚焦于特定功能或问题
   - 避免在一次修改中涉及过多不相关的模块
   - 保持代码的内聚性和模块化

3. **保持一致性**
   - 新代码应与现有代码风格保持一致
   - 遵循项目已建立的命名约定和格式规范
   - 使用项目中已有的工具类和通用方法，避免重复造轮子

## 文档更新规范

1. **同步更新架构文档**
   - 每次添加新功能或修改现有功能时，同步更新`PROJECT_ARCHITECTURE.md`
   - 确保架构文档反映最新的系统设计和组件关系
   - 对于重大变更，需要在文档中特别说明

2. **同步更新开发计划**
   - 完成任务后，在`DEVELOPMENT_PLAN.md`中标记为已完成
   - 添加新任务时，更新开发计划文档
   - 调整任务优先级或依赖关系时，更新相关说明

3. **保持文档的一致性**
   - 确保各文档之间的描述一致，避免矛盾
   - 文档使用统一的术语和概念
   - 文档应当简明扼要，避免冗余

## 代码审查清单

在提交代码前，请检查以下项目：

- [ ] 代码是否有适当的中文注释
- [ ] 复杂逻辑是否有详细解释
- [ ] 代码是否符合项目架构
- [ ] 是否避免了过度发散
- [ ] 是否与现有代码保持一致性
- [ ] 是否更新了项目架构文档
- [ ] 是否更新了开发计划文档
- [ ] 文档是否保持一致性

## 最佳实践

1. **先设计，后编码**
   - 在编写代码前，先思考设计和架构
   - 对于复杂功能，可以先草拟伪代码或流程图

2. **小步迭代**
   - 采用小步骤开发，频繁测试
   - 一次实现一个小功能，而不是大量功能

3. **持续重构**
   - 定期重构代码，保持代码整洁
   - 消除重复代码，提高代码复用性

4. **测试驱动**
   - 尽可能编写单元测试
   - 确保修改不会破坏现有功能

遵循这些规则将有助于保持代码库的整洁、可维护性和一致性，同时确保项目文档始终反映最新的系统状态。
