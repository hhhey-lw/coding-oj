<template>
  <div class="interview-prep-page">
    <!-- 顶部导航栏 -->
    <a-space direction="horizontal" class="nav-tabs">
      <button
          key="position"
          :class="{ active: currentTab === 'position' }"
          @click="switchTab('position')"
      >岗位信息</button>
      <IconRight/>
      <button
          key="resume"
          :class="{ active: currentTab === 'resume' }"
          @click="switchTab('resume')"
      >选择简历</button>
      <IconRight/>
      <button
          key="complete"
          :class="{ active: currentTab === 'complete' }"
          @click="switchTab('complete')"
      >准备完成
      </button>
    </a-space>

    <!-- 主体内容区 -->
    <div class="content-area">
      <!-- 岗位信息表单 -->
      <a-form v-if="currentTab === 'position'" class="position-form">
        <h2 class="main-title">准备面试什么岗位呢？</h2>

        <div class="form-container">
          <a-form-item label="岗位名称">
            <a-input
                v-model="formData.positionName"
                placeholder="请输入岗位名称"
                allow-clear
            />
          </a-form-item>

          <a-form-item label="岗位要求">
            <a-textarea
                v-model="formData.responsibilities"
                placeholder="请输入岗位职责描述"
                :auto-size="{
                  minRows: 10,
                  maxRows: 10
                }"
                allow-clear
            />
          </a-form-item>

          <button class="button-class" @click="clearPositionInformation">清空信息</button>
        </div>
      </a-form>

      <!-- 其他标签页占位（示例） -->
      <div v-if="currentTab === 'resume'" class="tab-placeholder">
        <h2>上传简历</h2>
        <a-textarea
            v-model="formData.resumeInformation"
            placeholder="简历信息"
            :auto-size="{
                  minRows: 10,
                  maxRows: 10
                }"
            allow-clear
        />
        <button class="button-class" @click="uploadResumeFile">上传PDF简历</button>
      </div>

      <div v-if="currentTab === 'complete'" class="tab-placeholder">
        <h3>准备完成</h3>
        <a-result status="success" title="准备就绪，开始吧！">
          <template #extra>
            <button @click="startInterview" class="button-class">开始面试</button>
          </template>
        </a-result>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {
  Space as ASpace,
  Form as AForm,
  Input as AInput,
  Textarea as ATextarea,
  Result as AResult, Message
} from '@arco-design/web-vue';
import {FileControllerService} from '@/api/generated-chat/services/FileControllerService';
import {OpenAPI} from '@/api/generated-chat/core/OpenAPI'
import store from "@/store";

import {
  IconRight
} from '@arco-design/web-vue/es/icon';
import { useRouter } from "vue-router";

const router = useRouter();


// 数据绑定
const currentTab = ref<string>('position'); // 默认显示岗位信息页

// 添加类型定义
interface FormDataType {
  positionName: string;
  responsibilities: string;
  resumeInformation: string;
}
const formData = ref<FormDataType>({
  positionName: 'Java实习岗位',
  responsibilities: '1.计算机相关专业，每周工作至少5天以上，能够持续3个月以上实习；有过相关项目实习经验者优先\n' +
      '2.掌握计算机编程原理、常用数据结构与算法；\n' +
      '3.有扎实的数据库知识，熟悉sql语言、Mysql/oracle等主流数据库；\n' +
      '4.熟悉Java开发，有扎实的计算机语言基础；\n' +
      '5.了解J2EE架构、SpringBoot/MyBatis等一种或几种java Web开发相关技术；\n' +
      '6.具有良好的逻辑推理、归纳总结能力，有独立发现问题、解决问题的能力；',
  resumeInformation: ''
});

// 方法定义
const switchTab = (tab:string) => {
  currentTab.value = tab;
};

const startInterview = async () => {
  if (store.state.user.token === '') {
    alert("请先登录！");
    return;
  }
  // 检查岗位信息是否填写完整
  if (formData.value.positionName.trim() === '' || formData.value.responsibilities.trim() === '' || formData.value.resumeInformation.trim() === '') {
    Message.error('信息不完整，请填写岗位信息和简历内容');
    return;
  }
  // 1. 上传文件到后端
  const formDataObj = new FormData();
  formDataObj.append('positionName', formData.value.positionName);
  formDataObj.append('responsibilities', formData.value.responsibilities);
  formDataObj.append('resumeInformation', formData.value.resumeInformation);
  const uploadResponse = await fetch(OpenAPI.BASE + `/resume/start/upload/info`, {
    headers: {
      'Authorization': `${store.state.user.token || ''}`
    },
    method: 'POST',
    body: formDataObj
  }).then(res => res.json());

  if (uploadResponse.code !== 0 || !uploadResponse.data) {
    Message.error(uploadResponse.message || '上传失败');
    return;
  }

  // 2. 跳转到面试页面
  router.push({
    path: "/interview/simulate"
  });

};

function uploadResumeFile() {
  if (store.state.user.token === '') {
    alert("请先登录！");
    return;
  }

  // 创建文件选择器
  const fileInput = document.createElement('input');
  fileInput.type = 'file';
  fileInput.accept = '.pdf';

  // 添加选择文件后的处理逻辑
  fileInput.onchange = async (event: any) => {
    const file = event.target.files?.[0];
    if (!file) {
      Message.warning('请选择一个PDF文件');
      return;
    }

    // 文件大小限制检查（10MB）
    if (file.size > 10 * 1024 * 1024) {
      Message.error('文件大小不能超过10MB');
      return;
    }

    // 显示上传中的提示
    const loadingMsg = Message.loading({
      content: '正在上传简历，请稍候...',
      duration: 0
    });

    try {
      // 1. 上传文件到后端
      const formDataObj = new FormData();
      formDataObj.append('file', file);
      formDataObj.append('biz', 'user_resume');

      // 调用上传API
      const uploadResponse = await fetch(OpenAPI.BASE + `/file/upload/resume/pdf`, {
        headers: {
          'Authorization': `${store.state.user.token || ''}`
        },
        method: 'POST',
        body: formDataObj
      }).then(res => res.json());

      if (uploadResponse.code !== 0 || !uploadResponse.data) {
        throw new Error(uploadResponse.message || '上传失败');
      }

      // 上传成功，获取文件URL
      const fileUrl = uploadResponse.data;

      // 更新提示消息
      loadingMsg.content = '正在识别简历内容...';

      // 2. 请求简历识别服务
      const recognizeResponse = await fetch(OpenAPI.BASE + `/resume/recognize/pdf?resumeFileUrl=${fileUrl}`, {
        headers: {
          'Authorization': `${store.state.user.token || ''}`
        },
        method: 'GET',
      }).then(res => res.json());

      if (recognizeResponse.code !== 0 || !recognizeResponse.data) {
        throw new Error(recognizeResponse.message || '识别失败');
      }

      // 3. 将识别结果绑定到表单
      formData.value.resumeInformation = recognizeResponse.data;

      // 关闭加载提示
      loadingMsg.close();

      // 显示成功提示
      Message.success('简历上传并识别成功');

    } catch (error: any) {
      // 关闭加载提示
      loadingMsg.close();

      // 显示错误信息
      console.error('简历处理错误:', error);
      Message.error(`简历处理失败: ${error.message || '未知错误'}`);
    }
  };

  // 触发文件选择对话框
  fileInput.click();
}

function clearPositionInformation() {
  formData.value.positionName = '';
  formData.value.responsibilities = '';
}

function loadUserResumeInfo() {
  // 如果用户已登录，加载用户简历信息
  if (store.state.user.token) {
    fetch(OpenAPI.BASE + `/resume/get/info`, {
      headers: {
        'Authorization': `${store.state.user.token || ''}`
      },
      method: 'GET',
    }).then(res => res.json()).then(data => {
      if (data.code === 0 && data.data) {
        formData.value.resumeInformation = data.data.resumeInformation || '';
        formData.value.positionName = data.data.positionName || '';
        formData.value.responsibilities = data.data.responsibilities || '';
      } else {
        Message.error(data.message || '加载简历信息失败');
      }
    }).catch(error => {
      console.error('加载简历信息错误:', error);
      Message.error('加载简历信息失败');
    });
  }
}

onMounted(() => {
  // 初始化时可以加载一些数据或状态
  loadUserResumeInfo();
});

</script>

<style scoped>
.interview-prep-page {
  padding: 20px;
  background-color: #e5efeb;
  height: calc(100vh - 120px); /* 减去顶部导航栏高度 */
  overflow: hidden;
}

.nav-tabs {
  display: flex;
  flex-direction: row;
  justify-content: space-around;
  padding: 0.5REM 0;
  background-color: #fff;
  width: 70%;
  margin: 0 auto 20px;
  border-radius: 1REM;
}

.nav-tabs button {
  width: 100%;
  height: 2REM;
  line-height: 2REM;
  background-color: #fff;
  border: none;
  cursor: pointer;
  font-size: 16px;
}

.content-area {
  background-color: white;
  padding: 24px;
  border-radius: 8px;
}

.main-title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 20px;
}

.form-container {
  flex: 1;
  margin-left: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}


.tab-placeholder {
  height: 100%;
  padding: 20px;
  text-align: center;
}

.active {
  font-weight: bold;
  color: #4eaa91;
}

.button-class {
  margin: 2REM auto 0;
  white-space: nowrap;
  background-color: #8ebc8e;
  border: 2px solid #2d8a55;
  border-radius: 3REM;
  height: 3REM;
  line-height: 1.5REM;
  color: white;
  width: 10REM;
  font-size: 16px;
  font-weight: bold;
}

.button-class:hover {
  background-color: #2d8a55;
  border-color: #1c5d39;
  cursor: pointer;
}

@media (max-width: 768px) {
  .nav-tabs {
    width: 100%;
  }
  .nav-tabs button {
    font-size: 12px;
  }
}

</style>