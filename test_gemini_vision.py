#!/usr/bin/env python3
"""
测试Gemini Vision OCR功能的独立脚本
"""

import os
import io
import fitz  # PyMuPDF
from PIL import Image
import google.generativeai as genai

# 配置Gemini API
GEMINI_API_KEY = "AIzaSyDFLyEYqgaC6plSFF5IjvQEW0FEug6o14o"
genai.configure(api_key=GEMINI_API_KEY)

def test_gemini_vision_ocr(pdf_path: str):
    """
    测试Gemini Vision OCR功能
    """
    print(f"测试文件: {pdf_path}")
    
    if not os.path.exists(pdf_path):
        print(f"错误：文件不存在 {pdf_path}")
        return
    
    try:
        # 创建Gemini模型实例
        model = genai.GenerativeModel("models/gemini-2.5-pro-preview-05-06")
        print("✅ Gemini模型创建成功")
        
        # 打开PDF文件
        pdf_document = fitz.open(pdf_path)
        print(f"✅ PDF文件打开成功，共 {len(pdf_document)} 页")
        
        # 处理第一页
        page = pdf_document[0]
        
        # 将页面转换为图像
        mat = fitz.Matrix(2.0, 2.0)  # 提高分辨率
        pix = page.get_pixmap(matrix=mat)
        img_data = pix.tobytes("png")
        print(f"✅ 页面转换为图像成功，图像大小: {len(img_data)} bytes")
        
        # 将图像数据转换为PIL Image
        image = Image.open(io.BytesIO(img_data))
        print(f"✅ PIL Image创建成功，尺寸: {image.size}")
        
        # 日语OCR提示词
        prompt = """
        この画像に含まれる日本語のテキストをすべて正確に抽出してください。

        要求：
        1. 元のレイアウトと書式を保持する
        2. 漢字、ひらがな、カタカナを正確に認識する
        3. 数字、英字、記号も含める
        4. 上から下、左から右の順序でテキストを抽出する

        テキストのみを返してください。説明は不要です。
        """
        
        print("🔄 Gemini Vision OCR処理中...")
        
        # 调用Gemini Vision API
        response = model.generate_content(
            [prompt, image],
            generation_config={
                "temperature": 0.1,
                "top_p": 0.8,
                "top_k": 40,
                "max_output_tokens": 4096,
            }
        )
        
        print("✅ Gemini API调用成功")
        
        # 检查响应
        if response and hasattr(response, 'text') and response.text:
            extracted_text = response.text.strip()
            print(f"✅ 提取的文本长度: {len(extracted_text)}")
            print("=" * 50)
            print("提取的文本内容:")
            print("=" * 50)
            print(extracted_text)
            print("=" * 50)
        else:
            print("❌ 没有提取到文本")
            print(f"响应对象: {response}")
            if hasattr(response, 'text'):
                print(f"响应文本: '{response.text}'")
            else:
                print("响应对象没有text属性")
                
        # 关闭PDF文档
        pdf_document.close()
        
    except Exception as e:
        print(f"❌ 错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    # 测试PDF文件路径
    test_pdf = r"C:\Users\q9951\Desktop\卒業.pdf"
    
    print("🚀 开始测试Gemini Vision OCR")
    print("=" * 60)
    
    test_gemini_vision_ocr(test_pdf)
    
    print("=" * 60)
    print("🏁 测试完成")
