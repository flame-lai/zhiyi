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
          <a-button type="primary" :loading="tscPrinting" @click="handleTscPrintAll">
            TSC打印全部
          </a-button>
          <a-button
            :loading="tscPrinting"
            @click="handleTscPrintSelected"
            :disabled="selectedBundles.length === 0"
          >
            TSC打印选中 ({{ selectedBundles.length }})
          </a-button>
          <a-button @click="handlePrintSelected" :disabled="selectedBundles.length === 0">
            浏览器打印选中
          </a-button>
          <a-button @click="handlePrintAll">浏览器打印全部</a-button>
          <a-button @click="openSettings">打印设置</a-button>
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
                  <td class="pv-val preview-size-value">{{ bundle.size }}</td>
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

  <a-modal
    v-model:open="settingsVisible"
    title="TSC 打印设置"
    ok-text="保存"
    cancel-text="取消"
    @ok="saveSettings"
  >
    <a-form layout="vertical">
      <a-form-item label="PrintServer WebSocket 地址">
        <a-input
          v-model:value="settingsForm.endpoint"
          placeholder="ws://127.0.0.1:8888"
        />
      </a-form-item>
      <a-form-item label="连接方式">
        <a-select v-model:value="settingsForm.connectionMode">
          <a-select-option value="usb">USB 自动连接</a-select-option>
          <a-select-option value="driver">Windows 驱动</a-select-option>
          <a-select-option value="network">网络打印机</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item v-if="settingsForm.connectionMode === 'driver'" label="Windows 驱动名称">
        <a-input v-model:value="settingsForm.printerName" placeholder="TSC TTP-244 Pro" />
      </a-form-item>
      <template v-if="settingsForm.connectionMode === 'network'">
        <a-form-item label="打印机 IP">
          <a-input v-model:value="settingsForm.networkHost" placeholder="192.168.1.50" />
        </a-form-item>
        <a-form-item label="打印机端口">
          <a-input v-model:value="settingsForm.networkPort" placeholder="9100" />
        </a-form-item>
      </template>
      <a-form-item label="Windows 字体">
        <a-input v-model:value="settingsForm.windowsFont" placeholder="SimSun" />
      </a-form-item>
      <a-form-item label="编码">
        <a-input v-model:value="settingsForm.codepage" placeholder="UTF-8" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import JsBarcode from 'jsbarcode'
import QRCode from 'qrcode'
import {
  loadTscPrintSettings,
  printBundlesToTsc,
  saveTscPrintSettings,
  type TscConnectionMode,
  type TscPrintSettings,
} from '../utils/tscPrinter'

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
const tscPrinting = ref(false)
const settingsVisible = ref(false)
const settingsForm = ref<TscPrintSettings>(loadTscPrintSettings())

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

function handleTscPrintAll() {
  printTscBundles(props.bundles)
}

function handleTscPrintSelected() {
  const selected = props.bundles.filter(b => selectedBundles.value.includes(b.id))
  printTscBundles(selected)
}

async function printTscBundles(bundlesToPrint: BundleData[]) {
  tscPrinting.value = true
  try {
    await printBundlesToTsc(bundlesToPrint, props.cuttingBarcode, loadTscPrintSettings())
    message.success(`已发送 ${bundlesToPrint.length} 张菲仔到 TSC 打印服务`)
  } catch (error) {
    const err = error instanceof Error ? error.message : 'TSC 打印失败'
    message.error(err)
  } finally {
    tscPrinting.value = false
  }
}

function openSettings() {
  settingsForm.value = loadTscPrintSettings()
  settingsVisible.value = true
}

function saveSettings() {
  saveTscPrintSettings({
    ...settingsForm.value,
    connectionMode: normalizeConnectionMode(settingsForm.value.connectionMode),
  })
  settingsVisible.value = false
  message.success('TSC 打印设置已保存')
}

function normalizeConnectionMode(mode: TscConnectionMode): TscConnectionMode {
  if (mode === 'driver' || mode === 'network') return mode
  return 'usb'
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
          font-size: 12px;
          line-height: 1.15;
        }
        .label-page {
          width: 50mm;
          height: 80mm;
          padding: 1.5mm;
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
          height: 20mm;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          border-bottom: 1px dashed #ccc;
          margin-bottom: 0.6mm;
        }
        .p-hole {
          width: 4.5mm;
          height: 4.5mm;
          border: 1px solid #999;
          border-radius: 50%;
        }
        /* 顶部条形码 */
        .p-barcode {
          display: flex;
          flex-direction: column;
          align-items: center;
          margin-bottom: 0.8mm;
          flex-shrink: 0;
        }
        .p-barcode svg {
          width: 42mm;
          height: 7mm;
        }
        .p-barcode-text {
          font-size: 13px;
          font-weight: bold;
          margin-top: 0.3mm;
          letter-spacing: 0;
        }
        /* 统一表格 */
        .p-tbl {
          width: 100%;
          border-collapse: collapse;
          flex-shrink: 0;
        }
        .p-tbl td {
          border: 1px solid #333;
          padding: 0.25mm 0.8mm;
          font-size: 14px;
          line-height: 1.03;
        }
        .p-key {
          width: 21%;
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
          margin-top: 1mm;
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
          bottom: 5.5mm;
          width: 20mm;
          height: 20mm;
        }
        .p-size-value {
          font-weight: 700;
        }
        .p-bundle-no {
          position: absolute;
          left: 1.5mm;
          right: 1.5mm;
          bottom: 0.8mm;
          text-align: center;
          font-size: 12px;
          font-weight: bold;
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
          <svg class="print-barcode" data-value="${escapeHtml(props.cuttingBarcode)}"></svg>
          <div class="p-barcode-text">${escapeHtml(props.cuttingBarcode)}</div>
        </div>
        <table class="p-tbl">
          <tr>
            <td class="p-key">部位</td>
            <td class="p-val p-val-bold">${escapeHtml(bundle.part)}</td>
          </tr>
          <tr>
            <td class="p-key">订单号</td>
            <td class="p-val">${escapeHtml(bundle.orderNo)}</td>
          </tr>
          <tr>
            <td class="p-key">款号</td>
            <td class="p-val">${escapeHtml(bundle.styleCode)}</td>
          </tr>
          <tr>
            <td class="p-key">颜色</td>
            <td class="p-val">${escapeHtml(bundle.color)}</td>
          </tr>
        </table>
        <div class="p-bottom">
          <table class="p-tbl">
            <tr>
              <td class="p-key">尺码</td>
              <td class="p-val p-size-value">${escapeHtml(bundle.size)}</td>
            </tr>
            <tr>
              <td class="p-key">扎号</td>
              <td class="p-val">${escapeHtml(bundle.bundleNo)}</td>
            </tr>
            <tr>
              <td class="p-key">数量</td>
              <td class="p-val">${escapeHtml(String(bundle.qty))}</td>
            </tr>
            <tr>
              <td class="p-key">床号</td>
              <td class="p-val">${escapeHtml(bundle.bedNo ? String(bundle.bedNo) : '')}</td>
            </tr>
          </table>
        </div>
        <img class="p-qr" src="${escapeHtml(qrCodeUrls[bundle.id] || '')}" />
        <div class="p-bundle-no">${escapeHtml(bundle.globalBundleNo)}</div>
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

function escapeHtml(value: unknown) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
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
  padding: 6px;
  border: 1px solid #333;
  font-size: 12px;
  line-height: 1.15;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

/* 穿孔区 */
.preview-hole-zone {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-bottom: 1px dashed #ccc;
  margin-bottom: 4px;
}

.preview-hole {
  width: 18px;
  height: 18px;
  border: 1px solid #999;
  border-radius: 50%;
}

/* 顶部条形码 */
.preview-barcode {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 4px;
  flex-shrink: 0;
}

.preview-barcode svg {
  width: 150px;
  height: 28px;
}

.preview-barcode-text {
  font-size: 14px;
  font-weight: bold;
  margin-top: 1px;
  letter-spacing: 0;
}

/* 统一表格 */
.preview-tbl {
  width: 100%;
  border-collapse: collapse;
  flex-shrink: 0;
}

.preview-tbl td {
  border: 1px solid #333;
  padding: 1px 5px;
  font-size: 14px;
  line-height: 1.03;
}

.pv-key {
  width: 21%;
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
  margin-top: 4px;
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
  width: 80px;
  height: 80px;
}

.preview-size-value {
  font-weight: 700;
}

.preview-bundle-no {
  position: absolute;
  left: 6px;
  right: 6px;
  bottom: 3px;
  text-align: center;
  font-size: 12px;
  font-weight: bold;
}
</style>
