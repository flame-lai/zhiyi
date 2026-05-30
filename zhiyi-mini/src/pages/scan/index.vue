<template>
  <View class="scan-page">
    <View class="scan-actions">
      <View class="scan-card" @tap="scanBundle">
        <Text class="scan-card-title">扫描扎号QR码</Text>
        <Text class="scan-card-desc">扫描扎号上的二维码进行报工</Text>
      </View>

      <View class="scan-card" @tap="scanCutting">
        <Text class="scan-card-title">扫描裁床条形码</Text>
        <Text class="scan-card-desc">扫描裁床条形码进行报工</Text>
      </View>
    </View>

    <View class="manual-section">
      <Text class="section-title">手动输入</Text>
      <View class="manual-form">
        <Input
          class="manual-input"
          type="text"
          placeholder="输入扎号或条形码"
          :value="manualCode"
          @input="onManualInput"
        />
        <View class="manual-type-row">
          <View
            class="type-tag"
            :class="{ active: manualType === 'bundle' }"
            @tap="manualType = 'bundle'"
          >
            <Text class="type-tag-text">扎号</Text>
          </View>
          <View
            class="type-tag"
            :class="{ active: manualType === 'cutting' }"
            @tap="manualType = 'cutting'"
          >
            <Text class="type-tag-text">裁床码</Text>
          </View>
        </View>
        <Button class="submit-btn" :loading="submitting" @tap="submitManual">
          提交报工
        </Button>
      </View>
    </View>

    <View class="result-card" v-if="resultMsg">
      <Text class="result-title">报工结果</Text>
      <Text class="result-text">{{ resultMsg }}</Text>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro from '@tarojs/taro'
import { reportByBundle, reportByCutting } from '../../api/report'

definePageConfig({
  navigationBarTitleText: '扫码报工',
})

const manualCode = ref('')
const manualType = ref<'bundle' | 'cutting'>('bundle')
const submitting = ref(false)
const resultMsg = ref('')

function onManualInput(e) {
  manualCode.value = e.detail.value
}

async function scanBundle() {
  try {
    const res = await Taro.scanCode({ scanType: ['qrCode'] })
    const code = res.result
    if (!code) return
    submitting.value = true
    const bundleId = parseInt(code, 10)
    if (isNaN(bundleId)) {
      Taro.showToast({ title: '无效的扎号', icon: 'none' })
      return
    }
    const apiRes = await reportByBundle({ bundleId, processIds: [] })
    resultMsg.value = apiRes.message || '扎号报工成功'
    Taro.showToast({ title: '报工成功', icon: 'success' })
  } catch (err) {
    // handled by request util
  } finally {
    submitting.value = false
  }
}

async function scanCutting() {
  try {
    const res = await Taro.scanCode({ scanType: ['barCode'] })
    const code = res.result
    if (!code) return
    submitting.value = true
    const apiRes = await reportByCutting({ barcode: code })
    resultMsg.value = apiRes.message || '裁床报工成功'
    Taro.showToast({ title: '报工成功', icon: 'success' })
  } catch (err) {
    // handled by request util
  } finally {
    submitting.value = false
  }
}

async function submitManual() {
  const code = manualCode.value.trim()
  if (!code) {
    Taro.showToast({ title: '请输入编码', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    if (manualType.value === 'bundle') {
      const bundleId = parseInt(code, 10)
      if (isNaN(bundleId)) {
        Taro.showToast({ title: '请输入有效的扎号', icon: 'none' })
        return
      }
      const res = await reportByBundle({ bundleId, processIds: [] })
      resultMsg.value = res.message || '扎号报工成功'
    } else {
      const res = await reportByCutting({ barcode: code })
      resultMsg.value = res.message || '裁床报工成功'
    }
    Taro.showToast({ title: '报工成功', icon: 'success' })
    manualCode.value = ''
  } catch (err) {
    // handled by request util
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss">
.scan-page {
  min-height: 100vh;
  background: #F5F7FA;
  padding: 24px;
  box-sizing: border-box;
}

.scan-actions {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 32px;
}

.scan-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 32px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.scan-card-title {
  display: block;
  font-size: 32px;
  font-weight: bold;
  color: #333333;
  margin-bottom: 8px;
}

.scan-card-desc {
  display: block;
  font-size: 24px;
  color: #999999;
}

.manual-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}

.section-title {
  display: block;
  font-size: 28px;
  font-weight: bold;
  color: #333333;
  margin-bottom: 16px;
}

.manual-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.manual-input {
  width: 100%;
  height: 80px;
  border: 1px solid #E0E0E0;
  border-radius: 8px;
  padding: 0 24px;
  font-size: 28px;
  box-sizing: border-box;
}

.manual-type-row {
  display: flex;
  gap: 16px;
}

.type-tag {
  padding: 12px 32px;
  border-radius: 8px;
  border: 1px solid #E0E0E0;
  background: #F5F7FA;
}

.type-tag.active {
  border-color: #4A6FA5;
  background: rgba(74, 111, 165, 0.08);
}

.type-tag-text {
  font-size: 26px;
  color: #666666;
}

.type-tag.active .type-tag-text {
  color: #4A6FA5;
}

.submit-btn {
  width: 100%;
  height: 80px;
  line-height: 80px;
  background: #4A6FA5;
  color: #ffffff;
  font-size: 30px;
  border-radius: 8px;
  border: none;
}

.result-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  border-left: 4px solid #4CAF50;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.result-title {
  display: block;
  font-size: 28px;
  font-weight: bold;
  color: #333333;
  margin-bottom: 8px;
}

.result-text {
  display: block;
  font-size: 26px;
  color: #666666;
}
</style>
