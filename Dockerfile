# 使用官方 Python 3.11 slim 镜像作为基础镜像
FROM python:3.11-slim as builder

# 设置工作目录
WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    # 编译工具
    gcc g++ \
    # 图像处理库
    libjpeg-dev \
    libpng-dev \
    libtiff5-dev \
    libfreetype6-dev \
    liblcms2-dev \
    libwebp-dev \
    libopenjp2-7-dev \
    zlib1g-dev \
    # PDF 处理库
    libmupdf-dev \
    mupdf-tools \
    # 字体支持
    fontconfig \
    fonts-dejavu-core \
    # 清理缓存
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 复制并安装 Python 依赖
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip setuptools wheel && \
    pip install --no-cache-dir -r requirements.txt

# 运行阶段
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 安装运行时系统依赖
RUN apt-get update && apt-get install -y \
    # 图像处理运行时库（修正包名）
    libjpeg62-turbo \
    libpng16-16 \
    libtiff6 \
    libfreetype6 \
    liblcms2-2 \
    libwebp7 \
    libopenjp2-7 \
    # PDF 处理运行时库
    libmupdf-dev \
    # 字体支持
    fontconfig \
    fonts-dejavu-core \
    # 网络工具（调试用）
    curl \
    # 清理缓存
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 从构建阶段复制 Python 包
COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# 复制应用代码
COPY ocr_service.py .

# 创建必要的目录
RUN mkdir -p temp_uploads

# 设置环境变量（移除 docling 相关的环境变量）
ENV PYTHONPATH="/app" \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# 暴露端口
EXPOSE 8012

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8012/api/ocr/status || exit 1

# 启动命令
CMD ["uvicorn", "ocr_service:app", "--host", "0.0.0.0", "--port", "8012", "--workers", "1"] 