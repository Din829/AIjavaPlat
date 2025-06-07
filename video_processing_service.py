# video_processing_service.py
"""
视频处理微服务
基于FastAPI，提供视频下载、元数据提取和音频转换功能
参考n8n工作流的成功架构，集成yt-dlp和FFmpeg
"""

from fastapi import FastAPI, HTTPException, Form
from pydantic import BaseModel, HttpUrl
import yt_dlp
import subprocess
import tempfile
import os
import logging
import json
from typing import Optional, Dict, Any
import asyncio
from pathlib import Path
import shutil

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Video Processing Service", 
    version="1.0.0",
    description="视频处理微服务，支持视频下载、元数据提取和音频转换"
)

class VideoProcessRequest(BaseModel):
    """视频处理请求模型"""
    url: HttpUrl
    language: Optional[str] = "auto"
    
class VideoProcessResponse(BaseModel):
    """视频处理响应模型"""
    success: bool
    video_id: str
    title: str
    description: str
    duration: float  # 修改为float类型支持小数秒
    wav_download_url: str
    platform: str
    error_message: Optional[str] = None

class HealthResponse(BaseModel):
    """健康检查响应模型"""
    status: str
    service: str
    version: str

# 全局配置
TEMP_DIR = Path("/tmp/video_processing")
TEMP_DIR.mkdir(parents=True, exist_ok=True)

# 支持的视频平台配置
SUPPORTED_PLATFORMS = {
    'youtube.com': 'YouTube',
    'youtu.be': 'YouTube', 
    'bilibili.com': 'Bilibili',
    'b23.tv': 'Bilibili',
    'vimeo.com': 'Vimeo',
    'dailymotion.com': 'Dailymotion',
    'twitch.tv': 'Twitch'
}

def detect_platform(url: str) -> str:
    """
    检测视频平台
    
    Args:
        url: 视频URL
        
    Returns:
        平台名称
    """
    url_lower = url.lower()
    for domain, platform in SUPPORTED_PLATFORMS.items():
        if domain in url_lower:
            return platform
    return "Unknown"

async def download_and_convert_audio(url: str, video_id: str, output_dir: Path) -> str:
    """
    下载视频并转换为16kHz单声道WAV格式
    参考n8n工作流的优化参数配置
    
    Args:
        url: 视频URL
        video_id: 视频ID
        output_dir: 输出目录
        
    Returns:
        WAV文件路径
    """
    output_path = output_dir / f"{video_id}.wav"
    
    # FFmpeg路径调试
    ffmpeg_path = 'C:/Program Files/ffmpeg-7.1.1-full_build/bin/ffmpeg.exe'
    logger.info(f"[调试] 使用FFmpeg路径: {ffmpeg_path}")
    logger.info(f"[调试] FFmpeg文件是否存在: {os.path.exists(ffmpeg_path)}")
    
    # 测试FFmpeg是否可执行
    try:
        result = subprocess.run([ffmpeg_path, '-version'], 
                              capture_output=True, text=True, timeout=10)
        logger.info(f"[调试] FFmpeg测试返回码: {result.returncode}")
        if result.returncode == 0:
            logger.info(f"[调试] FFmpeg版本信息: {result.stdout[:100]}...")
        else:
            logger.error(f"[调试] FFmpeg错误: {result.stderr}")
    except Exception as e:
        logger.error(f"[调试] FFmpeg执行测试失败: {e}")
    
    # yt-dlp配置，使用postprocessor_args指定FFmpeg参数
    ydl_opts = {
        'format': 'bestaudio/best',
        'outtmpl': str(output_dir / f'{video_id}.%(ext)s'),
        'postprocessors': [{
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'wav',
            'preferredquality': '192',
        }],
        # 使用postprocessor_args全局指定FFmpeg参数
        'postprocessor_args': {
            'ffmpeg': ['-ar', '16000', '-ac', '1']
        },
        # 明确指定FFmpeg路径，解决找不到FFmpeg的问题
        'ffmpeg_location': ffmpeg_path,
        'quiet': False,  # 启用详细日志用于调试
        'no_warnings': False,  # 显示警告信息
        'verbose': True,  # 启用详细模式
    }
    
    logger.info(f"[调试] yt-dlp配置: {ydl_opts}")
    
    try:
        logger.info(f"开始下载和转换音频: {url}")
        
        # 使用yt-dlp下载并转换
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            logger.info(f"[调试] yt-dlp对象创建成功，开始下载...")
            try:
                await asyncio.get_event_loop().run_in_executor(
                    None, ydl.download, [url]
                )
                logger.info(f"[调试] yt-dlp下载完成")
            except Exception as ydl_error:
                logger.error(f"[调试] yt-dlp下载失败: {type(ydl_error).__name__}: {str(ydl_error)}")
                # 打印更详细的错误信息
                import traceback
                logger.error(f"[调试] 详细错误堆栈: {traceback.format_exc()}")
                raise
        
        # 检查输出文件是否存在
        if output_path.exists():
            logger.info(f"音频转换成功: {output_path}")
            return str(output_path)
        else:
            # 查找可能的输出文件
            for file in output_dir.glob(f"{video_id}.*"):
                if file.suffix == '.wav':
                    logger.info(f"找到转换后的音频文件: {file}")
                    return str(file)
            
            raise Exception("音频转换完成但找不到输出文件")
            
    except Exception as e:
        logger.error(f"音频下载转换失败: {e}")
        raise HTTPException(status_code=500, detail=f"音频处理失败: {str(e)}")

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """健康检查端点"""
    return HealthResponse(
        status="healthy",
        service="video-processing-service", 
        version="1.0.0"
    )

@app.post("/process-video", response_model=VideoProcessResponse)
async def process_video(request: VideoProcessRequest):
    """
    处理视频链接，提取元数据并转换为WAV格式
    参考n8n工作流中的视频处理逻辑
    
    Args:
        request: 视频处理请求
        
    Returns:
        视频处理响应，包含元数据和WAV下载URL
    """
    url_str = str(request.url)
    logger.info(f"收到视频处理请求: {url_str}")
    
    try:
        # 1. 检测平台
        platform = detect_platform(url_str)
        logger.info(f"检测到平台: {platform}")
        
        # 2. 使用yt-dlp提取视频信息（不下载）
        ydl_opts = {
            'quiet': True,
            'no_warnings': True,
        }
        
        logger.info("开始提取视频元数据...")
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = await asyncio.get_event_loop().run_in_executor(
                None, ydl.extract_info, url_str, False
            )
        
        # 3. 提取关键信息
        video_id = info.get('id', 'unknown')
        title = info.get('title', '无标题')
        description = info.get('description', '')
        duration = info.get('duration', 0)
        
        logger.info(f"视频信息提取成功: ID={video_id}, 标题={title}, 时长={duration}秒")
        
        # 4. 创建临时目录
        temp_dir = TEMP_DIR / video_id
        temp_dir.mkdir(exist_ok=True)
        
        # 5. 下载并转换音频
        logger.info("开始下载和转换音频...")
        wav_path = await download_and_convert_audio(url_str, video_id, temp_dir)
        
        # 6. 生成下载URL
        wav_download_url = f"/download/{video_id}.wav"
        
        logger.info(f"视频处理完成: {video_id}")
        
        return VideoProcessResponse(
            success=True,
            video_id=video_id,
            title=title,
            description=description,
            duration=duration if duration else 0,
            wav_download_url=wav_download_url,
            platform=platform
        )
        
    except Exception as e:
        logger.error(f"视频处理失败: {e}")
        return VideoProcessResponse(
            success=False,
            video_id="",
            title="",
            description="",
            duration=0,
            wav_download_url="",
            platform="",
            error_message=str(e)
        )

@app.get("/download/{video_id}.wav")
async def download_wav_file(video_id: str):
    """
    下载转换后的WAV文件
    
    Args:
        video_id: 视频ID
        
    Returns:
        WAV文件流
    """
    from fastapi.responses import FileResponse
    
    wav_path = TEMP_DIR / video_id / f"{video_id}.wav"
    
    if not wav_path.exists():
        # 尝试查找其他可能的文件名
        temp_dir = TEMP_DIR / video_id
        if temp_dir.exists():
            for file in temp_dir.glob("*.wav"):
                wav_path = file
                break
    
    if not wav_path.exists():
        raise HTTPException(status_code=404, detail="WAV文件不存在")
    
    return FileResponse(
        path=str(wav_path),
        media_type="audio/wav",
        filename=f"{video_id}.wav"
    )

@app.get("/status")
async def get_status():
    """获取服务状态，用于健康检查"""
    return {
        "status": "running",
        "service": "video-processing-service",
        "version": "1.0.0",
        "supported_platforms": list(SUPPORTED_PLATFORMS.values())
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=9000)
