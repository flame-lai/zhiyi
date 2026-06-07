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
      <div class="print-options">
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
          <div class="bundle-label" :id="'label-' + bundle.id">
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
                <svg :id="'barcode-order-' + bundle.id"></svg>
                <div class="code-text">订单码</div>
              </div>
              <div class="code-item">
                <svg :id="'barcode-bundle-' + bundle.id"></svg>
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
const selectedBundles = ref<number[]>([])

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

function generateAllBarcodes() {
  props.bundles.forEach(bundle => {
    // 生成订单条形码
    const barcodeEl = document.getElementById(`barcode-order-${bundle.id}`)
    if (barcodeEl) {
      try {
        JsBarcode(barcodeEl, props.orderBarcode, {
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

    // 生成扎号二维码 - 用canvas转图片
    generateQRCode(bundle)
  })
}

async function generateQRCode(bundle: BundleData) {
  try {
    const canvas = document.createElement('canvas')
    await QRCode.toCanvas(canvas, bundle.globalBundleNo, {
      width: 60,
      margin: 2,
    })
    // 转成图片
    const img = document.createElement('img')
    img.src = canvas.toDataURL('image/png')
    img.style.width = '60px'
    img.style.height = '60px'
    const container = document.getElementById(`barcode-bundle-${bundle.id}`)
    if (container) {
      container.innerHTML = ''
      container.appendChild(img)
    }
  } catch (e) {
    console.error('生成扎号二维码失败:', e)
  }
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
  printBundles(props.bundles)
}

function handlePrintSelected() {
  const selected = props.bundles.filter(b => selectedBundles.value.includes(b.id))
  printBundles(selected)
}

async function printBundles(bundlesToPrint: BundleData[]) {
  // 先生成所有二维码的data URL
  const qrCodeUrls: Record<number, string> = {}
  for (const bundle of bundlesToPrint) {
    try {
      const dataUrl = await QRCode.toDataURL(bundle.globalBundleNo || 'UNKNOWN', {
        width: 80,
        margin: 1,
        errorCorrectionLevel: 'M'
      })
      qrCodeUrls[bundle.id] = dataUrl
      console.log('QR generated for', bundle.bundleNo, dataUrl.substring(0, 30))
    } catch (e) {
      console.error('生成二维码失败:', e, bundle.globalBundleNo)
    }
  }

  console.log('QR codes generated:', Object.keys(qrCodeUrls).length)

  const printWindow = window.open('', '_blank')
  if (!printWindow) return

  let html = `
    <!DOCTYPE html>
    <html>
    <head>
      <title>打印菲仔</title>
      <script src="https://cdn.jsdelivr.net/npm/jsbarcode@3.11.6/dist/JsBarcode.all.min.js"><\/script>
      <style>
        @page {
          size: 70mm 50mm;
          margin: 0;
        }
        * {
          margin: 0;
          padding: 0;
          box-sizing: border-box;
        }
        html, body {
          width: 70mm;
          font-family: sans-serif;
          font-size: 9px;
          line-height: 1.2;
        }
        .label-page {
          width: 70mm;
          height: 50mm;
          padding: 2mm;
          border: 0.5pt solid #000;
          page-break-after: always;
          page-break-inside: avoid;
          overflow: hidden;
          display: flex;
          flex-direction: column;
        }
        .label-page:last-child {
          page-break-after: auto;
        }
        .label-header {
          border-bottom: 0.5pt solid #000;
          padding-bottom: 1mm;
          margin-bottom: 1mm;
          flex-shrink: 0;
        }
        .label-row {
          display: flex;
          line-height: 1.3;
        }
        .label-key {
          width: 30px;
          color: #333;
          flex-shrink: 0;
        }
        .label-value {
          flex: 1;
        }
        .label-body {
          flex: 1;
          margin-bottom: 1mm;
          overflow: hidden;
        }
        .label-info {
          border-top: 0.5pt dashed #000;
          border-bottom: 0.5pt dashed #000;
          padding: 1mm 0;
          margin-bottom: 1mm;
          flex-shrink: 0;
        }
        .bundle-no { font-size: 12px; font-weight: bold; }
        .qty { font-size: 11px; font-weight: bold; }
        .label-codes {
          display: flex;
          justify-content: space-between;
          align-items: flex-end;
          flex-shrink: 0;
        }
        .code-item {
          text-align: center;
        }
        .code-item svg {
          width: 25mm;
          height: 8mm;
        }
        .code-item img {
          width: 12mm;
          height: 12mm;
        }
        .code-text {
          font-size: 6px;
          color: #666;
          margin-top: 0.5mm;
        }
      </style>
    </head>
    <body>
  `

  bundlesToPrint.forEach((bundle, index) => {
    html += `
      <div class="label-page">
        <div class="label-header">
          <div class="label-row">
            <span class="label-key">订单号：</span>
            <span class="label-value">${bundle.orderNo}</span>
          </div>
          <div class="label-row">
            <span class="label-key">款号：</span>
            <span class="label-value">${bundle.styleCode}</span>
          </div>
        </div>
        <div class="label-body">
          <div class="label-row">
            <span class="label-key">颜色：</span>
            <span class="label-value">${bundle.color}</span>
          </div>
          <div class="label-row">
            <span class="label-key">尺码：</span>
            <span class="label-value">${bundle.size}</span>
          </div>
          <div class="label-row">
            <span class="label-key">部位：</span>
            <span class="label-value">${bundle.part}</span>
          </div>
          <div class="label-row">
            <span class="label-key">面料：</span>
            <span class="label-value">${bundle.fabric}</span>
          </div>
        </div>
        <div class="label-info">
          <div class="label-row">
            <span class="label-key">扎号：</span>
            <span class="label-value bundle-no">${bundle.bundleNo}</span>
          </div>
          <div class="label-row">
            <span class="label-key">件数：</span>
            <span class="label-value qty">${bundle.qty}件</span>
          </div>
        </div>
        <div class="label-codes">
          <div class="code-item">
            <svg class="print-barcode" data-value="${props.orderBarcode}"></svg>
            <div class="code-text">订单码</div>
          </div>
          <div class="code-item">
            <img src="${qrCodeUrls[bundle.id] || ''}" />
            <div class="code-text">扎号码</div>
          </div>
        </div>
      </div>
    `
  })

  html += `
      <script>
        document.querySelectorAll('.print-barcode').forEach(function(el) {
          JsBarcode(el, el.getAttribute('data-value'), {
            format: 'CODE128',
            width: 1.5,
            height: 25,
            displayValue: false,
            margin: 1
          });
        });
        setTimeout(function() { window.print(); window.close(); }, 500);
      <\/script>
    </body>
    </html>
  `

  printWindow.document.write(html)
  printWindow.document.close()
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding: 4px 0;
  border-top: 1px dashed #ccc;
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

.code-item svg,
.code-item img {
  max-width: 120px;
}

.code-text {
  font-size: 10px;
  color: #666;
  margin-top: 4px;
}
</style>
