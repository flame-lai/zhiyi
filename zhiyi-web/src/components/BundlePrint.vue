<template>
  <a-modal
    v-model:open="visible"
    title="打印菲仔"
    :footer="null"
    width="800px"
    @cancel="handleClose"
  >
    <div class="print-container">
      <!-- 打印选项 -->
      <div class="print-options" v-if="!printing">
        <a-space>
          <a-button type="primary" @click="handlePrintAll">批量打印全部</a-button>
          <a-button @click="handlePrintSelected" :disabled="selectedBundles.length === 0">
            打印选中 ({{ selectedBundles.length }})
          </a-button>
        </a-space>
      </div>

      <!-- 菲仔预览 -->
      <div class="bundle-preview">
        <div
          v-for="bundle in bundles"
          :key="bundle.id"
          class="bundle-item"
          :class="{ selected: selectedBundles.includes(bundle.id) }"
          @click="toggleSelect(bundle.id)"
        >
          <div class="bundle-label">
            <div class="label-header">
              <div class="label-row">
                <span class="label-key">订单号：</span>
                <span class="label-value">{{ bundle.orderNo }}</span>
              </div>
              <div class="label-row">
                <span class="label-key">款号：</span>
                <span class="label-value">{{ bundle.styleCode }}</span>
              </div>
            </div>
            <div class="label-body">
              <div class="label-row">
                <span class="label-key">颜色：</span>
                <span class="label-value">{{ bundle.color }}</span>
              </div>
              <div class="label-row">
                <span class="label-key">尺码：</span>
                <span class="label-value">{{ bundle.size }}</span>
              </div>
              <div class="label-row">
                <span class="label-key">部位：</span>
                <span class="label-value">{{ bundle.part }}</span>
              </div>
              <div class="label-row">
                <span class="label-key">面料：</span>
                <span class="label-value">{{ bundle.fabric }}</span>
              </div>
            </div>
            <div class="label-info">
              <div class="label-row">
                <span class="label-key">扎号：</span>
                <span class="label-value bold">{{ bundle.bundleNo }}</span>
              </div>
              <div class="label-row">
                <span class="label-key">件数：</span>
                <span class="label-value bold">{{ bundle.qty }}件</span>
              </div>
            </div>
            <div class="label-codes">
              <div class="code-item">
                <canvas :ref="el => setBarcodeRef(el, bundle, 'order')"></canvas>
                <div class="code-text">订单码</div>
              </div>
              <div class="code-item">
                <canvas :ref="el => setBarcodeRef(el, bundle, 'bundle')"></canvas>
                <div class="code-text">扎号码</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import JsBarcode from 'jsbarcode'
import QRCode from 'qrcode'

interface BundleData {
  id: number
  orderNo: string
  styleCode: string
  color: string
  size: string
  part: string
  fabric: string
  bundleNo: string
  globalBundleNo: string
  qty: number
}

const props = defineProps<{
  visible: boolean
  bundles: BundleData[]
  orderBarcode: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const visible = ref(props.visible)
const printing = ref(false)
const selectedBundles = ref<number[]>([])

// 存储canvas引用
const barcodeRefs = ref<Map<string, HTMLCanvasElement>>(new Map())

watch(() => props.visible, (val) => {
  visible.value = val
  if (val) {
    selectedBundles.value = props.bundles.map(b => b.id)
    nextTick(() => generateAllBarcodes())
  }
})

watch(visible, (val) => {
  emit('update:visible', val)
})

function setBarcodeRef(el: any, bundle: BundleData, type: 'order' | 'bundle') {
  if (el) {
    const key = `${bundle.id}-${type}`
    barcodeRefs.value.set(key, el as HTMLCanvasElement)
  }
}

function generateAllBarcodes() {
  props.bundles.forEach(bundle => {
    // 生成订单条形码
    const orderCanvas = barcodeRefs.value.get(`${bundle.id}-order`)
    if (orderCanvas) {
      try {
        JsBarcode(orderCanvas, props.orderBarcode, {
          format: 'CODE128',
          width: 1.5,
          height: 30,
          displayValue: false,
          margin: 2,
        })
      } catch (e) {
        console.error('生成订单条形码失败:', e)
      }
    }

    // 生成扎号二维码
    const bundleCanvas = barcodeRefs.value.get(`${bundle.id}-bundle`)
    if (bundleCanvas) {
      try {
        QRCode.toCanvas(bundleCanvas, bundle.globalBundleNo, {
          width: 60,
          margin: 2,
          color: {
            dark: '#000000',
            light: '#ffffff',
          },
        })
      } catch (e) {
        console.error('生成扎号二维码失败:', e)
      }
    }
  })
}

function toggleSelect(id: number) {
  const index = selectedBundles.value.indexOf(id)
  if (index > -1) {
    selectedBundles.value.splice(index, 1)
  } else {
    selectedBundles.value.push(id)
  }
}

function handlePrintAll() {
  printing.value = true
  nextTick(() => {
    window.print()
    setTimeout(() => {
      printing.value = false
    }, 500)
  })
}

function handlePrintSelected() {
  printing.value = true
  nextTick(() => {
    window.print()
    setTimeout(() => {
      printing.value = false
    }, 500)
  })
}

function handleClose() {
  visible.value = false
}
</script>

<style scoped>
.print-container {
  max-height: 600px;
  overflow-y: auto;
}

.print-options {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.bundle-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.bundle-item {
  cursor: pointer;
  border: 2px solid #e8e8e8;
  border-radius: 4px;
  padding: 4px;
  transition: all 0.2s;
}

.bundle-item:hover {
  border-color: #4A6FA5;
}

.bundle-item.selected {
  border-color: #4A6FA5;
  background: rgba(74, 111, 165, 0.05);
}

.bundle-label {
  width: 280px;
  padding: 12px;
  border: 1px solid #333;
  font-size: 12px;
  line-height: 1.5;
}

.label-header {
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ccc;
}

.label-body {
  margin-bottom: 8px;
}

.label-info {
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ccc;
}

.label-row {
  display: flex;
}

.label-key {
  width: 50px;
  color: #666;
}

.label-value {
  flex: 1;
}

.label-value.bold {
  font-weight: bold;
  font-size: 14px;
}

.label-codes {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: 8px;
}

.code-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.code-item canvas {
  max-width: 120px;
}

.code-text {
  font-size: 10px;
  color: #666;
  margin-top: 4px;
}
</style>

<!-- 打印样式不能用scoped -->
<style>
@media print {
  /* 隐藏非打印内容 */
  .ant-modal-mask,
  .ant-modal-wrap .ant-modal-header,
  .ant-modal-wrap .ant-modal-footer,
  .print-options {
    display: none !important;
  }

  /* 设置页面大小 */
  @page {
    size: 70mm 50mm;
    margin: 2mm;
  }

  body {
    margin: 0;
    padding: 0;
  }

  .ant-modal-wrap {
    position: static;
    overflow: visible;
  }

  .ant-modal {
    position: static;
    width: 100%;
    max-width: none;
    margin: 0;
    padding: 0;
  }

  .ant-modal-content {
    box-shadow: none;
    border: none;
  }

  .ant-modal-body {
    padding: 0;
  }

  .print-container {
    max-height: none;
    overflow: visible;
  }

  .bundle-preview {
    display: block;
  }

  .bundle-item {
    border: none !important;
    padding: 0 !important;
    margin: 0 !important;
    page-break-after: always;
    break-after: page;
    width: 70mm;
    height: 50mm;
    box-sizing: border-box;
  }

  .bundle-item:last-child {
    page-break-after: auto;
    break-after: auto;
  }

  .bundle-item.selected {
    border: none !important;
    background: none !important;
  }

  .bundle-label {
    width: 100%;
    height: 100%;
    border: 0.5pt solid #000 !important;
    margin: 0;
    padding: 2mm;
    font-size: 8pt;
    line-height: 1.3;
    box-sizing: border-box;
  }

  .label-header {
    border-bottom: 0.5pt solid #000 !important;
    margin-bottom: 1mm;
    padding-bottom: 1mm;
  }

  .label-info {
    border-top: 0.5pt dashed #000 !important;
    border-bottom: 0.5pt dashed #000 !important;
    margin-top: 1mm;
    padding-top: 1mm;
    padding-bottom: 1mm;
  }

  .label-row {
    display: flex;
  }

  .label-key {
    width: 40px;
    color: #000;
  }

  .label-value {
    flex: 1;
  }

  .label-value.bold {
    font-weight: bold;
    font-size: 10pt;
  }

  .label-codes {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-top: 1mm;
  }

  .code-item {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .code-item canvas {
    max-width: 25mm;
    max-height: 8mm;
  }

  .code-text {
    font-size: 6pt;
    color: #000;
    margin-top: 0.5mm;
  }
}
</style>
