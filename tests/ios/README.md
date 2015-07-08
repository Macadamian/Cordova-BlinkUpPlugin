iOS Unit Tests for BlinkUp Plugin
=========
These tests ensure that the correct error codes, JSON format, and methods are called for various usage scenarios of the plugin on iOS. To test that the plugin and its methods are properly defined in the Javascript, please see the plugin tests that use the cordova-plugin-framework. More information on those tests can be found in the root ReadMe file.

Installation
---------
**Step 1**<br>
Navigate to the current folder in Terminal and install `cordova-ios` with:
```
npm install cordova-ios
```

**Step 2**<br>
Open `tests/ios/Cordova-BlinkUpTests/Cordova-BlinkUpTests.xcodeproj` in Xcode

**Step 3**<br>
Select the Cordova-BlinkUpTests project in Xcode's sidebar, and choose `File > Add Files to Cordova-BlinkUpTests...`. Select the `BlinkUp.embeddedframework` file and make sure that *Copy items if needed* and *Add to targets: Cordova-BlinkUpTest* are both selected

Running the Tests
---------

**Step 1**<br>
From the scheme drop-down menu in Xcode (just to the right of the Stop button), choose "Cordova-BlinkUpTests"

**Step 2**<br>
Click and hold on the `Play` button, and select the `Test` option (icon is a wrench) to run the tests
