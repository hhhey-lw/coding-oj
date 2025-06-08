<template>
  <div class="oj-home-container">

    <a-layout>
      <!-- 主内容区 -->
      <a-layout-content class="content">
        <div class="hero-section">
          <h1 class="title animate__animated animate__fadeInDown">𝐂𝐨𝐝𝐢𝐧𝐠 𝐎𝐉</h1>
          <p class="subtitle animate__animated animate__fadeInUp">专注于算法竞赛的在线评测系统</p>
          <p class="description animate__animated animate__fadeInUp animate__delay-1s">
            为编程爱好者提供专业的算法训练平台
          </p>

          <!-- 使用栅格系统的按钮组 -->
          <a-row
              class="action-buttons animate__animated animate__fadeInUp animate__delay-2s"
              :gutter="[16, 16]"
          >
            <a-col :xs="24" :sm="8" :md="8" :lg="8" :xl="8">
              <a-button
                  type="primary"
                  size="large"
                  class="start-btn"
                  @click="toQuestionPage"
                  block
              >
                <template #icon>
                  <icon-code />
                </template>
                开始刷题
              </a-button>
            </a-col>

            <a-col :xs="24" :sm="8" :md="8" :lg="8" :xl="8">
              <a-button
                  size="large"
                  class="contest-btn"
                  block
              >
                <template #icon>
                  <icon-trophy />
                </template>
                参加比赛
              </a-button>
            </a-col>

            <a-col :xs="24" :sm="8" :md="8" :lg="8" :xl="8">
              <a-button
                  size="large"
                  class="rank-btn"
                  block
              >
                <template #icon>
                  <icon-trophy />
                </template>
                查看排名
              </a-button>
            </a-col>
          </a-row>
        </div>
      </a-layout-content>
    </a-layout>
  </div>
</template>

<script setup>
import '@arco-design/web-vue/dist/arco.css';
import {
  IconCode,
  IconTrophy,
} from '@arco-design/web-vue/es/icon';
import { useRouter } from "vue-router";

// 路由跳转
const router = useRouter();
const toQuestionPage = () => {
  router.push({
    path: `/questions`,
  });
};


// 引入Animate.css CDN
const loadAnimateCSS = () => {
  const link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = 'https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css';
  document.head.appendChild(link);
};

loadAnimateCSS();
</script>

<style scoped>
.oj-home-container {
  height: 100vh;
  background-color: #fff;
}

.content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 128px);
  padding: 24px;
}

.hero-section {
  text-align: center;
  max-width: 800px;
  margin: 0 auto;
}

.title {
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 16px;
  color: #00b96b;
}

.subtitle {
  font-size: 20px;
  color: #333;
  margin-bottom: 8px;
}

.description {
  font-size: 16px;
  color: #666;
  margin-bottom: 32px;
}

.action-buttons {
  margin-top: 32px;
}

.start-btn {
  background-color: #165dff;
  border-color: #165dff;
}

.contest-btn,
.rank-btn {
  color: #00b96b;
  border-color: #00b96b;
}

.contest-btn:hover,
.rank-btn:hover {
  background-color: rgba(0, 185, 107, 0.1);
}


/* 响应式调整 */
@media (max-width: 768px) {
  .title {
    font-size: 36px;
  }

  .subtitle {
    font-size: 18px;
  }

  .description {
    font-size: 14px;
  }

  .action-buttons {
    width: 100%;
  }
}
</style>