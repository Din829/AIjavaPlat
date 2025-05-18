"""
Docling + Gemini OCR处理程序

这个程序结合了Docling的PDF处理能力和Gemini 2.5 Pro的理解能力，
用于处理PDF文件（包括扫描版和非扫描版）并提取结构化信息。
"""

import os
import sys
import json
import logging
import base64
from datetime import datetime
from typing import Dict, Any, Optional, List

import google.generativeai as genai

# 尝试导入PyPDF2，用于直接提取PDF文本
try:
    import PyPDF2
    PYPDF2_AVAILABLE = True
except ImportError:
    PYPDF2_AVAILABLE = False
    logging.warning("PyPDF2 not available. Direct PDF text extraction will be disabled.")

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 创建本地缓存目录
LOCAL_CACHE_DIR = "docling_cache"
os.makedirs(LOCAL_CACHE_DIR, exist_ok=True)

# 设置环境变量
os.environ["HF_HOME"] = os.path.abspath(LOCAL_CACHE_DIR)
os.environ["TRANSFORMERS_CACHE"] = os.path.join(os.path.abspath(LOCAL_CACHE_DIR), "transformers")
os.environ["HF_DATASETS_CACHE"] = os.path.join(os.path.abspath(LOCAL_CACHE_DIR), "datasets")
os.environ["HUGGINGFACE_HUB_CACHE"] = os.path.join(os.path.abspath(LOCAL_CACHE_DIR), "hub")
os.environ["DOCLING_CACHE_DIR"] = os.path.abspath(LOCAL_CACHE_DIR)

# 创建所有必要的子目录
for subdir in ["transformers", "datasets", "hub"]:
    os.makedirs(os.path.join(LOCAL_CACHE_DIR, subdir), exist_ok=True)

logger.info(f"Using local cache directory: {os.path.abspath(LOCAL_CACHE_DIR)}")

# Gemini API配置
GEMINI_API_KEY = "AIzaSyDFLyEYqgaC6plSFF5IjvQEW0FEug6o14o"  # 使用提供的API密钥

# 配置Gemini API
try:
    # 尝试使用最新的API版本
    genai.configure(api_key=GEMINI_API_KEY, transport="rest")
    logger.info("Configured Gemini API with REST transport")
except Exception as e:
    logger.warning(f"Failed to configure Gemini API with REST transport: {e}")
    # 回退到默认配置
    genai.configure(api_key=GEMINI_API_KEY)
    logger.info("Configured Gemini API with default settings")

def process_with_docling(file_path: str, force_ocr: bool = False) -> Dict[str, Any]:
    """
    使用Docling处理PDF文件

    Args:
        file_path: 要处理的PDF文件路径
        force_ocr: 是否强制使用OCR，即使PDF包含文本层

    Returns:
        包含提取文本和元数据的字典
    """
    try:
        # 导入Docling
        from docling.document_converter import DocumentConverter
        logger.info("Successfully imported DocumentConverter from docling")

        # 尝试导入并配置pipeline_options
        try:
            from docling.pipeline_options import PipelineOptions
            pipeline_options = PipelineOptions()

            # 设置缓存目录
            pipeline_options.cache_dir = os.path.abspath(LOCAL_CACHE_DIR)

            # 启用OCR，并根据参数决定是否强制使用OCR
            pipeline_options.do_ocr = True
            pipeline_options.force_ocr = force_ocr

            # 尝试配置OCR选项，特别是对日语的支持
            try:
                from docling.pipeline_options import EasyOcrOptions
                ocr_options = EasyOcrOptions()
                # 添加日语支持
                ocr_options.languages = ["ja", "en"]
                pipeline_options.ocr_options = ocr_options
                logger.info("Configured EasyOCR with Japanese and English language support")
            except ImportError:
                logger.warning("Could not import EasyOcrOptions, using default OCR settings")

            logger.info(f"Using pipeline options with cache_dir: {pipeline_options.cache_dir}, OCR enabled: {pipeline_options.do_ocr}, force OCR: {pipeline_options.force_ocr}")
            converter = DocumentConverter(pipeline_options=pipeline_options)
        except Exception as e:
            logger.warning(f"Failed to configure custom pipeline options: {e}. Using default options.")
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

        # 获取文档对象
        document = getattr(docling_result, 'document', None)
        if not document:
            logger.error("Docling conversion successful but no document object returned")
            return {"error": "Docling error: No document object in result"}

        # 创建结果字典
        result = {
            "document_type": str(type(document)),
            "processed_at": datetime.now().isoformat(),
            "original_filename": os.path.basename(file_path),
            "pages": []
        }

        # 提取页面信息
        all_text = []
        if hasattr(document, 'pages'):
            pages = document.pages
            logger.info(f"Document has {len(pages)} pages")

            # 遍历页面
            for i, page in enumerate(pages):
                page_data = {
                    "page_number": i+1,
                    "elements": []
                }

                # 提取页面元素
                if hasattr(page, 'elements'):
                    elements = page.elements
                    logger.info(f"Page {i+1} has {len(elements)} elements")

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
                            all_text.append(elem.text)
                        elif hasattr(elem, 'texts'):
                            texts = elem.texts
                            text_parts = []
                            for text_run in texts:
                                if hasattr(text_run, 'text'):
                                    text_parts.append(text_run.text)
                            if text_parts:
                                elem_data["text"] = "".join(text_parts)
                                all_text.append("".join(text_parts))

                        # 添加元素到页面数据
                        page_data["elements"].append(elem_data)

                # 添加页面到结果
                result["pages"].append(page_data)

        # 添加完整文本
        result["full_text"] = "\n".join(all_text)

        return result

    except Exception as e:
        logger.exception(f"Error processing file {file_path}: {e}")
        return {"error": f"Error processing file: {str(e)}"}

def process_with_gemini(pdf_path: str, prompt: Optional[str] = None) -> Dict[str, Any]:
    """
    使用Gemini 2.5 Pro处理PDF文件

    Args:
        pdf_path: PDF文件路径
        prompt: 可选的提示词，用于指导Gemini的处理

    Returns:
        包含Gemini处理结果的字典
    """
    try:
        # 检查文件是否存在
        if not os.path.exists(pdf_path):
            return {"error": f"File not found: {pdf_path}"}

        # 读取PDF文件
        with open(pdf_path, "rb") as f:
            pdf_content = f.read()

        # 编码为base64
        pdf_base64 = base64.b64encode(pdf_content).decode("utf-8")

        # 设置默认提示词
        if not prompt:
            prompt = """
            请分析这个PDF文档，并提取以下信息：
            1. 文档的主要内容和主题
            2. 文档中的关键信息和数据
            3. 文档的结构和组织方式
            4. 如果有表格，请提取表格内容
            5. 如果有图表或图像，请描述它们

            请以JSON格式返回结果，包含以下字段：
            {
                "title": "文档标题",
                "summary": "文档摘要",
                "key_points": ["关键点1", "关键点2", ...],
                "tables": [{"table_title": "表格标题", "content": "表格内容"}],
                "images": [{"image_description": "图像描述"}],
                "full_text_analysis": "完整文本分析"
            }
            """

        # 使用指定的Gemini 2.5 Pro Preview 05-06模型
        model_name = 'models/gemini-2.5-pro-preview-05-06'
        logger.info(f"Using specified Gemini model: {model_name}")

        try:
            # 尝试使用指定的模型
            model = genai.GenerativeModel(model_name)
        except Exception as e:
            logger.warning(f"Failed to use specified model {model_name}: {e}")

            # 如果指定模型不可用，尝试使用其他可用模型
            available_models = ['models/gemini-1.5-pro', 'models/gemini-1.5-flash', 'models/gemini-pro-vision', 'models/gemini-pro']
            model = None

            try:
                models = genai.list_models()
                model_names = [m.name for m in models]
                logger.info(f"Available Gemini models: {model_names}")

                # 选择第一个可用的模型
                for fallback_model in available_models:
                    if fallback_model in model_names:
                        logger.info(f"Using fallback Gemini model: {fallback_model}")
                        model = genai.GenerativeModel(fallback_model)
                        break
            except Exception as e2:
                logger.warning(f"Failed to list available models: {e2}")

            # 如果无法获取模型列表，使用默认模型
            if model is None:
                logger.info("Using default Gemini model: gemini-pro")
                model = genai.GenerativeModel('gemini-pro')

        # 创建请求
        try:
            # 尝试使用Gemini 2.5 Pro Preview格式
            response = model.generate_content(
                contents=[
                    {
                        "role": "user",
                        "parts": [
                            {"text": prompt},
                            {"file_data": {"mime_type": "application/pdf", "data": pdf_base64}}
                        ]
                    }
                ],
                generation_config={
                    "temperature": 0.2,
                    "top_p": 0.8,
                    "top_k": 40,
                    "max_output_tokens": 8192,
                }
            )
            logger.info("Successfully used Gemini 2.5 Pro Preview format")
        except Exception as e:
            logger.warning(f"Failed to use Gemini 2.5 Pro Preview format: {e}")

            try:
                # 尝试使用新的API格式
                response = model.generate_content(
                    [
                        prompt,
                        {"mime_type": "application/pdf", "data": pdf_base64}
                    ]
                )
                logger.info("Successfully used new API format")
            except Exception as e2:
                logger.warning(f"Failed to use new API format: {e2}")

                try:
                    # 尝试使用旧的API格式
                    response = model.generate_content(
                        contents=[
                            {
                                "role": "user",
                                "parts": [
                                    {"text": prompt},
                                    {"inline_data": {"mime_type": "application/pdf", "data": pdf_base64}}
                                ]
                            }
                        ]
                    )
                    logger.info("Successfully used old API format")
                except Exception as e3:
                    logger.error(f"Failed to use all API formats: {e3}")
                    raise Exception(f"Failed to generate content with Gemini: {e3}")

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
        except Exception as e:
            logger.warning(f"Failed to parse JSON from Gemini response: {e}")
            result["parsed_result"] = None

        return result

    except Exception as e:
        logger.exception(f"Error processing file with Gemini: {e}")
        return {"error": f"Error processing file with Gemini: {str(e)}"}

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
                text = page.extract_text()

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
                "pages": pages,
                "full_text": "\n".join(all_text)
            }

            return result

    except Exception as e:
        logger.exception(f"Error extracting text with PyPDF2: {e}")
        return {"error": f"Error extracting text with PyPDF2: {str(e)}"}

def process_pdf(file_path: str, use_docling: bool = True, use_gemini: bool = True,
                force_ocr: bool = False, gemini_prompt: Optional[str] = None,
                use_pypdf2: bool = True) -> Dict[str, Any]:
    """
    综合处理PDF文件，结合Docling和Gemini的能力

    Args:
        file_path: PDF文件路径
        use_docling: 是否使用Docling处理
        use_gemini: 是否使用Gemini处理
        force_ocr: 是否强制使用OCR（仅适用于Docling）
        gemini_prompt: 可选的Gemini提示词
        use_pypdf2: 是否使用PyPDF2直接提取文本

    Returns:
        包含处理结果的字典
    """
    result = {
        "processed_at": datetime.now().isoformat(),
        "original_filename": os.path.basename(file_path)
    }

    # 使用PyPDF2直接提取文本
    if use_pypdf2 and PYPDF2_AVAILABLE:
        logger.info(f"Extracting text with PyPDF2: {file_path}")
        pypdf2_result = extract_text_with_pypdf2(file_path)
        result["pypdf2_result"] = pypdf2_result

    # 使用Docling处理
    if use_docling:
        logger.info(f"Processing with Docling: {file_path}")
        docling_result = process_with_docling(file_path, force_ocr)
        result["docling_result"] = docling_result

    # 使用Gemini处理
    if use_gemini:
        logger.info(f"Processing with Gemini: {file_path}")
        gemini_result = process_with_gemini(file_path, gemini_prompt)
        result["gemini_result"] = gemini_result

    # 保存结果
    output_base = os.path.splitext(os.path.basename(file_path))[0]
    output_dir = os.path.dirname(os.path.abspath(file_path))

    # 保存JSON结果
    json_file = os.path.join(output_dir, f"{output_base}_docling_gemini_result.json")
    with open(json_file, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, ensure_ascii=False)
    logger.info(f"Saved JSON output to {json_file}")

    # 添加输出文件路径到结果
    result["output_files"] = {
        "json": json_file
    }

    return result

def main():
    """主函数"""
    import argparse

    parser = argparse.ArgumentParser(description="使用Docling和Gemini处理PDF文件")
    parser.add_argument("file_path", help="PDF文件路径")
    parser.add_argument("--docling-only", action="store_true", help="仅使用Docling处理")
    parser.add_argument("--gemini-only", action="store_true", help="仅使用Gemini处理")
    parser.add_argument("--force-ocr", action="store_true", help="强制使用OCR，即使PDF包含文本层")
    parser.add_argument("--output-dir", help="输出目录，默认为当前目录")
    parser.add_argument("--prompt", help="自定义Gemini提示词")
    args = parser.parse_args()

    file_path = args.file_path
    if not os.path.exists(file_path):
        print(f"错误: 文件 '{file_path}' 不存在")
        sys.exit(1)

    # 设置处理选项
    use_docling = not args.gemini_only
    use_gemini = not args.docling_only

    print(f"开始处理PDF文件: {file_path}")
    print(f"使用Docling: {'是' if use_docling else '否'}")
    print(f"使用Gemini: {'是' if use_gemini else '否'}")
    print(f"强制OCR: {'是' if args.force_ocr else '否'}")

    # 处理PDF
    result = process_pdf(
        file_path=file_path,
        use_docling=use_docling,
        use_gemini=use_gemini,
        force_ocr=args.force_ocr,
        gemini_prompt=args.prompt
    )

    if "error" in result:
        print(f"处理失败: {result['error']}")
        sys.exit(1)

    print("\n处理成功!")

    # 显示结果摘要
    if "docling_result" in result and "full_text" in result["docling_result"]:
        full_text = result["docling_result"]["full_text"]
        if full_text:
            preview = full_text[:200] + "..." if len(full_text) > 200 else full_text
            print(f"\nDocling提取的文本预览:\n{preview}")

    if "gemini_result" in result and "gemini_response" in result["gemini_result"]:
        gemini_response = result["gemini_result"]["gemini_response"]
        if gemini_response:
            preview = gemini_response[:200] + "..." if len(gemini_response) > 200 else gemini_response
            print(f"\nGemini处理结果预览:\n{preview}")

    print("\n结果已保存到以下文件:")
    if "output_files" in result and "json" in result["output_files"]:
        print(f"- JSON: {result['output_files']['json']}")

if __name__ == "__main__":
    main()
