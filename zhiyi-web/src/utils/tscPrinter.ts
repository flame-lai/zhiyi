export interface TscBundleData {
  id: number
  orderNo: string
  styleCode: string
  color: string
  size: string
  part: string
  fabric?: string
  bundleNo: string
  globalBundleNo: string
  qty: number
  cuttingPieces?: number
  bedNo?: number
}

export type TscConnectionMode = 'usb' | 'driver' | 'network'

export interface TscPrintSettings {
  endpoint: string
  connectionMode: TscConnectionMode
  printerName: string
  networkHost: string
  networkPort: string
  textFont: string
  codepage: string
}

type TscFunction = Record<string, string>

export const DEFAULT_TSC_PRINT_SETTINGS: TscPrintSettings = {
  endpoint: import.meta.env.VITE_TSC_PRINT_URL || 'ws://127.0.0.1:8888',
  connectionMode: normalizeConnectionMode(import.meta.env.VITE_TSC_CONNECTION_MODE),
  printerName: import.meta.env.VITE_TSC_PRINTER_NAME || '',
  networkHost: import.meta.env.VITE_TSC_NETWORK_HOST || '',
  networkPort: import.meta.env.VITE_TSC_NETWORK_PORT || '9100',
  textFont: import.meta.env.VITE_TSC_TEXT_FONT || 'TSS24.BF2',
  codepage: import.meta.env.VITE_TSC_CODEPAGE || 'UTF-8',
}

const SETTINGS_KEY = 'zhiyi.tscPrintSettings'
const DOTS_PER_MM = 8
const WEBSOCKET_TIMEOUT_MS = 15000

export function loadTscPrintSettings(): TscPrintSettings {
  const stored = localStorage.getItem(SETTINGS_KEY)
  if (!stored) return DEFAULT_TSC_PRINT_SETTINGS

  try {
    const parsed = JSON.parse(stored) as Partial<TscPrintSettings & { requestFormat?: string }>
    const isLegacyHttpSetting = Boolean(parsed.requestFormat && !parsed.connectionMode)
    return normalizeSettings({
      ...DEFAULT_TSC_PRINT_SETTINGS,
      ...parsed,
      endpoint: isLegacyHttpSetting
        ? DEFAULT_TSC_PRINT_SETTINGS.endpoint
        : parsed.endpoint || DEFAULT_TSC_PRINT_SETTINGS.endpoint,
      connectionMode: isLegacyHttpSetting
        ? DEFAULT_TSC_PRINT_SETTINGS.connectionMode
        : parsed.connectionMode || DEFAULT_TSC_PRINT_SETTINGS.connectionMode,
    })
  } catch {
    return DEFAULT_TSC_PRINT_SETTINGS
  }
}

export function saveTscPrintSettings(settings: TscPrintSettings) {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(normalizeSettings(settings)))
}

export async function printBundlesToTsc(
  bundles: TscBundleData[],
  cuttingBarcode: string,
  settings: TscPrintSettings = loadTscPrintSettings(),
) {
  if (!bundles.length) {
    throw new Error('没有可打印的菲仔')
  }

  const normalizedSettings = normalizeSettings(settings)
  if (!normalizedSettings.endpoint) {
    throw new Error('请先配置 TSC PrintServer WebSocket 地址')
  }

  const command = buildBundleBatchTspl(bundles, cuttingBarcode, normalizedSettings)
  await sendTsplByWebSocket(command, normalizedSettings)
}

export function buildBundleBatchTspl(
  bundles: TscBundleData[],
  cuttingBarcode: string,
  settings: Pick<TscPrintSettings, 'textFont' | 'codepage'> = DEFAULT_TSC_PRINT_SETTINGS,
) {
  return bundles.map((bundle) => buildBundleTspl(bundle, cuttingBarcode, settings)).join('\r\n')
}

export function buildBundleTspl(
  bundle: TscBundleData,
  cuttingBarcode: string,
  settings: Pick<TscPrintSettings, 'textFont' | 'codepage'> = DEFAULT_TSC_PRINT_SETTINGS,
) {
  const textFont = settings.textFont || DEFAULT_TSC_PRINT_SETTINGS.textFont
  const codepage = settings.codepage || DEFAULT_TSC_PRINT_SETTINGS.codepage
  const labelWidth = mm(50)

  return [
    'SIZE 50 mm,80 mm',
    'GAP 2 mm,0 mm',
    'DENSITY 8',
    'SPEED 4',
    'DIRECTION 1',
    'REFERENCE 0,0',
    `CODEPAGE ${codepage}`,
    'CLS',
    'BOX 188,48,212,72,2',
    'BAR 198,54,4,12',
    barcode(30, 182, cuttingBarcode),
    text(92, 246, '3', cuttingBarcode, 1, 1),
    line(16, 282, 368),
    line(16, 322, 368),
    line(16, 362, 368),
    line(16, 402, 368),
    line(16, 442, 368),
    vLine(104, 282, 160),
    text(24, 290, textFont, '部位'),
    text(120, 290, textFont, value(bundle.part)),
    text(24, 330, textFont, '订单号'),
    text(120, 330, textFont, value(bundle.orderNo)),
    text(24, 370, textFont, '款号'),
    text(120, 370, textFont, value(bundle.styleCode)),
    text(24, 410, textFont, '颜色'),
    text(120, 410, textFont, value(bundle.color)),
    line(16, 474, 168),
    line(16, 514, 168),
    line(16, 554, 168),
    line(16, 594, 168),
    line(16, 634, 168),
    vLine(86, 474, 160),
    text(24, 482, textFont, '尺码'),
    text(104, 478, '3', value(bundle.size), 2, 2),
    text(24, 522, textFont, '扎号'),
    text(104, 522, textFont, value(bundle.bundleNo)),
    text(24, 562, textFont, '数量'),
    text(104, 562, textFont, String(bundle.qty ?? '')),
    text(24, 602, textFont, '床号'),
    text(104, 602, textFont, bundle.bedNo == null ? '' : String(bundle.bedNo)),
    qrcode(labelWidth - 172, 472, value(bundle.globalBundleNo || bundle.bundleNo)),
    text(52, 616, '3', value(bundle.globalBundleNo || bundle.bundleNo), 1, 1),
    'PRINT 1,1',
  ].join('\r\n')
}

async function sendTsplByWebSocket(command: string, settings: TscPrintSettings) {
  const payload = {
    functions_inorder: [
      buildOpenPortFunction(settings),
      ...command
        .split(/\r?\n/)
        .map((lineText) => lineText.trim())
        .filter(Boolean)
        .map((lineText) => ({ sendcommand_crlf: lineText })),
      { closeport: '' },
    ],
  }

  await websocketRequest(settings.endpoint, payload)
}

function websocketRequest(endpoint: string, payload: unknown) {
  return new Promise<void>((resolve, reject) => {
    const websocketUrl = normalizeWebSocketEndpoint(endpoint)
    let settled = false
    const websocket = new WebSocket(websocketUrl)
    const timer = window.setTimeout(() => {
      finish(new Error(`TSC PrintServer 连接超时：${websocketUrl}`))
      websocket.close()
    }, WEBSOCKET_TIMEOUT_MS)

    function finish(error?: Error) {
      if (settled) return
      settled = true
      window.clearTimeout(timer)
      if (error) reject(error)
      else resolve()
    }

    websocket.onopen = () => {
      websocket.send(JSON.stringify(payload))
    }

    websocket.onmessage = (event) => {
      if (event.data === 'Finished') {
        websocket.close()
        finish()
        return
      }

      try {
        const result = JSON.parse(String(event.data)) as {
          Function_Failed?: string
          [key: string]: unknown
        }
        if (result.Function_Failed) {
          websocket.close()
          finish(new Error(`TSC PrintServer 执行失败：${result.Function_Failed}`))
        }
      } catch {
        websocket.close()
        finish(new Error(`TSC PrintServer 返回了无法识别的数据：${String(event.data)}`))
      }
    }

    websocket.onerror = () => {
      finish(new Error(`无法连接 TSC PrintServer：${websocketUrl}`))
    }

    websocket.onclose = (event) => {
      if (!settled && event.code !== 1000) {
        finish(new Error(`TSC PrintServer 连接已关闭：code=${event.code}`))
      }
    }
  })
}

function buildOpenPortFunction(settings: TscPrintSettings): TscFunction {
  if (settings.connectionMode === 'network') {
    if (!settings.networkHost) throw new Error('请填写打印机 IP')
    if (!settings.networkPort) throw new Error('请填写打印机端口')
    return { openport_net: `${settings.networkHost},${settings.networkPort}` }
  }
  if (settings.connectionMode === 'driver') {
    if (!settings.printerName) throw new Error('请填写 Windows 驱动名称')
    return { openport_driver: settings.printerName }
  }

  return { openport_usb: '' }
}

function normalizeSettings(settings: TscPrintSettings): TscPrintSettings {
  return {
    ...settings,
    endpoint: normalizeWebSocketEndpoint(settings.endpoint),
    connectionMode: normalizeConnectionMode(settings.connectionMode),
    printerName: settings.printerName?.trim() || '',
    networkHost: settings.networkHost?.trim() || '',
    networkPort: settings.networkPort?.trim() || '9100',
    textFont: settings.textFont?.trim() || DEFAULT_TSC_PRINT_SETTINGS.textFont,
    codepage: settings.codepage?.trim() || DEFAULT_TSC_PRINT_SETTINGS.codepage,
  }
}

function normalizeConnectionMode(mode?: string): TscConnectionMode {
  if (mode === 'driver' || mode === 'network') return mode
  return 'usb'
}

function normalizeWebSocketEndpoint(endpoint?: string) {
  const value = endpoint?.trim()
  if (!value) return ''
  if (/^\d+$/.test(value)) return `ws://127.0.0.1:${value}`
  if (/^wss?:\/\//i.test(value)) return value.replace(/\/$/, '')
  if (/^https?:\/\//i.test(value)) {
    return value.replace(/^http/i, 'ws').replace(/\/$/, '')
  }
  return `ws://${value}`.replace(/\/$/, '')
}

function mm(valueInMm: number) {
  return Math.round(valueInMm * DOTS_PER_MM)
}

function value(input: unknown) {
  return input == null ? '' : String(input)
}

function escapeTsplText(input: string) {
  return input.replace(/"/g, '\\"')
}

function text(x: number, y: number, font: string, content: string, xMul = 1, yMul = 1) {
  return `TEXT ${x},${y},"${font}",0,${xMul},${yMul},"${escapeTsplText(content)}"`
}

function barcode(x: number, y: number, content: string) {
  return `BARCODE ${x},${y},"128",52,0,0,2,2,"${escapeTsplText(content)}"`
}

function qrcode(x: number, y: number, content: string) {
  return `QRCODE ${x},${y},L,5,A,0,"${escapeTsplText(content)}"`
}

function line(x: number, y: number, width: number) {
  return `BAR ${x},${y},${width},2`
}

function vLine(x: number, y: number, height: number) {
  return `BAR ${x},${y},2,${height}`
}
