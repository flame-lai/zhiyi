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
            <!-- 顶部条形码（裁床码） -->
            <div class="label-barcode-top">
              <svg :id="'barcode-cutting-' + bundle.id"></svg>
              <div class="code-text">裁床码</div>
            </div>

            <div class="label-header">
              <div class="label-row">
                <span class="label-key">订单号：</span>
                <span class="label-value">{{ bundle.orderNo }}</span>
              </div>
              <div class="label-row">
                <span class="label-key">款号：</span>
                <span class="label-value">{{ bundle.styleCode }}</span>
              </div>
              <div class="label-row">
                <span class="label-key">面料：</span>
                <span class="label-value bold">{{ bundle.fabric }}</span>
              </div>
            </div>
            <div class="label-body">
              <div class="label-row-2col">
                <div class="label-col">
                  <span class="label-key">颜色：</span>
                  <span class="label-value">{{ bundle.color }}</span>
                </div>
                <div class="label-col">
                  <span class="label-key">尺码：</span>
                  <span class="label-value">{{ bundle.size }}</span>
                </div>
              </div>
              <div class="label-row-2col">
                <div class="label-col">
                  <span class="label-key">部位：</span>
                  <span class="label-value">{{ bundle.part }}</span>
                </div>
                <div class="label-col">
                  <span class="label-key">件数：</span>
                  <span class="label-value bold">{{ bundle.qty }}件</span>
                </div>
              </div>
            </div>
            <div class="label-info">
              <span class="bundle-no">{{ bundle.bundleNo }}</span>
            </div>
            <!-- 底部二维码（扎号码） -->
            <div class="label-qrcode-bottom">
              <img v-if="qrCodeUrls[bundle.id]" :src="qrCodeUrls[bundle.id]" class="qr-code-img" />
              <svg v-else :id="'barcode-bundle-' + bundle.id"></svg>
              <div class="code-text">扎号码</div>
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
  cuttingPieces?: number
  bedNo?: number
}

const props = defineProps<{
  visible: boolean
  bundles: BundleData[]
  cuttingBarcode: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const visible = ref(props.visible)
const selectedBundles = ref<number[]>([])
const qrCodeUrls = ref<Record<number, string>>({})

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

async function generateAllBarcodes() {
  // 生成二维码data URL
  const urls: Record<number, string> = {}
  for (const bundle of props.bundles) {
    try {
      const dataUrl = await QRCode.toDataURL(bundle.globalBundleNo || 'UNKNOWN', {
        width: 120,
        margin: 1,
        errorCorrectionLevel: 'M'
      })
      urls[bundle.id] = dataUrl
    } catch (e) {
      console.error('生成二维码失败:', e)
    }
  }
  qrCodeUrls.value = urls

  // 生成条形码
  nextTick(() => {
    props.bundles.forEach(bundle => {
      const barcodeEl = document.getElementById(`barcode-cutting-${bundle.id}`)
      if (barcodeEl) {
        try {
          JsBarcode(barcodeEl, props.cuttingBarcode, {
            format: 'CODE128',
            width: 1.5,
            height: 35,
            displayValue: false,
            margin: 2,
          })
        } catch (e) {
          console.error('生成裁床条形码失败:', e)
        }
      }
    })
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
  printBundles(props.bundles)
}

function handlePrintSelected() {
  const selected = props.bundles.filter(b => selectedBundles.value.includes(b.id))
  printBundles(selected)
}

async function printBundles(bundlesToPrint: BundleData[]) {
  // 先生成所有二维码的data URL（高分辨率）
  const qrCodeUrls: Record<number, string> = {}
  for (const bundle of bundlesToPrint) {
    try {
      const dataUrl = await QRCode.toDataURL(bundle.globalBundleNo || 'UNKNOWN', {
        width: 200,
        margin: 1,
        errorCorrectionLevel: 'M'
      })
      qrCodeUrls[bundle.id] = dataUrl
    } catch (e) {
      console.error('生成二维码失败:', e)
    }
  }

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
          size: 50mm 70mm;
          margin: 0;
        }
        * {
          margin: 0;
          padding: 0;
          box-sizing: border-box;
        }
        html, body {
          width: 50mm;
          font-family: sans-serif;
          font-size: 10px;
          line-height: 1.35;
        }
        .label-page {
          width: 50mm;
          height: 70mm;
          padding: 1.5mm;
          border: 1px solid #333;
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
          border-bottom: 1px dashed #ccc;
          padding-bottom: 0.8mm;
          margin-bottom: 0.8mm;
          flex-shrink: 0;
        }
        .label-row {
          display: flex;
          line-height: 1.35;
        }
        .label-row-2col {
          display: flex;
          line-height: 1.35;
        }
        .label-row-2col .label-col {
          flex: 1;
          display: flex;
        }
        .label-key {
          width: 50px;
          color: #666;
          flex-shrink: 0;
          margin-right: 1px;
        }
        .label-value {
          flex: 1;
        }
        .label-value.bold {
          font-weight: bold;
        }
        .fabric-bold {
          font-weight: bold;
        }
        .label-body {
          flex: 1;
          margin-bottom: 0.8mm;
        }
        .label-info {
          text-align: center;
          font-size: 15px;
          font-weight: bold;
          padding: 1mm 0;
          margin-bottom: 1mm;
          border-top: 1px dashed #ccc;
          border-bottom: 1px dashed #ccc;
          flex-shrink: 0;
        }
        .label-barcode-top {
          display: flex;
          flex-direction: column;
          align-items: center;
          border-bottom: 1px dashed #ccc;
          padding-bottom: 0.8mm;
          margin-bottom: 0.8mm;
          flex-shrink: 0;
        }
        .label-barcode-top svg {
          width: 35mm;
          height: 9mm;
        }
        .label-qrcode-bottom {
          display: flex;
          flex-direction: column;
          align-items: center;
          margin-top: auto;
          flex-shrink: 0;
        }
        .label-qrcode-bottom img {
          width: 15mm;
          height: 15mm;
        }
        .code-text {
          font-size: 9px;
          color: #666;
          margin-top: 0.3mm;
        }
      </style>
    </head>
    <body>
  `

  bundlesToPrint.forEach((bundle) => {
    html += `
      <div class="label-page">
        <div class="label-barcode-top">
          <svg class="print-barcode" data-value="${props.cuttingBarcode}"></svg>
          <div class="code-text">裁床码</div>
        </div>
        <div class="label-header">
          <div class="label-row">
            <span class="label-key">订单号：</span><span class="label-value">${bundle.orderNo}</span>
          </div>
          <div class="label-row">
            <span class="label-key">款号：</span><span class="label-value">${bundle.styleCode}</span>
          </div>
          <div class="label-row">
            <span class="label-key">面料：</span><span class="label-value fabric-bold">${bundle.fabric || ''}</span>
          </div>
        </div>
        <div class="label-body">
          <div class="label-row-2col">
            <div class="label-col"><span class="label-key">颜色：</span><span class="label-value">${bundle.color}</span></div>
            <div class="label-col"><span class="label-key">尺码：</span><span class="label-value">${bundle.size}</span></div>
          </div>
          <div class="label-row-2col">
            <div class="label-col"><span class="label-key">部位：</span><span class="label-value">${bundle.part}</span></div>
            <div class="label-col"><span class="label-key">件数：</span><span class="label-value bold">${bundle.qty}件</span></div>
          </div>
        </div>
        <div class="label-info">
          ${bundle.bundleNo}
        </div>
        <div class="label-qrcode-bottom">
          <img src="${qrCodeUrls[bundle.id] || ''}" />
          <div class="code-text">扎号码</div>
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
            height: 30,
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
  width: 200px;
  padding: 8px;
  border: 1px solid #333;
  font-size: 11px;
  line-height: 1.4;
}

.label-header {
  margin-bottom: 6px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #ccc;
}

.label-body {
  margin-bottom: 6px;
}

.label-row {
  display: flex;
}

.label-row-2col {
  display: flex;
}

.label-row-2col .label-col {
  flex: 1;
  display: flex;
}

.label-key {
  width: 50px;
  color: #666;
  flex-shrink: 0;
}

.label-value {
  flex: 1;
}

.label-value.bold {
  font-weight: bold;
}

.fabric-bold {
  font-weight: bold;
}

.label-info {
  text-align: center;
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 6px;
  padding: 4px 0;
  border-top: 1px dashed #ccc;
  border-bottom: 1px dashed #ccc;
}

.label-barcode-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 6px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #ccc;
}

.label-barcode-top svg {
  max-width: 150px;
  height: 35px;
}

.label-qrcode-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 4px;
}

.label-qrcode-bottom img {
  width: 50px;
  height: 50px;
}

.code-text {
  font-size: 10px;
  color: #666;
  margin-top: 2px;
}
</style>
