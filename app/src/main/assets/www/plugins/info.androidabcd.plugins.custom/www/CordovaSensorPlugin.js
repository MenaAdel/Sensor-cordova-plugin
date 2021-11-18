cordova.define("info.androidabcd.plugins.custom.CordovaSensorPlugin", function(require, exports, module) {
var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'CordovaSensorPlugin', 'coolMethod', [arg0]);
};

});
