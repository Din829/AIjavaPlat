"""
AI Platform OCR Microservice

这个服务提供了PDF文档的OCR和结构化分析功能，使用多层处理策略：
1. 使用PyPDF2直接提取文本（适用于非扫描版PDF）
2. 使用Docling进行OCR和文档结构解析（适用于扫描版PDF）
3. 使用Google Gemini API进行高级文本理解和分析

服务提供RESTful API接口，支持文件上传和处理。
"""

# 启用 Docling 模型下载
import os
os.environ["DOCLING_ALLOW_DOWNLOADS"] = "1"
import sys
import uuid
import shutil
import json
import base64
import logging
from typing import Dict, List, Any, Optional, Union
from datetime import datetime
from pathlib import Path

# FastAPI相关导入
from fastapi import FastAPI, UploadFile, File, HTTPException, Query, BackgroundTasks
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# --- 尝试导入必要的库 ---
# 1. PyPDF2 - 用于直接提取PDF文本
PYPDF2_AVAILABLE = False
try:
    import PyPDF2
    PYPDF2_AVAILABLE = True
    logger.info("Successfully imported PyPDF2.")
except ImportError:
    logger.warning("PyPDF2 not available. Direct PDF text extraction will be disabled.")

# 2. Docling - 用于OCR和文档结构解析
DOCLING_AVAILABLE = False
try:
    from docling.document_converter import DocumentConverter
    DOCLING_AVAILABLE = True
    logger.info("Successfully imported DocumentConverter from docling.")
except ImportError as e:
    logger.warning(f"Failed to import Docling components: {e}")
    logger.warning("Docling OCR processing will be disabled.")

# 3. Google Gemini API - 用于高级文本理解和分析
GEMINI_ENABLED = False
try:
    import google.generativeai as genai
    import PIL.Image  # 确保导入PIL.Image用于Gemini图像处理
    logger.info("Successfully imported google.generativeai.")
    GEMINI_ENABLED = True
except ImportError:
    logger.warning("google.generativeai module not found. Gemini enhancements will be disabled.")

# --- FastAPI 应用实例 ---
app = FastAPI(title="AI Platform OCR Microservice")

# --- Pydantic 模型定义 ---
# 简化的数据模型，专注于实用性和易用性

class ProcessingInfo(BaseModel):
    """处理信息，包括使用的处理方法和状态"""
    pypdf2_used: bool = False
    docling_used: bool = False
    gemini_used: bool = False
    force_ocr_used: bool = False
    processing_time_seconds: Optional[float] = None
    status: str = "success"
    error_message: Optional[str] = None

class DocumentMetadata(BaseModel):
    """文档元数据"""
    original_filename: Optional[str] = None
    processed_at: Optional[str] = None
    page_count: Optional[int] = None
    source_format: Optional[str] = None
    language: Optional[str] = None
    title: Optional[str] = None
    author: Optional[str] = None
    creation_date: Optional[str] = None

class TableInfo(BaseModel):
    """表格信息"""
    table_id: str
    page_number: int
    title: Optional[str] = None
    headers: Optional[List[str]] = None
    rows: List[List[str]] = []
    raw_text: Optional[Union[str, Dict[str, Any], List[Dict[str, Any]]]] = None

class ImageInfo(BaseModel):
    """图像信息"""
    image_id: str
    page_number: int
    description: Optional[str] = None
    ocr_text: Optional[str] = None
    mime_type: Optional[str] = None
    data: Optional[str] = None  # Base64编码的图像数据

class PageContent(BaseModel):
    """页面内容"""
    page_number: int
    text: str = ""
    tables: List[TableInfo] = []
    images: List[ImageInfo] = []

class GeminiAnalysis(BaseModel):
    """Gemini分析结果"""
    summary: Optional[str] = None
    key_points: List[str] = []
    structured_data: Optional[Dict[str, Any]] = None
    translation: Optional[str] = None
    raw_response: Optional[str] = None

class OcrResponse(BaseModel):
    """OCR响应"""
    document_metadata: DocumentMetadata
    processing_info: ProcessingInfo
    pages: List[PageContent] = []
    full_text: str = ""
    tables: List[TableInfo] = []
    images: List[ImageInfo] = []
    gemini_analysis: Optional[GeminiAnalysis] = None


# --- 全局变量与常量 ---
# 上传文件临时目录
UPLOAD_DIR = "temp_uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

# 创建本地缓存目录
LOCAL_CACHE_DIR = "docling_cache"
os.makedirs(LOCAL_CACHE_DIR, exist_ok=True)

# 设置环境变量，告诉Hugging Face和Docling使用本地缓存目录
os.environ["HF_HOME"] = os.path.abspath(LOCAL_CACHE_DIR)
os.environ["TRANSFORMERS_CACHE"] = os.path.join(os.path.abspath(LOCAL_CACHE_DIR), "transformers")
os.environ["HF_DATASETS_CACHE"] = os.path.join(os.path.abspath(LOCAL_CACHE_DIR), "datasets")
os.environ["HUGGINGFACE_HUB_CACHE"] = os.path.join(os.path.abspath(LOCAL_CACHE_DIR), "hub")
os.environ["DOCLING_CACHE_DIR"] = os.path.abspath(LOCAL_CACHE_DIR)

# 创建所有必要的子目录
for subdir in ["transformers", "datasets", "hub"]:
    os.makedirs(os.path.join(LOCAL_CACHE_DIR, subdir), exist_ok=True)

logger.info(f"Using local cache directory: {os.path.abspath(LOCAL_CACHE_DIR)}")

# --- Gemini API配置 ---
# 从环境变量获取Gemini API密钥
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "AIzaSyDFLyEYqgaC6plSFF5IjvQEW0FEug6o14o")

# 配置Gemini API
if GEMINI_ENABLED:
    try:
        # 尝试使用最新的API版本
        genai.configure(api_key=GEMINI_API_KEY, transport="rest")
        logger.info("Configured Gemini API with REST transport")

        # 检查可用的模型
        try:
            models = genai.list_models()
            model_names = [m.name for m in models]
            logger.info(f"Available Gemini models: {model_names}")

            # 设置默认模型
            DEFAULT_GEMINI_MODEL = "models/gemini-2.5-pro-preview-05-06"
            if DEFAULT_GEMINI_MODEL not in model_names:
                # 如果首选模型不可用，使用备选模型
                for model_name in ["models/gemini-1.5-pro", "models/gemini-1.5-flash", "models/gemini-pro"]:
                    if model_name in model_names:
                        DEFAULT_GEMINI_MODEL = model_name
                        break

            logger.info(f"Using default Gemini model: {DEFAULT_GEMINI_MODEL}")
        except Exception as e:
            logger.warning(f"Failed to list available Gemini models: {e}")
            DEFAULT_GEMINI_MODEL = "models/gemini-pro"
    except Exception as e:
        logger.error(f"Failed to configure Gemini API: {e}")
        GEMINI_ENABLED = False
else:
    DEFAULT_GEMINI_MODEL = None

# --- 默认提示词 ---
DEFAULT_PDF_PROMPT = """
请分析这个PDF文档，并提取以下信息：
1. 文档的标题和主题
2. 文档的主要内容和结构
3. 文档中的关键信息和数据
4. 表格内容（如有）
5. 图表或图像描述（如有）

请以JSON格式返回结果：
{
    "title": "文档标题",
    "document_type": "文档类型",
    "language": "文档语言",
    "summary": "文档摘要",
    "key_points": ["关键点1", "关键点2", ...],
    "tables": [{"table_title": "表格标题", "content": "表格内容"}],
    "images": [{"description": "图像描述"}],
    "translation": "文档内容的中文翻译（如果原文不是中文）"
}
"""

# 日语文档的默认提示词
DEFAULT_JAPANESE_PDF_PROMPT = """
你是一个专业的日语文档分析专家。请分析这个日语PDF文档，并提取以下信息：

1. 文档的标题和主题
2. 文档的主要内容和结构
3. 文档中的关键信息和数据
4. 表格内容（如有）
5. 图表或图像描述（如有）

请以JSON格式返回结果：
{
    "title": "文档标题",
    "document_type": "文档类型",
    "language": "文档语言",
    "summary": "文档摘要",
    "key_fields": [{"field_name": "名称", "field_value": "值"}],
    "tables": [{"table_title": "表格标题", "content": "表格内容"}],
    "images": [{"description": "图像描述"}],
    "translation": "文档内容的中文翻译"
}
"""

# --- 辅助函数 ---

def cleanup_temp_file(file_path: str):
    """删除临时文件"""
    try:
        if os.path.exists(file_path):
            os.remove(file_path)
    except Exception as e:
        logger.error(f"Error cleaning up temp file {file_path}: {e}")

def extract_text_with_pypdf2(file_path: str) -> Dict[str, Any]:
    """
    使用PyPDF2直接从PDF文件中提取文本

    Args:
        file_path: PDF文件路径

    Returns:
        包含提取文本的字典
    """
    if not PYPDF2_AVAILABLE:
        return {"error": "PyPDF2 not available"}

    try:
        # 打开PDF文件
        with open(file_path, "rb") as f:
            pdf_reader = PyPDF2.PdfReader(f)

            # 获取页面数量
            num_pages = len(pdf_reader.pages)
            logger.info(f"PDF has {num_pages} pages")

            # 提取文本
            all_text = []
            pages = []

            for i in range(num_pages):
                page = pdf_reader.pages[i]
                text = page.extract_text() or ""

                # 创建页面数据
                page_data = {
                    "page_number": i+1,
                    "text": text
                }

                pages.append(page_data)
                all_text.append(text)

            # 创建结果
            result = {
                "document_type": "PDF",
                "processed_at": datetime.now().isoformat(),
                "original_filename": os.path.basename(file_path),
                "page_count": num_pages,
                "pages": pages,
                "full_text": "\n".join(all_text)
            }

            # 尝试提取文档信息
            if hasattr(pdf_reader, 'metadata') and pdf_reader.metadata:
                metadata = pdf_reader.metadata
                result["title"] = metadata.get('/Title', None)
                result["author"] = metadata.get('/Author', None)
                result["creation_date"] = metadata.get('/CreationDate', None)

            return result

    except Exception as e:
        logger.exception(f"Error extracting text with PyPDF2: {e}")
        return {"error": f"Error extracting text with PyPDF2: {str(e)}"}

def process_with_docling(file_path: str, force_ocr: bool = False) -> Dict[str, Any]:
    """
    使用Docling处理PDF文件

    Args:
        file_path: 要处理的PDF文件路径
        force_ocr: 是否强制使用OCR，即使PDF包含文本层

    Returns:
        包含提取文本和元数据的字典
    """
    if not DOCLING_AVAILABLE:
        return {"error": "Docling not available"}

    try:
        # 尝试导入并配置pipeline_options
        try:
            # 根据 Docling 2.32.0 版本的文档，正确的导入路径是 docling.datamodel.pipeline_options
            try:
                from docling.datamodel.pipeline_options import PdfPipelineOptions, EasyOcrOptions
                logger.info("Successfully imported from docling.datamodel.pipeline_options")

                # 使用 PdfPipelineOptions 更针对PDF处理
                pipeline_options = PdfPipelineOptions()

                # 设置缓存目录 - 在 Docling 2.32.0 中，可能使用不同的属性名
                # 尝试设置缓存目录，如果属性不存在则忽略
                try:
                    pipeline_options.cache_dir = os.path.abspath(LOCAL_CACHE_DIR)
                except Exception as e:
                    logger.info(f"Could not set cache_dir: {e}. This is normal for newer Docling versions.")
                    # 尝试其他可能的属性名
                    try:
                        pipeline_options.artifacts_path = os.path.abspath(LOCAL_CACHE_DIR)
                    except Exception as e2:
                        logger.info(f"Could not set artifacts_path: {e2}. Using default cache location.")

                # 启用OCR，并根据参数决定是否强制使用OCR
                # 尝试设置OCR选项，如果属性不存在则忽略
                try:
                    pipeline_options.do_ocr = True
                except Exception as e:
                    logger.info(f"Could not set do_ocr: {e}. This is normal for newer Docling versions.")

                try:
                    pipeline_options.force_ocr = force_ocr
                except Exception as e:
                    logger.info(f"Could not set force_ocr: {e}. This is normal for newer Docling versions.")

                # 尝试配置OCR选项，特别是对日语的支持
                ocr_options = EasyOcrOptions()
                # 添加日语支持 - 在不同版本中，属性名可能不同
                try:
                    # 尝试设置 languages 属性
                    ocr_options.languages = ["ja", "en"]
                except Exception as e:
                    logger.info(f"Could not set languages: {e}. Trying alternative attribute names.")
                    # 尝试设置 lang 属性
                    try:
                        ocr_options.lang = ["ja", "en"]
                    except Exception as e2:
                        logger.info(f"Could not set lang: {e2}. Trying alternative attribute names.")
                        # 尝试设置 language 属性
                        try:
                            ocr_options.language = ["ja", "en"]
                        except Exception as e3:
                            logger.info(f"Could not set language: {e3}. Using default language settings.")

                # 尝试设置 OCR 选项
                try:
                    pipeline_options.ocr_options = ocr_options
                except Exception as e:
                    logger.info(f"Could not set ocr_options: {e}. Using default OCR settings.")
                logger.info("Configured EasyOCR with Japanese and English language support")
            except ImportError:
                # 尝试其他可能的导入路径
                logger.info("Trying alternative import paths for Docling 2.32.0")
                try:
                    # 尝试从 docling.pipeline_options 导入
                    from docling.pipeline_options import PdfPipelineOptions, EasyOcrOptions
                    logger.info("Successfully imported from docling.pipeline_options")

                    pipeline_options = PdfPipelineOptions()
                    # 设置缓存目录 - 在 Docling 2.32.0 中，可能使用不同的属性名
                    # 尝试设置缓存目录，如果属性不存在则忽略
                    try:
                        pipeline_options.cache_dir = os.path.abspath(LOCAL_CACHE_DIR)
                    except Exception as e:
                        logger.info(f"Could not set cache_dir: {e}. This is normal for newer Docling versions.")
                        # 尝试其他可能的属性名
                        try:
                            pipeline_options.artifacts_path = os.path.abspath(LOCAL_CACHE_DIR)
                        except Exception as e2:
                            logger.info(f"Could not set artifacts_path: {e2}. Using default cache location.")
                    # 尝试设置OCR选项，如果属性不存在则忽略
                    try:
                        pipeline_options.do_ocr = True
                    except Exception as e:
                        logger.info(f"Could not set do_ocr: {e}. This is normal for newer Docling versions.")

                    try:
                        pipeline_options.force_ocr = force_ocr
                    except Exception as e:
                        logger.info(f"Could not set force_ocr: {e}. This is normal for newer Docling versions.")

                    # 尝试配置OCR选项，特别是对日语的支持
                    ocr_options = EasyOcrOptions()
                    # 添加日语支持 - 在不同版本中，属性名可能不同
                    try:
                        # 尝试设置 languages 属性
                        ocr_options.languages = ["ja", "en"]
                    except Exception as e:
                        logger.info(f"Could not set languages: {e}. Trying alternative attribute names.")
                        # 尝试设置 lang 属性
                        try:
                            ocr_options.lang = ["ja", "en"]
                        except Exception as e2:
                            logger.info(f"Could not set lang: {e2}. Trying alternative attribute names.")
                            # 尝试设置 language 属性
                            try:
                                ocr_options.language = ["ja", "en"]
                            except Exception as e3:
                                logger.info(f"Could not set language: {e3}. Using default language settings.")

                    # 尝试设置 OCR 选项
                    try:
                        pipeline_options.ocr_options = ocr_options
                    except Exception as e:
                        logger.info(f"Could not set ocr_options: {e}. Using default OCR settings.")
                    logger.info("Configured EasyOCR with Japanese and English language support (alternative path)")
                except ImportError:
                    # 尝试从 docling.core.pipeline_options 导入
                    try:
                        from docling.core.pipeline_options import PdfPipelineOptions, EasyOcrOptions
                        logger.info("Successfully imported from docling.core.pipeline_options")

                        pipeline_options = PdfPipelineOptions()
                        # 设置缓存目录 - 在 Docling 2.32.0 中，可能使用不同的属性名
                        # 尝试设置缓存目录，如果属性不存在则忽略
                        try:
                            pipeline_options.cache_dir = os.path.abspath(LOCAL_CACHE_DIR)
                        except Exception as e:
                            logger.info(f"Could not set cache_dir: {e}. This is normal for newer Docling versions.")
                            # 尝试其他可能的属性名
                            try:
                                pipeline_options.artifacts_path = os.path.abspath(LOCAL_CACHE_DIR)
                            except Exception as e2:
                                logger.info(f"Could not set artifacts_path: {e2}. Using default cache location.")
                        # 尝试设置OCR选项，如果属性不存在则忽略
                        try:
                            pipeline_options.do_ocr = True
                        except Exception as e:
                            logger.info(f"Could not set do_ocr: {e}. This is normal for newer Docling versions.")

                        try:
                            pipeline_options.force_ocr = force_ocr
                        except Exception as e:
                            logger.info(f"Could not set force_ocr: {e}. This is normal for newer Docling versions.")

                        # 尝试配置OCR选项，特别是对日语的支持
                        ocr_options = EasyOcrOptions()
                        # 添加日语支持 - 在不同版本中，属性名可能不同
                        try:
                            # 尝试设置 languages 属性
                            ocr_options.languages = ["ja", "en"]
                        except Exception as e:
                            logger.info(f"Could not set languages: {e}. Trying alternative attribute names.")
                            # 尝试设置 lang 属性
                            try:
                                ocr_options.lang = ["ja", "en"]
                            except Exception as e2:
                                logger.info(f"Could not set lang: {e2}. Trying alternative attribute names.")
                                # 尝试设置 language 属性
                                try:
                                    ocr_options.language = ["ja", "en"]
                                except Exception as e3:
                                    logger.info(f"Could not set language: {e3}. Using default language settings.")

                        # 尝试设置 OCR 选项
                        try:
                            pipeline_options.ocr_options = ocr_options
                        except Exception as e:
                            logger.info(f"Could not set ocr_options: {e}. Using default OCR settings.")
                        logger.info("Configured EasyOCR with Japanese and English language support (alternative path)")
                    except ImportError:
                        # 尝试从 docling_core.pipeline_options 导入
                        from docling_core.pipeline_options import PdfPipelineOptions, EasyOcrOptions
                        logger.info("Successfully imported from docling_core.pipeline_options")

                        pipeline_options = PdfPipelineOptions()
                        # 设置缓存目录 - 在 Docling 2.32.0 中，可能使用不同的属性名
                        # 尝试设置缓存目录，如果属性不存在则忽略
                        try:
                            pipeline_options.cache_dir = os.path.abspath(LOCAL_CACHE_DIR)
                        except Exception as e:
                            logger.info(f"Could not set cache_dir: {e}. This is normal for newer Docling versions.")
                            # 尝试其他可能的属性名
                            try:
                                pipeline_options.artifacts_path = os.path.abspath(LOCAL_CACHE_DIR)
                            except Exception as e2:
                                logger.info(f"Could not set artifacts_path: {e2}. Using default cache location.")
                        # 尝试设置OCR选项，如果属性不存在则忽略
                        try:
                            pipeline_options.do_ocr = True
                        except Exception as e:
                            logger.info(f"Could not set do_ocr: {e}. This is normal for newer Docling versions.")

                        try:
                            pipeline_options.force_ocr = force_ocr
                        except Exception as e:
                            logger.info(f"Could not set force_ocr: {e}. This is normal for newer Docling versions.")

                        # 尝试配置OCR选项，特别是对日语的支持
                        ocr_options = EasyOcrOptions()
                        # 添加日语支持 - 在不同版本中，属性名可能不同
                        try:
                            # 尝试设置 languages 属性
                            ocr_options.languages = ["ja", "en"]
                        except Exception as e:
                            logger.info(f"Could not set languages: {e}. Trying alternative attribute names.")
                            # 尝试设置 lang 属性
                            try:
                                ocr_options.lang = ["ja", "en"]
                            except Exception as e2:
                                logger.info(f"Could not set lang: {e2}. Trying alternative attribute names.")
                                # 尝试设置 language 属性
                                try:
                                    ocr_options.language = ["ja", "en"]
                                except Exception as e3:
                                    logger.info(f"Could not set language: {e3}. Using default language settings.")

                        # 尝试设置 OCR 选项
                        try:
                            pipeline_options.ocr_options = ocr_options
                        except Exception as e:
                            logger.info(f"Could not set ocr_options: {e}. Using default OCR settings.")
                        logger.info("Configured EasyOCR with Japanese and English language support (alternative path)")

            logger.info(f"Using pipeline options: {type(pipeline_options)}, OCR enabled: {getattr(pipeline_options, 'do_ocr', 'N/A')}, force OCR: {getattr(pipeline_options, 'force_ocr', 'N/A')}")

            # 在 Docling 2.32.0 中，需要使用 format_options 参数
            from docling.datamodel.base_models import InputFormat
            from docling.document_converter import PdfFormatOption

            # 启用模型下载
            import os
            os.environ["DOCLING_ALLOW_DOWNLOADS"] = "1"

            # 创建 DocumentConverter 实例
            converter = DocumentConverter(
                format_options={
                    InputFormat.PDF: PdfFormatOption(pipeline_options=pipeline_options)
                }
            )
        except ImportError as e_import: # 捕获导入错误
            logger.warning(f"Failed to import Docling pipeline options (ImportError: {e_import}). Using default DocumentConverter options. THIS IS LIKELY DUE TO AN INSTALLATION ISSUE.")
            # 使用默认的 DocumentConverter，不指定任何特殊选项
            converter = DocumentConverter()
        except Exception as e_config: # 捕获其他配置错误
            logger.warning(f"Failed to configure custom Docling pipeline options (Exception: {e_config}). Using default DocumentConverter options.")
            # 使用默认的 DocumentConverter，不指定任何特殊选项
            converter = DocumentConverter()

        # 开始处理PDF
        logger.info(f"Starting Docling conversion for '{file_path}'")
        start_time = datetime.now()
        docling_result = converter.convert(source=file_path)
        end_time = datetime.now()
        conversion_time = (end_time - start_time).total_seconds()
        logger.info(f"Docling conversion finished in {conversion_time:.2f} seconds")

        # 检查转换结果
        if not docling_result:
            logger.error(f"Docling processing failed: No result returned")
            return {"error": "Docling processing failed: No result returned"}

        # 检查转换状态
        if hasattr(docling_result, 'status') and hasattr(docling_result.status, 'name'):
            if docling_result.status.name != "SUCCESS":
                status_msg = getattr(docling_result, 'status_message', "Unknown error")
                logger.error(f"Docling processing failed: {status_msg}")
                return {"error": f"Docling processing error: {status_msg}"}

        # 获取文档对象
        document = getattr(docling_result, 'document', None)
        if not document:
            logger.error("Docling conversion successful but no document object returned")
            return {"error": "Docling error: No document object in result"}

        # 创建结果字典
        result = {
            "document_type": "PDF",
            "processed_at": datetime.now().isoformat(),
            "original_filename": os.path.basename(file_path),
            "conversion_time": conversion_time
        }

        # 提取页面信息
        all_text = []
        if hasattr(document, 'pages'):
            pages = document.pages
            result["page_count"] = len(pages)
            logger.info(f"Document has {len(pages)} pages")

            # 遍历页面
            result_pages = []
            for i, page in enumerate(pages):
                page_data = {
                    "page_number": i+1,
                    "elements": []
                }

                # 提取页面元素
                if hasattr(page, 'elements'):
                    elements = page.elements
                    logger.info(f"Page {i+1} has {len(elements)} elements")

                    # 提取页面文本
                    page_text_parts = []

                    # 遍历元素
                    for j, elem in enumerate(elements):
                        elem_data = {
                            "element_id": f"elem_{i+1}_{j+1}",
                            "element_type": "unknown"
                        }

                        # 提取元素类型
                        if hasattr(elem, 'type'):
                            elem_type = elem.type
                            if hasattr(elem_type, 'name'):
                                elem_data["element_type"] = elem_type.name
                            elif hasattr(elem_type, 'value'):
                                elem_data["element_type"] = elem_type.value

                        # 提取元素文本
                        if hasattr(elem, 'text'):
                            elem_data["text"] = elem.text
                            page_text_parts.append(elem.text)
                        elif hasattr(elem, 'texts'):
                            texts = elem.texts
                            text_parts = []
                            for text_run in texts:
                                if hasattr(text_run, 'text'):
                                    text_parts.append(text_run.text)
                            if text_parts:
                                elem_data["text"] = "".join(text_parts)
                                page_text_parts.append("".join(text_parts))

                        page_data["elements"].append(elem_data)

                    # 合并页面文本
                    page_data["text"] = "\n".join(page_text_parts)
                    all_text.append(page_data["text"])

                result_pages.append(page_data)

            result["pages"] = result_pages

        # 添加完整文本
        result["full_text"] = "\n".join(all_text)

        # ---- START DEBUG PRINTS ----
        logger.info(f"Docling Result (inside process_with_docling): {result}")
        if hasattr(document, 'pages') and document.pages:
            for i, page_obj in enumerate(document.pages):
                logger.info(f"  Page {i+1} Object Type: {type(page_obj)}")
                logger.info(f"  Page {i+1} Object Attributes: {dir(page_obj)}")
                if hasattr(page_obj, 'text_content'): # Common attribute for page text
                    logger.info(f"  Page {i+1} Text (from .text_content): {str(page_obj.text_content)[:500]}")
                elif hasattr(page_obj, 'text'):
                    logger.info(f"  Page {i+1} Text (from .text): {str(page_obj.text)[:500]}")

                # Log elements within the page to see their text attributes
                if hasattr(page_obj, 'elements') and page_obj.elements:
                    logger.info(f"  Page {i+1} has {len(page_obj.elements)} elements.")
                    for elem_idx, elem in enumerate(page_obj.elements):
                        elem_text_parts_debug = []
                        if hasattr(elem, 'text'):
                            elem_text_parts_debug.append(f"elem.text: {str(elem.text)}")
                        if hasattr(elem, 'texts'):
                             elem_text_parts_debug.append(f"elem.texts: {[str(t.text) for t in elem.texts if hasattr(t, 'text')]}")
                        if elem_text_parts_debug:
                            logger.info(f"    Element {elem_idx + 1} Text Debug: {' | '.join(elem_text_parts_debug)[:200]}")
        # ---- END DEBUG PRINTS ----

        return result

    except Exception as e:
        logger.exception(f"Error processing file with Docling: {e}")
        return {"error": f"Error processing file with Docling: {str(e)}"}

def process_with_gemini(pdf_path: str, prompt: Optional[str] = None, language: str = "auto") -> Dict[str, Any]:
    """
    使用Gemini处理PDF文件

    Args:
        pdf_path: PDF文件路径
        prompt: 可选的提示词，用于指导Gemini的处理
        language: 文档语言，用于选择合适的提示词

    Returns:
        包含Gemini处理结果的字典
    """
    if not GEMINI_ENABLED:
        return {"error": "Gemini API not available"}

    try:
        # 检查文件是否存在
        if not os.path.exists(pdf_path):
            return {"error": f"File not found: {pdf_path}"}

        # 读取PDF文件为bytes
        with open(pdf_path, "rb") as f:
            pdf_bytes = f.read()

        # 将PDF转换为base64
        pdf_base64 = base64.b64encode(pdf_bytes).decode("utf-8")

        # 注意：不再使用 Part 类，因为它在当前版本中不可用

        # 设置默认提示词
        if not prompt:
            if language.lower() == "ja" or language.lower() == "japanese":
                prompt = DEFAULT_JAPANESE_PDF_PROMPT
            else:
                prompt = DEFAULT_PDF_PROMPT

        # 使用Gemini处理PDF
        model = genai.GenerativeModel(DEFAULT_GEMINI_MODEL)

        # 创建请求，尝试多种格式
        try:
            # 尝试方法1: 使用内联数据格式
            logger.info(f"Attempting to generate content with model {DEFAULT_GEMINI_MODEL} using inline data format.")

            # 创建请求内容
            content = [
                {"role": "user", "parts": [{"text": prompt}]},
                {"role": "user", "parts": [{"inline_data": {"mime_type": "application/pdf", "data": pdf_base64}}]}
            ]

            response = model.generate_content(
                content,
                generation_config={
                    "temperature": 0.2,
                    "top_p": 0.8,
                    "top_k": 40,
                    "max_output_tokens": 8192,
                }
            )
            logger.info("Successfully used inline data format with Gemini.")
        except Exception as e_inline_data:
            logger.warning(f"Failed to use inline data format: {e_inline_data}. Trying fallback.")

            # 尝试方法2: 使用简化的API格式
            try:
                logger.info("Trying simplified API format...")

                # 创建简化的请求内容
                response = model.generate_content(
                    prompt + "\n\n[PDF文档内容已提取，但由于技术限制无法直接传递]",
                    generation_config={
                        "temperature": 0.2,
                        "top_p": 0.8,
                        "top_k": 40,
                        "max_output_tokens": 8192,
                    }
                )
                logger.info("Successfully used simplified API format")
            except Exception as e_new_api:
                logger.warning(f"Failed to use new API format: {e_new_api}. Trying simpler approach.")

                # 尝试方法3: 使用最简单的方法，只发送提示文本
                try:
                    logger.info("Trying text-only approach with PDF content summary...")
                    # 尝试从PDF中提取一些文本作为上下文
                    context = ""
                    # 直接从文件中提取一些文本
                    if PYPDF2_AVAILABLE:
                        try:
                            with open(pdf_path, "rb") as f:
                                pdf_reader = PyPDF2.PdfReader(f)
                                if len(pdf_reader.pages) > 0:
                                    # 提取第一页文本
                                    context = pdf_reader.pages[0].extract_text() or ""
                                    # 限制长度
                                    context = context[:1000]
                        except Exception as e_extract:
                            logger.warning(f"Failed to extract text from PDF: {e_extract}")

                    # 创建带有上下文的提示
                    enhanced_prompt = f"{prompt}\n\n文档内容摘要:\n{context}"

                    response = model.generate_content(
                        enhanced_prompt,
                        generation_config={
                            "temperature": 0.2,
                            "top_p": 0.8,
                            "top_k": 40,
                            "max_output_tokens": 8192,
                        }
                    )
                    logger.info("Successfully used text-only approach")
                except Exception as e_text_only:
                    logger.error(f"All attempts to generate content with Gemini failed. Last error: {e_text_only}")
                    raise Exception(f"Failed to generate content with Gemini after trying all formats")

        # 解析响应
        result = {
            "processed_at": datetime.now().isoformat(),
            "original_filename": os.path.basename(pdf_path),
            "gemini_response": response.text
        }

        # 尝试解析JSON响应
        try:
            # 查找JSON部分
            json_start = response.text.find('{')
            json_end = response.text.rfind('}') + 1
            if json_start >= 0 and json_end > json_start:
                json_str = response.text[json_start:json_end]
                parsed_json = json.loads(json_str)
                result["parsed_result"] = parsed_json

                # 提取关键信息
                if "title" in parsed_json:
                    result["title"] = parsed_json["title"]
                if "document_type" in parsed_json:
                    result["document_type"] = parsed_json["document_type"]
                if "language" in parsed_json:
                    result["language"] = parsed_json["language"]
                if "summary" in parsed_json:
                    result["summary"] = parsed_json["summary"]
                if "key_points" in parsed_json:
                    result["key_points"] = parsed_json["key_points"]
                if "translation" in parsed_json:
                    translation_content = parsed_json["translation"]
                    if isinstance(translation_content, dict):
                        # 将字典的值拼接起来，或者根据需要选择特定字段
                        result["translation"] = " | ".join(str(v) for v in translation_content.values())
                        logger.info(f"Converted dict translation to string: {result['translation'][:200]}...")
                    elif isinstance(translation_content, str):
                        result["translation"] = translation_content
                    else:
                        result["translation"] = str(translation_content) # Fallback
        except Exception as e:
            logger.warning(f"Failed to parse JSON from Gemini response: {e}")
            result["parsed_result"] = None

        return result

    except Exception as e:
        logger.exception(f"Error processing file with Gemini: {e}")
        return {"error": f"Error processing file with Gemini: {str(e)}"}

def process_pdf(file_path: str, use_pypdf2: bool = True, use_docling: bool = True,
                use_gemini: bool = True, force_ocr: bool = False,
                gemini_prompt: Optional[str] = None, language: str = "auto") -> Dict[str, Any]:
    """
    综合处理PDF文件，结合PyPDF2、Docling和Gemini的能力

    Args:
        file_path: PDF文件路径
        use_pypdf2: 是否使用PyPDF2处理
        use_docling: 是否使用Docling处理
        use_gemini: 是否使用Gemini处理
        force_ocr: 是否强制使用OCR（仅适用于Docling）
        gemini_prompt: 可选的Gemini提示词
        language: 文档语言，用于选择合适的提示词

    Returns:
        包含处理结果的字典
    """
    start_time = datetime.now()

    # 创建处理信息
    processing_info = {
        "pypdf2_used": use_pypdf2 and PYPDF2_AVAILABLE,
        "docling_used": use_docling and DOCLING_AVAILABLE,
        "gemini_used": use_gemini and GEMINI_ENABLED,
        "force_ocr_used": force_ocr,
        "status": "success"
    }

    # 创建结果字典
    result = {
        "document_metadata": {
            "original_filename": os.path.basename(file_path),
            "processed_at": datetime.now().isoformat()
        },
        "processing_info": processing_info,
        "pages": [],
        "full_text": "",
        "tables": [],
        "images": []
    }

    # 使用PyPDF2处理
    pypdf2_result = None
    if use_pypdf2 and PYPDF2_AVAILABLE:
        logger.info(f"Processing with PyPDF2: {file_path}")
        pypdf2_result = extract_text_with_pypdf2(file_path)

        if "error" not in pypdf2_result:
            # 更新文档元数据
            if "page_count" in pypdf2_result:
                result["document_metadata"]["page_count"] = pypdf2_result["page_count"]
            if "title" in pypdf2_result:
                result["document_metadata"]["title"] = pypdf2_result["title"]
            if "author" in pypdf2_result:
                result["document_metadata"]["author"] = pypdf2_result["author"]
            if "creation_date" in pypdf2_result:
                result["document_metadata"]["creation_date"] = pypdf2_result["creation_date"]

            # 更新页面内容
            if "pages" in pypdf2_result:
                for page_data in pypdf2_result["pages"]:
                    result["pages"].append({
                        "page_number": page_data["page_number"],
                        "text": page_data["text"],
                        "tables": [],
                        "images": []
                    })

            # 更新全文
            if "full_text" in pypdf2_result:
                result["full_text"] = pypdf2_result["full_text"]
        else:
            logger.warning(f"PyPDF2 processing failed: {pypdf2_result['error']}")

    # 使用Docling处理
    docling_result = None
    if use_docling and DOCLING_AVAILABLE:
        logger.info(f"Processing with Docling: {file_path}")
        docling_result = process_with_docling(file_path, force_ocr)

        if "error" not in docling_result:
            # 如果PyPDF2没有提取到文本，使用Docling的结果
            if not result["full_text"] and "full_text" in docling_result:
                result["full_text"] = docling_result["full_text"]

            # 如果PyPDF2没有提取到页面内容，使用Docling的结果
            if not result["pages"] and "pages" in docling_result:
                for page_data in docling_result["pages"]:
                    result["pages"].append({
                        "page_number": page_data["page_number"],
                        "text": page_data.get("text", ""),
                        "tables": [],
                        "images": []
                    })

            # 更新文档元数据
            if "page_count" in docling_result and not result["document_metadata"].get("page_count"):
                result["document_metadata"]["page_count"] = docling_result["page_count"]
        else:
            logger.warning(f"Docling processing failed: {docling_result['error']}")

    # 使用Gemini处理
    gemini_result = None
    if use_gemini and GEMINI_ENABLED:
        logger.info(f"Processing with Gemini: {file_path}")
        gemini_result = process_with_gemini(file_path, gemini_prompt, language)

        if "error" not in gemini_result:
            # 创建Gemini分析结果
            gemini_analysis = {
                "raw_response": gemini_result.get("gemini_response", "")
            }

            # 提取解析结果
            if "parsed_result" in gemini_result:
                parsed_result = gemini_result["parsed_result"]

                # 提取摘要
                if "summary" in parsed_result:
                    gemini_analysis["summary"] = parsed_result["summary"]

                # 提取关键点
                if "key_points" in parsed_result:
                    gemini_analysis["key_points"] = parsed_result["key_points"]

                # 提取结构化数据
                gemini_analysis["structured_data"] = parsed_result

                # 提取翻译
                if "translation" in parsed_result:
                    translation_content = parsed_result["translation"]
                    if isinstance(translation_content, dict):
                        # 将字典的值拼接起来，或者根据需要选择特定字段
                        gemini_analysis["translation"] = " | ".join(str(v) for v in translation_content.values())
                        logger.info(f"Converted dict translation to string: {gemini_analysis['translation'][:200]}...")
                    elif isinstance(translation_content, str):
                        gemini_analysis["translation"] = translation_content
                    else:
                        gemini_analysis["translation"] = str(translation_content) # Fallback

                # 提取表格
                if "tables" in parsed_result:
                    tables = parsed_result["tables"]
                    for i, table in enumerate(tables):
                        try:
                            table_info = {
                                "table_id": f"table_{i+1}",
                                "page_number": 1,  # 默认值，Gemini可能无法确定表格所在页面
                                "title": table.get("table_title"),
                                "raw_text": table.get("content"),
                                "rows": []  # 初始化为空列表
                            }

                            # 尝试提取表格行和列
                            content = table.get("content")
                            if isinstance(content, dict) and "headers" in content and "rows" in content:
                                table_info["headers"] = content["headers"]
                                # 确保rows是列表的列表
                                if isinstance(content["rows"], list):
                                    if content["rows"] and isinstance(content["rows"][0], list):
                                        # 已经是正确的格式
                                        table_info["rows"] = content["rows"]
                                    elif content["rows"] and isinstance(content["rows"][0], dict):
                                        # 字典列表，需要转换
                                        rows = []
                                        for row_dict in content["rows"]:
                                            row = [str(row_dict.get(header, "")) for header in content["headers"]]
                                            rows.append(row)
                                        table_info["rows"] = rows
                                    else:
                                        # 其他情况，使用空列表
                                        table_info["rows"] = []
                                else:
                                    # 不是列表，使用空列表
                                    table_info["rows"] = []
                            elif isinstance(content, list) and all(isinstance(item, dict) for item in content):
                                # 如果内容是字典列表，尝试提取表头和行
                                if content:
                                    headers = list(content[0].keys())
                                    table_info["headers"] = headers
                                    rows = []
                                    for item in content:
                                        row = [str(item.get(header, "")) for header in headers]
                                        rows.append(row)
                                    table_info["rows"] = rows
                            elif isinstance(content, str):
                                # 如果内容是字符串，尝试解析表格结构
                                try:
                                    # 分割行
                                    lines = content.strip().split('\n')
                                    if lines:
                                        # 第一行可能是表头
                                        header_line = lines[0]
                                        # 使用 | 分割列
                                        if '|' in header_line:
                                            headers = [h.strip() for h in header_line.split('|')]
                                            table_info["headers"] = headers

                                            # 处理数据行
                                            rows = []
                                            for line in lines[1:]:
                                                if '|' in line:
                                                    row = [cell.strip() for cell in line.split('|')]
                                                    rows.append(row)
                                            table_info["rows"] = rows
                                except Exception as e:
                                    logger.warning(f"Failed to parse table content string: {e}")
                                    # 保存原始文本
                                    table_info["raw_text"] = content

                            # 确保rows是列表的列表
                            if "rows" in table_info and not isinstance(table_info["rows"], list):
                                table_info["rows"] = []
                            elif "rows" in table_info and table_info["rows"] and not isinstance(table_info["rows"][0], list):
                                # 如果rows是字典列表，尝试转换
                                if isinstance(table_info["rows"], list) and table_info["rows"] and isinstance(table_info["rows"][0], dict):
                                    # 获取所有键
                                    all_keys = set()
                                    for row_dict in table_info["rows"]:
                                        all_keys.update(row_dict.keys())

                                    # 转换为有序列表
                                    keys_list = sorted(list(all_keys))

                                    # 创建表头
                                    if "headers" not in table_info or not table_info["headers"]:
                                        table_info["headers"] = keys_list

                                    # 转换每一行
                                    new_rows = []
                                    for row_dict in table_info["rows"]:
                                        row_values = [str(row_dict.get(key, "")) for key in keys_list]
                                        new_rows.append(row_values)

                                    table_info["rows"] = new_rows
                                else:
                                    # 其他情况，使用空列表
                                    table_info["rows"] = []

                            result["tables"].append(table_info)
                        except Exception as e:
                            logger.warning(f"Failed to process table {i+1}: {e}")
                            # 添加一个简单的表格信息
                            result["tables"].append({
                                "table_id": f"table_{i+1}",
                                "page_number": 1,
                                "title": table.get("table_title", f"Table {i+1}"),
                                "raw_text": str(table.get("content", "")),
                                "rows": []
                            })

                # 提取图像
                if "images" in parsed_result:
                    images = parsed_result["images"]
                    for i, image in enumerate(images):
                        image_info = {
                            "image_id": f"image_{i+1}",
                            "page_number": 1,  # 默认值，Gemini可能无法确定图像所在页面
                            "description": image.get("description") or image.get("image_description")
                        }
                        result["images"].append(image_info)

                # 更新文档元数据
                if "title" in parsed_result and not result["document_metadata"].get("title"):
                    result["document_metadata"]["title"] = parsed_result["title"]
                if "document_type" in parsed_result:
                    result["document_metadata"]["document_type"] = parsed_result["document_type"]
                if "language" in parsed_result:
                    result["document_metadata"]["language"] = parsed_result["language"]

            result["gemini_analysis"] = gemini_analysis
        else:
            logger.warning(f"Gemini processing failed: {gemini_result['error']}")

    # 计算处理时间
    end_time = datetime.now()
    processing_time = (end_time - start_time).total_seconds()
    result["processing_info"]["processing_time_seconds"] = processing_time

    # 检查处理结果
    if not result["full_text"] and not result["pages"]:
        result["processing_info"]["status"] = "error"
        result["processing_info"]["error_message"] = "Failed to extract any text from the document"

    return result


# --- API Endpoints ---
@app.post("/api/ocr/upload", response_model=OcrResponse)
async def ocr_upload(
    file: UploadFile = File(...),
    use_pypdf2: bool = True,
    use_docling: bool = True,
    use_gemini: bool = True,
    force_ocr: bool = False,
    language: str = "auto"
):
    """
    接收文件上传，使用多层处理策略进行OCR和文档结构解析，
    并返回结构化的JSON。

    Args:
        file: 上传的PDF文件
        use_pypdf2: 是否使用PyPDF2处理
        use_docling: 是否使用Docling处理
        use_gemini: 是否使用Gemini处理
        force_ocr: 是否强制使用OCR（仅适用于Docling）
        language: 文档语言，用于选择合适的提示词

    Returns:
        OcrResponse: 包含处理结果的结构化JSON
    """
    temp_file_path = None
    try:
        # 1. 保存上传文件到临时位置
        # 清理文件名，防止目录遍历或其他攻击
        safe_filename = "".join(c if c.isalnum() or c in ('.', '_', '-') else '_' for c in file.filename)
        temp_file_path = os.path.join(UPLOAD_DIR, f"{uuid.uuid4().hex}_{safe_filename}")

        with open(temp_file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        logger.info(f"File '{file.filename}' uploaded to '{temp_file_path}'")

        # 2. 处理PDF文件
        logger.info(f"Processing PDF file: {temp_file_path}")
        result = process_pdf(
            file_path=temp_file_path,
            use_pypdf2=use_pypdf2 and PYPDF2_AVAILABLE,
            use_docling=use_docling and DOCLING_AVAILABLE,
            use_gemini=use_gemini and GEMINI_ENABLED,
            force_ocr=force_ocr,
            language=language
        )

        # 3. 创建响应
        # 创建处理信息
        processing_info = ProcessingInfo(
            pypdf2_used=result["processing_info"]["pypdf2_used"],
            docling_used=result["processing_info"]["docling_used"],
            gemini_used=result["processing_info"]["gemini_used"],
            force_ocr_used=result["processing_info"]["force_ocr_used"],
            processing_time_seconds=result["processing_info"]["processing_time_seconds"],
            status=result["processing_info"]["status"],
            error_message=result["processing_info"].get("error_message")
        )

        # 创建文档元数据
        document_metadata = DocumentMetadata(
            original_filename=result["document_metadata"]["original_filename"],
            processed_at=result["document_metadata"]["processed_at"],
            page_count=result["document_metadata"].get("page_count"),
            source_format=result["document_metadata"].get("source_format"),
            language=result["document_metadata"].get("language"),
            title=result["document_metadata"].get("title"),
            author=result["document_metadata"].get("author"),
            creation_date=result["document_metadata"].get("creation_date")
        )

        # 创建页面内容
        pages = []
        for page_data in result["pages"]:
            page = PageContent(
                page_number=page_data["page_number"],
                text=page_data["text"],
                tables=page_data.get("tables", []),
                images=page_data.get("images", [])
            )
            pages.append(page)

        # 创建Gemini分析结果
        gemini_analysis = None
        if "gemini_analysis" in result:
            gemini_data = result["gemini_analysis"]
            gemini_analysis = GeminiAnalysis(
                summary=gemini_data.get("summary"),
                key_points=gemini_data.get("key_points", []),
                structured_data=gemini_data.get("structured_data"),
                translation=gemini_data.get("translation"),
                raw_response=gemini_data.get("raw_response")
            )

        # 处理表格数据
        tables = []
        for table in result.get("tables", []):
            # 确保raw_text是字符串
            if "raw_text" in table and not isinstance(table["raw_text"], str):
                try:
                    table["raw_text"] = json.dumps(table["raw_text"])
                except Exception as e:
                    logger.warning(f"Failed to convert table raw_text to JSON string: {e}")
                    table["raw_text"] = str(table["raw_text"])

            # 确保rows是列表的列表格式
            if "rows" in table:
                # 如果rows是字典列表或其他格式，转换为列表的列表
                if not isinstance(table["rows"], list) or (table["rows"] and not isinstance(table["rows"][0], list)):
                    try:
                        # 如果是字典列表，尝试提取值
                        if isinstance(table["rows"], list) and table["rows"] and isinstance(table["rows"][0], dict):
                            # 获取所有键
                            all_keys = set()
                            for row_dict in table["rows"]:
                                all_keys.update(row_dict.keys())

                            # 转换为有序列表
                            keys_list = sorted(list(all_keys))

                            # 创建表头
                            if "headers" not in table or not table["headers"]:
                                table["headers"] = keys_list

                            # 转换每一行
                            new_rows = []
                            for row_dict in table["rows"]:
                                row_values = [str(row_dict.get(key, "")) for key in keys_list]
                                new_rows.append(row_values)

                            table["rows"] = new_rows
                        # 如果是字典，可能是Gemini返回的表格内容
                        elif isinstance(table["rows"], dict):
                            # 创建一个空的行列表
                            table["rows"] = []
                    except Exception as e:
                        logger.warning(f"Failed to convert table rows to list format: {e}")
                        # 如果转换失败，使用空列表
                        table["rows"] = []

            # 创建TableInfo对象
            try:
                tables.append(TableInfo(**table))
            except Exception as e:
                logger.warning(f"Failed to create TableInfo object: {e}. Skipping this table.")
                # 记录详细信息以便调试
                logger.debug(f"Table data: {table}")

        # 创建最终响应
        response = OcrResponse(
            document_metadata=document_metadata,
            processing_info=processing_info,
            pages=pages,
            full_text=result["full_text"],
            tables=tables,
            images=result.get("images", []),
            gemini_analysis=gemini_analysis
        )

        logger.info(f"Successfully processed and converted '{file.filename}'")
        return response

    except Exception as e:
        logger.exception(f"Error processing file {file.filename}: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")
    finally:
        # 4. 清理临时文件
        if temp_file_path:
            cleanup_temp_file(temp_file_path)
            logger.info(f"Cleaned up temporary file: {temp_file_path}")

@app.get("/api/ocr/status")
async def ocr_status():
    """
    获取OCR服务的状态信息

    Returns:
        包含服务状态信息的JSON
    """
    return {
        "status": "running",
        "version": "1.0.0",
        "capabilities": {
            "pypdf2_available": PYPDF2_AVAILABLE,
            "docling_available": DOCLING_AVAILABLE,
            "gemini_enabled": GEMINI_ENABLED,
            "default_gemini_model": DEFAULT_GEMINI_MODEL if GEMINI_ENABLED else None
        },
        "cache_dir": os.path.abspath(LOCAL_CACHE_DIR),
        "upload_dir": os.path.abspath(UPLOAD_DIR)
    }

# --- Main execution (for local testing) ---
if __name__ == "__main__":
    import uvicorn
    # Make sure to install uvicorn and python-multipart:
    # pip install uvicorn python-multipart PyPDF2 google-generativeai Pillow
    # May also need to install docling if you want to use it:
    # pip install docling easyocr

    logger.info("Starting OCR microservice with Uvicorn on http://127.0.0.1:8011")
    uvicorn.run(app, host="127.0.0.1", port=8011)

# Example of how to run from command line:
# python ocr_service.py

# Example of how to test with curl:
# curl -X POST -F "file=@/path/to/your/document.pdf" http://127.0.0.1:8009/api/ocr/upload