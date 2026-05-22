<template>
  <el-radio-group :model-value="mode" @update:model-value="$emit('change', $event)" size="small">
    <el-radio-button value="DIRECT">直接问答</el-radio-button>
    <el-radio-button value="RAG">知识库增强</el-radio-button>
  </el-radio-group>
</template>

<script setup>
defineProps({ mode: String })
defineEmits(['change'])
</script>
