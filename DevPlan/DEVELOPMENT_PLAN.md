# 社内業務サポートAIプラットフォーム - 后端开发计划 (正式版)

## MVP 完成状况总结

后端 MVP 开发阶段已完成以下核心内容：
- 项目基础架构、数据库设计与 ORM 配置。
- 核心用户认证与安全机制 (Spring Security, JWT)。
- API Token 管理功能，包括加密存储和用户级 Token 调用支持。
- Prompt 管理 (CRUD) 功能。
- 网页内容摘要功能，集成了 Spring AI 并支持用户 Token。
- 完成了大部分单元测试和关键API的集成测试点。

主要待完成的 MVP 收尾工作包括：部分覆盖更全面的集成测试（如CORS、端到端流程）、性能测试以及部署准备。

---

## 正式版开发规划

### 新增模块：智能OCR及文档理解服务

**总体方向：**
构建一个强大的后端服务，用于处理用户上传的多种格式文档（PDF、图片、Office文档等）。核心能力包括高精度的OCR、文档结构解析（如布局、表格、图片提取），并能以结构化方式（如JSON、Markdown）输出处理结果。此服务将作为未来高级AI功能（如文档问答、智能摘要增强、信息提取）的基础。

**关键技术实现路径（方案A，根据实际情况调整）、架构集成与API设计：**

1.  **核心文档处理（Python微服务 - Docling主导，Gemini辅助）：**
    *   **主要职责：**
        *   **Docling进行深度文档解析：** 利用Docling内置的解析器、布局分析模型（如DocLayNet）、表格识别模型（如TableFormer）及可配置的OCR引擎（如Tesseract, EasyOCR），对上传的文档（PDF、图片、Office文档等）进行全面的结构化解析。目标是生成一个包含详细页面布局、文本块、段落、标题、列表、表格、图片等元素及其属性（如坐标、类型）的`DoclingDocument`对象。
        *   **Gemini进行文档理解与分析：** 已成功集成Gemini API，用于对文档内容进行高级理解、分析和翻译。特别是对于日语文档，Gemini能够提供准确的中文翻译和内容摘要，大大提升了跨语言文档处理能力。
        *   **核心输出与实现进展：**
            *   **当前实现状态：** 已完成Python微服务(`ocr_service.py`)的基础架构，成功集成了PyPDF2、Docling和Gemini API。微服务能够处理PDF文件，提取文本内容，并通过Gemini进行内容分析和翻译。
            *   **解决的技术挑战：**
                * 成功解决了Docling模型下载问题，通过设置环境变量`DOCLING_ALLOW_DOWNLOADS="1"`启用了模型自动下载。
                * 解决了Docling API版本兼容性问题，通过实现健壮的异常处理和属性检测机制，使代码能够适应不同版本的Docling API。
                * 实现了多层次的文本提取策略：首先尝试使用PyPDF2直接提取文本，如果失败或结果不理想，则使用Docling进行OCR处理。
            *   **输出结构：** 微服务当前输出一个结构化的JSON文档，包含文档元数据、页面内容、提取的文本以及Gemini分析结果。随着开发的深入，将逐步完善JSON结构，以包含更详细的文档元素信息。
            *   **JSON结构当前实现：**
            ```json
            {
              "document_metadata": {
                "original_filename": "文件名.pdf",
                "processed_at": "处理时间戳",
                "page_count": 页数,
                "title": "文档标题（如果可提取）",
                "language": "检测到的语言"
              },
              "processing_info": {
                "pypdf2_used": true/false,
                "docling_used": true/false,
                "gemini_used": true/false,
                "force_ocr_used": true/false,
                "processing_time_seconds": 处理时间,
                "status": "success/failed/partial"
              },
              "pages": [
                {
                  "page_number": 页码,
                  "text": "页面提取的文本内容",
                  "tables": [表格数据数组],
                  "images": [图片数据数组]
                }
              ],
              "full_text": "整个文档的完整文本",
              "gemini_analysis": {
                "summary": "文档摘要",
                "key_points": ["关键点1", "关键点2", "..."],
                "translation": "文档翻译（如果需要）"
              },
              "tables": [所有表格的汇总],
              "images": [所有图片的汇总]
            }
            ```

            **目标JSON结构（未来迭代目标）：**
            ```json
            {
              "document_metadata": {
                "original_filename": "文件名.pdf",
                "processed_at": "处理时间戳",
                "page_count": 页数,
                "source_format": "文件格式",
                "ocr_engine_used": "使用的OCR引擎",
                "vlm_enhancement_used": "使用的VLM模型"
              },
              "pages": [
                {
                  "page_number": 页码,
                  "width": 页面宽度,
                  "height": 页面高度,
                  "elements": [
                    {
                      "id": "元素ID",
                      "type": "元素类型", // "paragraph", "heading", "list", "table", "image", "separator", "code_block", "footer", "header", "block" (通用块)
                      "bbox": [左, 上, 右, 下], // 元素在页面中的边界框坐标
                      "content_text": "元素文本内容", // 对于type="block"，此字段可能为空
                      "confidence": 置信度,
                      "style_info": {
                        "font_name": "字体名称",
                        "font_size": 字体大小,
                        "is_bold": true/false,
                        "is_italic": true/false
                      }
                    },
                    {
                      "id": "表格元素ID",
                      "type": "table",
                      "bbox": [左, 上, 右, 下],
                      "header_rows": 表头行数,
                      "rows": [
                        {
                          "cells": [
                            {
                              "bbox": [左, 上, 右, 下],
                              "row_span": 行跨度,
                              "col_span": 列跨度,
                              "is_header": true/false,
                              "content_text": "单元格文本"
                            }
                          ]
                        }
                      ],
                      "caption": "表格标题"
                    }
                  ]
                }
              ],
              "gemini_analysis": {
                "summary": "文档摘要",
                "key_points": ["关键点1", "关键点2", "..."],
                "translation": "文档翻译（如果需要）"
              }
            }
            ```
    *   **技术实现与进展：**
        *   **已完成：**
            *   成功实现了基于FastAPI的Python微服务，提供了RESTful API接口用于文件上传和处理。
            *   实现了健壮的Docling集成，通过运行时属性探查和异常处理，解决了API版本兼容性问题。
            *   成功集成了Google Generative AI SDK，实现了对Gemini 2.5 Pro Preview模型的调用，用于文档分析和翻译。
            *   实现了多层次的文本提取策略，结合PyPDF2和Docling的优势，提高了文本提取的准确性。
            *   解决了Docling模型下载问题，确保了OCR功能的正常运行。
        *   **下一步计划：**
            *   进一步优化Docling文档结构解析，提取更详细的元素信息（如表格、图片等）。
            *   改进错误处理和日志记录，提高服务的稳定性和可调试性。
            *   实现更高级的文档分析功能，如表格结构识别和提取。
            *   探索更多文档格式的支持，如Office文档、图片等。
            *   考虑云部署方案，如Docker + Serverless容器服务。

2.  **Java后端集成 (`aiplatjava` 项目内)：**
    *   **当前进展：**
        *   已完成Python OCR微服务的基础功能开发，包括文件上传、OCR处理和结果返回。
        *   微服务已经能够处理PDF文件，提取文本内容，并通过Gemini进行内容分析和翻译。
        *   测试脚本已经开发完成，可以用于验证OCR服务的功能。
        *   已完成Java后端与Python OCR微服务的集成，包括：
            *   `OcrController`: 提供 `/api/ocr/...` RESTful API 端点，处理前端的文件上传、任务状态查询、结果获取等请求。
            *   `OcrService` 和 `OcrServiceImpl`: 实现核心业务逻辑，包括文件上传、异步任务处理和结果管理。
            *   `OcrProcessingService` 和 `OcrProcessingServiceImpl`: 负责与Python OCR微服务通信，处理OCR请求和结果。
            *   DTOs: `OcrUploadRequestDto`, `OcrResponseDto`, `OcrTaskStatusDto` 等数据传输对象。
            *   实体类: `OcrTask` 用于存储OCR任务信息，包括状态、结果等。
            *   数据访问层: `OcrTaskMapper` 用于操作数据库中的OCR任务记录。
        *   已实现异步处理机制，使用Spring的`@Async`注解处理耗时OCR任务。
        *   已实现与Python微服务的通信，使用HTTP协议和JSON数据格式。
        *   已实现错误处理和日志记录，确保OCR服务的稳定性和可调试性。
        *   已完成Gemini API集成，使用Gemini 2.5 Pro Preview模型进行文档分析和翻译。
    *   **当前进展更新：**
        *   **前端集成已完成：**（还有报错）
            *   已实现OCR文件上传组件，支持拖放和文件选择。
            *   已实现OCR处理状态显示和进度反馈。
            *   已设计OCR结果展示界面，包括文本内容、表格和Gemini分析结果的可视化展示。
            *   已实现前端服务层和状态管理，包括`ocrService.ts`和`ocrStore.ts`。
            *   已实现类型定义，在`types/ocr.ts`中定义了OCR相关的类型。

    *   **下一步计划：**
        *   **性能优化：**
            *   优化Gemini API调用，减少处理时间。
            *   考虑使用GPU加速OCR处理，提高处理速度。
            *   实现更高效的文件处理和缓存机制。
        *   **功能增强：**
            *   支持更多文档格式，如Office文档、图片等。
            *   改进表格结构识别和提取。
            *   增强文档分析功能，如关键信息提取、文档分类等。
            *   实现实时进度反馈机制。
        *   **安全性与错误处理：**
            *   进一步完善错误处理机制，提高服务的稳定性。
            *   增强安全性，防止恶意文件上传和处理。
    *   **前端集成已完成：**
        *   已设计文件上传组件，支持拖放和文件选择。
        *   已实现OCR处理状态显示和进度反馈。
        *   已设计结果展示界面，包括文本内容、表格和Gemini分析结果的可视化展示。
        *   未来计划：提供下载和分享功能，方便用户保存和分享OCR结果。

3.  **(远期) 文档问答与RAG支持：**
    *   此方向不变，基于OCR服务提供的结构化文本进行。

(后续具体计划将在此处添加)