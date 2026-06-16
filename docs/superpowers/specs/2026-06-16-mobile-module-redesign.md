# 移动端模块重实现设计

日期：2026-06-16

## 背景

当前仓库已有一个小程序项目 [zhiyi-mini/](../../../zhiyi-mini/)，技术栈为 Taro 4、Vue3、TypeScript、Sass。现有实现已经包含登录、首页、扫码、工资、管理员看板/异常/计时/查询等页面，但整体更接近原型或 MVP：

- 登录页只有工号登录，缺少员工/管理员双登录模式。
- 首页入口固定，没有按角色移动端权限动态渲染菜单。
- 扫码页扫码后直接报工，没有“解析详情 → 选择扎号/工序 → 确认上报”的完整流程。
- 当前小程序把二维码内容当作 `bundleId` 解析，但现有打印菲仔二维码内容实际是 `Bundle.globalBundleNo`。
- 管理员端页面和接口还不完整。

本轮目标是在现有 Taro 小程序基础上重新实现移动端模块，同时补齐后端移动端专用接口，完整覆盖需求文档 [模块4-移动端.md](../../../需求文档/模块4-移动端.md) 中的员工端和管理员端功能。

## 总体方案

采用“在现有 Taro 项目上分层重构”的方案：

- 保留 [zhiyi-mini/](../../../zhiyi-mini/) 工程，不改为原生小程序，不重建项目。
- 重做页面结构、交互和 UI。
- 新增 `/api/mobile/**` 后端接口，面向移动端体验设计 DTO/VO。
- 后端移动端 Controller/Service 复用现有员工、角色、裁床、扎号、报工、异常、计时工资等业务能力。
- 保留旧接口 `/api/report/**`、`/api/admin/**`，避免影响现有 web 端。
- 兼容已打印二维码和条形码，不要求重新打印菲仔。

## 前端架构

继续使用 Taro + Vue3 + TypeScript + Sass。建议结构调整为：

```text
zhiyi-mini/src/
├── api/
│   ├── mobileAuth.ts
│   ├── mobileReport.ts
│   ├── mobileAdmin.ts
│   └── types.ts
├── components/
│   ├── AppCard.vue
│   ├── StatusTag.vue
│   ├── EmptyState.vue
│   ├── ConfirmPanel.vue
│   └── ProcessCheckList.vue
├── utils/
│   ├── request.ts
│   ├── auth.ts
│   └── format.ts
├── pages/
│   ├── login/
│   ├── index/
│   ├── scan/
│   ├── wage/
│   ├── report-records/
│   └── admin/
└── app.config.ts
```

### 前端原则

- 页面只负责展示和交互，接口调用集中在 `api/`。
- token、登录失效、业务错误、网络错误统一在 `utils/request.ts` 处理。
- 用户信息、菜单和权限保存在本地状态与 storage 中。
- 首页、底部导航、页面访问控制都根据后端返回的菜单和权限判断，不在前端硬编码管理员身份。
- 使用自研轻量组件，不引入额外 UI 库，降低小程序包体和适配风险。

## 后端架构

新增移动端模块：

```text
zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/
├── controller/
│   ├── MobileAuthController.java
│   ├── MobileReportController.java
│   └── MobileAdminController.java
├── dto/
├── vo/
└── service/
    └── MobileService.java
```

API 路径统一为 `/api/mobile/**`。

### 后端原则

- Controller、DTO、VO 按移动端场景设计。
- Service 复用现有 Mapper/Service，避免复制业务规则。
- 报工、清除、补录、计时工资新增/修改/删除都使用事务。
- 后端以数据库状态为准，不信任前端传入的可选状态。
- 移动端权限在后端校验，前端隐藏入口只作为体验优化。

## 登录与权限

移动端登录统一走：

```text
POST /api/mobile/auth/login-by-code
POST /api/mobile/auth/login-by-account
POST /api/mobile/auth/logout
GET  /api/mobile/auth/profile
```

登录模式：

- 员工登录：输入工号/登录码，免密登录。
- 管理员登录：输入账号和密码。

登录成功返回：

```ts
interface MobileLoginResult {
  token: string
  employee: {
    id: number
    code: string
    name: string
  }
  role: {
    id: number
    name: string
  }
  menus: MobileMenu[]
  permissions: string[]
  defaultHomeType: 'employee' | 'admin'
}

interface MobileMenu {
  code: string
  title: string
  path: string
  group: 'employee' | 'admin'
  icon?: string
  sort: number
}
```

权限码示例：

```text
mobile:scan
mobile:wage
mobile:report-records
mobile:dashboard
mobile:anomaly
mobile:timed-wage
mobile:bundle-query
mobile:order-query
mobile:report-clear
mobile:report-supplement
```

登录失败提示：

- 账号不存在
- 密码错误
- 账号已停用，请联系管理员
- 无权限，请联系管理员

## 页面设计

### 登录页

重做为双 Tab：

- 员工登录：工号/登录码 + 登录按钮。
- 管理员登录：账号 + 密码 + 登录按钮。

已有有效 token 时，启动小程序后调用 profile 接口恢复登录态。token 失效时清空本地状态并跳转登录页。

### 首页

首页根据后端菜单动态渲染。

员工常见入口：

- 扫码报工
- 我的工资
- 报工记录

管理员常见入口：

- 生产看板
- 异常预警
- 计时录入
- 扎号查询
- 订单查询

如果用户同时有员工和管理员功能，首页按分组展示：

- 我的工作
- 管理工具

底部导航保留稳定入口：

- 首页
- 扫码
- 我的

管理员功能从首页卡片进入，避免底部 Tab 过多。

### 扫码报工

扫码报工改为三步流程。

#### 第一步：扫码或手动输入

用户点击“扫码识别”或手动输入码，前端调用：

```text
POST /api/mobile/report/scan/resolve
```

后端自动识别：

- `Bundle.globalBundleNo`：单扎。
- `CuttingPlan.barcode`：整床。

#### 第二步：展示详情并选择

单扎场景：

- 展示订单号、款号、颜色、尺码、扎号、件数。
- 展示工序列表。
- 已完成工序灰色不可选，并显示报工人。
- 未完成工序可多选。

整床场景：

- 展示订单号、款号、颜色、尺码、床次、总扎数。
- 展示扎号列表，默认全选，可取消部分扎。
- 展示工序列表，可多选。
- 已经在所选扎中全部完成的工序不可选。
- 部分完成的工序明确提示，避免误扫。

#### 第三步：确认上报

前端调用：

```text
POST /api/mobile/report/submit
```

请求包含：

```ts
interface ReportSubmitRequest {
  scanType: 'BUNDLE' | 'CUTTING'
  code: string
  selectedBundleIds: number[]
  selectedProcessIds: number[]
}
```

成功后展示：

- 工序
- 件数
- 单价
- 金额
- 合计金额
- 被跳过的扎号/工序及原因（如有）

### 我的工资

接口：

```text
GET /api/mobile/report/wage-summary
```

页面展示：

- 今日已赚
- 本月累计
- 按日期分组的计件工资明细

仅展示有效计件工资。管理员清除误扫记录后，工资页重新加载时实时反映扣减。

### 报工记录

接口：

```text
GET /api/mobile/report/records
```

筛选：

- 近 7 天
- 本月
- 自定义日期范围

列表倒序展示：

- 报工时间
- 订单号
- 款号
- 扎号
- 工序
- 件数
- 工价
- 金额

### 管理员端

#### 生产看板

接口：

```text
GET /api/mobile/admin/dashboard
```

展示：

- 日期筛选，默认今天。
- 订单维度：订单号、款号、总扎数、各工序完成进度、整体完成率、瓶颈工序。
- 工人维度：工号、姓名、今日报工件数、今日工资。
- 支持下拉刷新。

#### 异常预警

接口：

```text
GET  /api/mobile/admin/anomalies
POST /api/mobile/admin/anomalies/{id}/ignore
POST /api/mobile/admin/anomalies/{id}/clear
POST /api/mobile/admin/anomalies/{id}/supplement
```

操作：

- 忽略：填写原因。
- 清除：填写原因后清除后置工序报工记录。
- 补录：选择工序、工人、件数、时间后补录。

#### 计时录入

接口：

```text
GET    /api/mobile/admin/timed-wages
POST   /api/mobile/admin/timed-wages
PUT    /api/mobile/admin/timed-wages/{id}
DELETE /api/mobile/admin/timed-wages/{id}
```

字段：

- 工人
- 日期
- 工时
- 时薪
- 工作内容

金额自动按工时 × 时薪计算。

#### 扎号查询

接口：

```text
GET /api/mobile/admin/bundles/search
```

支持扫码或输入扎号，展示扎号详情与工序完成状态，并支持有权限用户清除/补录指定工序。

#### 订单查询

接口：

```text
GET /api/mobile/admin/orders/search
```

输入订单号搜索，展示订单基础信息、明细和整体完成率。

## 扫码数据契约

### 解析扫码内容

```text
POST /api/mobile/report/scan/resolve
```

请求：

```ts
interface ScanResolveRequest {
  code: string
}
```

返回单扎：

```ts
interface BundleScanResult {
  scanType: 'BUNDLE'
  code: string
  bundle: {
    id: number
    globalBundleNo: string
    bundleNo: string
    qty: number
    orderNo: string
    styleNo: string
    color: string
    size: string
  }
  processes: ProcessReportStatus[]
}
```

返回整床：

```ts
interface CuttingScanResult {
  scanType: 'CUTTING'
  code: string
  cutting: {
    id: number
    barcode: string
    bedNo: string
    totalBundleCount: number
    totalQty: number
    orderNo: string
    styleNo: string
    color: string
    size: string
  }
  bundles: Array<{
    id: number
    globalBundleNo: string
    bundleNo: string
    qty: number
    disabled?: boolean
    disabledReason?: string
  }>
  processes: ProcessReportStatus[]
}
```

工序状态：

```ts
interface ProcessReportStatus {
  processId: number
  processName: string
  price: number
  status: 'AVAILABLE' | 'DONE' | 'PARTIAL_DONE' | 'UNAVAILABLE'
  doneBy?: string
  doneTime?: string
  hint?: string
}
```

### 确认上报

```text
POST /api/mobile/report/submit
```

返回：

```ts
interface ReportSubmitResult {
  totalQty: number
  totalAmount: number
  items: Array<{
    processId: number
    processName: string
    qty: number
    price: number
    amount: number
  }>
  skippedItems?: Array<{
    bundleNo: string
    processName: string
    reason: string
  }>
}
```

业务规则：

- 同一扎同一工序不可重复有效报工。
- 同一扎同一工序如已被其他工人报工，不可重复报工。
- 部分可提交时提交可提交项，并返回跳过项。
- 全部不可提交时返回业务错误。
- 后端重新校验所选扎号是否属于扫码解析出的床次。

## 数据流

### 小程序启动

```text
app 启动
→ 读取 token
→ 无 token：进入登录页
→ 有 token：GET /api/mobile/auth/profile
→ 返回用户 + 菜单 + 权限
→ 渲染首页
```

### 扫码报工

```text
扫码/手输
→ scan/resolve
→ 展示扎/床详情
→ 用户选择扎号和工序
→ submit
→ 展示成功汇总
→ 返回扫码页或继续扫码
```

### 管理员操作

```text
进入管理页面
→ 拉取列表/详情
→ 执行忽略/清除/补录/计时录入
→ 后端事务处理
→ 页面刷新
```

## 错误处理

### 小程序端

- 未登录/token 失效：清空本地登录态并跳转登录页。
- 无权限：提示“无权限，请联系管理员”，返回首页。
- 扫码无效：提示“未识别到有效扎号或裁床码”。
- 单扎全部已完成：提示“该扎所选工序已报工”。
- 整床部分已完成：在确认结果中展示跳过项。
- 网络异常：提示“网络异常，请稍后重试”。
- 空数据：使用统一空状态组件。

### 后端

业务异常需要返回明确 message：

- 账号不存在
- 密码错误
- 账号已停用
- 无权限，请联系管理员
- 扎号不存在
- 裁床码无效
- 请先开菲
- 工序不存在
- 工序已报工
- 无可提交报工记录
- 无权限执行该操作

## 测试策略

### 后端测试重点

- 员工工号登录成功/失败。
- 管理员账号密码登录成功/失败。
- 登录后返回菜单权限。
- `globalBundleNo` 解析单扎成功。
- `CuttingPlan.barcode` 解析整床成功。
- 单扎多工序报工成功。
- 同一扎同一工序重复报工被拦截或跳过。
- 整床选择部分扎号、多工序报工成功。
- 工资汇总只统计有效记录。
- 清除报工后工资扣减。
- 补录报工后工资增加。
- 计时工资新增/编辑/删除。

### 小程序测试重点

- 登录页双 Tab。
- 不同权限菜单展示不同入口。
- 无 token 自动跳登录。
- 手动输入兼容现有码。
- 单扎解析、选择工序、提交汇总。
- 整床解析、默认全选扎号、取消部分扎号、提交汇总。
- 工资页和记录页日期筛选。
- 管理员看板、异常处理、计时录入、查询页面基本流程。
- `npm run build:weapp` 构建通过。

## 迁移策略

- 不删除旧接口，新增 `/api/mobile/**`。
- 不改变当前打印码格式。
- 不要求重新打印已存在菲仔。
- 数据库结构尽量复用现有表。
- 如果角色权限结构已存在，优先扩展移动端权限码。
- 先实现登录和菜单权限，再实现员工端扫码闭环，最后实现管理员端。
- 每完成一个闭环后进行后端编译/测试和小程序构建验证。

## 不在本轮范围内

- 工人拍照异常反馈。
- 员工端计时工资展示。
- 移动端工资审核/财务报表。
- 推送通知。
- 离线扫码缓存。
- 多租户切换 UI。
- 重做电脑端页面。

## 验收标准

- 小程序可登录员工和管理员。
- 首页根据角色权限展示菜单。
- 当前已打印菲仔二维码和裁床条形码可被识别。
- 员工可完成单扎/整床扫码报工，并看到成功金额汇总。
- 员工可查看工资和报工记录。
- 管理员可查看生产看板、异常预警、计时录入、扎号查询、订单查询。
- 清除/补录会正确影响报工记录和工资。
- 不破坏现有 web 端报工、裁床、打印逻辑。
