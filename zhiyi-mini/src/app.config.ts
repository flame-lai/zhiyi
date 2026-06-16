export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/login/index',
    'pages/scan/index',
    'pages/wage/index',
    'pages/report-records/index',
    'pages/admin/dashboard/index',
    'pages/admin/anomaly/index',
    'pages/admin/anomaly-detail/index',
    'pages/admin/timed-wage/index',
    'pages/admin/bundle-query/index',
    'pages/admin/order-query/index',
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#667eea',
    navigationBarTitleText: '智衣ERP',
    navigationBarTextStyle: 'white',
  },
  tabBar: {
    color: '#9ca3af',
    selectedColor: '#667eea',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      { pagePath: 'pages/index/index', text: '首页' },
      { pagePath: 'pages/scan/index', text: '扫码' },
      { pagePath: 'pages/wage/index', text: '我的' },
    ],
  },
})
