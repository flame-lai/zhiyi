<template>
  <View class="process-list">
    <View class="process-item" v-for="p in processes" :key="p.processId"
      :class="{ done: p.status === 'DONE', disabled: isBatchDisabled(p), selected: isSelected(p.processId) }"
      @tap="toggleProcess(p)">
      <View class="process-check">
        <View class="check-box" v-if="p.status === 'DONE'"><Text class="check-icon">✓</Text></View>
        <View class="check-box unchecked" v-else><View class="check-inner" v-if="isSelected(p.processId)" /></View>
      </View>
      <View class="process-info">
        <Text class="process-name">{{ p.processName }}</Text>
        <Text class="process-price" v-if="p.status !== 'DONE'">{{ showPrice ? ('¥' + p.price + '/件') : '- 元/件' }}</Text>
        <Text class="process-done-info" v-if="p.status === 'DONE' && p.doneBy">已完成（{{ p.doneBy }}）</Text>
        <Text class="process-disabled-info" v-if="isBatchDisabled(p) && p.status !== 'DONE'">不支持批量上报</Text>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { ProcessReportStatus } from '../api/types'

const props = defineProps<{ processes: ProcessReportStatus[]; showMobilePrice?: number; scanType?: string }>()
const selectedIds = ref<number[]>([])
const emit = defineEmits<{ (e: 'change', ids: number[]): void }>()

const showPrice = computed(() => props.showMobilePrice !== 0)

// 只有裁剪扫码(CUTTING)才检查 batchReport，单扎扫码(BUNDLE)不检查
function isBatchDisabled(p: ProcessReportStatus): boolean {
  return props.scanType === 'CUTTING' && p.batchReport === 0
}

watch(() => props.processes, () => { selectedIds.value = []; emit('change', []) }, { immediate: true })

function isSelected(id: number) { return selectedIds.value.includes(id) }

function toggleProcess(p: ProcessReportStatus) {
  if (p.status === 'DONE' || isBatchDisabled(p)) return
  const idx = selectedIds.value.indexOf(p.processId)
  if (idx > -1) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(p.processId)
  emit('change', [...selectedIds.value])
}
</script>

<style lang="scss">
.process-list { display: flex; flex-direction: column; gap: 16px; }
.process-item { display: flex; align-items: center; padding: 20px 16px; background: #f9fafb; border-radius: 12px; border: 2px solid transparent; }
.process-item.selected { border-color: #4A6FA5; background: #f0f0ff; }
.process-item.done { opacity: 0.5; background: #f3f4f6; }
.process-item.disabled { opacity: 0.4; background: #f3f4f6; }
.process-check { margin-right: 16px; flex-shrink: 0; }
.check-box { width: 40px; height: 40px; border-radius: 8px; background: #10b981; display: flex; align-items: center; justify-content: center; }
.check-icon { color: #fff; font-size: 24px; }
.check-box.unchecked { background: #fff; border: 2px solid #d1d5db; display: flex; align-items: center; justify-content: center; }
.check-inner { width: 24px; height: 24px; border-radius: 4px; background: #4A6FA5; }
.process-info { flex: 1; }
.process-name { display: block; font-size: 28px; font-weight: 500; color: #4A6FA5; }
.process-price { display: block; font-size: 24px; color: #9ca3af; margin-top: 4px; }
.process-done-info { display: block; font-size: 24px; color: #6b7280; margin-top: 4px; }
.process-disabled-info { display: block; font-size: 22px; color: #ef4444; margin-top: 4px; }
</style>
