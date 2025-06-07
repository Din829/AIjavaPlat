# whisper_service.py
"""
Whisper转写微服务
基于n8nWhisper.py的成功架构，提供高性能语音转写功能
完全复用现有的优化配置和智能prompt构建策略
"""

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.concurrency import run_in_threadpool
from faster_whisper import WhisperModel
import os
import shutil
import tempfile
import re
import logging
from typing import Optional
from pydantic import BaseModel

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Whisper Transcription Service",
    version="1.0.0", 
    description="基于Faster-Whisper的高性能语音转写服务"
)

# 初始化 Whisper 模型（复用n8nWhisper.py的高性能配置）
logger.info("正在初始化Whisper模型...")
model = WhisperModel("large-v3", device="cuda", compute_type="float16")
logger.info("Whisper模型初始化完成")

class TranscriptionResponse(BaseModel):
    """转写响应模型"""
    language: str
    language_probability: float
    segments: list
    full_text: str
    processing_info: dict

class HealthResponse(BaseModel):
    """健康检查响应模型"""
    status: str
    service: str
    version: str
    model: str

def normalize_text(text: str) -> str:
    """
    规范化文本，忽略空格、标点和大小写差异
    完全复用n8nWhisper.py的文本处理逻辑
    """
    if not isinstance(text, str): 
        return ""
    text = re.sub(r'[^\w\s]', '', text.strip().lower())
    text = re.sub(r'\s+', ' ', text)
    return text

def build_intelligent_prompt(title: str, description: str, custom_prompt: str) -> str:
    """
    智能prompt构建，完全复用n8nWhisper.py的成功策略
    
    Args:
        title: 视频标题
        description: 视频描述
        custom_prompt: 自定义prompt
        
    Returns:
        构建的最终prompt
    """
    # 基础prompt（复用n8nWhisper.py的配置）
    base_prompt = "会议、项目、技术、商务、programming、AI、machine learning、一般会話"
    dynamic_prompt_parts = [base_prompt]

    if title and isinstance(title, str) and title.strip():
        cleaned_title = title.strip()
        dynamic_prompt_parts.append(f"Video Title Hint: {cleaned_title}")

    if description and isinstance(description, str) and description.strip():
        cleaned_desc = description.strip()
        dynamic_prompt_parts.append(f"Video Description Hint: {cleaned_desc[:150]}")
    
    if custom_prompt and isinstance(custom_prompt, str) and custom_prompt.strip():
        dynamic_prompt_parts.append(f"Custom Context: {custom_prompt}")

    final_prompt = ". ".join(dynamic_prompt_parts)
    return final_prompt

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """健康检查端点"""
    return HealthResponse(
        status="healthy",
        service="whisper-transcription-service",
        version="1.0.0",
        model="large-v3"
    )

@app.post("/transcribe", response_model=TranscriptionResponse)
async def transcribe_audio(
    file: UploadFile = File(...),
    language: Optional[str] = Form(default=None),
    title: Optional[str] = Form(default=None),
    description: Optional[str] = Form(default=None),
    custom_prompt: Optional[str] = Form(default=None)
):
    """
    音频转写服务，完全复用n8nWhisper.py的核心逻辑
    
    Args:
        file: 音频文件
        language: 语言代码
        title: 视频标题（用于增强转写精度）
        description: 视频描述（用于增强转写精度）
        custom_prompt: 自定义prompt
        
    Returns:
        转写结果，包含分段信息和完整文本
    """
    audio_path = None
    logger.info(f"收到转写请求: 文件={file.filename}, 语言={language}")
    
    try:
        # 1. 保存上传的音频文件
        with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as temp_file:
            try:
                shutil.copyfileobj(file.file, temp_file)
                audio_path = temp_file.name
                logger.info(f"音频文件已保存到: {audio_path}")
            finally:
                if hasattr(file, 'file') and not file.file.closed:
                    file.file.close()
    except Exception as e:
        if audio_path and os.path.exists(audio_path):
            try: 
                os.remove(audio_path)
            except OSError: 
                pass
        raise HTTPException(status_code=500, detail=f"保存音频文件失败: {str(e)}")

    if not audio_path or not os.path.exists(audio_path):
        raise HTTPException(status_code=500, detail="临时音频文件创建失败")

    try:
        # 2. 处理语言参数
        # 当language为"auto", "mixed"或None时，让Whisper自动检测语言
        transcribe_language = None if language in ["auto", "mixed"] or not language else language
        
        # 3. 构建智能prompt（复用n8nWhisper.py的策略）
        final_prompt = build_intelligent_prompt(title, description, custom_prompt)
        logger.info(f"使用prompt: '{final_prompt}'")

        # 4. 执行转写（复用n8nWhisper.py的高性能配置）
        logger.info("开始执行Whisper转写...")
        segments, info = await run_in_threadpool(
            model.transcribe,
            audio_path,
            language=transcribe_language,
            initial_prompt=final_prompt,
            beam_size=10,
            temperature=0.0,
            vad_filter=True,
            vad_parameters=dict(min_silence_duration_ms=1000, threshold=0.5)
        )

        # 5. 文本后处理（复用n8nWhisper.py的去重逻辑）
        prev_text = ""
        merged_segments = []
        full_text_parts = []
        
        for segment in segments:
            normalized_segment_text = normalize_text(segment.text)
            if normalized_segment_text != prev_text:
                segment_data = {
                    "start": segment.start,
                    "end": segment.end,
                    "text": segment.text.strip()
                }
                merged_segments.append(segment_data)
                full_text_parts.append(segment.text.strip())
                prev_text = normalized_segment_text

        # 6. 构建完整文本
        full_text = " ".join(full_text_parts)
        
        logger.info(f"转写完成: 语言={info.language}, 置信度={info.language_probability:.2f}, 分段数={len(merged_segments)}, 文本长度={len(full_text)}")

        return TranscriptionResponse(
            language=info.language,
            language_probability=info.language_probability,
            segments=merged_segments,
            full_text=full_text,
            processing_info={
                "segments_count": len(merged_segments),
                "text_length": len(full_text),
                "model": "large-v3",
                "prompt_used": final_prompt
            }
        )

    except Exception as e:
        logger.error(f"转写过程中发生错误: {e}")
        raise HTTPException(status_code=500, detail=f"转写失败: {str(e)}")
    finally:
        # 7. 清理临时文件
        if audio_path and os.path.exists(audio_path):
            try:
                os.remove(audio_path)
                logger.info(f"已清理临时文件: {audio_path}")
            except OSError as e:
                logger.warning(f"清理临时文件失败: {e}")

@app.get("/status")
async def get_status():
    """获取服务状态，用于健康检查"""
    return {
        "status": "running",
        "service": "whisper-transcription-service",
        "version": "1.0.0",
        "model": "large-v3",
        "device": "cuda",
        "compute_type": "float16"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=9999)
