<template>
  <Viewer :value="value" :plugins="plugins" />
</template>

<script setup lang="ts">
// 确保在首行引入样式
import 'highlight.js/styles/github.css';
import gfm from "@bytemd/plugin-gfm";
import highlight from "@bytemd/plugin-highlight";
import { Viewer } from "@bytemd/vue-next";
import { withDefaults, defineProps } from "vue";

interface Props {
  value: string;
}

const plugins = [
  gfm(),
  highlight(),
  // Add more plugins here
];

const props = withDefaults(defineProps<Props>(), {
  value: () => '',
});
</script>

<style scoped>
</style>