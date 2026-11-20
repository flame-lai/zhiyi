# Plan: Issue #2 — 关闭其他页签

## 实现路径
1. `stores/tabs.ts`：新增并导出 `closeOtherTabs(path: string)`。
   - 查找目标页签；不存在时直接返回。
   - 仅保留目标项，`activeTab` 指向该页签。
2. `layouts/MainLayout.vue`：
   - 页签栏 `.tab-bar` 内、`.tab-list` 之后增加右侧操作区（`a-dropdown` + 触发图标）。
   - 菜单项「关闭其他」，页签数 <2 时禁用。
   - `handleCloseOthers()` 取 `tabStore.activeTab`（空则 `route.path` 兜底），调用 store action，不做导航。
3. 样式：`.tab-list` 可横向滚动并占满可用空间，操作区 `flex-shrink: 0` 靠右。

## 测试 seam
- 仓库未配置前端单测框架，不引入新框架；以 `npm run build`（vue-tsc + vite build）与手工用例作为验收。

## 回归点
单个关闭按钮、切换页签、详情页复用页签（`addTab` 更新已有 path）、左侧菜单选中态。
