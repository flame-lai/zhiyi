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
              <span class="order-no">{{ bundle.orderNo }}</span>
              <span class="style-code">{{ bundle.styleCode }}</span>
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
              <div class="bundle-no">{{ bundle.bundleNo }}</div>
              <div class="qty">{{ bundle.qty }}件</div>
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

/* 标签样式 - 40mm x 60mm */
.bundle-label {
  width: 151px;  /* 40mm ≈ 151px at 96dpi */
  height: 226px; /* 60mm ≈ 226px at 96dpi */
  padding: 6px;
  border: 1px solid #333;
  font-size: 10px;
  line-height: 1.3;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.label-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid #333;
}

.order-no {
  font-weight: bold;
  font-size: 12px;
}

.style-code {
  font-weight: bold;
  font-size: 12px;
}

.label-body {
  flex: 1;
  margin-bottom: 4px;
}

.label-row {
  display: flex;
}

.label-key {
  width: 40px;
  color: #666;
  flex-shrink: 0;
}

.label-value {
  flex: 1;
}

.label-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  padding: 4px 0;
  border-top: 1px dashed #ccc;
  border-bottom: 1px dashed #ccc;
}

.bundle-no {
  font-size: 16px;
  font-weight: bold;
}

.qty {
  font-size: 14px;
  font-weight: bold;
}

.label-codes {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.code-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.code-item canvas {
  max-width: 100px;
  max-height: 35px;
}

.code-text {
  font-size: 9px;
  color: #666;
  margin-top: 2px;
}

/* 打印样式 */
@media print {
  /* 设置页面大小为标签大小 */
  @page {
    size: 40mm 60mm;
    margin: 0;
  }

  * {
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  body {
    margin: 0;
    padding: 0;
  }

  .print-options {
    display: none !important;
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
    width: 40mm;
    height: 60mm;
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
    width: 40mm;
    height: 60mm;
    border: 1px solid #000 !important;
    margin: 0;
    padding: 2mm;
    font-size: 8pt;
  }

  .label-header {
    border-bottom: 1px solid #000 !important;
  }

  .label-info {
    border-top: 1px dashed #000 !important;
    border-bottom: 1px dashed #000 !important;
  }
}
</style>
