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

**关键技术实现路径（方案A）、架构集成与API设计：**

1.  **核心文档处理（Python微服务 - Docling主导，Gemini辅助）：**
    *   **主要职责：**
        *   **Docling进行深度文档解析：** 利用Docling内置的解析器、布局分析模型（如DocLayNet）、表格识别模型（如TableFormer）及可配置的OCR引擎（如Tesseract, EasyOCR），对上传的文档（PDF、图片、Office文档等）进行全面的结构化解析。目标是生成一个包含详细页面布局、文本块、段落、标题、列表、表格、图片等元素及其属性（如坐标、类型）的`DoclingDocument`对象。
        *   **（可选）Gemini进行靶向增强：** 对于Docling初步处理后仍存在识别困难的特定图像区域（例如，低质量扫描件中的图片文字、复杂图表内的文字），或需要对特定内容块进行高级理解/转换时，Python微服务将负责调用Gemini API (或其他顶级VLM) 对这些*特定元素*进行处理。
        *   **输出定义：** 微服务的核心输出是**一份详尽的、结构化的JSON文档**，该JSON由`DoclingDocument`对象序列化而来，并融合了Gemini（如果调用）的增强结果。其目标结构草案如下：
            ```json
            {
              "document_metadata": {
                "original_filename": "example.pdf",
                "processed_at": "2024-07-30T10:30:00Z",
                "page_count": 2,
                "source_format": "pdf",
                "ocr_engine_used": "Tesseract",
                "vlm_enhancement_used": "Gemini 1.5 Pro"
              },
              "pages": [
                {
                  "page_number": 1,
                  "width": 595,
                  "height": 842,
                  "elements": [
                    {
                      "id": "elem_1_1",
                      "type": "paragraph", // "paragraph", "heading", "list", "table", "image", "separator", "code_block", "footer", "header", "block" (通用块)
                      "bbox": [50, 50, 400, 100],
                      "content_text": "这是第一段的文本内容。", // 对于type="block"，此字段可能为空
                      "confidence": 0.95,
                      "style_info": {
                        "font_name": "Arial",
                        "font_size": 12,
                        "is_bold": false,
                        "is_italic": false
                      },
                      // heading特有
                      "level": 1, 
                      // list特有
                      "list_type": "unordered", // "ordered"
                      "items": [
                        {
                          "item_id": "li_1_3_1",
                          "bbox": [70, 170, 440, 185],
                          "content_text": "列表项一", // 简单列表项的文本
                          "elements_in_item": [ /* 可选：复杂列表项内可包含其他elements */ ]
                        }
                      ],
                      // table特有
                      "header_rows": 1,
                      "rows": [
                        {
                          "cells": [
                            {
                              "bbox": [55, 255, 150, 275],
                              "row_span": 1,
                              "col_span": 1,
                              "is_header": true,
                              "content_text": "表头1",
                              "elements_in_cell": [ /* 可选：复杂单元格内可包含其他elements */ ]
                            }
                          ]
                        }
                      ],
                      "caption": "表1：示例表格",
                      // image特有
                      "src_type": "base64", // "url", "internal_ref"
                      "data": "iVBORw0KGgoAAAANS...",
                      "original_filename": "figure1.png",
                      // "caption": "图1：流程图示例", // 图片的caption也可以通过相邻的paragraph元素表示
                      "ocr_text_from_image": "图片中识别出的文字...",
                      "description_from_vlm": "这是一个流程图，描述了...",
                      // block (通用块) 特有
                      "content_items": [
                        {
                          "type": "text_run",
                          "text": "加粗文本",
                          "bbox_relative_to_parent": [0,0,50,20],
                          "style_info": {"is_bold": true}
                        },
                        {
                          "type": "image_ref",
                          "ref_id": "img_page1_item1", // 指向images_store
                          "bbox_relative_to_parent": [60,0,80,20]
                        }
                      ]
                    }
                  ]
                }
              ],
              "images_store": { // (可选) 统一存放图片数据，元素中通过ref_id引用
                "img_page1_item1": {
                  "src_type": "base64",
                  "data": "...",
                  "original_filename": "icon.png"
                }
              },
              "extracted_tables_summary": [ /* 可选 */ ],
              "extracted_images_summary": [ /* 可选 */ ]
            }
            ```
    *   **技术调研与实现要点：**
        *   深入研究`DoclingDocument`对象的结构，以及如何高效、完整地将其序列化为我们期望的JSON格式（探索其内置JSON导出或自定义序列化方案）。
        *   确定调用Gemini API的最佳实践（如使用官方SDK、`litellm`），包括错误处理和成本效益考量。
        *   Python微服务框架选型（如FastAPI, Flask）及API接口设计。
        *   云部署方案调研与决策（如Docker + Serverless容器服务Cloud Run/Fargate，或托管K8s）。

2.  **Java后端集成 (`aiplatjava` 项目内)：**
    *   **新增核心组件：**
        *   `OcrController`: 提供 `/api/ocr/...` RESTful API 端点，处理前端的文件上传、任务状态查询、结果获取等请求。
        *   `OcrService`: 实现核心业务逻辑，包括调用Python OCR微服务、管理异步任务（如果采用）、处理和转换Python微服务返回的JSON数据。
        *   DTOs: `OcrUploadRequestDto`, `OcrResponseDto` (其核心荷载为Python微服务返回的结构化JSON), `OcrTaskStatusDto`。
    *   **与Python微服务的通信：**
        *   **主要协议：** HTTP/HTTPS。
        *   **核心数据交换格式：** Python微服务向Java后端返回的**结构化JSON** (如上定义)。此JSON格式的定义是关键，需要包含但不限于：页面信息、内容块列表（类型、文本、坐标）、表格数据（行列结构）、图片信息（引用、Base64或URL、坐标）等。
        *   配置Python微服务地址等信息于`application.properties`。
    *   **异步处理机制：**
        *   初步考虑采用Spring的`@Async`处理耗时OCR任务。
        *   可能需要数据库表（如`ocr_processing_tasks`）管理任务状态和结果。
        *   远期可评估消息队列。
    *   **安全性与错误处理：** 确保内部服务间调用的安全，并对Python微服务可能发生的错误进行妥善处理。

3.  **(远期) 文档问答与RAG支持：**
    *   此方向不变，基于OCR服务提供的结构化文本进行。

(后续具体计划将在此处添加) 