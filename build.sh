#!/bin/bash

# OCR 服务 Docker 构建脚本
# 使用方法: ./build.sh [选项]
# 选项:
#   --no-cache     无缓存构建
#   --push         构建后推送到仓库
#   --dev          构建开发版本

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置变量
IMAGE_NAME="ai-platform-ocr"
IMAGE_TAG="latest"
REGISTRY_URL=""  # 如果需要推送到私有仓库，在这里设置

# 参数处理
BUILD_ARGS=""
PUSH_IMAGE=false
DEV_BUILD=false

for arg in "$@"; do
    case $arg in
        --no-cache)
        BUILD_ARGS="$BUILD_ARGS --no-cache"
        echo -e "${YELLOW}警告: 无缓存构建将花费更长时间${NC}"
        ;;
        --push)
        PUSH_IMAGE=true
        echo -e "${BLUE}信息: 构建完成后将推送镜像${NC}"
        ;;
        --dev)
        DEV_BUILD=true
        IMAGE_TAG="dev"
        echo -e "${BLUE}信息: 构建开发版本${NC}"
        ;;
        *)
        echo -e "${RED}未知参数: $arg${NC}"
        echo "使用方法: $0 [--no-cache] [--push] [--dev]"
        exit 1
        ;;
    esac
done

echo -e "${GREEN}===========================================${NC}"
echo -e "${GREEN}开始构建 OCR 微服务 Docker 镜像${NC}"
echo -e "${GREEN}===========================================${NC}"

# 检查必要文件
echo -e "${BLUE}检查必要文件...${NC}"
if [[ ! -f "ocr_service.py" ]]; then
    echo -e "${RED}错误: ocr_service.py 文件未找到${NC}"
    exit 1
fi

if [[ ! -f "requirements.txt" ]]; then
    echo -e "${RED}错误: requirements.txt 文件未找到${NC}"
    exit 1
fi

if [[ ! -f "Dockerfile" ]]; then
    echo -e "${RED}错误: Dockerfile 文件未找到${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 所有必要文件存在${NC}"

# 显示构建信息
echo -e "${BLUE}构建信息:${NC}"
echo -e "  镜像名称: ${IMAGE_NAME}:${IMAGE_TAG}"
echo -e "  构建参数: ${BUILD_ARGS:-"默认"}"
echo -e "  推送镜像: $([ "$PUSH_IMAGE" = true ] && echo "是" || echo "否")"

# 开始构建
echo -e "${BLUE}开始构建 Docker 镜像...${NC}"
docker build $BUILD_ARGS -t ${IMAGE_NAME}:${IMAGE_TAG} .

if [[ $? -eq 0 ]]; then
    echo -e "${GREEN}✓ Docker 镜像构建成功${NC}"
else
    echo -e "${RED}✗ Docker 镜像构建失败${NC}"
    exit 1
fi

# 显示镜像信息
echo -e "${BLUE}镜像信息:${NC}"
docker images ${IMAGE_NAME}:${IMAGE_TAG}

# 推送镜像（如果需要）
if [[ "$PUSH_IMAGE" = true ]]; then
    if [[ -z "$REGISTRY_URL" ]]; then
        echo -e "${YELLOW}警告: 未设置 REGISTRY_URL，跳过推送${NC}"
    else
        echo -e "${BLUE}推送镜像到 ${REGISTRY_URL}...${NC}"
        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_TAG}
        docker push ${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_TAG}
        
        if [[ $? -eq 0 ]]; then
            echo -e "${GREEN}✓ 镜像推送成功${NC}"
        else
            echo -e "${RED}✗ 镜像推送失败${NC}"
            exit 1
        fi
    fi
fi

echo -e "${GREEN}===========================================${NC}"
echo -e "${GREEN}构建完成！${NC}"
echo -e "${GREEN}===========================================${NC}"

# 运行建议
echo -e "${BLUE}运行建议:${NC}"
echo -e "  单独运行: docker run -p 8012:8012 -e GEMINI_API_KEY=your_key ${IMAGE_NAME}:${IMAGE_TAG}"
echo -e "  使用 compose: docker-compose up ocr-service"
echo -e "  健康检查: curl http://localhost:8012/api/ocr/status"

# 清理建议
echo -e "${YELLOW}清理建议:${NC}"
echo -e "  删除镜像: docker rmi ${IMAGE_NAME}:${IMAGE_TAG}"
echo -e "  清理构建缓存: docker builder prune" 