export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/login/index',
    'pages/scan/index',
    'pages/wage/index',
    'pages/admin/dashboard/index',
    'pages/admin/anomaly/index',
    'pages/admin/timed-wage/index',
    'pages/admin/query/index',
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#4A6FA5',
    navigationBarTitleText: '智衣ERP',
    navigationBarTextStyle: 'white',
  },
  tabBar: {
    color: '#999999',
    selectedColor: '#4A6FA5',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '首页',
      },
      {
        pagePath: 'pages/scan/index',
        text: '扫码报工',
      },
      {
        pagePath: 'pages/wage/index',
        text: '我的工资',
      },
    ],
  },
})
