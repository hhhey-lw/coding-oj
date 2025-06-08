<template>
  <a-space id="addDiscussionView" direction="vertical" style="width: 90vw;">
    <h2>发布帖子</h2>
    <a-form :model="form" label-align="left" class="box-class">
      <a-form-item field="title" label="标题" style="max-width: 1000px">
        <a-input v-model="form.title" placeholder="请输入标题" />
      </a-form-item>
      <a-form-item field="tags" label="标签" style="max-width: 1000px">
        <a-input-tag v-model="form.tags" placeholder="请选择标签" allow-clear />
      </a-form-item>
      <a-form-item field="content" label="内容">
        <MdEditor :value="form.content" :handle-change="onContentChange" />
      </a-form-item>
      <div style="margin-top: 16px" />
      <a-form-item>
        <a-button type="primary" style="min-width: 200px" @click="doSubmit"
        >发布
        </a-button>
      </a-form-item>
    </a-form>
  </a-space>
</template>

<script setup lang="ts">
import { ref } from "vue";
import MdEditor from "@/components/MdEditor.vue";
import {PostAddRequest, PostControllerService} from "../../../generated";
import message from "@arco-design/web-vue/es/message";

let form = ref<PostAddRequest>({
  title: "",
  tags: [],
  content: ""
});

const doSubmit = async () => {
  console.log(form.value);

  const res = await PostControllerService.addPostUsingPost(
      form.value
  );
  if (res.code === 0) {
    message.success("创建成功");
    alert("发布成功！");
    form.value.content = "";
    form.value.tags = "";
    form.value.title = "";
  } else {
    message.error("创建失败，" + res.message);
  }
};

const onContentChange = (value: string) => {
  form.value.content = value;
};

</script>

<style scoped>
.box-class {
  display: flex;
  flex-direction: column;
  justify-content: center;
}
</style>
