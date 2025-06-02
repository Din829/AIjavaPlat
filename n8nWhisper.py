# n8nWhisper.py (修改后的版本)
from fastapi import FastAPI, UploadFile, File, Form, HTTPException   # ✅ 加上 HTTPException
from fastapi.concurrency import run_in_threadpool   
from fastapi import FastAPI, UploadFile, File, Form
from faster_whisper import WhisperModel
import os
import shutil
import tempfile
import re
from typing import Optional # <--- 导入 Optional

app = FastAPI()

# 初始化 Whisper 模型
model = WhisperModel("large-v3", device="cuda", compute_type="float16")

def normalize_text(text: str) -> str:
    """规范化文本，忽略空格、标点和大小写差异"""
    if not isinstance(text, str): return "" # 处理非字符串输入
    text = re.sub(r'[^\w\s]', '', text.strip().lower())
    text = re.sub(r'\s+', ' ', text)
    return text

@app.post("/transcribe")
async def transcribe(
    file: UploadFile = File(...), 
    language: Optional[str] = Form(default=None), 
    title: Optional[str] = Form(default=None), 
    description: Optional[str] = Form(default=None),
    initial_prompt: Optional[str] = Form(default=None)  # ✅ 支持外部 prompt 传入
):
    audio_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as temp_file:
            try:
                shutil.copyfileobj(file.file, temp_file)
                audio_path = temp_file.name
            finally:
                if hasattr(file, 'file') and not file.file.closed:
                    file.file.close()
    except Exception as e:
        if audio_path and os.path.exists(audio_path):
            try: os.remove(audio_path)
            except OSError: pass
        raise HTTPException(status_code=500, detail=f"Failed to save uploaded file: {str(e)}") from e

    if not audio_path or not os.path.exists(audio_path):
        raise HTTPException(status_code=500, detail="Temporary audio file not created or found.")

    try:
        transcribe_language = None if language == "mixed" or not language else language

        # 默认提示词构建
        base_prompt = "会议、项目、技术、商务、programming、AI、machine learning、一般会話"
        dynamic_prompt_parts = [base_prompt]

        if title and isinstance(title, str) and title.strip():
            cleaned_title = title.strip()
            dynamic_prompt_parts.append(f"Video Title Hint: {cleaned_title}")

        if description and isinstance(description, str) and description.strip():
            cleaned_desc = description.strip()
            dynamic_prompt_parts.append(f"Video Description Hint: {cleaned_desc[:150]}")

        # ✅ 最终使用的 prompt（优先使用用户传入 initial_prompt）
        final_prompt = initial_prompt if initial_prompt else ". ".join(dynamic_prompt_parts)

        print(f"Whisper - Using initial_prompt: '{final_prompt}'")

        # ✅ 异步线程运行模型，避免阻塞
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

        prev_text = ""
        merged_segments = []
        for segment in segments:
            normalized_segment_text = normalize_text(segment.text)
            if normalized_segment_text != prev_text:
                merged_segments.append({
                    "start": segment.start,
                    "end": segment.end,
                    "text": segment.text.strip()
                })
                prev_text = normalized_segment_text

        return {
            "language": info.language,
            "language_probability": info.language_probability,
            "segments": merged_segments
        }

    except Exception as e:
        print(f"Error during transcription for {audio_path}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Transcription failed: {str(e)}") from e
    finally:
        if audio_path and os.path.exists(audio_path):
            try: os.remove(audio_path)
            except OSError as e:
                print(f"Error removing temporary file {audio_path}: {e}")
