<template>
  <!-- 帖子列表 -->
  <div v-for="item in currentPageData" :key="item.id">
    <a-card :bordered="true"
            :style="{
          borderRadius: '12px',
          boxShadow: '0 2px 2px rgba(0, 0, 0, 0.1)',
          marginBottom: '16px'
        }"
            :body-style="{
          padding: '16px'
        }"
            @click="toDetailDiscussion(item.id)"
            style="cursor: pointer">
      <a-space direction="vertical" style="width: 100%;">
        <!-- 卡片内容保持不变... -->
        <!-- 标签 -->
        <a-space wrap>
          <!--       :color="tag.color"      -->
          <a-tag v-for="(tag,index) in item.tagList" :key="tag" :color="tagColor[index]">
            {{ tag }}
          </a-tag>
        </a-space>

        <!-- 标题 -->
        <a-typography-title :heading="5">{{ item.title }}</a-typography-title>

        <!-- 内容摘要 -->
        <a-typography-paragraph :ellipsis="{ rows: 2 }">
          {{ item.content }}
        </a-typography-paragraph>

        <!-- 底部信息 -->
        <a-row
            type="flex"
            align="middle"
            justify="space-between"
            style="width: 100%"
        >
          <a-space>
            <a-avatar>
              <img :key="item.user.userAvatar" :src="item.user.userAvatar" :size="24" />
            </a-avatar>
            <span>{{ item.user.userName }}</span>
          </a-space>
          <a-space>
            <a-space>
              <svg viewBox="0 0 24 24" width="16" height="16">
                <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
              </svg>{{ item.pageView }}
            </a-space>
            <a-space>
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
              </svg>
              {{ item.commentNum }}
            </a-space>
            <a-space>
              <span class="icon-hover"> <IconThumbUpFill /> </span>
              {{ item.thumbNum }}
            </a-space>
            <span>&nbsp&nbsp{{ formatUtcDateTime(item.createTime) }}</span>
          </a-space>
        </a-row>
      </a-space>
    </a-card>
  </div>

  <!-- 分页导航 -->
  <a-pagination
      :current="queryParams.current"
      :total="queryParams.total"
      :page-size="queryParams.pageSize"
      show-total
      :style="{ marginTop: '20px', justifyContent: 'center' }"
      @change="handlePageChange"
  />
</template>

<script setup lang="ts">
import {IconThumbUpFill} from "@arco-design/web-vue/es/icon";
import {computed, onMounted, ref} from "vue";
import {PostControllerService, PostFavourControllerService, PostQueryRequest, PostVO} from "../../../generated";
import { useRouter } from "vue-router";
// =====> 变量定义 <=====
const router = useRouter();

let postList = ref<PostVO[]>([]);

const tagColor = [
  "#19be6b",
  "#ed4014",
  "#ff9900",
  "#2d8cf0"
]

let queryParams = ref<PostQueryRequest>({
  current: 1,
  pageSize: 2,
  total: 0,
  sortField: 'createTime',
  sortOrder: 'descend',
  searchText: ''
})


// =====> 函数定义 <=====
const loadPostData = async () => {
  try {
    console.log("加载数据中...")
    const res = await PostFavourControllerService.listMyFavourPostByPageUsingPost({
      current: queryParams.value.current,
      pageSize: queryParams.value.pageSize,
    });
    console.log(res)
    if (res.code === 0 && res.data?.records) {
      postList.value = res.data.records; // 使用.value更新响应式数据
      queryParams.value.total = res.data.total;
    }
  } catch (error) {
    console.error('加载数据失败:', error);
  }
}

// 计算当前页显示的数据
const currentPageData = computed(() => {
  return postList.value
})

// 时间格式化
const formatUtcDateTime = (isoString: string): string => {
  // 验证输入
  if (!isoString || typeof isoString !== 'string') {
    console.warn('Invalid input: Expected ISO 8601 date string');
    return 'Invalid date';
  }

  const date = new Date(isoString);

  // 验证日期是否有效
  if (isNaN(date.getTime())) {
    console.warn('Invalid date format: Could not parse date');
    return 'Invalid date';
  }

  const pad = (num: number): string => num.toString().padStart(2, '0');

  // 使用 getUTC* 方法获取 UTC 时间
  const year = date.getUTCFullYear();
  const month = pad(date.getUTCMonth() + 1);
  const day = pad(date.getUTCDate());
  const hours = pad(date.getUTCHours());
  const minutes = pad(date.getUTCMinutes());
  const seconds = pad(date.getUTCSeconds());

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

// 页码变化处理
const handlePageChange = (page: number) => {
  queryParams.value.current = page
  loadPostData();
  // 这里可以添加滚动到顶部的逻辑
  window.scrollTo({ top: 0, behavior: 'smooth' })
}


// =====> 路由部分 <=====
const toDetailDiscussion = (id:number) => {
  router.push({
    path: `/discussion/detail/${id}`
  });
};

// =====> 生命周期 <=====
onMounted(() => {
  loadPostData();
});

</script>

<style scoped>
.box-class{
  width: 80%
}
@media screen and (max-width: 768px)  {
  .mobile-class {
    width: 100%;
  }
}
</style>