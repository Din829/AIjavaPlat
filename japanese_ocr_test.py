"""
日语PDF OCR测试程序 - 使用Docling处理日语PDF文件
"""

import os
import sys
import json
import logging
from datetime import datetime
from typing import Dict, List, Any, Optional

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

def process_japanese_pdf(file_path: str, force_ocr: bool = False) -> Dict[str, Any]:
    """
    处理日语PDF文件，使用Docling进行OCR和文档结构解析

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

        # 输出文档信息
        logger.info(f"Document type: {type(document)}")

        # 创建结果字典
        result = {
            "document_type": str(type(document)),
            "processed_at": datetime.now().isoformat(),
            "original_filename": os.path.basename(file_path),
            "pages": []
        }

        # 提取页面信息
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
                        elif hasattr(elem, 'texts'):
                            texts = elem.texts
                            text_parts = []
                            for text_run in texts:
                                if hasattr(text_run, 'text'):
                                    text_parts.append(text_run.text)
                            if text_parts:
                                elem_data["text"] = "".join(text_parts)

                        # 添加元素到页面数据
                        page_data["elements"].append(elem_data)

                # 添加页面到结果
                result["pages"].append(page_data)

        # 提取所有文本内容
        all_text = []
        for page in result["pages"]:
            for elem in page["elements"]:
                if "text" in elem and elem["text"]:
                    all_text.append(elem["text"])

        result["full_text"] = "\n".join(all_text)

        # 保存结果到文件
        output_base = os.path.splitext(os.path.basename(file_path))[0]

        # 获取输出目录（如果在函数参数中提供）
        output_dir = os.path.dirname(os.path.abspath(file_path))

        # 保存JSON
        json_file = os.path.join(output_dir, f"{output_base}_japanese_ocr.json")
        with open(json_file, "w", encoding="utf-8") as f:
            json.dump(result, f, indent=2, ensure_ascii=False)
        logger.info(f"Saved JSON output to {json_file}")

        # 保存纯文本
        text_file = os.path.join(output_dir, f"{output_base}_japanese_ocr.txt")
        with open(text_file, "w", encoding="utf-8") as f:
            f.write(result["full_text"])
        logger.info(f"Saved text output to {text_file}")

        # 添加输出文件路径到结果
        result["output_files"] = {
            "json": json_file,
            "text": text_file
        }

        return result

    except Exception as e:
        logger.exception(f"Error processing file {file_path}: {e}")
        return {"error": f"Error processing file: {str(e)}"}

def main():
    """主函数"""
    import argparse

    parser = argparse.ArgumentParser(description="处理日语PDF文件")
    parser.add_argument("file_path", help="PDF文件路径")
    parser.add_argument("--force-ocr", action="store_true", help="强制使用OCR，即使PDF包含文本层")
    parser.add_argument("--output-dir", help="输出目录，默认为当前目录")
    args = parser.parse_args()

    file_path = args.file_path
    if not os.path.exists(file_path):
        print(f"错误: 文件 '{file_path}' 不存在")
        sys.exit(1)

    # 设置输出目录
    if args.output_dir:
        if not os.path.exists(args.output_dir):
            os.makedirs(args.output_dir, exist_ok=True)
        output_dir = args.output_dir
    else:
        output_dir = os.path.dirname(os.path.abspath(file_path))

    print(f"开始处理日语PDF文件: {file_path}")
    print(f"强制OCR: {'是' if args.force_ocr else '否'}")
    print(f"输出目录: {output_dir}")

    result = process_japanese_pdf(file_path, force_ocr=args.force_ocr)

    if "error" in result:
        print(f"处理失败: {result['error']}")
        sys.exit(1)

    print("\n处理成功!")
    print(f"文档类型: {result['document_type']}")
    print(f"处理时间: {result['processed_at']}")
    print(f"页面数量: {len(result['pages'])}")

    # 打印每页元素数量
    for i, page in enumerate(result["pages"]):
        print(f"\n第 {page['page_number']} 页:")
        print(f"  元素数量: {len(page['elements'])}")

        # 打印前5个元素的类型和文本
        for j, elem in enumerate(page["elements"][:5]):
            elem_type = elem.get("element_type", "unknown")
            elem_text = elem.get("text", "")
            print(f"  元素 {j+1}:")
            print(f"    类型: {elem_type}")
            if elem_text:
                preview = elem_text[:50] + "..." if len(elem_text) > 50 else elem_text
                print(f"    文本: {preview}")

    # 打印文本预览
    full_text = result.get("full_text", "")
    if full_text:
        preview = full_text[:200] + "..." if len(full_text) > 200 else full_text
        print(f"\n提取的文本预览:\n{preview}")

    print("\n结果已保存到以下文件:")
    print(f"- JSON: {result['output_files']['json']}")
    print(f"- 文本: {result['output_files']['text']}")

if __name__ == "__main__":
    main()
