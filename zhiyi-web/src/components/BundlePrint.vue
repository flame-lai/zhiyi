<template>
  <a-modal
    v-model:open="visible"
    title="打印菲仔"
    :footer="null"
    width="800px"
    @cancel="handleClose"
  >
    <div class="print-container">
      <div class="print-options">
        <a-space>
          <a-button type="primary" @click="handlePrintAll">批量打印全部</a-button>
          <a-button @click="handlePrintSelected" :disabled="selectedBundles.length === 0">
            打印选中 ({{ selectedBundles.length }})
          </a-button>
        </a-space>
      </div>

      <div class="bundle-preview">
        <div
          v-for="bundle in bundles"
          :key="bundle.id"
          class="bundle-item"
          :class="{ selected: selectedBundles.includes(bundle.id) }"
          @click="toggleSelect(bundle.id)"
        >
          <div class="bundle-label">
            <!-- 顶部穿孔区 -->
            <div class="preview-hole-zone"><div class="preview-hole"></div></div>
            <!-- 顶部条形码 -->
            <div class="preview-barcode">
              <svg :id="'barcode-cutting-' + bundle.id"></svg>
              <div class="preview-barcode-text">{{ cuttingBarcode }}</div>
            </div>

            <!-- 中间信息表格 -->
            <table class="preview-tbl">
              <tr>
                <td class="pv-key">部位</td>
                <td class="pv-val pv-val-bold">{{ bundle.part }}</td>
              </tr>
              <tr>
                <td class="pv-key">订单号</td>
                <td class="pv-val">{{ bundle.orderNo }}</td>
              </tr>
              <tr>
                <td class="pv-key">款号</td>
                <td class="pv-val">{{ bundle.styleCode }}</td>
              </tr>
              <tr>
                <td class="pv-key">颜色</td>
                <td class="pv-val">{{ bundle.color }}</td>
              </tr>
            </table>

            <!-- 底部区域：明细表格 -->
            <div class="preview-bottom">
              <table class="preview-tbl">
                <tr>
                  <td class="pv-key">尺码</td>
                  <td class="pv-val">{{ bundle.size }}</td>
                </tr>
                <tr>
                  <td class="pv-key">扎号</td>
                  <td class="pv-val">{{ bundle.bundleNo }}</td>
                </tr>
                <tr>
                  <td class="pv-key">数量</td>
                  <td class="pv-val">{{ bundle.qty }}</td>
                </tr>
                <tr>
                  <td class="pv-key">床号</td>
                  <td class="pv-val">{{ bundle.bedNo || '' }}</td>
                </tr>
              </table>
            </div>
            <!-- 二维码：绝对定位在标签右下角 -->
            <img v-if="qrCodeUrls[bundle.id]" :src="qrCodeUrls[bundle.id]" class="preview-qr" />
            <!-- 底部扎号码 -->
            <div class="preview-bundle-no">{{ bundle.globalBundleNo }}</div>
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
          size: 50mm 80mm;
          margin: 0;
        }
        * {
          margin: 0;
          padding: 0;
          box-sizing: border-box;
        }
        html, body {
          width: 50mm;
          font-family: "SimSun", "宋体", sans-serif;
          font-size: 10px;
          line-height: 1.3;
        }
        .label-page {
          width: 50mm;
          height: 80mm;
          padding: 2mm;
          border: 1px solid #333;
          page-break-after: always;
          page-break-inside: avoid;
          overflow: hidden;
          display: flex;
          flex-direction: column;
          position: relative;
        }
        .label-page:last-child {
          page-break-after: auto;
        }
        /* 顶部穿孔区 */
        .p-hole-zone {
          height: 18mm;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          border-bottom: 1px dashed #ccc;
          margin-bottom: 1mm;
        }
        .p-hole {
          width: 5mm;
          height: 5mm;
          border: 1px solid #999;
          border-radius: 50%;
        }
        /* 顶部条形码 */
        .p-barcode {
          display: flex;
          flex-direction: column;
          align-items: center;
          margin-bottom: 1.5mm;
          flex-shrink: 0;
        }
        .p-barcode svg {
          width: 42mm;
          height: 8mm;
        }
        .p-barcode-text {
          font-size: 10px;
          font-weight: bold;
          margin-top: 0.5mm;
          letter-spacing: 0.5px;
        }
        /* 统一表格 */
        .p-tbl {
          width: 100%;
          border-collapse: collapse;
          flex-shrink: 0;
        }
        .p-tbl td {
          border: 1px solid #333;
          padding: 0.5mm 1mm;
          font-size: 9px;
          line-height: 1.3;
        }
        .p-key {
          width: 22%;
          font-weight: bold;
          white-space: nowrap;
          background-color: #f5f5f5;
        }
        .p-val {
          width: auto;
        }
        .p-val-bold {
          font-weight: bold;
        }
        /* 底部区域 */
        .p-bottom {
          margin-top: auto;
          position: relative;
          flex-shrink: 0;
          overflow: hidden;
        }
        .p-bottom .p-tbl {
          width: 50%;
        }
        /* 二维码绝对定位在标签右下角 */
        .p-qr {
          position: absolute;
          right: 2mm;
          bottom: 5mm;
          width: 18mm;
          height: 18mm;
        }
        .p-bundle-no {
          text-align: center;
          font-size: 9px;
          font-weight: bold;
          padding-top: 1mm;
          flex-shrink: 0;
        }
      </style>
    </head>
    <body>
  `

  bundlesToPrint.forEach((bundle) => {
    html += `
      <div class="label-page">
        <div class="p-hole-zone"><div class="p-hole"></div></div>
        <div class="p-barcode">
          <svg class="print-barcode" data-value="${props.cuttingBarcode}"></svg>
          <div class="p-barcode-text">${props.cuttingBarcode}</div>
        </div>
        <table class="p-tbl">
          <tr>
            <td class="p-key">部位</td>
            <td class="p-val p-val-bold">${bundle.part}</td>
          </tr>
          <tr>
            <td class="p-key">订单号</td>
            <td class="p-val">${bundle.orderNo}</td>
          </tr>
          <tr>
            <td class="p-key">款号</td>
            <td class="p-val">${bundle.styleCode}</td>
          </tr>
          <tr>
            <td class="p-key">颜色</td>
            <td class="p-val">${bundle.color}</td>
          </tr>
        </table>
        <div class="p-bottom">
          <table class="p-tbl">
            <tr>
              <td class="p-key">尺码</td>
              <td class="p-val">${bundle.size}</td>
            </tr>
            <tr>
              <td class="p-key">扎号</td>
              <td class="p-val">${bundle.bundleNo}</td>
            </tr>
            <tr>
              <td class="p-key">数量</td>
              <td class="p-val">${bundle.qty}</td>
            </tr>
            <tr>
              <td class="p-key">床号</td>
              <td class="p-val">${bundle.bedNo || ''}</td>
            </tr>
          </table>
        </div>
        <img class="p-qr" src="${qrCodeUrls[bundle.id] || ''}" />
        <div class="p-bundle-no">${bundle.globalBundleNo}</div>
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

/* 预览标签：50:80 比例 */
.bundle-label {
  width: 200px;
  height: 320px;
  padding: 8px;
  border: 1px solid #333;
  font-size: 10px;
  line-height: 1.35;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

/* 穿孔区 */
.preview-hole-zone {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-bottom: 1px dashed #ccc;
  margin-bottom: 6px;
}

.preview-hole {
  width: 20px;
  height: 20px;
  border: 1px solid #999;
  border-radius: 50%;
}

/* 顶部条形码 */
.preview-barcode {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 6px;
  flex-shrink: 0;
}

.preview-barcode svg {
  width: 150px;
  height: 32px;
}

.preview-barcode-text {
  font-size: 11px;
  font-weight: bold;
  margin-top: 2px;
  letter-spacing: 0.5px;
}

/* 统一表格 */
.preview-tbl {
  width: 100%;
  border-collapse: collapse;
  flex-shrink: 0;
}

.preview-tbl td {
  border: 1px solid #333;
  padding: 3px 6px;
  font-size: 10px;
  line-height: 1.35;
}

.pv-key {
  width: 22%;
  font-weight: bold;
  white-space: nowrap;
  background-color: #f5f5f5;
}

.pv-val {
  width: auto;
}

.pv-val-bold {
  font-weight: bold;
}

/* 底部区域 */
.preview-bottom {
  margin-top: auto;
  flex-shrink: 0;
}

.preview-bottom .preview-tbl {
  width: 50%;
}

/* 二维码绝对定位在标签右下角 */
.preview-qr {
  position: absolute;
  right: 8px;
  bottom: 22px;
  width: 68px;
  height: 68px;
}

.preview-bundle-no {
  text-align: center;
  font-size: 10px;
  font-weight: bold;
  flex-shrink: 0;
}
</style>
