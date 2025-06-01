<template>
  <div class="rich-text-display">
    <div v-for="(segment, index) in parsedContent" :key="index" class="text-segment">
      <!-- 文本段落 -->
      <pre v-if="segment.type === 'text'" class="text-content">{{ segment.content }}</pre>
      
      <!-- 图像段落 -->
      <div v-else-if="segment.type === 'image'" class="image-content">
        <div class="image-wrapper">
          <img 
            :src="`data:${segment.image.mime_type};base64,${segment.image.data}`"
            :alt="segment.image.description"
            class="embedded-image"
            @error="handleImageError"
          />
          <div class="image-caption">
            {{ segment.image.description }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  text: {
    type: String,
    required: true
  },
  images: {
    type: Array,
    default: () => []
  }
})

// 解析文本中的图像标记
const parsedContent = computed(() => {
  if (!props.text) return []
  
  const segments = []
  const imageRegex = /\[IMAGE:([^:]+):([^\]]+)\]/g
  let lastIndex = 0
  let match
  
  // 创建图像映射，便于快速查找
  const imageMap = {}
  props.images.forEach(img => {
    imageMap[img.image_id] = img
  })
  
  while ((match = imageRegex.exec(props.text)) !== null) {
    const [fullMatch, imageId, description] = match
    const matchStart = match.index
    const matchEnd = match.index + fullMatch.length
    
    // 添加图像标记前的文本
    if (matchStart > lastIndex) {
      const textContent = props.text.substring(lastIndex, matchStart)
      if (textContent.trim()) {
        segments.push({
          type: 'text',
          content: textContent
        })
      }
    }
    
    // 添加图像
    const imageData = imageMap[imageId]
    if (imageData) {
      segments.push({
        type: 'image',
        image: imageData
      })
    } else {
      // 如果找不到图像数据，显示占位符
      segments.push({
        type: 'text',
        content: `[图像: ${description}]`
      })
    }
    
    lastIndex = matchEnd
  }
  
  // 添加最后剩余的文本
  if (lastIndex < props.text.length) {
    const remainingText = props.text.substring(lastIndex)
    if (remainingText.trim()) {
      segments.push({
        type: 'text',
        content: remainingText
      })
    }
  }
  
  // 如果没有找到任何图像标记，返回原始文本
  if (segments.length === 0) {
    segments.push({
      type: 'text',
      content: props.text
    })
  }
  
  return segments
})

const handleImageError = (event) => {
  console.error('图像加载失败:', event.target.src)
  event.target.style.display = 'none'
}
</script>

<style scoped>
.rich-text-display {
  font-family: 'Courier New', monospace;
  line-height: 1.6;
}

.text-segment {
  margin-bottom: 8px;
}

.text-content {
  margin: 0;
  padding: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  font-size: 14px;
  color: #333;
}

.image-content {
  margin: 16px 0;
  text-align: center;
}

.image-wrapper {
  display: inline-block;
  max-width: 100%;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 8px;
  background-color: #fafafa;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.embedded-image {
  max-width: 100%;
  max-height: 300px;
  height: auto;
  border-radius: 4px;
  display: block;
  margin: 0 auto;
}

.image-caption {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  font-style: italic;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .embedded-image {
    max-height: 200px;
  }
  
  .text-content {
    font-size: 13px;
  }
}
</style>
