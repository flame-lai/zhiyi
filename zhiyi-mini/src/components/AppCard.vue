<template>
  <View class="app-card" @tap="$emit('tap')">
    <View class="app-card-icon" v-if="icon">
      <Text class="icon-text">{{ icon }}</Text>
    </View>
    <View class="app-card-body">
      <Text class="app-card-title">{{ title }}</Text>
      <Text class="app-card-desc" v-if="desc">{{ desc }}</Text>
    </View>
    <View class="app-card-arrow" v-if="arrow">
      <Text class="arrow-text">›</Text>
    </View>
  </View>
</template>

<script setup lang="ts">
defineProps<{ icon?: string; title: string; desc?: string; arrow?: boolean }>()
defineEmits(['tap'])
</script>

<style lang="scss">
.app-card { display: flex; align-items: center; background: #fff; border-radius: 16px; padding: 28px 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
.app-card-icon { width: 80px; height: 80px; border-radius: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); display: flex; align-items: center; justify-content: center; margin-right: 24px; flex-shrink: 0; }
.icon-text { font-size: 36px; color: #fff; }
.app-card-body { flex: 1; min-width: 0; }
.app-card-title { display: block; font-size: 30px; font-weight: 600; color: #1a1a2e; margin-bottom: 6px; }
.app-card-desc { display: block; font-size: 24px; color: #9ca3af; }
.app-card-arrow { flex-shrink: 0; padding-left: 16px; }
.arrow-text { font-size: 36px; color: #d1d5db; }
</style>
