<template>
  <View class="status-tag" :class="typeClass">
    <Text class="status-tag-text">{{ text }}</Text>
  </View>
</template>

<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ text: string; type?: 'success' | 'warning' | 'danger' | 'info' | 'default' }>()
const typeClass = computed(() => `status-tag--${props.type || 'default'}`)
</script>

<style lang="scss">
.status-tag { display: inline-flex; align-items: center; padding: 4px 16px; border-radius: 8px; font-size: 22px; }
.status-tag-text { font-size: 22px; }
.status-tag--success { background: #d1fae5; } .status-tag--success .status-tag-text { color: #065f46; }
.status-tag--warning { background: #fef3c7; } .status-tag--warning .status-tag-text { color: #92400e; }
.status-tag--danger { background: #fee2e2; } .status-tag--danger .status-tag-text { color: #991b1b; }
.status-tag--info { background: #dbeafe; } .status-tag--info .status-tag-text { color: #1e40af; }
.status-tag--default { background: #f3f4f6; } .status-tag--default .status-tag-text { color: #6b7280; }
</style>
