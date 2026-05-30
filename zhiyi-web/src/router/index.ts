import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { noAuth: true },
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { title: '生产看板' },
      },
      // 系统管理
      {
        path: 'system/employees',
        name: 'Employees',
        component: () => import('../views/system/EmployeeList.vue'),
        meta: { title: '员工档案' },
      },
      {
        path: 'system/roles',
        name: 'Roles',
        component: () => import('../views/system/RoleList.vue'),
        meta: { title: '角色管理' },
      },
      {
        path: 'system/menus',
        name: 'Menus',
        component: () => import('../views/system/MenuList.vue'),
        meta: { title: '菜单管理' },
      },
      // 基础配置
      {
        path: 'base/processes',
        name: 'Processes',
        component: () => import('../views/base/ProcessList.vue'),
        meta: { title: '工序库' },
      },
      {
        path: 'base/styles',
        name: 'Styles',
        component: () => import('../views/base/StyleList.vue'),
        meta: { title: '款号管理' },
      },
      {
        path: 'base/customers',
        name: 'Customers',
        component: () => import('../views/base/CustomerList.vue'),
        meta: { title: '客户档案' },
      },
      {
        path: 'base/suppliers',
        name: 'Suppliers',
        component: () => import('../views/base/SupplierList.vue'),
        meta: { title: '供应商档案' },
      },
      // 订单管理
      {
        path: 'order/list',
        name: 'OrderList',
        component: () => import('../views/order/OrderList.vue'),
        meta: { title: '订单列表' },
      },
      {
        path: 'order/detail/:id',
        name: 'OrderDetail',
        component: () => import('../views/order/OrderDetail.vue'),
        meta: { title: '订单详情' },
      },
      {
        path: 'order/shipments',
        name: 'Shipments',
        component: () => import('../views/order/ShipmentList.vue'),
        meta: { title: '出货管理' },
      },
      // 裁床管理
      {
        path: 'cutting/plans',
        name: 'CuttingPlans',
        component: () => import('../views/cutting/CuttingPlanList.vue'),
        meta: { title: '裁床计划' },
      },
      {
        path: 'cutting/bundles',
        name: 'Bundles',
        component: () => import('../views/cutting/BundleList.vue'),
        meta: { title: '扎号管理' },
      },
      // 管理后台
      {
        path: 'admin/anomalies',
        name: 'Anomalies',
        component: () => import('../views/admin/AnomalyList.vue'),
        meta: { title: '异常预警' },
      },
      {
        path: 'admin/timed-wages',
        name: 'TimedWages',
        component: () => import('../views/admin/TimedWageList.vue'),
        meta: { title: '计时工资' },
      },
      {
        path: 'admin/wages',
        name: 'Wages',
        component: () => import('../views/admin/WageList.vue'),
        meta: { title: '工资管理' },
      },
      // 财务管理
      {
        path: 'finance/payments',
        name: 'Payments',
        component: () => import('../views/finance/PaymentList.vue'),
        meta: { title: '收支管理' },
      },
      {
        path: 'finance/profit',
        name: 'Profit',
        component: () => import('../views/finance/ProfitAnalysis.vue'),
        meta: { title: '利润分析' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  if (to.meta.noAuth) {
    next()
    return
  }
  const token = localStorage.getItem('satoken')
  if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
