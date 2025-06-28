<template>
  <a-row class="container" :gutter="0">
    <!-- 面试官面板 -->
    <a-col :span="4" class="panel interviewer-panel">
      <div class="call-header">
        <a-dropdown @select="handleVoiceSelect">
          <button class="class-header-tag">
            <icon-user /> {{ selectedVoiceName }}
          </button>
          <template #content>
            <a-doption v-for="voice in synthesisVoices" :key="voice.name" :value="voice.name">
              {{ voice.name }}
            </a-doption>
          </template>
        </a-dropdown>
      </div>
      <div class="call-avatar">
        <div class="blur-ball" />
      </div>

      <div class="voice-svg-wave" v-show="showVoiceWave">
        <svg viewBox="0 0 120 30">
          <rect x="0" y="10" width="4" height="10"></rect>
          <rect x="10" y="6" width="4" height="18"></rect>
          <rect x="20" y="4" width="4" height="22"></rect>
          <rect x="30" y="6" width="4" height="18"></rect>
          <rect x="40" y="10" width="4" height="10"></rect>
          <rect x="50" y="6" width="4" height="18"></rect>
          <rect x="60" y="2" width="4" height="26"></rect>
          <rect x="70" y="6" width="4" height="18"></rect>
          <rect x="80" y="10" width="4" height="10"></rect>
          <rect x="90" y="6" width="4" height="18"></rect>
          <rect x="100" y="4" width="4" height="22"></rect>
          <rect x="110" y="6" width="4" height="18"></rect>
        </svg>
      </div>
      <div class="call-status" v-show="!showVoiceWave">{{ chatStatusText }}</div>

      <div class="interview-panel-switch-box-class">
        <button v-if="!isStartInterView" class="btn end" @click="endCall">
          <icon-phone />
        </button>
        <button v-else class="btn end" @click="endCall">
          <icon-poweroff />
        </button>

        <button class="btn play-btn" @click="playSpeechBtn">
          <icon-play-arrow />
        </button>
      </div>
    </a-col>

    <!-- 聊天面板 -->
    <a-col :span="8" class="panel chat-panel">
      <div class="chat-box">
        <a-skeleton v-if="chatList.length===0">
          <a-space direction="vertical" style="width:100%; margin-top: 1REM;" size="large">
            <a-skeleton-line :rows="10" />
          </a-space>
        </a-skeleton>
        <t-chat
            v-else
            ref="chatRef"
            :clear-history="chatList.length > 0 && !isStreamLoad"
            :data="chatList"
            :text-loading="loading"
            :is-stream-load="isStreamLoad"
            style="height: 100%; min-height: 250px"
            @scroll="handleChatScroll"
            @clear="clearConfirm"
        >
          <template #content="{ item }">
            <t-chat-content :content="item.content" />
          </template>
        </t-chat>
      </div>
      <div class="input-sender-box-class">
        <input v-model="replyInterviewText" placeholder="请输入回复内容" class="input-box-class" />

        <div class="call-controls">
          <button v-if="!isRecording" class="btn" @click="startRecording">
            <IconVoice />
          </button>
          <button v-else class="btn" @click="endRecording">
            <icon-stop />
          </button>

          <button class="btn" @click="sendMessage">
            <icon-send />
          </button>
        </div>
      </div>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, Ref } from 'vue';
import {
  IconUser,
  IconVoice,
  IconPhone,
  IconStop,
  IconSend,
  IconRefresh,
  IconPlayArrow,
  IconPoweroff,
  IconArrowDown
} from '@arco-design/web-vue/es/icon';
import {
  Dropdown as ADropdown,
  Doption as ADoption,
} from '@arco-design/web-vue';
import { Message } from '@arco-design/web-vue';
import store from "@/store";
import { OpenAPI } from '@/api/generated-chat/core/OpenAPI';
import {Content} from "tdesign-vue-next";


// Chat Panel
const fetchCancel = ref<any>(null);
const loading = ref<boolean>(false);
// 流式数据加载中
const isStreamLoad = ref(false);

const chatRef = ref<any>(null);
const isShowToBottom = ref(false);
// 滚动到底部
// 是否显示回到底部按钮
const handleChatScroll = function ({ e }: { e: any }) {
  const scrollTop = e.target.scrollTop;
  isShowToBottom.value = scrollTop < 0;
};

// 倒序渲染
const chatList = ref<ChatMessage[]>([]);

const onStop = function () {
  if (fetchCancel.value) {
    fetchCancel.value.controller.close();
    loading.value = false;
    isStreamLoad.value = false;
  }
};

// --- 状态定义 ---
const chatStatusEnum = {
  WAITING: 0,
  RECORDING: 1,
  THINKING: 2,
  ANSWERING: 3,
};
const chatStatus = ref(chatStatusEnum.WAITING);
const isRecording = ref(false);

const isStartInterView = ref<boolean>(false);

// --- MediaRecorder 录音相关状态 ---
const mediaRecorder: Ref<MediaRecorder | null> = ref(null);
let mediaStream: MediaStream | null = null;


const selectedVoiceName = ref('声音A');
const synthesisVoices = ref([
  { name: '声音A', value: 'A' },
  { name: '声音B', value: 'B' }]);

const receiveInterviewText = ref<String>("")
const finalRecognizeText = ref<String>("")
const tempRecognizeText = ref<String>("")

// --- 方法定义 ---
// 1. 初始化 Web Audio API，采样率为 24kHz
let audioContext = new AudioContext({ sampleRate: 24000 });
const audioQueue: AudioBuffer[] = [];
let isPlaying = ref<boolean>(false);
let nextPlayTime = 0;
// 添加存储所有语音数据的数组
const allAudioData = ref<string[]>([]);

// 播放队列调度函数
const schedulePlayback = () => {
  // 如果已经在播放或队列为空，则无需调度
  if (isPlaying.value || audioQueue.length === 0) return;
  isPlaying.value = true;

  // 从队列中取出下一个音频块
  const bufferToPlay = audioQueue.shift();
  if (!bufferToPlay) {
    isPlaying.value = false;
    return;
  }

  const source = audioContext.createBufferSource();
  source.buffer = bufferToPlay;
  source.connect(audioContext.destination);

  const currentTime = audioContext.currentTime;
  // 确保播放时间不会早于当前时间
  const startTime = Math.max(currentTime, nextPlayTime);
  source.start(startTime);

  console.log("播放音频中");

  // 更新下个音频块的计划开始时间
  nextPlayTime = startTime + bufferToPlay.duration;

  source.onended = () => {
    // 检查队列中是否还有音频块
    if (audioQueue.length > 0) {
      // 递归调用 schedulePlayback 以调度下一个音频块
      isPlaying.value = false;
      schedulePlayback();
    } else {
      // 如果队列为空，重置 isPlaying 状态
      isPlaying.value = false;

      // 延迟一小段时间后更新状态，确保 onended 事件完全处理完毕
      setTimeout(() => {
        if (audioQueue.length === 0 && !isPlaying.value) {
          chatStatus.value = chatStatusEnum.WAITING;
          Message.info('AI 回答完毕，请继续提问。');
        }
      }, 100);
    }
  };
};

function pushSpeechData2Buffer(data: string) {
  if (!data || data === '') {
    return;
  }

  // Base64 解码
  const binaryString = atob(data);
  const bytes = new Uint8Array(binaryString.length);
  for (let j = 0; j < binaryString.length; j++) {
    bytes[j] = binaryString.charCodeAt(j);
  }

  // PCM 16-bit Little Endian -> Float32Array
  const pcmData = new Float32Array(bytes.length / 2);
  const view = new DataView(bytes.buffer);
  for (let k = 0; k < pcmData.length; k++) {
    pcmData[k] = view.getInt16(k * 2, true) / 32768.0;
  }

  // 创建 AudioBuffer 并加入队列
  const audioBuffer = audioContext.createBuffer(1, pcmData.length, audioContext.sampleRate);
  audioBuffer.copyToChannel(pcmData, 0);
  audioQueue.push(audioBuffer);
}

function playSpeechBtn() {
  if (allAudioData.value.length === 0) {
    Message.warning('没有可播放的语音消息');
    return;
  }

  // 尝试停止当前可能的语音识别
  if (isRecording.value) {
    endRecording();
  }

  // 释放系统可能占用的音频资源
  if (audioContext.state === 'suspended') {
    // 使用用户交互事件来恢复音频上下文
    audioContext.resume().then(() => {
      console.log('AudioContext 已恢复');
      processAudioForPlayback();
    }).catch(err => {
      console.error('恢复 AudioContext 失败:', err);
      Message.error('无法播放音频，请重试');
    });
  } else {
    processAudioForPlayback();
  }
}

function processAudioForPlayback() {
  // 清空当前队列
  audioQueue.length = 0;

  // 重新处理所有已接收的语音数据
  for (let data of allAudioData.value) {
    pushSpeechData2Buffer(data);
  }

  // 设置状态并开始播放
  chatStatus.value = chatStatusEnum.ANSWERING;
  isPlaying.value = false;
  nextPlayTime = 0;

  // 在音频处理完成后确保一个短暂延迟再开始播放
  setTimeout(() => {
    schedulePlayback();
    Message.info('正在重播语音消息');
  }, 100);
}
/**
 * 开始录音（修正版本：采样率降采样为16kHz + PCM小端格式 + 稳定传输）
 */
const startRecording = async () => {
  if (isPlaying.value) {
    Message.warning("面试官正在说话中，请稍后。");
    return;
  }
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    Message.error('WebSocket 连接已断开，无法进行通话。');
    return;
  }

  try {
    // 1. 请求麦克风权限
    const streamConstraints = {
      audio: {
        channelCount: 1,
        sampleRate: 16000, // 实际不会生效，需手动降采样
        sampleSize: 16,
        echoCancellation: true,
        noiseSuppression: true
      }
    };

    mediaStream = await navigator.mediaDevices.getUserMedia(streamConstraints);
    isRecording.value = true;
    chatStatus.value = chatStatusEnum.RECORDING;

    // 2. 创建音频上下文（注意：sampleRate 实际不一定生效）
    const audioCtx = new AudioContext();
    const inputSampleRate = audioCtx.sampleRate;
    console.log("实际音频采样率：", inputSampleRate); // 通常是 44100 或 48000

    const source = audioCtx.createMediaStreamSource(mediaStream);
    const processor = audioCtx.createScriptProcessor(1024, 1, 1);

    source.connect(processor);
    processor.connect(audioCtx.destination);

    // 3. 通知后端开始
    ws.send(JSON.stringify({
      type: 'speech_start',
      round: round.value,
      data: ''
    }));

    Message.info('正在录音，实时发送数据...');

    // 4. 降采样函数：从 inputSampleRate => 16000Hz
    const downsampleBuffer = (buffer: Float32Array, inputRate: number, outputRate = 16000): Float32Array => {
      if (inputRate === outputRate) return buffer;
      const ratio = inputRate / outputRate;
      const newLen = Math.round(buffer.length / ratio);
      const result = new Float32Array(newLen);
      let offset = 0;
      for (let i = 0; i < newLen; i++) {
        const nextOffset = Math.round((i + 1) * ratio);
        let sum = 0, count = 0;
        for (let j = offset; j < nextOffset && j < buffer.length; j++) {
          sum += buffer[j];
          count++;
        }
        result[i] = sum / count;
        offset = nextOffset;
      }
      return result;
    };

    processor.onaudioprocess = (e) => {
      if (!isRecording.value || !ws || ws.readyState !== WebSocket.OPEN) return;

      const inputData = e.inputBuffer.getChannelData(0);
      const downsampled = downsampleBuffer(inputData, inputSampleRate); // 降采样至16k

      // 转为 16-bit PCM，小端格式
      const buffer = new ArrayBuffer(downsampled.length * 2);
      const view = new DataView(buffer);
      for (let i = 0; i < downsampled.length; i++) {
        const s = Math.max(-1, Math.min(1, downsampled[i]));
        view.setInt16(i * 2, s * 0x7FFF, true); // Little Endian
      }

      // 编码为 Base64 并发送
      const uint8Array = new Uint8Array(buffer);
      const base64Data = btoa(String.fromCharCode(...uint8Array));

      ws.send(JSON.stringify({
        data: base64Data,
        type: 'speech',
        round: round.value
      }));
    };

    // 存储引用用于停止
    mediaRecorder.value = {
      audioCtx,
      processor,
      source,
      stop: () => {
        processor.disconnect();
        source.disconnect();
        audioCtx.close();
        mediaStream?.getTracks().forEach(track => track.stop());

        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({
            type: 'speech_end',
            data: '',
            round: round.value
          }));
          console.log("已发送语音结束信号");
        }
      }
    } as any;

  } catch (err) {
    console.error('无法获取麦克风权限或录音失败:', err);
    Message.error('无法获取麦克风权限，请检查系统或浏览器设置。');
    isRecording.value = false;
    chatStatus.value = chatStatusEnum.WAITING;
  }
};

/**
 * 手动停止录音
 */
const endRecording = () => {
  if (mediaRecorder.value && isRecording.value) {
    mediaRecorder.value.stop(); // 使用自定义stop方法
    isRecording.value = false;
    chatStatus.value = chatStatusEnum.WAITING;
    Message.info('录音已停止');
  }
};

/**
 * 结束通话，停止所有语音活动
 */
const endCall = () => {
  if (isStartInterView.value) {
    isStartInterView.value = false;
    if (mediaRecorder.value) {
      mediaRecorder.value.stop();
    }
    isRecording.value = false;
    chatStatus.value = chatStatusEnum.WAITING;
    mediaStream?.getTracks().forEach(track => track.stop()); // 确保麦克风被释放
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({
        type: 'end_call',
        round: round.value,
        data: ''
      }));
      ws.close();
    }
    Message.info('通话已结束');
  } else {
    console.log("开始电话");
    isStartInterView.value = true;
    chatStatus.value = chatStatusEnum.WAITING;
    createWebSocket();
  }
};

/**
 * 处理声音选择
 */
const handleVoiceSelect = (value: any) => {
  const voice = synthesisVoices.value.find(v => v.name === value);
  if (voice) {
    selectedVoiceName.value = voice.name;
    Message.success(`声音已切换为: ${selectedVoiceName.value}`);
  }
};
// --- 生命周期函数 ---
onMounted(() => {
  Message.warning({
    content: '录音功能需要手动开放权限(推荐Chrome浏览器)，浏览器输入 chrome://flags/#unsafely-treat-insecure-origin-as-secure 将http://oj.longcoding.top设置为安全源，并启用',
    duration: 20000,
    closable: true,
    position: 'top'
  })
});

let ws: WebSocket | null = null;
// websocket
const createWebSocket = () => {
  ws = new WebSocket("ws://longcoding.top:8102/api/interview");

  ws.onopen = function() {
    Message.info("连接已建立");
    const msg = {
      data: store.state.user.token,
      round: round.value,
      type: 'token'
    }
    // 发送开始通话信号
    ws.send(JSON.stringify(msg));
  };

  ws.onmessage = function(event) {
    const res = JSON.parse(event.data);
    console.log(res.code, res.message);
    // Message.info("服务器: " + res.message);
    if (res.code !== 200) {
      Message.error("错误信息: " + res.message);
      return;
    }

    // 处理非JSON格式的消息
    // if (typeof event.data === 'string') {
    //   Message.warning(`服务器消息: ${event.data}`);
    // }

    if (res.message == "speech") {
      // 处理音频数据
      if (res.data && res.data.length > 0) {
        // 保存音频数据用于重播
        allAudioData.value.push(res.data);

        pushSpeechData2Buffer(res.data);

        // 自动播放
        if (!isPlaying.value) {
          schedulePlayback();
        }
      }
    } else if (res.message == "text") {
      // 获取最近的AI消息（列表第一个元素）
      let aiMessage = chatList.value.find(item => item.role === 'assistant');
      if (!aiMessage) {
        // 如果没有AI消息，创建一个新的
        chatList.value.unshift({
          avatar: 'https://tdesign.gtimg.com/site/chat-avatar.png',
          name: 'AI面试官',
          content: '',
          reasoning: '',
          role: 'assistant',
        });
        aiMessage = chatList.value.find(item => item.role === 'assistant');
      }
      if (aiMessage) {
        // 流式添加文本到AI回复
        aiMessage.content += res.data || '';
        chatStatus.value = chatStatusEnum.ANSWERING;
      }

      // 保存完整回复以备参考
      receiveInterviewText.value += res.data || '';
    } else if (res.message == "recognize-intermediate") {
      // replyInterviewText.value = res.data;
      tempRecognizeText.value += res.data; // 更新临时识别文本
    } else if (res.message == "recognize-final") {
      // replyInterviewText.value += res.data; // 更新最终回复内容
      tempRecognizeText.value = ""; // 清空临时识别文本
      finalRecognizeText.value += res.data; // 更新最终识别文本
      // chatStatus.value = chatStatusEnum.WAITING; // 识别完成，恢复状态
    } else if (res.message == "speech_start") {
      chatStatus.value = chatStatusEnum.THINKING; // 开始语音识别
    } else if (res.message == "speech_end") {
      chatStatus.value = chatStatusEnum.WAITING; // 语音识别结束
    }
    else{
      Message.error("未知消息类型: " + res.message);
    }
  };

  ws.onclose = function() {
    Message.info("连接已关闭");
  };

  ws.onerror = function(error) {
    Message.error("WebSocket 错误");
  };
}

const round = ref<number>(0);

// 添加类型定义
interface ChatMessage {
  avatar: string;
  name: string;
  content: string;
  reasoning: string;
  role: 'user' | 'assistant' | 'error';
  datetime?: string;
}

function sendMessage() {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    Message.info("WebSocket 连接未建立，请稍后再试。");
    return;
  }
  if (replyInterviewText.value.trim() === '') {
    Message.warning("请输入回复内容");
    return;
  }

  if (isPlaying.value) {
    Message.warning("面试官正在说话中，请稍后。");
    return;
  }

  // 添加用户消息到聊天界面
  chatList.value.unshift({
    avatar: 'https://tdesign.gtimg.com/site/avatar.jpg',
    name: '自己',
    content: replyInterviewText.value,
    role: 'user',
    reasoning: '',
  });

  // 添加AI空消息占位
  chatList.value.unshift({
    avatar: 'https://tdesign.gtimg.com/site/chat-avatar.png',
    name: 'AI面试官',
    content: '',
    reasoning: '',
    role: 'assistant',
  });

  const msg = {
    data: replyInterviewText.value,
    round: round.value,
    type: 'text'
  }
  ws.send(JSON.stringify(msg));
  // replyInterviewText.value = ""; // 清空输入内容
  finalRecognizeText.value = ""; // 清空最终识别文本
  tempRecognizeText.value = ""; // 清空临时识别文本

  allAudioData.value = []; // 清空所有音频数据

  // 确保聊天滚动到最新消息
  setTimeout(() => {
    if (chatRef.value) {
      chatRef.value.scrollToBottom();
    }
  }, 100);
}

// --- 计算属性 ---
const chatStatusText = computed(() => {
  switch (chatStatus.value) {
    case chatStatusEnum.WAITING:
      return '点击麦克风开始提问';
    case chatStatusEnum.RECORDING:
      return '正在聆听...';
    case chatStatusEnum.THINKING:
      return '语音识别中...';
    case chatStatusEnum.ANSWERING:
      return '回答中...';
    default:
      return '';
  }
});

const showVoiceWave = computed(() => {
  return isPlaying.value || chatStatus.value === chatStatusEnum.RECORDING;
});

const replyInterviewText = computed({
  get: () => {
    if (finalRecognizeText.value === "" && tempRecognizeText.value === "") {
      return '';
    }
    if (tempRecognizeText.value !== "") {
      return finalRecognizeText.value.toString() + tempRecognizeText.value.toString();
    }
    return finalRecognizeText.value.toString();
  },
  set: (newValue) => {
    finalRecognizeText.value = newValue;
    tempRecognizeText.value = "";
  }
});

const clearConfirm = function () {
  chatList.value = [];
};
</script>

<style scoped>
.container {
  width: 100vw;
  max-width: 100%;
  display: flex;
  flex-direction: row;
  height: calc(100vh - 100px);
  box-sizing: border-box;
  margin: 0;
  padding: 4px; /* 确保无额外空隙 */
  /* 隐藏滚动条 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE10+ */

  /* Chrome、Safari */
}

.chat-box::-webkit-scrollbar {
  display: none;
}

.panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  margin: 0.5REM;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1),
  0 10px 15px rgba(0, 0, 0, 0.05); /* 阴影效果 */
  padding: 20px; /* 内边距 */
  transition: box-shadow 0.3s ease; /* 过渡效果 */

  border-radius: 1REM;
}

/* 可选：悬停时的阴影效果 */
.panel:hover {
  box-shadow: 0 6px 10px rgba(0, 0, 0, 0.13),
  0 12px 17px rgba(0, 0, 0, 0.07);
}

/* 面试官面板占4份 */
.interviewer-panel {
  flex: 4;
  width: 100%;
  background-color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  margin-right: 1REM;
}

/* 聊天面板占6份 */
.chat-panel {
  flex: 6;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between; /* 上下空间均匀分布 */
}

.call-header {
  display: flex;
  justify-content: center;
  align-self: center;
}

.class-header-tag {
  min-width: 6REM;
  padding: 0 1rem;
  height: 2.2REM;
  background-color: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 2REM;
  text-align: center;
  font-size: 14px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.call-avatar {
  flex-grow: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.blur-ball {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, #67e8f9, #3b82f6);
  border-radius: 50%;
  filter: blur(8px);
  opacity: 0.9;
  transition: background 0.3s ease;
}

.call-controls {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.call-status {
  font-size: 18px;
  margin-bottom: 60px;
  text-align: center;
  color: #333;
  min-height: 27px;
  margin-top: 1REM;
}

@media (max-width: 768px) {
  .blur-ball {
    width: 48px;
    height: 48px;
  }
  ::v-deep .arco-row {
    flex-flow: column; /* 或其他你想要的值 */
  }
}

/** Chat Panel **/
/* 应用滚动条样式 */
::-webkit-scrollbar-thumb {
  background-color: var(--td-scrollbar-color);
}
::-webkit-scrollbar-thumb:horizontal:hover {
  background-color: var(--td-scrollbar-hover-color);
}
::-webkit-scrollbar-track {
  background-color: var(--td-scroll-track-color);
}
.chat-box {
  background-color: white;
  border-radius: 1REM;
  flex: 1;
  margin-bottom: 1REM;
  width: 100%;
  position: relative;
  overflow: scroll;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE 10+ 和 Edge */
}

@media (max-width: 768px) {
  .container {
    display: flex;
    flex-direction: column;
    margin: 0 auto;
  }
  .panel {
    margin: 0 auto;
  }
  .interviewer-panel {
    width: 100%;
    height: 250px !important;
    max-height: 250px;
  }
  .chat-panel {
    width: 100%;
    flex: 2; /* 让两个面板平分剩余空间 */
    height: calc(100vh - 350px) !important;
    max-height: calc(100vh - 350px) !important; /* 减去顶部面试官面板的高度 */
    padding: 16px;
    border-right: none;
    border-bottom: 1px solid #eee;
  }

  .chat-box {
    height: 100%; /* 减去顶部头像和状态栏的高度 */
    overflow-y: scroll; /* 允许垂直滚动 */
  }

  .call-controls {
    flex-direction: row;
  }

  .call-avatar {
    margin: 1rem auto;
  }

  .call-status {
    font-size: 14px;
    margin-bottom: 0.5rem;
  }

  .voice-svg-wave {
    transform: scale(0.6);
  }
}

.input-sender-box-class {
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: center;
}

.input-box-class {
  width: 100%;
  height: 2REM;
  padding: 0.5REM;
  border-radius: 2REM;
  border: 1px solid #e5e7eb;
}

.call-controls {
  margin-left: 0.5REM;
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center; /* 垂直居中 */
  height: 100%; /* 确保父容器有高度 */
}

.btn {
  line-height: 3REM;
  font-size: 24px;
  font-weight: bold;
  padding: 0;
  margin: 0;
  width: 3REM;
  height: 3REM;
  border-radius: 50%;
  cursor: pointer;
  border: none;
}

.btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1),
  0 10px 15px rgba(0, 0, 0, 0.05);
}

.end {
  color: white;
  background-color: #f24944;
}

.interview-panel-switch-box-class {
  display: flex;
  flex-direction: row;
  gap: 2REM;
}

/* 声浪 */
.voice-svg-wave {
  width: 240px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1REM;
}

.voice-svg-wave svg rect {
  fill: #3b82f6;
  animation: waveMove 1.2s infinite ease-in-out;
}

.voice-svg-wave svg rect:nth-child(1)  { animation-delay: 0s;   }
.voice-svg-wave svg rect:nth-child(2)  { animation-delay: 0.1s; }
.voice-svg-wave svg rect:nth-child(3)  { animation-delay: 0.2s; }
.voice-svg-wave svg rect:nth-child(4)  { animation-delay: 0.3s; }
.voice-svg-wave svg rect:nth-child(5)  { animation-delay: 0.4s; }
.voice-svg-wave svg rect:nth-child(6)  { animation-delay: 0.5s; }
.voice-svg-wave svg rect:nth-child(7)  { animation-delay: 0.6s; }
.voice-svg-wave svg rect:nth-child(8)  { animation-delay: 0.7s; }
.voice-svg-wave svg rect:nth-child(9)  { animation-delay: 0.8s; }
.voice-svg-wave svg rect:nth-child(10) { animation-delay: 0.9s; }
.voice-svg-wave svg rect:nth-child(11) { animation-delay: 1.0s; }
.voice-svg-wave svg rect:nth-child(12) { animation-delay: 1.1s; }

@keyframes waveMove {
  0%, 100% {
    height: 10px;
    y: 10;
  }
  50% {
    height: 26px;
    y: 2;
  }
}

</style>