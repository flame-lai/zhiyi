/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_TSC_PRINT_URL?: string
  readonly VITE_TSC_CONNECTION_MODE?: 'usb' | 'driver' | 'network'
  readonly VITE_TSC_PRINTER_NAME?: string
  readonly VITE_TSC_NETWORK_HOST?: string
  readonly VITE_TSC_NETWORK_PORT?: string
  readonly VITE_TSC_WINDOWS_FONT?: string
  readonly VITE_TSC_TEXT_FONT?: string
  readonly VITE_TSC_CODEPAGE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
