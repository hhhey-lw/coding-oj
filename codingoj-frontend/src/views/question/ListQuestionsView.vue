<template>
  <div id="questionsView">
      <h2 class="section-title">推荐</h2>
    <div class="recommendation-wrapper">
      <a-row class="recommendation-container" :gutter="[20, 20]">
        <!-- 平台卡片 -->
        <a-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
          <a-card class="recommend-card saikr-card" hoverable>
            <div class="card-content">
              <h3 class="card-title">Long Oj</h3>
              <p class="card-subtitle">全国大学生竞赛活动平台</p>
            </div>
          </a-card>
        </a-col>

        <!-- 真题集锦卡片 -->
        <a-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
          <a-card class="recommend-card past-questions-card" hoverable>
            <div class="card-content">
              <h3 class="card-title">往届真题集锦</h3>
              <p class="card-desc">图论、月赛 & 高精度</p>
            </div>
          </a-card>
        </a-col>

        <!-- 算法100例卡片 -->
        <a-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
          <a-card class="recommend-card algorithm-card" hoverable>
            <div class="card-content">
              <div class="big-number">100</div>
              <h3 class="card-title">基础算法100例</h3>
              <p class="card-desc">适合算法入门</p>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <a-form :model="searchParams" layout="inline" style="display: flex; margin-bottom: -1REM;margin-top: 0.5REM; flex-direction: row; justify-content: start; align-content: center">
      <a-form-item field="title" label="搜索：" style="font-weight: bold; display: flex">
        <a-input v-model="searchParams.title" class="len-input-class" placeholder="搜索题目标题"/>
      </a-form-item>

      <a-form-item style="display: inline">
        <button class="button-class"  @click="doSubmit">搜索</button>
      </a-form-item>
    </a-form>
    <a-divider/>
    <a-table
      :ref="tableRef"
      :columns="columns"
      :data="dataList"
      :pagination="{
        showTotal: true,
        pageSize: searchParams.pageSize,
        current: searchParams.current,
        total,
      }"
      :header-cell-class="() => 'bold-header'"
      @page-change="onPageChange"
      @cell-click="toQuestionPage"
      style="cursor: pointer"
      stripe
      :header-cell-style="{
        fontWeight: 'bold',
        whiteSpace: 'nowrap',
        width: 'auto',
      }"
      :scroll="{ x: 500 }"
      :scrollbar="true"
      >
      <template #status = "{ record }">
        <a-checkbox value="1" disabled></a-checkbox>
      </template>
      <template  #title="{ record }">
        <div style="color: #333;">
          {{record.title}}
        </div>
      </template>
      <template  #difficulty="{ record }">
        <div style="color: darkcyan;">
          {{record.difficulty}} 星
        </div>
      </template>
      <template #tags="{ record }">
        <a-space wrap>
          <a-tag v-for="(tag, index) of record.tags" :key="index" color="green"
            >{{ tag }}
          </a-tag>
        </a-space>
      </template>
      <template #acceptedRate="{ record }">
        {{record.submitNum > 0 ? (record.acceptedNum / record.submitNum * 100).toFixed(2) : "00.00"}} %
      </template>
    </a-table>
    <a-divider/>

    <UserCheckInView  :xs="24" :sm="24" :md="8" :lg="8" :xl="8" style="box-shadow: 0 2px 8px rgba(0, 0, 0, .05); border-radius: 12px; width: 60%"/>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watchEffect } from "vue";
import UserCheckInView from "@/views/user/UserCheckInView.vue";
import { Card as ACard, Button as AButton } from '@arco-design/web-vue'
import {
  Question,
  QuestionControllerService,
  QuestionQueryRequest,
} from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";

const tableRef = ref();

const dataList = ref([]);
const total = ref(0);
const searchParams = ref<QuestionQueryRequest>({
  title: "",
  tags: [],
  pageSize: 10,
  current: 1,
});


const loadData = async () => {
  const res = await QuestionControllerService.listQuestionVoByPageUsingPost(
    searchParams.value
  );
  if (res.code === 0) {
    dataList.value = res.data.records;
    total.value = res.data.total;
  } else {
    message.error("加载失败，" + res.message);
  }
};

/**
 * 监听 searchParams 变量，改变时触发页面的重新加载
 */
watchEffect(() => {
  loadData();
});

/**
 * 页面加载时，请求数据
 */
onMounted(() => {
  loadData();
});

const columns = [
  {
    title: "状态",
    slotName: "status",
  },
  {
    title: "题目名称",
    slotName: "title",
  },
  {
    title: "难度",
    slotName: "difficulty",
  },
  {
    title: "标签",
    slotName: "tags",
  },
  {
    title: "通过率",
    slotName: "acceptedRate",
  }
];

const onPageChange = (page: number) => {
  searchParams.value = {
    ...searchParams.value,
    current: page,
  };
};

const router = useRouter();

/**
 * 跳转到做题页面
 * @param question
 */
const toQuestionPage = (question: Question) => {
  router.push({
    path: `/view/question/${question.id}`,
  });
};

/**
 * 确认搜索，重新加载数据
 */
const doSubmit = () => {
  // 这里需要重置搜索页号
  searchParams.value = {
    ...searchParams.value,
    current: 1,
  };
};

</script>

<style scoped>
#questionsView {
  width: 80%;
  margin: 0 auto;
}

:deep(.bold-header) {
  font-weight: bold !important;
  /* 可选附加样式 */
  background-color: #f7f8fa;
  color: #333;
}

:deep(.arco-table-th) {
  font-weight: bold !important;
}

@media (max-width: 768px) {
  .mobile-responsive-table {
    width: 100%;
    overflow-x: scroll;
    -webkit-overflow-scrolling: touch;
  }

  .mobile-responsive-table :deep(.arco-table) {
    min-width: 100%;
    width: auto;
  }

  .mobile-responsive-table :deep(.arco-table-th),
  .mobile-responsive-table :deep(.arco-table-td) {
    white-space: nowrap;
    min-width: 120px;
    padding: 8px 12px;
  }
}

.recommendation-wrapper {
  width: 100%;
  padding: 20px 0;
  background-color: white;
  display: flex;
  justify-content: center;
  align-items: stretch;
  align-content: center;
}

.section-title {
  text-align: center;
  margin-bottom: 24px;
  font-size: 24px;
  color: #1d2129;
}

.recommendation-container {
  display: flex;
  justify-content: center;
  align-items: stretch;
  align-content: center;
  max-width: 100vw;
  margin: 0 auto;
  width: 100%;
}

.recommend-card {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.recommend-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  height: 100%;
}

.card-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 8px;
  color: white;
}

.card-subtitle {
  font-size: 14px;
  margin-bottom: 20px;
  color: white;
  opacity: 0.9;
}

.card-desc {
  font-size: 14px;
  color: white;
  opacity: 0.8;
}

.action-button {
  margin-top: auto;
  width: 120px;
}

/* 卡片颜色样式 */
.saikr-card {
  background: linear-gradient(135deg, #52c41a, #389e0d);
}

.past-questions-card {
  background: linear-gradient(135deg, #1890ff, #096dd9);
}

.algorithm-card {
  background: linear-gradient(135deg, #722ed1, #531dab);
}

/* 大数字样式 */
.big-number {
  font-size: 48px;
  font-weight: bold;
  color: white;
  margin-bottom: 10px;
}

/* 图标样式 */
.card-icon {
  font-size: 36px;
  color: white;
  margin-bottom: 15px;
}

@media screen and (min-width: 768px) {
  .len-input-class {
    min-width: 500px;
  }
}

@media screen and (max-width: 768px) {
  #questionsView {
    width: 100%;
    margin: 0 auto;
  }
}

.button-class {
  white-space: nowrap;
  background-color: #8ebc8e;
  border: 2px solid #2d8a55;
  border-radius: 5%;
  height: 36px;
  color: white;
  width: 5REM;
  font-size: 16px;
}

.button-class:hover {
  background-color: #2d8a55;
  border-color: #1c5d39;
  cursor: pointer;
}

</style>
