<template>
  <View class="empty-state">
    <Text class="empty-icon">📋</Text>
    <Text class="empty-text">{{ text || '暂无数据' }}</Text>
  </View>
</template>

<script setup lang="ts">
defineProps<{ text?: string }>()
</script>

<style lang="scss">
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; }
.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-text { font-size: 28px; color: #9ca3af; }
</style>
