"""
测试Docling+Gemini OCR解决方案
"""

import os
import sys
import json
from datetime import datetime

def print_json_pretty(json_data, indent=2):
    """美化打印JSON数据"""
    print(json.dumps(json_data, indent=indent, ensure_ascii=False))

def test_pdf_processing(file_path, use_docling=True, use_gemini=True, use_pypdf2=True, force_ocr=False, prompt=None):
    """测试PDF处理功能"""
    from docling_gemini_ocr import process_pdf

    print(f"开始处理PDF文件: {file_path}")
    print(f"使用Docling: {'是' if use_docling else '否'}")
    print(f"使用Gemini: {'是' if use_gemini else '否'}")
    print(f"使用PyPDF2: {'是' if use_pypdf2 else '否'}")
    print(f"强制OCR: {'是' if force_ocr else '否'}")
    if prompt:
        print(f"使用自定义提示词: {prompt[:50]}..." if len(prompt) > 50 else prompt)

    # 处理PDF
    start_time = datetime.now()
    result = process_pdf(
        file_path=file_path,
        use_docling=use_docling,
        use_gemini=use_gemini,
        use_pypdf2=use_pypdf2,
        force_ocr=force_ocr,
        gemini_prompt=prompt
    )
    end_time = datetime.now()
    processing_time = (end_time - start_time).total_seconds()

    print(f"\n处理完成，耗时: {processing_time:.2f}秒")

    if "error" in result:
        print(f"处理失败: {result['error']}")
        return

    # 显示结果摘要
    print("\n结果摘要:")

    # PyPDF2结果
    if use_pypdf2 and "pypdf2_result" in result:
        pypdf2_result = result["pypdf2_result"]
        if "error" in pypdf2_result:
            print(f"PyPDF2处理失败: {pypdf2_result['error']}")
        else:
            print("\n--- PyPDF2处理结果 ---")
            if "pages" in pypdf2_result:
                print(f"页面数量: {len(pypdf2_result['pages'])}")

                # 显示每页文本长度
                for page in pypdf2_result["pages"]:
                    text_length = len(page.get('text', ''))
                    print(f"第 {page['page_number']} 页: {text_length} 个字符")

            # 显示文本预览
            if "full_text" in pypdf2_result and pypdf2_result["full_text"]:
                full_text = pypdf2_result["full_text"]
                preview = full_text[:200] + "..." if len(full_text) > 200 else full_text
                print(f"\nPyPDF2提取的文本预览:\n{preview}")
            else:
                print("\nPyPDF2未提取到文本")

    # Docling结果
    if use_docling and "docling_result" in result:
        docling_result = result["docling_result"]
        if "error" in docling_result:
            print(f"Docling处理失败: {docling_result['error']}")
        else:
            print("\n--- Docling处理结果 ---")
            if "pages" in docling_result:
                print(f"页面数量: {len(docling_result['pages'])}")

                # 显示每页元素数量
                for page in docling_result["pages"]:
                    print(f"第 {page['page_number']} 页: {len(page['elements'])} 个元素")

            # 显示文本预览
            if "full_text" in docling_result and docling_result["full_text"]:
                full_text = docling_result["full_text"]
                preview = full_text[:200] + "..." if len(full_text) > 200 else full_text
                print(f"\nDocling提取的文本预览:\n{preview}")
            else:
                print("\nDocling未提取到文本")

    # Gemini结果
    if use_gemini and "gemini_result" in result:
        gemini_result = result["gemini_result"]
        if "error" in gemini_result:
            print(f"Gemini处理失败: {gemini_result['error']}")
        else:
            print("\n--- Gemini处理结果 ---")

            # 显示解析后的JSON结果
            if "parsed_result" in gemini_result and gemini_result["parsed_result"]:
                print("\nGemini解析结果:")
                print_json_pretty(gemini_result["parsed_result"])

            # 显示原始响应预览
            if "gemini_response" in gemini_result and gemini_result["gemini_response"]:
                gemini_response = gemini_result["gemini_response"]
                preview = gemini_response[:200] + "..." if len(gemini_response) > 200 else gemini_response
                print(f"\nGemini原始响应预览:\n{preview}")
            else:
                print("\nGemini未返回响应")

    # 显示输出文件路径
    if "output_files" in result and "json" in result["output_files"]:
        print(f"\n结果已保存到: {result['output_files']['json']}")

    return result

def main():
    """主函数"""
    import argparse

    parser = argparse.ArgumentParser(description="测试Docling+Gemini OCR解决方案")
    parser.add_argument("file_path", help="PDF文件路径")

    # 处理方法选项
    method_group = parser.add_argument_group('处理方法选项')
    method_group.add_argument("--docling-only", action="store_true", help="仅使用Docling处理")
    method_group.add_argument("--gemini-only", action="store_true", help="仅使用Gemini处理（使用Gemini 2.5 Pro Preview）")
    method_group.add_argument("--pypdf2-only", action="store_true", help="仅使用PyPDF2处理（适用于非扫描版PDF）")
    method_group.add_argument("--no-pypdf2", action="store_true", help="不使用PyPDF2处理")

    # OCR选项
    ocr_group = parser.add_argument_group('OCR选项')
    ocr_group.add_argument("--force-ocr", action="store_true", help="强制使用OCR，即使PDF包含文本层（适用于扫描版PDF）")

    # Gemini选项
    gemini_group = parser.add_argument_group('Gemini选项')
    gemini_group.add_argument("--prompt-file", help="包含Gemini提示词的文本文件路径")
    args = parser.parse_args()

    file_path = args.file_path
    if not os.path.exists(file_path):
        print(f"错误: 文件 '{file_path}' 不存在")
        sys.exit(1)

    # 设置处理选项
    if args.pypdf2_only:
        use_docling = False
        use_gemini = False
        use_pypdf2 = True
    elif args.docling_only:
        use_docling = True
        use_gemini = False
        use_pypdf2 = not args.no_pypdf2
    elif args.gemini_only:
        use_docling = False
        use_gemini = True
        use_pypdf2 = not args.no_pypdf2
    else:
        use_docling = True
        use_gemini = True
        use_pypdf2 = not args.no_pypdf2

    # 读取提示词文件
    prompt = None
    if args.prompt_file and os.path.exists(args.prompt_file):
        with open(args.prompt_file, "r", encoding="utf-8") as f:
            prompt = f.read()

    # 测试PDF处理
    test_pdf_processing(
        file_path=file_path,
        use_docling=use_docling,
        use_gemini=use_gemini,
        use_pypdf2=use_pypdf2,
        force_ocr=args.force_ocr,
        prompt=prompt
    )

if __name__ == "__main__":
    main()
