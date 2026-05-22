<template>
  <el-select :model-value="activeId" @update:model-value="$emit('change', $event)"
    placeholder="选择模型" size="small" style="width: 180px">
    <el-option v-for="p in providers" :key="p.id" :label="p.name" :value="p.id" />
  </el-select>
</template>

<script setup>
defineProps({ providers: Array, activeId: [Number, String] })
defineEmits(['change'])
</script>
