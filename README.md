Installation
==============

**STEP 1**<br>
Navigate to your project directory and install the plugin with `cordova plugin add /path/to/plugin`. Add both platforms if you haven't already with `cordova platform add ios` and `cordova platform add android`.

iOS
--------------
Open `/path/to/project/platforms/ios/<ProjectName>.xcodeproj` in Xcode and choose File > Add Files to Project. Select the `BlinkUp.framework` file given to you by Electric Imp, and ensure that both "*Copy items if needed*" and "*Add to targets: <ProjectName>*" are selected. Do the same for `BlinkUp.framework/resources/versions/A/BlinkUp.bundle`.


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
```
fs.writeFileSync(path.join(projectPath, 'settings.gradle'),
    '// GENERATED FILE - DO NOT EDIT\n' +
    'include ":"\n' +
    'include ":blinkup_sdk"\n' +
    'include "' + subProjectsAsGradlePaths.join('"\ninclude "') + '"\n');
```

**STEP 3**<br>
Open `MainActivity.java`. If your project is *com.company.project* then it's located in `platforms/android/src/com/company/project`. Add the following imports:
```
import android.content.Intent;
import com.electricimp.blinkup.BlinkupController;
```
And the following method:
```
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
There are three calls from the plugin exposed to the javascript. All take success and failure callbacks as arguments. See the "Callbacks" section below for more information.

**invokeBlinkUp(apiKey, planId, timeoutMs, generatePlanId, success, failure)**<br>
Presents the native BlinkUp interface, where user can input wifi info and connect to the Imp.<br>
`apiKey` *string*: you must enter your apiKey or the plugin won't function.<br>
`planId` *string*: optional, see "Testing the Plugin" below.<br>
`timeoutMs` *integer*: how long to wait for device info from servers, default is 30000 (1 min).<br>
`generatePlanId` *boolean*: Set to true if you want to generate a new plan ID every BlinkUp. See https://electricimp.com/docs/manufacturing/planids/ for more info about plan IDs.<br>

**abortBlinkUp(success, failure)**<br>
Cancels server polling for device info if in progress. 

**clearWifiAndCache(success, failure)**<br>
Immediately initiates the BlinkUp flashing process that will clear the imp's wifi info. Also clears the cached planId if there is one.

Callbacks
----------
It is recommended to use the same function as the success callback and failure callback, as the JSON parsing will be common to both. See the "JSON format" section for information regarding the JSON sent back to the javascript.

An example callback function is below, where `errorForCode` and `statusForCode` are functions you must define that map error codes and status codes to their respective messages.
```
var callback = function (message) {
    try {
        var jsonData = JSON.parse("(" + message + ")"); 
        
        if (jsonData.sate == "error") {
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
-----------
If you are testing devices for development, you can input your own development planID to see the Imps in the Electric Imp IDE. Just set it in the `index.js` files when performing the above step, and make sure you pass *false* for `generatePlanId`.<br>

IMPORTANT NOTE: if a development planID makes it into production, the consumer's device will not configure. Please read http://electricimp.com/docs/manufacturing/planids/ for more info.

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
[2] - Status of plugin. Null if state is "error". See "Status Codes" below for status codes<br>
[3] - Stores error information if state is "error". Null if state is "started" or "completed"<br>
[4] - If error sent from SDK, "blinkup". If error handled within native code of plugin, "plugin"<br>
[5] - BlinkUp SDK error code if errorType is "blinkup". Custom error code if "plugin". See "Error Codes" below for custom error codes.<br>
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
300 - "Invalid API key. You must set your BlinkUp API key in index.js." 
301 - "Could not verify API key with Electric Imp servers."
302 - "Error generating JSON string."
```
