<template>
  <View class="page">
    <View class="section">
      <View class="card">
        <View class="card-header">
          <Text class="card-title">计时工资录入</Text>
        </View>

        <View class="form">
          <View class="form-item">
            <Text class="form-label">员工ID</Text>
            <Input class="form-input" type="number" placeholder="请输入员工ID" :value="form.workerId" @input="e => form.workerId = e.detail.value" />
          </View>

          <View class="form-item">
            <Text class="form-label">工时(小时)</Text>
            <Input class="form-input" type="digit" placeholder="请输入工时" :value="form.hours" @input="e => form.hours = e.detail.value" />
          </View>

          <View class="form-item">
            <Text class="form-label">时薪(元)</Text>
            <Input class="form-input" type="digit" placeholder="请输入时薪" :value="form.hourlyWage" @input="e => form.hourlyWage = e.detail.value" />
          </View>

          <View class="calc-section">
            <View class="calc-row">
              <Text class="calc-label">金额</Text>
              <Text class="calc-value">¥{{ amount }}</Text>
            </View>
          </View>

          <Button class="submit-btn" :disabled="submitting" @tap="onSubmit">
            {{ submitting ? '提交中...' : '提交' }}
          </Button>
        </View>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { createTimedWage } from '../../../api/admin'

definePageConfig({
  navigationBarTitleText: '计时工资',
})

const form = ref({
  workerId: '',
  hours: '',
  hourlyWage: '',
})

const submitting = ref(false)

const amount = computed(() => {
  const h = parseFloat(form.value.hours) || 0
  const w = parseFloat(form.value.hourlyWage) || 0
  return (h * w).toFixed(2)
})

async function onSubmit() {
  if (!form.value.workerId) {
    Taro.showToast({ title: '请输入员工ID', icon: 'none' })
    return
  }
  if (!form.value.hours || parseFloat(form.value.hours) <= 0) {
    Taro.showToast({ title: '请输入有效工时', icon: 'none' })
    return
  }
  if (!form.value.hourlyWage || parseFloat(form.value.hourlyWage) <= 0) {
    Taro.showToast({ title: '请输入有效时薪', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await createTimedWage({
      workerId: parseInt(form.value.workerId),
      hours: parseFloat(form.value.hours),
      hourlyWage: parseFloat(form.value.hourlyWage),
    })
    Taro.showToast({ title: '录入成功', icon: 'success' })
    form.value = { workerId: '', hours: '', hourlyWage: '' }
  } catch {
    // error handled by request util
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: #F5F7FA;
}

.section {
  padding: 24px;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.card-header {
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.card-title {
  font-size: 32px;
  font-weight: 600;
  color: #333;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-label {
  font-size: 26px;
  color: #666;
  font-weight: 500;
}

.form-input {
  height: 80px;
  padding: 0 20px;
  background: #F5F7FA;
  border-radius: 8px;
  font-size: 28px;
  color: #333;
}

.calc-section {
  background: #F5F7FA;
  border-radius: 8px;
  padding: 24px;
  margin-top: 8px;
}

.calc-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.calc-label {
  font-size: 28px;
  color: #666;
  font-weight: 500;
}

.calc-value {
  font-size: 36px;
  color: #4A6FA5;
  font-weight: 700;
}

.submit-btn {
  margin-top: 16px;
  height: 88px;
  line-height: 88px;
  background: #4A6FA5;
  color: #fff;
  font-size: 30px;
  font-weight: 600;
  border-radius: 8px;
  border: none;
}

.submit-btn::after {
  border: none;
}

.submit-btn[disabled] {
  background: #a0b8d4;
  color: #fff;
}
</style>
