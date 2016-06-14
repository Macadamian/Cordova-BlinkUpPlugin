module.exports = function(ctx) {
  'use strict';
  // make sure android platform is part of build
  if (ctx.opts.platforms && ctx.opts.platforms.indexOf('android') < 0) {
    return;
  }
  var fs = ctx.requireCordovaModule('fs');
  var path = ctx.requireCordovaModule('path');
  var deferral = ctx.requireCordovaModule('q').defer();
  var jsonFile = JSON.parse(fs.readFileSync(path.join(__dirname, 'manifest_permission_inject.json')));
  var injectPermissions = jsonFile.basePermission.concat(jsonFile.addPermissions);

  var manifestPath = 'platforms/android/AndroidManifest.xml';

  fs.readFile(path.join(ctx.opts.projectRoot, manifestPath),
    'utf-8',
    function(err, data) {
      if (err) {
        return deferral.reject('CordovaBlinkUp Plugin: Read file operation failed for ' + manifestPath);
      }

      if (data.indexOf(jsonFile.addPermissions[0]) !== -1) {
        console.log('CordovaBlinkUp Plugin: AndroidManifest already injected');
        return deferral.resolve();
      }

      var result = data.replace(jsonFile.basePermission, injectPermissions.join('\n\t\t'));

      fs.writeFile(path.join(ctx.opts.projectRoot,
        'platforms/android/AndroidManifest.xml'), result, 'utf-8', function(err) {
        if (err) {
          return deferral.reject('Write file operation failed');
        }
        console.log('CordovaBlinkUp Plugin: AndroidManifest injected successfullly');
        deferral.resolve();
      });
  });
  return deferral.promise;
};
