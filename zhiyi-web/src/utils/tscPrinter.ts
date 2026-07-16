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
const DEBUG_KEY = 'zhiyi.lastTscPrintDebug'
const DOTS_PER_MM = 8
const ROTATED_PRINT_Y_OFFSET_DOTS = 120
const PRINT_X_OFFSET_DOTS = 0
const LABEL_WIDTH_DOTS = 400
const LABEL_HEIGHT_DOTS = 640
const BITMAP_BLACK_THRESHOLD = 480
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

  // 使用支持中文字体的位图函数渲染流
  const functions = buildBundleBatchFunctions(bundles, cuttingBarcode, normalizedSettings)
  await sendFunctionsByWebSocket(functions, normalizedSettings)
}

export function buildBundleBatchTspl(
  bundles: TscBundleData[],
  cuttingBarcode: string,
  settings: Pick<TscPrintSettings, 'textFont' | 'codepage'> = DEFAULT_TSC_PRINT_SETTINGS,
) {
  const codepage = settings.codepage || DEFAULT_TSC_PRINT_SETTINGS.codepage
  const textFont = settings.textFont || DEFAULT_TSC_PRINT_SETTINGS.textFont

  // 1. 全局初始化配置，整个打印流只发一次
  const header = [
    'SIZE 50 mm,80 mm',
    'GAP 3 mm,0 mm',
    'DENSITY 8',
    'SPEED 2',
    'DIRECTION 1',
    `REFERENCE ${PRINT_X_OFFSET_DOTS},0`,
    `CODEPAGE ${codepage}`,
  ].join('\r\n')

  // 2. 每一张菲仔的内容体（清空画布 -> 绘制 -> 打印）
  const body = bundles.map((bundle) => {
    const labelWidth = mm(50)
    return [
      'CLS',
      barcode(30, 150, cuttingBarcode),
      text(92, 214, '3', cuttingBarcode, 1, 1),
      line(16, 250, 368),
      line(16, 290, 368),
      line(16, 330, 368),
      line(16, 370, 368),
      line(16, 410, 368),
      vLine(104, 250, 160),
      text(24, 258, textFont, '部位'),
      text(120, 258, textFont, value(bundle.part)),
      text(24, 298, textFont, '单号'),
      text(120, 298, textFont, value(bundle.orderNo)),
      text(24, 338, textFont, '款号'),
      text(120, 338, textFont, value(bundle.styleCode)),
      text(24, 378, textFont, '颜色'),
      text(120, 378, textFont, value(bundle.color)),
      line(16, 422, 168),
      line(16, 460, 168),
      line(16, 498, 168),
      line(16, 536, 168),
      line(16, 574, 168),
      vLine(86, 422, 152),
      text(24, 430, textFont, '尺码'),
      text(104, 430, textFont, value(bundle.size)),
      text(24, 468, textFont, '扎号'),
      text(104, 468, textFont, value(bundle.bundleNo)),
      text(24, 506, textFont, '数量'),
      text(104, 506, textFont, String(bundle.qty ?? '')),
      text(24, 544, textFont, '床号'),
      text(104, 544, textFont, bundle.bedNo == null ? '' : String(bundle.bedNo)),
      qrcode(labelWidth - 195, 425, value(bundle.globalBundleNo || bundle.bundleNo)),
      text(52, 586, '3', value(bundle.globalBundleNo || bundle.bundleNo), 1, 1),
      'PRINT 1,1',
    ].join('\r\n')
  }).join('\r\n')

  return header + '\r\n' + body
}

export function buildBundleTspl(
  bundle: TscBundleData,
  cuttingBarcode: string,
  settings: Pick<TscPrintSettings, 'textFont' | 'codepage'> = DEFAULT_TSC_PRINT_SETTINGS,
) {
  return buildBundleBatchTspl([bundle], cuttingBarcode, settings)
}



function websocketRequest(endpoint: string, payload: unknown) {
  return new Promise<void>((resolve, reject) => {
    const websocketUrl = normalizeWebSocketEndpoint(endpoint)
    const debug = createTscDebugLog(websocketUrl, payload)
    let settled = false
    const websocket = new WebSocket(websocketUrl)
    const timer = window.setTimeout(() => {
      appendTscDebug(debug, 'timeout', `TSC PrintServer 连接超时：${websocketUrl}`)
      finish(new Error(`TSC PrintServer 连接超时：${websocketUrl}`))
      websocket.close()
    }, WEBSOCKET_TIMEOUT_MS)

    function finish(error?: Error) {
      if (settled) return
      settled = true
      window.clearTimeout(timer)
      appendTscDebug(debug, 'finish', error ? error.message : 'ok')
      console.info('[TSC PrintServer] debug log saved in localStorage:', DEBUG_KEY, debug)
      if (error) reject(error)
      else resolve()
    }

    websocket.onopen = () => {
      appendTscDebug(debug, 'open', 'WebSocket connected')
      websocket.send(JSON.stringify(payload))
      appendTscDebug(debug, 'send', payload)
    }

    websocket.onmessage = (event) => {
      appendTscDebug(debug, 'message', event.data)
      if (event.data === 'Finished') {
        websocket.close()
        finish()
        return
      }

      try {
        const result = JSON.parse(String(event.data)) as {
          Function_Failed?: string
          printerstatus?: string[]
          [key: string]: unknown
        }
        if (result.Function_Failed) {
          websocket.close()
          finish(new Error(`TSC PrintServer 执行失败：${result.Function_Failed}；原始返回：${event.data}`))
          return
        }
        if (Array.isArray(result.printerstatus) && result.printerstatus.length > 0) {
          const abnormalStatuses = result.printerstatus.filter((status) => {
            const normalizedStatus = String(status).trim().toLowerCase()
            return normalizedStatus && normalizedStatus !== 'ready'
          })
          if (abnormalStatuses.length > 0) {
            console.warn('[TSC PrintServer] printer status:', abnormalStatuses)
          }
        }
      } catch {
        websocket.close()
        finish(new Error(`TSC PrintServer 返回了无法识别的数据：${String(event.data)}`))
      }
    }

    websocket.onerror = () => {
      appendTscDebug(debug, 'error', 'WebSocket error')
      finish(new Error(`无法连接 TSC PrintServer：${websocketUrl}`))
    }

    websocket.onclose = (event) => {
      appendTscDebug(debug, 'close', {
        code: event.code,
        reason: event.reason,
        wasClean: event.wasClean,
      })
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

function createTscDebugLog(endpoint: string, payload: unknown) {
  const log = {
    endpoint,
    startedAt: new Date().toISOString(),
    payload,
    events: [] as Array<{ type: string; at: string; data: unknown }>,
  }
  saveTscDebug(log)
  return log
}

function appendTscDebug(
  log: ReturnType<typeof createTscDebugLog>,
  type: string,
  data: unknown,
) {
  log.events.push({
    type,
    at: new Date().toISOString(),
    data,
  })
  saveTscDebug(log)
}

function saveTscDebug(log: ReturnType<typeof createTscDebugLog>) {
  try {
    ;(window as unknown as { __ZHIYI_TSC_LAST_DEBUG__?: unknown }).__ZHIYI_TSC_LAST_DEBUG__ = log
    localStorage.setItem(DEBUG_KEY, JSON.stringify(log))
  } catch {
    // Debug logging must not block printing.
  }
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

function qrcode(x: number, y: number, content: string, rotation = 0) {
  const adjustedY = rotation === 180 ? y + ROTATED_PRINT_Y_OFFSET_DOTS : y
  return `QRCODE ${x},${adjustedY},H,6,A,${rotation},M2,S7,"${escapeTsplText(content)}"`
}

function buildBundleBatchFunctions(
  bundles: TscBundleData[],
  cuttingBarcode: string,
  settings: Pick<TscPrintSettings, 'windowsFont'>,
): TscFunction[] {
  // 1. 初始化设置仅在流头部发送一次
  const header: TscFunction[] = [
    { setup: '50,80,2,8,0,3,0' },
    { clearbuffer: '' },
    raw('DIRECTION 1'),
    raw(`REFERENCE ${PRINT_X_OFFSET_DOTS},0`),
  ]

  // 2. 依次生成每一张标签 of 绘制内容并打印
  const body = bundles.flatMap((bundle) => {
    const globalBundleNo = value(bundle.globalBundleNo || bundle.bundleNo)
    const windowsFontName = settings.windowsFont || DEFAULT_TSC_PRINT_SETTINGS.windowsFont

    return [
      buildTextBitmapFunction(bundle, cuttingBarcode, windowsFontName),
      { barcode: `370,${458 + ROTATED_PRINT_Y_OFFSET_DOTS},"128",52,0,180,2,2,"${escapeTscFunctionText(cuttingBarcode)}"` },
      raw(qrcode(195, 180, globalBundleNo, 180)),
      { printlabel: '1,1' },
      { clearbuffer: '' }, // 打印完当前页后清理画布缓冲区，准备绘制下一页
    ]
  })

  return [...header, ...body]
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

function buildTextBitmapFunction(
  bundle: TscBundleData,
  cuttingBarcode: string,
  windowsFontName: string,
) {
  const source = document.createElement('canvas')
  source.width = LABEL_WIDTH_DOTS
  source.height = LABEL_HEIGHT_DOTS + ROTATED_PRINT_Y_OFFSET_DOTS // 760
  const sourceContext = source.getContext('2d', { willReadFrequently: true })
  if (!sourceContext) throw new Error('浏览器无法生成 TSC 文字位图')

  sourceContext.fillStyle = '#000'
  sourceContext.textBaseline = 'top'

  // 在 Canvas 内部绘制表格框线，防止与文字产生驱动级别的坐标错位
  // Table 1 (部位、单号、款号、颜色)
  sourceContext.fillRect(16, 282, 368, 2)
  sourceContext.fillRect(16, 322, 368, 2)
  sourceContext.fillRect(16, 362, 368, 2)
  sourceContext.fillRect(16, 402, 368, 2)
  sourceContext.fillRect(16, 442, 368, 2)
  sourceContext.fillRect(16, 282, 2, 160)
  sourceContext.fillRect(382, 282, 2, 160)
  sourceContext.fillRect(104, 282, 2, 160)

  // Table 2 (尺码、扎号、数量、床号)
  sourceContext.fillRect(16, 454, 168, 2)
  sourceContext.fillRect(16, 492, 168, 2)
  sourceContext.fillRect(16, 530, 168, 2)
  sourceContext.fillRect(16, 568, 168, 2)
  sourceContext.fillRect(16, 606, 168, 2)
  sourceContext.fillRect(16, 454, 2, 152)
  sourceContext.fillRect(182, 454, 2, 152)
  sourceContext.fillRect(86, 454, 2, 152)

  // 绘制文本
  drawCanvasText(sourceContext, 92, 246, 30, cuttingBarcode, 'Arial')
  drawCanvasText(sourceContext, 24, 290, 27, '部位', windowsFontName)
  drawCanvasText(sourceContext, 120, 286, 30, value(bundle.part), windowsFontName, true)
  drawCanvasText(sourceContext, 24, 330, 27, '单号', windowsFontName)
  drawCanvasText(sourceContext, 120, 330, 27, value(bundle.orderNo), 'Arial')
  drawCanvasText(sourceContext, 24, 370, 27, '款号', windowsFontName)
  drawCanvasText(sourceContext, 120, 366, 30, value(bundle.styleCode), 'Arial')
  drawCanvasText(sourceContext, 24, 410, 27, '颜色', windowsFontName)
  drawCanvasText(sourceContext, 120, 406, 30, value(bundle.color), windowsFontName)
  
  // Table 2 文本
  drawCanvasText(sourceContext, 24, 460, 28, '尺码', windowsFontName)
  drawCanvasText(sourceContext, 104, 456, 32, value(bundle.size), 'Arial', true)
  drawCanvasText(sourceContext, 24, 498, 28, '扎号', windowsFontName)
  drawCanvasText(sourceContext, 104, 494, 32, value(bundle.bundleNo), 'Arial', true)
  drawCanvasText(sourceContext, 24, 536, 28, '数量', windowsFontName)
  drawCanvasText(sourceContext, 104, 532, 32, String(bundle.qty ?? ''), 'Arial', true)
  drawCanvasText(sourceContext, 24, 574, 28, '床号', windowsFontName)
  drawCanvasText(
    sourceContext,
    104,
    570,
    32,
    bundle.bedNo == null ? '' : String(bundle.bedNo),
    'Arial',
    true,
  )
  
  // 不绘制最底部的扎号大字编码

  const rotated = document.createElement('canvas')
  rotated.width = LABEL_WIDTH_DOTS
  rotated.height = LABEL_HEIGHT_DOTS + ROTATED_PRINT_Y_OFFSET_DOTS // 760
  const rotatedContext = rotated.getContext('2d', { willReadFrequently: true })
  if (!rotatedContext) throw new Error('浏览器无法旋转 TSC 文字位图')

  rotatedContext.translate(LABEL_WIDTH_DOTS, LABEL_HEIGHT_DOTS + ROTATED_PRINT_Y_OFFSET_DOTS)
  rotatedContext.rotate(Math.PI)
  rotatedContext.drawImage(source, 0, 0)

  return canvasToBitmapFunction(rotated)
}

function drawCanvasText(
  context: CanvasRenderingContext2D,
  x: number,
  y: number,
  height: number,
  content: string,
  font: string,
  bold = false,
) {
  const fontFamily = font === 'Arial' ? 'Arial' : `"${font}", SimSun, "Microsoft YaHei", sans-serif`
  context.font = `${bold ? '700 ' : ''}${height}px ${fontFamily}`
  context.fillText(content, x, y)
}

function canvasToBitmapFunction(canvas: HTMLCanvasElement): TscFunction {
  const context = canvas.getContext('2d', { willReadFrequently: true })
  if (!context) throw new Error('浏览器无法读取 TSC 文字位图')

  const image = context.getImageData(0, 0, canvas.width, canvas.height)
  const { data, width, height } = image
  let minX = width
  let minY = height
  let maxX = -1
  let maxY = -1

  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const index = (y * width + x) * 4
      const alpha = data[index + 3]
      const red = data[index]
      const green = data[index + 1]
      const blue = data[index + 2]
      if (alpha > 0 && red + green + blue < BITMAP_BLACK_THRESHOLD) {
        minX = Math.min(minX, x)
        minY = Math.min(minY, y)
        maxX = Math.max(maxX, x)
        maxY = Math.max(maxY, y)
      }
    }
  }

  if (maxX < minX || maxY < minY) {
    throw new Error('TSC 文字位图为空，请检查 Windows 字体设置')
  }

  const bitmapX = Math.max(0, Math.floor(minX / 8) * 8)
  const bitmapWidthDots = Math.ceil((maxX - bitmapX + 1) / 8) * 8
  const bitmapWidthBytes = bitmapWidthDots / 8
  const bitmapHeight = maxY - minY + 1
  const bitmapBytes = new Uint8Array(bitmapWidthBytes * bitmapHeight)
  let outputIndex = 0

  for (let y = minY; y <= maxY; y += 1) {
    for (let byteIndex = 0; byteIndex < bitmapWidthBytes; byteIndex += 1) {
      let byte = 0xFF
      for (let bit = 0; bit < 8; bit += 1) {
        const x = bitmapX + byteIndex * 8 + bit
        if (x >= width) continue
        const index = (y * width + x) * 4
        const alpha = data[index + 3]
        const red = data[index]
        const green = data[index + 1]
        const blue = data[index + 2]
        if (alpha > 0 && red + green + blue < BITMAP_BLACK_THRESHOLD) {
          byte &= ~(0x80 >> bit)
        }
      }
      bitmapBytes[outputIndex] = byte
      outputIndex += 1
    }
  }

  const header = new TextEncoder().encode(
    `BITMAP ${bitmapX},${minY},${bitmapWidthBytes},${bitmapHeight},0,`,
  )
  const command = new Uint8Array(header.length + bitmapBytes.length + 2)
  command.set(header, 0)
  command.set(bitmapBytes, header.length)
  command[command.length - 2] = 13
  command[command.length - 1] = 10

  return { sendUint8Array: uint8ArrayToBase64(command) }
}

function uint8ArrayToBase64(bytes: Uint8Array) {
  const chunkSize = 0x8000
  let binary = ''
  for (let offset = 0; offset < bytes.length; offset += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(offset, offset + chunkSize))
  }
  return window.btoa(binary)
}

function escapeTscFunctionText(input: string) {
  return input.replace(/,/g, '，').replace(/\r?\n/g, ' ')
}

function line(x: number, y: number, width: number) {
  return `BAR ${x},${y},${width},2`
}

function vLine(x: number, y: number, height: number) {
  return `BAR ${x},${y},2,${height}`
}
