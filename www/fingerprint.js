
cordova.define("cordova/plugin/Fingerprint", function(require, exports, module) {
    var exec = require('cordova/exec');
    var Fingerprint = function() {};
	Fingerprint.prototype.isAvailable = function (successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "Fingerprint", "isAvailable", []);
	};
	Fingerprint.prototype.verifyFingerprint = function (successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "Fingerprint", "verifyFingerprint", []);
	};
    var myplugin = new Fingerprint();
    module.exports = myplugin;
});
