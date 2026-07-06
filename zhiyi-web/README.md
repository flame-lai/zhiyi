# Vue 3 + TypeScript + Vite

This template should help get you started developing with Vue 3 and TypeScript in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

Learn more about the recommended Project Setup and IDE Support in the [Vue Docs TypeScript Guide](https://vuejs.org/guide/typescript/overview.html#project-setup).

## TSC 菲仔打印

菲仔打印弹窗支持两条路径：

- `TSC打印全部` / `TSC打印选中`：生成 TSPL 指令，并按 `TSC_Sample.html` 的 WebSocket 协议提交到本机 TSC PrintServer。
- `浏览器打印全部` / `浏览器打印选中`：保留原来的浏览器打印方式，作为兜底方案。

默认配置在 `src/utils/tscPrinter.ts`，也可以通过页面上的 `打印设置` 覆盖并保存到浏览器 localStorage：

```env
VITE_TSC_PRINT_URL=ws://127.0.0.1:8888
VITE_TSC_CONNECTION_MODE=usb
VITE_TSC_PRINTER_NAME=
VITE_TSC_NETWORK_HOST=
VITE_TSC_NETWORK_PORT=9100
VITE_TSC_TEXT_FONT=TSS24.BF2
VITE_TSC_CODEPAGE=UTF-8
```

`TSC PrintServer_Setup.exe` 这类 Web 打印服务需要安装在能连接打印机的 Windows 电脑上。官方资料把 TSC Console Web 标成 `Server`，客户端要求是支持 HTML5 的 Web browser；TSC 官方资源列表也把 `TTP-244 Pro` 列在 TTP Series 4-Inch Performance Desktop Printers，并列在 `Configure TSC printer by web browser` 兼容机型中。

TSC 官方 demo 使用 `ws://127.0.0.1:8888` 连接 PrintServer。`9100` 是网络打印机常见 RAW 打印端口，不是浏览器连接 PrintServer 的端口。默认连接方式是 `USB 自动连接`，会按官方 `openport_usb: ""` 语义连接第一台可用 USB 打印机；也可以在 `打印设置` 中切换到 `Windows 驱动` 或 `网络打印机`。
