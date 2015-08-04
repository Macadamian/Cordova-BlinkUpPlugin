Introduction
==============

This Cordova / Phonegap plugin allows you to easily integrate the native BlinkUp process to connect an Electric Imp device to the internet in your app. Note that because the BlinkUp SDK is private, you will need to add it to your project after installing the plugin. If you do not have access to the BlinkUp SDK, you may contact Electric Imp at sales@electricimp.com.

A sample Cordova app that demonstrates how to integrate the plugin can be found at https://github.com/Macadamian/Cordova-BlinkUpSample.

## Table of Contents

**[Installation](#installation)**<br>
&nbsp;&nbsp;&nbsp;&nbsp;[iOS](#ios)<br>
&nbsp;&nbsp;&nbsp;&nbsp;[Android](#android)<br>
**[Using the Plugin](#using-the-plugin)**<br>
&nbsp;&nbsp;&nbsp;&nbsp;[API Calls](#api-calls)<br>
&nbsp;&nbsp;&nbsp;&nbsp;[Callbacks](#callbacks)<br>
**[Testing the Plugin](#testing-the-plugin)**<br>
&nbsp;&nbsp;&nbsp;&nbsp;[Developer Plan ID's](#developer-plan-ids)<br>
&nbsp;&nbsp;&nbsp;&nbsp;[Cordova Unit Tests](#cordova-unit-tests)<br>
&nbsp;&nbsp;&nbsp;&nbsp;[iOS Unit Tests](#ios-unit-tests)<br>
**[JSON Format](#json-format)**<br>
&nbsp;&nbsp;&nbsp;&nbsp;[Status Codes](#status-codes)<br>
&nbsp;&nbsp;&nbsp;&nbsp;[Error Codes](#error-codes)<br>
**[Troubleshooting](#troubleshooting)**

Installation
==============
Navigate to your project directory and install the plugin with `cordova plugin add https://github.com/Macadamian/Cordova-BlinkUpPlugin.git`. Add both platforms if you haven't already with `cordova platform add ios` and `cordova platform add android`.

iOS
--------------
**STEP 1**<br>
Open `/path/to/project/platforms/ios/<ProjectName>.xcodeproj` in Xcode, select the "Frameworks" group and choose File > Add Files to \<ProjectName\>. Select the `BlinkUp.embeddedframework` file given to you by Electric Imp, and ensure that both "*Copy items if needed*" and "*Add to targets: \<ProjectName\>*" are selected. 

Expand the `BlinkUp.embeddedframework` you just added to Frameworks, and drag the `BlinkUp.framework` file  to `Link Binary with Libraries`, and `BlinkUp.bundle` (in BlinkUp.embeddedframework/Resources) to the `Copy Bundle Resources` in the project's `Build Phases`.

**STEP 2**<br>
Go to the project's Build Setting in Xcode, and in the `Apple LLVM - Preprocessing` section expand the "Preprocessor Macros" setting. Add the following to "Debug" (and only Debug!):
```
DEBUG=1
```

Android
--------------
**STEP 1**<br>
Copy the `blinkup_sdk` folder from the SDK package given to you by Electric Imp to `/path/to/project/platforms/android/`.

**STEP 2**<br>
Open `path/to/project/platforms/android/cordova/lib/build.js` and add the following line to the `fs.writeFileSync(path.join(projectPath, 'settings.gradle')` function (line 251):
```
'include ":blinkup_sdk"\n' +
```
It should now look like this:
```javascript
fs.writeFileSync(path.join(projectPath, 'settings.gradle'),
    '// GENERATED FILE - DO NOT EDIT\n' +
    'include ":"\n' +
    'include ":blinkup_sdk"\n' +
    'include "' + subProjectsAsGradlePaths.join('"\ninclude "') + '"\n');
```

**STEP 3**<br>
Open `MainActivity.java`. If your project is *com.company.project* then it's located in `platforms/android/src/com/company/project`. Add the following imports:

```java
import android.content.Intent;
import com.electricimp.blinkup.BlinkupController;
```

And the following method:

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    BlinkupController.getInstance().handleActivityResult(this, requestCode, resultCode, intent);
}
```
If you do not do this step, the BlinkUp controller will still function properly, but you will not receive the infomation passed to the callback (the status, device ID, agent URL etc).

**STEP 4**<br>
Navigate to your project root directory and run `cordova build android`. You only need to do this once, then you can run the project directly from Android Studio. To use Android Studio, select "Open Existing Project" and select the `path/to/project/platforms/android` folder. Press OK when prompted to generate a Gradle wrapper.

Using the Plugin
==========
When you are adding calls to the plugin in your javascript note that you must update `www/js/index.js`, `platforms/ios/www/js/index.js`, and `platforms/android/assets/www/js/index.js`. If you are making frequent changes, you may want to include a build step that copies the root `www` files to the platform-specific folders.

API Calls
----------
There are three calls from the plugin exposed to the javascript through the `blinkup` interface. For example, to show a BlinkUp you would call `blinkup.invokeBlinkUp(...);`. 

All calls take success and failure callbacks as arguments. See the "Callbacks" section below for more information.

**invokeBlinkUp(apiKey, planId, timeoutMs, generateNewPlanId, success, failure)**<br>
Presents the native BlinkUp interface, where user can input wifi info and connect to the Imp.<br>
`apiKey` *string*: you must enter your apiKey or the plugin won't function.<br>
`developmentPlanId` *string, default=""*: **IMPORTANT** - you must read "[Testing the Plugin](#testing-the-plugin)" before setting this value. Failure to do so can prevent users from connecting to wifi.<br>
`timeoutMs` *integer, default=30000*: how long to wait for device info from servers.<br>
`generateNewPlanId` *boolean, default=false*: Set to true if you want to generate a new plan ID every BlinkUp. See https://electricimp.com/docs/manufacturing/planids/ for more info.<br>

**abortBlinkUp(success, failure)**<br>
Cancels server polling for device info if in progress. 

**clearBlinkUpData(success, failure)**<br>
Immediately initiates the BlinkUp flashing process that will clear the imp's wifi info. Also clears the cached planId if there is one.

Callbacks
----------
It is recommended to use the same function as the success callback and failure callback, as the JSON parsing will be common to both. See the "JSON format" section for information regarding the JSON sent back to the javascript.

An example callback function is below, where `errorForCode` and `statusForCode` are functions you must define that map [error codes](#error-codes) and [status codes](#status-codes) to their respective messages.
```javascript
var callback = function (message) {
    try {
        var jsonData = JSON.parse("(" + message + ")"); 
        
        if (jsonData.state == "error") {
            if (jsonData.error.errorType == "blinkup") {
                var statusMsg = jsonData.error.errorMsg;
            } else {
                var statusMsg = errorForCode(jsonData.error.errorCode);
            }
        } else if (jsonData.state == "completed") {
            var statusMsg = statusForCode(jsonData.statusCode);
            if (jsonData.statusCode == "0") {
                var planId = jsonData.deviceInfo.planId;
                var deviceId = jsonData.deviceInfo.deviceId;
                var agentURL = jsonData.deviceInfo.agentURL;
                var verificationDate = jsonData.deviceInfo.verificationDate;
            }
        }
        
        // update ui here
        
        if (jsonData.state == "started") {
            // show progress indicator and abort button
        } else {
            // hide progress indicator and abort button
        }
    } catch (exception) {
        console.log("Error parsing json. " + exception);
    }
};
```

Testing the Plugin
===========
Developer Plan IDs
-----------
If you are testing devices for development, you can input your own development planID to see the Imps in the Electric Imp IDE. Just set it in the `index.js` files when making a call to `invokeBlinkUp` and ensure you pass *false* for `generateNewPlanId`. The development plan ID you set will override a cached plan ID (if there is one) if `generateNewPlanId` is false. If `generateNewPlanId` is true however, a new plan ID will be generated every BlinkUp and the development plan ID will be ignored.

When you pass in a development plan ID, the plugin will not cache it. Caching is only done on production plan ID's, and is used to save user settings across BlinkUp's (e.g. when they change their wifi password).

IMPORTANT NOTE: if a development plan ID makes it into production, the consumer's device will not configure, and will be unable to connect to wifi. There is a check in the native code on each platform which will ignore a development plan ID if the build configuration is set to release, but it is best to remove all references to the plan ID and pass an empty string from the Javascript when you're done debugging. Please read http://electricimp.com/docs/manufacturing/planids/ for more info.

Cordova Unit Tests
-----------
There are a set of unit tests for the BlinkUp plugin based on the `cordova-plugin-test-framework` that test if the BlinkUp interface is properly defined in the Javascript. If you wish to use the tests, do the following steps from your Cordova project directory.

**Step 1**<br>
Install the test framework with `cordova plugin add http://git-wip-us.apache.org/repos/asf/cordova-plugin-test-framework.git` 

**Step 2**<br>
Install the BlinkUp plugin tests with `cordova plugin add https://github.com/Macadamian/Cordova-BlinkUpPlugin.git#:/tests`

**Step 3**<br>
Open `config.xml` and change `<content src="index.html"/>` to `<content src="cdvtests/index.html"/>` or navigate to `cdvtests/index.html` from your app.

For more info about testing Cordova plugins, please visit [the test framework repo](https://github.com/apache/cordova-plugin-test-framework).

iOS Unit Tests
-----------
As well as the Cordova tests that ensure the plugin's interface is properly defined in Javascript, there are also native iOS tests to make sure that the correct error codes, status codes, and JSON format are received for different BlinkUp scenarios. Please see the ReadMe in `tests/ios` for more information.

JSON Format
===========
The plugin will return a JSON string in the following format. Footnotes in square brackets.
```
{
    "state": "started" | "completed" | "error", [1]
    "statusCode": "",                           [2]
    "error": {                                  [3]
        "errorType": "plugin" | "blinkup",      [4]
        "errorCode": "",                        [5]
        "errorMsg": ""                          [6]
    },
    "deviceInfo": {                             [7]
        "deviceId": "",
        "planId": "",
        "agentURL": "",
        "verificationDate": ""
    }
 }
```
[1] - *started*: flashing process has finished, waiting for device info from Electric Imp servers<br>
*completed*: Plugin done executing. This could be a clear-wifi completed or device info from servers has arrived<br>
[2] - Status of plugin. Null if state is "error". See [Status Codes](#status-codes) below for status codes<br>
[3] - Stores error information if state is "error". Null if state is "started" or "completed"<br>
[4] - If error sent from SDK, "blinkup". If error handled within native code of plugin, "plugin"<br>
[5] - BlinkUp SDK error code if errorType is "blinkup". Custom error code if "plugin". See [Error Codes](#error-codes) below for custom error codes.<br>
[6] - If errorType is "blinkup", error message from BlinkUp SDK. Null if errorType "plugin"<br>
[7] - Stores the deviceInfo from the Electric Imp servers. Null if state is "started" or "error"

Status Codes
-----------
These codes can be used to debug your application, or to present the users an appropriate message on success.
```
0   - "Device Connected"
200 - "Gathering device info..."
201 - "Wireless configuration cleared."
202 - "Wireless configuration and cached Plan ID cleared."
```

Error Codes
----------
IMPORTANT NOTE: the following codes apply ONLY if `errorType` is "plugin". Errors from the BlinkUp SDK will have their own error codes (which may overlap with those below). If `errorType` is "blinkup", you must use the `errorMsg` field instead. The errors in the 300's range are android only.
```
100 - "Invalid arguments in call to invokeBlinkUp."
101 - "Could not gather device info. Process timed out."
102 - "Process cancelled by user."
103 - "Invalid API key. You must set your BlinkUp API key in index.js." 
301 - "Could not verify API key with Electric Imp servers."
302 - "Error generating JSON string."
```

Troubleshooting
==========
###iOS
**BlinkUp/BlinkUp.h cannot be found**
- `BlinkUp.embeddedframework` is not in `path/to/project/platforms/ios/`
- `BlinkUp.framework` is not in the project's "Link binary with libraries" build phase
- "Framework Search Paths" in the project's build settings does not include `$(PROJECT_DIR)/BlinkUp.embeddedframework`

###Android
**Project with path "blinkup_sdk" could not be found**
- The `blinkup_sdk` folder is not in `path/to/project/platforms/android/`
- The `build.js` file was not updated as outlined in [installation](#android)
- `cordova build android` was not run after updating the `build.js` file

###BlinkUp
**BlinkUp process times out**
- Lighting significantly affects the BlinkUp process. It doesn't need to be pitch black to connect, but try to find somewhere out of the way of any direct light sources
- The network name and password are incorrect
- The Imp was moved, or was not pressed right up against the phone for the duration of the BlinkUp

**Imp is not lit up, and won't react to the BlinkUp process**
- The USB power cable is not connected to the Imp, or to a power source
- Sometimes you need to unplug and replug in the power cable to "wake" the Imp up. This should get it to start flashing, and ready to recognize a BlinkUp

**Javascript gives "blinkup not defined"**
- There is a typo in the function being called, or it is not one of the exposed functions outlined in [api calls](#api-calls)
- The function being called is not called on a `blinkup` object, as discussed in [api calls](#api-calls)
