/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by Stuart Douglas (sdouglas@macadamian.com) on June 11, 2015.
 * Copyright (c) 2015 Macadamian. All rights reserved.
 */

#import "BlinkUpPlugin.h"
#import "BlinkUpPluginResult.h"
#import <BlinkUp/BlinkUp.h>

NSString * const PLAN_ID_CACHE_KEY = @"planId";

// status codes
typedef NS_ENUM(NSInteger, BlinkUpStatusCodes) {
    DEVICE_CONNECTED              = 0,
    GATHERING_INFO                = 200,
    CLEAR_WIFI_COMPLETE           = 201,
    CLEAR_WIFI_AND_CACHE_COMPLETE = 202
};

// error codes
typedef NS_ENUM(NSInteger, BlinkUpErrorCodes) {
    INVALID_ARGUMENTS   = 100,
    PROCESS_TIMED_OUT   = 101,
    CANCELLED_BY_USER   = 102,
    INVALID_API_KEY     = 103
};

typedef NS_ENUM(NSInteger, BlinkupArguments) {
    BlinkUpArgumentApiKey = 0,
    BlinkUpArgumentDeveloperPlanId,
    BlinkUpArgumentTimeOut,
    BlinkUpArgumentGeneratePlanId
};

@implementation BlinkUpPlugin

/*********************************************************
 * Parses arguments from javascript and displays BlinkUp
 ********************************************************/
- (void)invokeBlinkUp:(CDVInvokedUrlCommand*)command {
    NSLog(@"invokeBlinkUp Started.");

    _callbackId = command.callbackId;

    [self.commandDelegate runInBackground:^{
        _apiKey = [command.arguments objectAtIndex:BlinkUpArgumentApiKey];
        _developerPlanId = [command.arguments objectAtIndex:BlinkUpArgumentDeveloperPlanId];
        _timeoutMs = [[command.arguments objectAtIndex:BlinkUpArgumentTimeOut] integerValue];
        _generatePlanId = [[command.arguments objectAtIndex:BlinkUpArgumentGeneratePlanId] boolValue];

        if ([self sendErrorToCallbackIfArgumentsInvalid]) {
            return;
        }

        NSLog(@"invokeBlinkUp with timeoutMS: %ld", (long)_timeoutMs);

        [self navigateToBlinkUpView];
    }];
}

/*********************************************************
 * Cancels device polling
 ********************************************************/
- (void)abortBlinkUp:(CDVInvokedUrlCommand *)command {
    NSLog(@"abortBlinkUp Started.");

    _callbackId = command.callbackId;

    [self.commandDelegate runInBackground:^{
        [_blinkUpController.devicePoller stopPolling];

        BlinkUpPluginResult *abortResult = [[BlinkUpPluginResult alloc] init];
        abortResult.state = Error;
        [abortResult setPluginError:CANCELLED_BY_USER];

        [self sendResultToCallback:abortResult];
    }];
}

/********************************************************
 * Clears wifi configuration of Imp and cached planId
 ********************************************************/
- (void) clearBlinkUpData:(CDVInvokedUrlCommand *)command {
    NSLog(@"clearBlinkUpData Started.");

    _callbackId = command.callbackId;

    [self.commandDelegate runInBackground:^{
        // clear cached planId
        [[NSUserDefaults standardUserDefaults] setObject:nil forKey:PLAN_ID_CACHE_KEY];

        // create a controller to clear network info
        BUNetworkConfig *clearConfig = [BUNetworkConfig clearNetworkConfig];

        if(_flashController == nil) {
            _flashController = [[BUFlashController alloc] init];
        } else if(_blinkUpController != nil) {
            _flashController = _blinkUpController.flashController;
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            // present the clear device flashing screen
            [_flashController presentFlashWithNetworkConfig:clearConfig configId:nil animated:YES resignActive:
             ^(BOOL willRespond, BUDevicePoller *devicePoller, NSError *error) {
                 [self blinkUpDidComplete:false userDidCancel:false error:nil clearedCache:true];
             }];
        });
    }];
}


/*********************************************************
 * shows default UI for BlinkUp process. Modify this method
 * if you wish to use a custom UI (refer to API docs)
 ********************************************************/
- (void) navigateToBlinkUpView {

    // load cached planID (if not cached yet, BlinkUp automatically generates a new one)
    NSString *planId = [[NSUserDefaults standardUserDefaults] objectForKey:PLAN_ID_CACHE_KEY];

    // If generateNewPlanId is false and a planId is passed from the JS, this will overwrite
    // the planId from the cache with the one passed from JS. This will only occur during debug builds.
    //
    // If the below ifdef is not evaluating to true, make sure that the project's Build Settings "Preprocessor
    // Macros" section contains the following under the Debug section (and ONLY the debug section): DEBUG=1
    //
    // IMPORTANT NOTE: if a developer planId makes it into production, the device will NOT connect.
    // See electricimp.com/docs/manufacturing/planids/ for more info about planIDs
    #ifdef DEBUG
        planId = ([_developerPlanId length] > 0) ? _developerPlanId : nil;
    #endif

    if (_blinkUpController == nil) {
        if (_generatePlanId || planId == nil) {
            _blinkUpController = [[BUBasicController alloc] initWithApiKey:_apiKey];
        }
        else {
            _blinkUpController = [[BUBasicController alloc] initWithApiKey:_apiKey planId:planId];
        }
    }

    dispatch_async(dispatch_get_main_queue(), ^{
        [_blinkUpController presentInterfaceAnimated:YES
            resignActive: ^(BOOL willRespond, BOOL userDidCancel, NSError *error) {
                [self blinkUpDidComplete:willRespond userDidCancel:userDidCancel error:error clearedCache:false];

                // device poller is nil until this block completes, so set its timeout 0.5 seconds from now
                // this is a HACK, need to solve before release
                dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), ^{
                    _blinkUpController.devicePoller.pollTimeout = (_timeoutMs / 1000.0);
                });
            }
            devicePollingDidComplete: ^(BUDeviceInfo *deviceInfo, BOOL timedOut, NSError *error) {
                [self deviceRequestDidCompleteWithDeviceInfo:deviceInfo timedOut:timedOut error:error];
            }
        ];
    });
}


/*********************************************************
 * Called when BlinkUp controller is closed, by user
 * cancelling, flashing process complete, or on error.
 * Sends status back to Cordova app.
 ********************************************************/
- (void) blinkUpDidComplete:(BOOL)willRespond userDidCancel:(BOOL)userDidCancel error:(NSError*)error clearedCache:(BOOL)clearedCache {

    NSLog(@"blinkUpDidComplete Started. willRespond: %d userDidCancel: %d, Error: %@",
          willRespond, userDidCancel, error);

    BlinkUpPluginResult *pluginResult = [[BlinkUpPluginResult alloc] init];

    if (willRespond) {
        pluginResult.state = Started;
        pluginResult.statusCode = GATHERING_INFO;
    }
    else if (userDidCancel) {
        pluginResult.state = Error;
        [pluginResult setPluginError:CANCELLED_BY_USER];
    }
    else if (error != nil) {
        pluginResult.state = Error;
        [pluginResult setBlinkUpError: error];
    }
    else {
        pluginResult.state = Completed;
        if (clearedCache) {
            pluginResult.statusCode = CLEAR_WIFI_AND_CACHE_COMPLETE;
        }
        else {
            pluginResult.statusCode = CLEAR_WIFI_COMPLETE;
        }
    }

    [self sendResultToCallback:pluginResult];
}


/*********************************************************
 * Called when device info has been loaded from Electric
 * Imp server, or when that request timed out.
 * Sends device info and status back to Cordova app.
 ********************************************************/
- (void) deviceRequestDidCompleteWithDeviceInfo:(BUDeviceInfo*)deviceInfo timedOut:(BOOL)timedOut error:(NSError*)error {

    NSLog(@"deviceRequestDidComplete Started. DeviceInfo: %@ TimedOut?: %d, Error: %@",
          deviceInfo, timedOut, error);

    BlinkUpPluginResult *pluginResult = [[BlinkUpPluginResult alloc] init];

    if (timedOut) {
        pluginResult.state = Error;
        [pluginResult setPluginError:PROCESS_TIMED_OUT];
    }
    else if (error != nil) {
        pluginResult.state = Error;
        [pluginResult setBlinkUpError:error];
    }
    else {
        // cache plan ID if it's not development ID (see electricimp.com/docs/manufacturing/planids/)
        if (![deviceInfo.planId isEqual: _developerPlanId]) {
            [[NSUserDefaults standardUserDefaults] setObject:deviceInfo.planId forKey:PLAN_ID_CACHE_KEY];
        }

        pluginResult.state = Completed;
        pluginResult.statusCode = DEVICE_CONNECTED;
        pluginResult.deviceInfo = deviceInfo;
    }

    [self sendResultToCallback:pluginResult];
}

/*********************************************************
 * Sends error to callback if arguments don't have correct
 * type, or if apiKey is invalid format.
 * @return YES if error was sent, NO otherwise
 ********************************************************/
- (BOOL) sendErrorToCallbackIfArgumentsInvalid {

    BOOL invalidArguments = (self.timeoutMs == 0);
    BOOL invalidApiKey = ![BlinkUpPlugin isApiKeyFormatValid:self.apiKey];

    // send error to callback
    if (invalidArguments || invalidApiKey) {
        BlinkUpPluginResult *pluginResult = [[BlinkUpPluginResult alloc] init];
        pluginResult.state = Error;

        [pluginResult setPluginError:(invalidApiKey ? INVALID_API_KEY : INVALID_ARGUMENTS)];

        [self sendResultToCallback:pluginResult];
    }

    return (invalidArguments || invalidApiKey);
}

/*********************************************************
 * Creates a cordova plugin result from pluginResult with
 * correct settings and sends to callback
 ********************************************************/
- (void) sendResultToCallback:(BlinkUpPluginResult *)pluginResult {
    CDVPluginResult *cordovaResult = [CDVPluginResult resultWithStatus:[pluginResult getCordovaStatus] messageAsString:[pluginResult getResultsAsJsonString]];
    [cordovaResult setKeepCallbackAsBool: [pluginResult getKeepCallback]];
    [self.commandDelegate sendPluginResult:cordovaResult callbackId:_callbackId];
}

/*********************************************************
 * Returns true iff api key is 32 alphanumeric characters
 ********************************************************/
+ (BOOL) isApiKeyFormatValid: (NSString *)apiKey {
    if (apiKey == nil || apiKey.length != 32) {
        return NO;
    }

    // must be only alphanumeric characters
    NSCharacterSet *alphaSet = [NSCharacterSet alphanumericCharacterSet];
    return ([[apiKey stringByTrimmingCharactersInSet:alphaSet] length] == 0);
}

@end
