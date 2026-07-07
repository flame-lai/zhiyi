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
  windowsFont: string
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
  windowsFont: import.meta.env.VITE_TSC_WINDOWS_FONT || 'SimSun',
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

  const functions = buildBundleBatchFunctions(bundles, cuttingBarcode, normalizedSettings)
  await sendFunctionsByWebSocket(functions, normalizedSettings)
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
    line(16, 454, 168),
    line(16, 492, 168),
    line(16, 530, 168),
    line(16, 568, 168),
    line(16, 606, 168),
    vLine(86, 454, 152),
    text(24, 462, textFont, '尺码'),
    text(104, 462, textFont, value(bundle.size)),
    text(24, 500, textFont, '扎号'),
    text(104, 500, textFont, value(bundle.bundleNo)),
    text(24, 538, textFont, '数量'),
    text(104, 538, textFont, String(bundle.qty ?? '')),
    text(24, 576, textFont, '床号'),
    text(104, 576, textFont, bundle.bedNo == null ? '' : String(bundle.bedNo)),
    qrcode(labelWidth - 195, 450, value(bundle.globalBundleNo || bundle.bundleNo)),
    text(52, 616, '3', value(bundle.globalBundleNo || bundle.bundleNo), 1, 1),
    'PRINT 1,1',
  ].join('\r\n')
}

function buildBundleBatchFunctions(
  bundles: TscBundleData[],
  cuttingBarcode: string,
  settings: Pick<TscPrintSettings, 'windowsFont'>,
): TscFunction[] {
  return bundles.flatMap((bundle) => buildBundleFunctions(bundle, cuttingBarcode, settings))
}

function buildBundleFunctions(
  bundle: TscBundleData,
  cuttingBarcode: string,
  settings: Pick<TscPrintSettings, 'windowsFont'>,
): TscFunction[] {
  const globalBundleNo = value(bundle.globalBundleNo || bundle.bundleNo)
  const windowsFontName = settings.windowsFont || DEFAULT_TSC_PRINT_SETTINGS.windowsFont

  return [
    { setup: '50,80,4,8,0,2,0' },
    { clearbuffer: '' },
    raw('BOX 188,48,212,72,2'),
    raw('BAR 198,54,4,12'),
    { barcode: `30,182,"128",52,0,0,2,2,"${escapeTscFunctionText(cuttingBarcode)}"` },
    windowsFont(92, 246, 30, cuttingBarcode, 2, 'Arial'),
    raw('BAR 16,282,368,2'),
    raw('BAR 16,322,368,2'),
    raw('BAR 16,362,368,2'),
    raw('BAR 16,402,368,2'),
    raw('BAR 16,442,368,2'),
    raw('BAR 104,282,2,160'),
    windowsFont(24, 288, 30, '部位', 2, windowsFontName),
    windowsFont(120, 288, 30, value(bundle.part), 2, windowsFontName),
    windowsFont(24, 328, 30, '订单号', 2, windowsFontName),
    windowsFont(120, 328, 30, value(bundle.orderNo), 0, 'Arial'),
    windowsFont(24, 368, 30, '款号', 2, windowsFontName),
    windowsFont(120, 368, 30, value(bundle.styleCode), 0, 'Arial'),
    windowsFont(24, 408, 30, '颜色', 2, windowsFontName),
    windowsFont(120, 408, 30, value(bundle.color), 0, windowsFontName),
    raw('BAR 16,454,168,2'),
    raw('BAR 16,492,168,2'),
    raw('BAR 16,530,168,2'),
    raw('BAR 16,568,168,2'),
    raw('BAR 16,606,168,2'),
    raw('BAR 86,454,2,152'),
    windowsFont(24, 460, 32, '尺码', 2, windowsFontName),
    windowsFont(104, 460, 32, value(bundle.size), 2, 'Arial'),
    windowsFont(24, 498, 32, '扎号', 2, windowsFontName),
    windowsFont(104, 498, 32, value(bundle.bundleNo), 0, 'Arial'),
    windowsFont(24, 536, 32, '数量', 2, windowsFontName),
    windowsFont(104, 536, 32, String(bundle.qty ?? ''), 0, 'Arial'),
    windowsFont(24, 574, 32, '床号', 2, windowsFontName),
    windowsFont(104, 574, 32, bundle.bedNo == null ? '' : String(bundle.bedNo), 0, 'Arial'),
    raw(qrcode(205, 450, globalBundleNo)),
    windowsFont(52, 616, 26, globalBundleNo, 2, 'Arial'),
    { printlabel: '1,1' },
  ]
}

async function sendFunctionsByWebSocket(functions: TscFunction[], settings: TscPrintSettings) {
  const payload = {
    functions_inorder: [
      buildOpenPortFunction(settings),
      ...functions,
      { closeport: '' },
    ],
  }

  await websocketRequest(settings.endpoint, payload)
}

function raw(command: string): TscFunction {
  return { sendcommand_crlf: command }
}

function windowsFont(
  x: number,
  y: number,
  height: number,
  content: string,
  style = 0,
  font = 'SimSun',
): TscFunction {
  return {
    windowsfont: [
      x,
      y,
      height,
      0,
      style,
      0,
      font,
      escapeTscFunctionText(content),
    ].join(','),
  }
}

function escapeTscFunctionText(input: string) {
  return input.replace(/,/g, '，').replace(/\r?\n/g, ' ')
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
    windowsFont: settings.windowsFont?.trim() || DEFAULT_TSC_PRINT_SETTINGS.windowsFont,
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
  return `QRCODE ${x},${y},L,6,A,0,"${escapeTsplText(content)}"`
}

function line(x: number, y: number, width: number) {
  return `BAR ${x},${y},${width},2`
}

function vLine(x: number, y: number, height: number) {
  return `BAR ${x},${y},2,${height}`
}
