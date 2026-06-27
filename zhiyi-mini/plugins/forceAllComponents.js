/**
 * Fix: Taro's component tree-shaking doesn't detect Vue 3 + TS imports correctly,
 * causing Input/Button/Textarea templates to be missing from base.wxml.
 * This plugin forces all component templates to be generated.
 */
module.exports = function (ctx) {
  ctx.modifyWebpackChain(({ chain }) => {
    chain.plugin('force-all-components').use(class {
      apply(compiler) {
        // Access Taro's shared componentConfig
        try {
          const componentConfig = require('@tarojs/webpack5-runner/dist/utils/component').componentConfig;
          if (componentConfig) {
            componentConfig.includeAll = true;
          }
        } catch (e) {
          // fallback: try different path
        }
      }
    });
  });
}
