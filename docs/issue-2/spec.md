# Spec: Issue #2 — 顶部页签增加「关闭其他」功能

> 来源：Issue #2 / issue agent Spec | 目标分支：main | 优先级：P2

## 目标
Web 端顶部页签栏新增「关闭其他」操作，一键关闭除当前激活页签外的所有页签，当前页保持激活且不重载。

## 范围
- In：`zhiyi-web/src/stores/tabs.ts` 新增 `closeOtherTabs(path)`；`zhiyi-web/src/layouts/MainLayout.vue` 页签栏右侧下拉入口、处理函数与样式。
- Out：「全部关闭」、拖拽排序、页签固定、右键菜单、持久化、后端/移动端改动。

## 关键设计
- 保留依据：以 `path` 精确保留目标页签，目标不存在时 no-op。
- 关闭后不调用 `router.push`，仅更新 store，避免 `keep-alive` 组件重载。
- 页签数 <2 时入口禁用（disabled），保持布局稳定。
- 关闭后由既有 `watch(route.path)` 维持左侧菜单选中态与页签一致。
