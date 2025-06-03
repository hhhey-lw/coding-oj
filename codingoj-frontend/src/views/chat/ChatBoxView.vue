<template>
  <div class="chat-container">
    <div ref="chatBoxRef" class="chat-box">
      <div v-for="(msg, index) in messages" :key="index" class="chat-message" :class="msg.role">
        <span class="label">{{ msg.role === 'user' ? '🧑' : '🤖' }}</span>
        <div class="content">{{ msg.content }}</div>
      </div>

      <div v-if="loading" class="chat-message assistant">
        <span class="label">🤖</span>
        <div class="content">{{ streamingText }}</div>
      </div>
    </div>

    <form @submit.prevent="sendMessage" class="chat-input">
      <input v-model="userInput" placeholder="请输入你的问题…" />
      <button type="submit">发送</button>
    </form>
  </div>
</template>

<script setup lang="ts">
import {nextTick, onMounted, ref, watch} from 'vue'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<Message[]>([])
const userInput = ref('')
const loading = ref(false)
const streamingText = ref('')

const sendMessage = async () => {
  const input = userInput.value.trim()
  if (!input) return

  messages.value.push({ role: 'user', content: input })
  userInput.value = ''

  // 模拟回答
  // simulateAIResponse(`你刚才说了："${input}"。这是模拟的回答内容。`)
  // 调用流式API
  await fetchAIResponse(input)
}

const fetchAIResponse = async (prompt: string) => {
  streamingText.value = ''
  loading.value = true

  try {
    const encodedPrompt = encodeURIComponent(prompt)
    const url = `http://localhost:8888/chat-memory/in-memory?prompt=${encodedPrompt}&chatId=longwei`

    const response = await fetch(url)

    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)
    if (!response.body) throw new Error('ReadableStream not supported')

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let done = false

    while (!done) {
      const { value, done: readerDone } = await reader.read()
      done = readerDone

      if (value) {
        const chunk = decoder.decode(value, { stream: true })
        streamingText.value += chunk
      }
    }

    // 最终保存完整消息
    messages.value.push({
      role: 'assistant',
      content: streamingText.value
    })

  } catch (error) {
    console.error('API请求失败:', error)
    messages.value.push({
      role: 'assistant',
      content: '抱歉，获取回复时出错'
    })
  } finally {
    streamingText.value = ''
    loading.value = false
  }
}

const simulateAIResponse = async (text: string) => {
  streamingText.value = ''
  loading.value = true

  for (let i = 0; i < text.length; i++) {
    streamingText.value += text[i]
    await new Promise((r) => setTimeout(r, 50))
  }

  messages.value.push({ role: 'assistant', content: streamingText.value })
  streamingText.value = ''
  loading.value = false
}

// 自动滚动
const chatBoxRef = ref<HTMLElement | null>(null)
const isUserScrolledUp = ref(false)

// 2. 核心滚动函数
const scrollToBottom = async (behavior: ScrollBehavior = 'smooth') => {
  await nextTick() // 等待DOM更新
  if (!chatBoxRef.value) return

  // 只有用户没有手动上滚时才自动滚动
  if (!isUserScrolledUp.value) {
    chatBoxRef.value.scrollTo({
      top: chatBoxRef.value.scrollHeight,
      behavior
    })
  }
}

// 3. 检测用户滚动行为
const handleScroll = () => {
  if (!chatBoxRef.value) return

  // 计算距离底部的距离（阈值设为100px）
  const threshold = 100
  const distanceToBottom = chatBoxRef.value.scrollHeight -
      (chatBoxRef.value.scrollTop + chatBoxRef.value.clientHeight)

  // 如果距离底部超过阈值，则认为用户手动上滚了
  isUserScrolledUp.value = distanceToBottom > threshold
}

// 5. 组件挂载时初始化
onMounted(() => {
  // 初始加载使用instant滚动更自然
  scrollToBottom('auto')

  // 添加滚动事件监听
  if (chatBoxRef.value) {
    chatBoxRef.value.addEventListener('scroll', handleScroll)
  }
})

// 4. 监听消息变化（自动触发滚动）
watch(streamingText, () => scrollToBottom(), { deep: true })
</script>

<style scoped>
/* 新增容器确保布局 */
.chat-container {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
  margin-top: -1REM;
  height: calc(90vh - 1REM);
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  overflow: scroll; /* 允许内容滚动 */
}

/* 聊天区域优化 */
.chat-box {
  scrollbar-width: none;
  -ms-overflow-style: none;
  overflow-y: auto;
  flex: 1;
  width: 100%;
  max-width: 800px;
  //margin: 0 auto;
  padding: 1rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.chat-container::-webkit-scrollbar,
.chat-box::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
  background: transparent;
}

/* 消息样式优化 */
.chat-message {
  display: flex;
  max-width: 80%;
}

.chat-message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.chat-message.assistant {
  align-self: flex-start;
}

.label {
  font-size: 1.5rem;
  margin: 0 0.5rem;
  align-self: flex-start;
}

.content {
  padding: 0.8rem 1.2rem;
  border-radius: 1rem;
  line-height: 1.5;
  word-break: break-word;
}

.chat-message.user .content {
  background: #e3f2fd;
  border-top-right-radius: 0;
}

.chat-message.assistant .content {
  background: #f1f1f1;
  border-top-left-radius: 0;
}

/* 输入框优化 */
.chat-input {
  width: 100%;
  max-width: 800px;
  padding: 1rem;
  display: flex;
  flex-direction: row;
  align-content: center;
  justify-content: center;
  background: white;
}

.chat-input input {
  flex: 1;
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 0.5rem 0 0 0.5rem;
  font-size: 1rem;
}

.chat-input button {
  padding: 0 1.5rem;
  background-color: #10a37f;
  color: white;
  border: none;
  border-radius: 0 0.5rem 0.5rem 0;
  font-size: 1rem;
  cursor: pointer;
  white-space: nowrap;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .chat-box, .chat-input {
    max-width: 100%;
    padding: 0.8rem;
  }

  .chat-message {
    max-width: 90%;
  }
}
</style>