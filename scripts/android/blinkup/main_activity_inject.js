module.exports = function(ctx) {
  'use strict';
  // make sure android platform is part of build
  if (ctx.opts.platforms && ctx.opts.platforms.indexOf('android') < 0) {
    return;
  }
  var fs = ctx.requireCordovaModule('fs');
  var path = ctx.requireCordovaModule('path');
  var deferral = ctx.requireCordovaModule('q').defer();
  var matchString1 = 'import org.apache.cordova.*;';
  var replaceString1 =
    'import org.apache.cordova.*;\nimport android.content.Intent;\nimport com.electricimp.blinkup.BlinkupController;'; // jshint ignore:line

  var matchString2 = 'public class MainActivity extends CordovaActivity\n{';
  var replaceString2 = fs.readFileSync(path.join(__dirname, 'main_activity_inject.txt'), 'utf-8');

  var cordovaUtil = ctx.requireCordovaModule('cordova-lib/src/cordova/util');
  var ConfigParser = ctx.requireCordovaModule('cordova-common').ConfigParser;
  var xml = cordovaUtil.projectConfig(ctx.opts.projectRoot);
  var cfg = new ConfigParser(xml);

  var mainActivityPath = 'platforms/android/src/com/macadamian/' + cfg.name() + '/MainActivity.java';
  fs.readFile(path.join(ctx.opts.projectRoot, mainActivityPath),
    'utf-8',
    function(err, data) {
      if (err) {
        return deferral.reject('Read file operation failed');
      }

      if (data.indexOf(replaceString1) !== -1) {
        console.log('CordovaBlinkUp Plugin: MainActivity already injected.');
        return deferral.resolve();
      }

      var result = data.replace(matchString1, replaceString1);
      result = result.replace(matchString2, replaceString2);

      fs.writeFile(path.join(ctx.opts.projectRoot, mainActivityPath),
        result, 'utf-8',
        function(err) {
          if (err) {
            return deferral.reject('Write file operation failed');
          }
          console.log('CordovaBlinkUp Plugin: MainActivity injection successfullly');
          deferral.resolve();
        });
    });
  return deferral.promise;
};
