# 制衣厂ERP系统 - 实现设计文档

## 概述

基于已完成的技术方案文档和需求文档，采用逐层开发方式实现完整的制衣厂ERP系统。

## 技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 实现方式 | 逐层开发（后端→API文档→前端→移动端→部署） | 符合依赖关系，API文档作为层间契约 |
| Java版本 | Java 17 | 技术方案指定，LTS稳定版 |
| 构建工具 | Maven | Spring Boot标准，IDE支持好 |
| 前端主题 | Steel Blue (#4A6FA5) | 低饱和度蓝灰，低调简约 |
| 移动端 | Taro 3 + Vue 3 微信小程序 | 技术方案指定 |

## 开发阶段

### 阶段1：后端基础设施

**目标**：搭建Spring Boot项目骨架，完成所有基础配置。

**内容**：
1. Maven项目初始化（pom.xml，依赖管理）
2. 数据库DDL执行（全部20+张表）
3. MyBatis-Plus配置（多租户插件、分页插件、逻辑删除）
4. Sa-Token认证配置
5. 统一响应体 `R<T>` + `PageResult<T>`
6. 全局异常处理 `GlobalExceptionHandler`
7. 租户拦截器 `TenantInterceptor` + `TenantContext`
8. 跨域配置、WebMvc配置
9. application.yml 多环境配置（dev/prod）

### 阶段2：后端业务模块

按依赖顺序实现7个业务模块：

#### 2.1 系统管理模块 (system)
- `AuthController`：登录/登出/获取菜单
- `EmployeeController`：员工CRUD、状态切换、重置登录码/密码
- `RoleController`：角色CRUD
- `MenuController`：菜单CRUD（树形）
- 权限校验：`@SaCheckPermission` 注解

#### 2.2 基础配置模块 (base)
- `ProcessController`：工序CRUD、分类管理、拖拽排序
- `StyleController`：款号CRUD、标准工序路由、标准物料清单
- `CustomerController`：客户CRUD
- `SupplierController`：供应商CRUD
- 编号自动生成：工号A001、客户C001、供应商S001

#### 2.3 订单管理模块 (order)
- `OrderController`：订单CRUD、状态流转、确认/结算
- `ShipmentController`：出货记录CRUD
- 订单明细管理（颜色尺码矩阵）
- 订单工序定价（从款号模板带出，可调整）
- 订单物料清单（从款号模板带出，可调整）
- 订单状态自动判断逻辑

#### 2.4 裁床开菲模块 (cutting)
- `CuttingPlanController`：裁床计划CRUD、录入实际数、开菲
- `BundleController`：扎号列表、详情、状态管理
- `PrintController`：打印数据接口（裁床条形码、菲仔标签）
- 自动分扎逻辑
- 裁床条形码生成
- QR码数据生成

#### 2.5 报工模块 (report)
- `ScanReportController`：扫码报工（扎号QR码+裁床条形码）
- `WageController`：工人查工资（今日/本月汇总+明细）
- 断层检测逻辑
- QR码失效/复活机制

#### 2.6 管理后台模块 (admin)
- `DashboardController`：生产看板（订单维度+工人维度）
- `AnomalyController`：断层预警列表、处理（忽略/清除/补录）
- `TimedWageController`：计时工资CRUD、批量新增
- `WageManageController`：工资汇总、审核、其他项目管理、导出

#### 2.7 财务模块 (finance)
- `PaymentController`：收支记录CRUD
- `ProfitController`：利润分析（按订单/月/季度）
- `ReportExportController`：财务报表导出
- 订单成本核算（计件自动+计时关联）
- 订单收入管理（收款记录、应收账款）
- 工资结算（已审核→已发放）

### 阶段3：API文档

- 集成 SpringDoc OpenAPI (Swagger UI)
- 所有Controller添加 `@Tag`、`@Operation` 注解
- 所有DTO添加 `@Schema` 注解
- 输出完整的API文档（可在线浏览+可导出）

### 阶段4：Web前端

#### 4.1 项目初始化
- Vite + Vue 3 + TypeScript
- Ant Design Vue 4.x
- Steel Blue主题配置
- Axios请求封装
- Pinia状态管理
- 路由配置（静态+动态）

#### 4.2 页面实现（按模块）
1. 登录页
2. 系统管理（菜单管理、角色管理、员工档案）
3. 基础配置（工序库、款号管理、客户档案、供应商档案）
4. 订单管理（订单列表、订单详情、出货管理）
5. 裁床管理（裁床计划、扎号管理、菲仔打印）
6. 管理后台（生产看板、异常处理、计时工资、工资管理、报表中心）
7. 财务管理（成本核算、收入管理、利润分析、工资结算、财务报表）

#### 4.3 公共组件
- 权限指令 `v-permission`
- 打印标签组件（菲仔标签渲染+打印）
- 条形码/二维码组件
- Excel导出工具

### 阶段5：移动端（Taro 3 + Vue 3）

#### 5.1 员工端
- 登录页（工号登录）
- 扫码报工（扎号QR码+裁床条形码）
- 我的工资（今日/本月汇总+明细）
- 报工记录

#### 5.2 管理员端
- 登录页（账号+密码）
- 生产看板
- 异常预警
- 计时工资录入
- 扎号查询
- 订单查询

### 阶段6：部署文档

- Docker Compose 完整配置
- Nginx 反向代理配置
- SSL证书配置说明
- 数据库初始化脚本
- 环境变量说明
- 部署步骤文档

## 前端主题规范

```
主色调：#4A6FA5（Steel Blue）
主色浅：#6B8FC4（hover状态）
主色深：#3A5F95（active状态）
背景色：#F5F7FA
白色：#FFFFFF
文字主色：#333333
文字次色：#666666
文字弱色：#999999
边框色：#E8E8E8
成功色：#52C41A
警告色：#FAAD14
错误色：#FF4D4F
信息色：#4A6FA5

卡片样式：白色背景 + box-shadow: 0 1px 4px rgba(0,0,0,0.08) + border-radius: 6px
按钮样式：实色填充，无渐变，border-radius: 4px
侧边栏：白色背景，左侧3px主色边框
顶栏：白色背景，底部1px边框
```

## 关键实现约定

1. 多租户：所有业务表含 `factory_id`，MyBatis-Plus自动追加
2. 金额：DECIMAL(12,2)，前端保留2位小数
3. 逻辑删除：`deleted` 字段，0/1
4. 统一响应：`R<T>` 包装
5. RESTful：路径前缀按模块划分
6. 权限：动态路由 + v-permission指令
7. 打印：前端HTML渲染 + window.print()
8. 编号规则：工号A001、订单号ORD+日期+序号、客户C001、供应商S001、扎号床次-序号
