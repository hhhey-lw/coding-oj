<template>
  <div id="app">
    <template v-if="route.path.startsWith('/user')">
      <router-view />
    </template>
    <template v-else>
      <BasicLayout />
    </template>
    <!-- 固定按钮 -->
    <a-avatar
        v-show="!hideAiAvatar"
        :size="64"
        style="position: fixed; bottom: 120px; right: 20px; z-index: 1000; background-color: white"
        @click="showChatDialog">
      <img
          alt="Coding AI"
          src="https://longoj-1350136079.cos.ap-nanjing.myqcloud.com/user_avatar/chat-ai-robot-icon"
      />
    </a-avatar>

    <!-- 聊天对话框组件 -->
    <ChatView
    style="max-width: 90vw !important; overflow-x: hidden"
    :visible="chatDialogVisible"
    @update:visible="chatDialogVisible = $event"/>
  </div>
</template>

<script setup lang="ts">
import BasicLayout from "@/layouts/BasicLayout.vue";
import {ref, onMounted, watch} from "vue";
import { useRoute } from "vue-router";
import ChatView from "@/views/chat/ChatView.vue";

import store from "@/store";
const route = useRoute();

const hideAiAvatar = ref(false);

// 监听路由变化，在面试页面、登录和注册页面隐藏头像
watch(() => route.path, (newPath) => {
  hideAiAvatar.value = newPath.includes('/interview') ||
      newPath.includes('/interview/simulate') ||
      newPath.includes('/user/login') ||
      newPath.includes('/user/register');
}, { immediate: true });

/**
 * 全局初始化函数，有全局单次调用的代码，都可以写到这里
 */
const doInit = () => {
  console.log("hello 欢迎来到我的项目");
};

const chatDialogVisible = ref(false);

const showChatDialog = () => {
  console.log("click btn");
  chatDialogVisible.value = true;
};

onMounted(() => {
  doInit();
});
</script>

<style>
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  height: 100vh;
}
</style>
