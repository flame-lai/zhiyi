<template>
  <View class="confirm-panel" v-if="visible">
    <View class="confirm-mask" @tap="$emit('cancel')" />
    <View class="confirm-body">
      <Text class="confirm-title">{{ title }}</Text>
      <View class="confirm-content"><slot /></View>
      <View class="confirm-actions">
        <View class="confirm-btn cancel" @tap="$emit('cancel')">
          <Text class="btn-text">取消</Text>
        </View>
        <View class="confirm-btn submit" :class="{ loading }" @tap="$emit('confirm')">
          <Text class="btn-text">{{ confirmText || '确认' }}</Text>
        </View>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
defineProps<{ visible: boolean; title: string; confirmText?: string; loading?: boolean }>()
defineEmits(['confirm', 'cancel'])
</script>

<style lang="scss">
.confirm-panel { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 999; display: flex; align-items: center; justify-content: center; }
.confirm-mask { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.4); }
.confirm-body { position: relative; width: 80%; max-width: 600px; background: #fff; border-radius: 20px; padding: 40px 32px 32px; z-index: 1; }
.confirm-title { display: block; font-size: 34px; font-weight: 600; color: #1a1a2e; text-align: center; margin-bottom: 24px; }
.confirm-content { margin-bottom: 32px; }
.confirm-actions { display: flex; gap: 24px; }
.confirm-btn { flex: 1; height: 80px; display: flex; align-items: center; justify-content: center; border-radius: 12px; }
.confirm-btn.cancel { background: #f3f4f6; } .confirm-btn.cancel .btn-text { color: #6b7280; }
.confirm-btn.submit { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); } .confirm-btn.submit .btn-text { color: #fff; }
.confirm-btn.loading { opacity: 0.6; }
.btn-text { font-size: 30px; font-weight: 500; }
</style>
