cordova.define('cordova/plugin_list', function(require, exports, module) {
  module.exports = [
    {
      "id": "info.androidabcd.plugins.custom.CordovaSensorPlugin",
      "file": "plugins/info.androidabcd.plugins.custom/www/CordovaSensorPlugin.js",
      "pluginId": "info.androidabcd.plugins.custom",
      "clobbers": [
        "cordova.plugins.CordovaSensorPlugin"
      ]
    }
  ];
  module.exports.metadata = {
    "cordova-plugin-whitelist": "1.3.5",
    "info.androidabcd.plugins.custom": "1.0.0"
  };
});