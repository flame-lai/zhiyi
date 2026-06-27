import { defineConfig } from '@tarojs/cli'

// Fix: Taro's tree-shaking doesn't detect Vue 3 + TS component imports,
// causing Input/Button/Textarea templates to be missing from base.wxml.
// Force all component templates to be generated.
try {
  const { componentConfig } = require('@tarojs/webpack5-runner/dist/utils/component')
  componentConfig.includeAll = true
} catch (e) {}

export default defineConfig({
  projectName: 'zhiyi-mini',
  date: '2026-05-30',
  designWidth: 750,
  deviceRatio: {
    640: 2.34 / 2,
    750: 1,
    375: 2,
    828: 1.81 / 2,
  },
  sourceRoot: 'src',
  outputRoot: 'dist',
  plugins: ['@tarojs/plugin-framework-vue3', '@tarojs/plugin-platform-weapp'],
  defineConstants: {},
  copy: {
    patterns: [
      { from: 'src/images', to: 'dist/images' }
    ],
    options: {}
  },
  framework: 'vue3',
  mini: {
    postcss: {
      pxtransform: { enable: true, config: {} },
      url: { enable: true, config: { limit: 1024 } },
      cssModules: { enable: false, config: { namingPattern: 'module', generateScopedName: '[name]__[local]___[hash:base64:5]' } },
    },
  },
  h5: {
    publicPath: '/',
    staticDirectory: 'static',
    postcss: {
      autoprefixer: { enable: true, config: {} },
      cssModules: { enable: false, config: { namingPattern: 'module', generateScopedName: '[name]__[local]___[hash:base64:5]' } },
    },
  },
})
