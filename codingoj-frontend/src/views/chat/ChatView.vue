<template>
  <TDialog
      style="z-index: 999;"
      :onBeforeOpen="loadUserData"
      :visible="visible"
      @update:visible="$emit('update:visible', $event)"
      :footer="false"
      header="Coding AI"
      mode="modeless"
      draggable
      :on-confirm="() => $emit('update:visible', false)"
  >
    <template #body>
      <TChat
          style="height: 600px"
          :clear-history="chatList.length > 0 && !isStreamLoad"
          @on-action="operation"
          @clear="clearConfirm"
      >
        <template v-for="(item, index) in chatList" :key="index">
          <TChatItem
              variant="base"
              :avatar="item.avatar"
              :name="item.name"
              :role="item.role"
              :datetime="item.datetime"
              :text-loading="index === 0 && loading"
              :content="item.content"
              :reasoning="item.showReasoning ? {
              expandIconPlacement: 'right',
              collapsePanelProps: {
                header: renderHeader(index === 0 && isStreamLoad && !item.content, item),
                content: renderReasoningContent(item.reasoning),
              }
            } : false"
          >
          </TChatItem>
        </template>
        <template #footer>
          <TChatSender
              v-model="inputValue"
              :stop-disabled="isStreamLoad"
              :textarea-props="{
              placeholder: '请输入消息...',
            }"
              @stop="onStop"
              @send="inputEnter"
          >
            <template #suffix="{ renderPresets }">
              <!-- 在这里可以进行自由的组合使用，或者新增预设 -->
              <!-- 不需要附件操作的使用方式 -->
              <component :is="renderPresets([])" />
            </template>
            <template #prefix>
              <div class="model-select">
                <TTooltip v-model:visible="allowToolTip" content="切换模型" trigger="hover">
                  <TSelect
                      v-model="selectValue"
                      :options="selectOptions"
                      value-type="object"
                      @focus="allowToolTip = false"
                  ></TSelect>
                </TTooltip>
                <TButton class="check-box" :class="{ 'is-active': isChecked }" variant="text" @click="checkClick">
                  <SystemSumIcon />
                  <span>深度思考</span>
                </TButton>
              </div>
            </template>
          </TChatSender>
        </template>
      </TChat>
    </template>
  </TDialog>
</template>
<script setup lang="jsx">
import {ref} from 'vue';
import store from "@/store";
// import { MockSSEResponse } from './mock-data/sseRequest-reasoning';
import { SystemSumIcon } from 'tdesign-icons-vue-next';
import { CheckCircleIcon } from 'tdesign-icons-vue-next';
import {ChatServiceURL} from '../../api/ServiceURL'
import {UserVO} from "../../../generated";

// 定义 props 和事件
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
});
defineEmits(['update:visible']);

const fetchCancel = ref(null);
const loading = ref(false);
// 流式数据加载中
const isStreamLoad = ref(false);
const inputValue = ref('');
// 滚动到底部
const operation = function (type, options) {
  console.log(type, options);
};
const selectOptions = [
  {
    label: 'DeepSeek',
    value: 'deepseek-v3',
  }
];
const selectValue = ref({
  label: '默认模型',
  value: 'deepseek-v3',
});
const allowToolTip = ref(false);
const isChecked = ref(false);
const checkClick = () => {
  isChecked.value = !isChecked.value;
};

/**
 * 渲染推理模块的头部自定义内容
 * @param {boolean} flag - 思维链内容是否加载中
 * @param {string} endText - 思维链加载完成时显示的文本
 * @returns {JSX.Element} 返回对应的头部组件
 */
const renderHeader = (flag, item) => {
  if (flag) {
    return <TChatLoading text="思考中..." />;
  }
  const endText = item.duration ? `已深度思考(用时${item.duration}秒)` : '已深度思考';
  return (
      <div style="display:flex;align-items:center">
        <CheckCircleIcon
            style={{
              color: 'var(--td-success-color-5)',
              fontSize: '20px',
              marginRight: '8px',
            }}
        />
        <span>{endText}</span>
      </div>
  );
};
const renderReasoningContent = (reasoningContent) => {
  return <TChatContent content={reasoningContent} role="assistant" />;
};
// 倒序渲染
const chatList = ref([
  {
    avatar: 'https://longoj-1350136079.cos.ap-nanjing.myqcloud.com/user_avatar/chat-ai-robot-icon',
    name: 'Coding AI',
    datetime: formatDate(new Date()),
    showReasoning: false,
    reasoning: "",
    content: 'Hi!, 我是Coding AI代码助手，有什么代码问题需要讨论吗？',
    role: 'assistant',
    duration: 10,
  },
]);
const clearConfirm = function () {
  chatList.value = [];
  clearChatHistory();
};

const onStop = function () {
  if (fetchCancel.value) {
    fetchCancel.value.controller.close();
    loading.value = false;
    isStreamLoad.value = false;
  }
};

const inputEnter = function () {
  if (store.state.user.token === '') {
    alert("请先登录！");
    return;
  }
  if (isStreamLoad.value) {return;}
  if (!inputValue.value) return;
  const params = {
    avatar: store.state.user.loginUser.userAvatar,
    name: 'You',
    datetime: formatDate(new Date()),
    content: inputValue.value,
    role: 'user',
  };
  chatList.value.unshift(params);
  // 空消息占位
  const params2 = {
    avatar: 'https://longoj-1350136079.cos.ap-nanjing.myqcloud.com/user_avatar/chat-ai-robot-icon',
    name: 'Coding AI',
    datetime: formatDate(new Date()),
    showReasoning: isChecked.value,
    content: '',
    reasoning: '',
    role: 'assistant',
  };
  chatList.value.unshift(params2);
  saveChatHistory(); // 发送消息后保存缓存
  handleData(inputValue.value);
  inputValue.value = '';
};

const handleData = async (prompt) => {
  loading.value = true;
  isStreamLoad.value = true;
  const lastItem = chatList.value[0];
  const modelName = isChecked.value ? "deepseek-r1" : "deepseek-v3";
  try {
    await fetchSSE(ChatServiceURL, {
      chatId: store.state.user.loginUser.id,
      prompt: prompt,
      modelName: modelName,
      isThinking: isChecked.value
    });
    saveChatHistory(); // 回复完成后保存缓存
  } catch (error) {
    lastItem.role = 'error';
    lastItem.content = error.message;
  } finally {
    isStreamLoad.value = false;
    loading.value = false;
  }
};

const fetchSSE = async (url, params) => {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': store.state.user.token
    },
    body: JSON.stringify(params)
  });
  if (!response.ok) {
    throw new Error(`HTTP 错误: ${response.status}`);
  }

  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error('响应流不可读');
  }

  const decoder = new TextDecoder();
  let buffer = ''; // 暂存未解析的 chunk 数据

  const lastItem = chatList.value[0]; // 获取最新的助手消息
  // 逐块读取流数据（真正的流式处理）
  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break; // 流结束
    }

    // 解码当前 chunk 并追加到 buffer
    buffer += decoder.decode(value, { stream: true });

    // 按行分割 buffer（SSE 数据以 `\n` 分隔）
    let lines = buffer.split(/\r?\n/);
    buffer = lines.pop() || ''; // 最后一行可能不完整，暂存到下一轮处理

    // 逐行处理 SSE 数据
    for (const line of lines) {
      if (line.startsWith('data:')) {
        try {
          // 提取 data: 后的 JSON 内容（跳过 "data:" 共 5 个字符）
          const sseData = JSON.parse(line.slice(5));

          if (sseData.code === 200) {
            loading.value = false;
            switch (sseData.type) {
              case 'thinking':
                // 思考内容直接更新（不需要流式）
                lastItem.reasoning += sseData.msg;
                break;

              case 'content':
                // 逐字显示内容（流式效果）
                lastItem.content += sseData.msg;
                break;
            }
          } else {
            // 控制终止按钮
            isStreamLoad.value = false;
            loading.value = false;
            lastItem.role = "error";
            lastItem.content += sseData.msg;
            throw new Error(`服务端错误: ${sseData.msg || '未知错误'}`);
          }
        } catch (e) {
          // 控制终止按钮
          isStreamLoad.value = false;
          loading.value = false;
          console.error('SSE 数据解析失败:', e);
        }
      }
    }
  }
};

// 页面加载时读取本地缓存
const loadChatHistory = () => {
  const savedHistory = localStorage.getItem('chatHistory');
  if (savedHistory) {
    chatList.value = JSON.parse(savedHistory);
  }
};

// 保存聊天记录到本地缓存
const saveChatHistory = () => {
  localStorage.setItem('chatHistory', JSON.stringify(chatList.value));
};

const clearChatHistory = () => {
  localStorage.removeItem('chatHistory');
};

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0'); // 月份从0开始，需要+1
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

const loadUserData = () => {
  console.log("loadUserData")
  // 模拟加载数据
  loadChatHistory(); // 页面加载时读取缓存
};

</script>
<style lang="less">
.model-select {
  display: flex;
  align-items: center;

  .t-select {
    width: 112px;
    height: var(--td-comp-size-m);
    margin-right: var(--td-comp-margin-s);

    .t-input {
      border-radius: 32px;
      padding: 0 15px;
    }

    .t-input.t-is-focused {
      box-shadow: none;
    }
  }

  .check-box {
    width: 112px;
    height: var(--td-comp-size-m);
    border-radius: 32px;
    border: 0;
    background: var(--td-bg-color-component);
    color: var(--td-text-color-primary);
    box-sizing: border-box;
    flex: 0 0 auto;

    .t-button__text {
      display: flex;
      align-items: center;
      justify-content: center;

      span {
        margin-left: var(--td-comp-margin-xs);
      }
    }
  }

  .check-box.is-active {
    border: 1px solid var(--td-brand-color-focus);
    background: var(--td-brand-color-light);
    color: var(--td-text-color-brand);
  }
}
</style>
