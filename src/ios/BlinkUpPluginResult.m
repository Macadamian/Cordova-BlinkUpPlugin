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
 * Created by Stuart Douglas (sdouglas@macadamian.com) on June 18, 2015.
 * Copyright (c) 2015 Macadamian. All rights reserved.
 */

#import "BlinkUpPluginResult.h"
#import "BUDeviceInfo+JSON.h"
#import <BlinkUp/BlinkUp.h>

@implementation BlinkUpPluginResult

//=====================================
// JSON keys for results
//=====================================
NSString * const STATE_KEY = @"state";
NSString * const STATUS_CODE_KEY = @"statusCode";

NSString * const ERROR_KEY = @"error";
NSString * const ERROR_TYPE_KEY = @"errorType";
NSString * const ERROR_CODE_KEY = @"errorCode";
NSString * const ERROR_MSG_KEY = @"errorMsg";

NSString * const DEVICE_INFO_KEY = @"deviceInfo";


/********************************************
 * Custom setters for the errors that set
 * related properties as well
 ********************************************/
- (void) setBlinkUpError:(NSError *)error {
    _state = Error;
    _errorType = BlinkUpSDKError;
    _errorCode = error.code;
    _errorMsg = error.localizedDescription;
}
- (void) setPluginError:(NSInteger)errorCode {
    _state = Error;
    _errorType = PluginError;
    _errorCode = errorCode;
}

/********************************************
 * Return necessary info for sending the
 * results back to Cordova
 ********************************************/
- (CDVCommandStatus)getCordovaStatus {
    if (_state == Error) {
        return CDVCommandStatus_ERROR;
    }
    return CDVCommandStatus_OK;
}
- (BOOL) getKeepCallback {
    return (_state == Started);
}

/********************************************
 * Sends a JSON string of the results back
 * to the callback, or if we only have a
 * status string, sends that (not in JSON)
 ********************************************/
- (NSString *) getResultsAsJsonString {

    NSMutableDictionary *resultsDict = [[NSMutableDictionary alloc] init];

    // set our state (never null)
    [resultsDict setObject:[self stateToJsonKey] forKey:STATE_KEY];

    // add error if necessary
    if (_state == Error) {
        [resultsDict setObject:[self generateErrorDict] forKey:ERROR_KEY];
    }

    // completed without error
    else {
        [resultsDict setObject:[@(_statusCode) stringValue] forKey:STATUS_CODE_KEY];
        if (_deviceInfo != nil) {
            [resultsDict setObject:[_deviceInfo toDictionary] forKey:DEVICE_INFO_KEY];
        }
    }

    return [self toJsonString:resultsDict];
}

/********************************************
 * returns dictionary containing error
 ********************************************/
- (NSMutableDictionary *) generateErrorDict {
    NSMutableDictionary *errorDict = [[NSMutableDictionary alloc] init];

    if (_errorType == BlinkUpSDKError) {
        [errorDict setObject:@"blinkup" forKey:ERROR_TYPE_KEY];
        [errorDict setObject:_errorMsg forKey:ERROR_MSG_KEY];
    }
    else {
        [errorDict setObject:@"plugin" forKey:ERROR_TYPE_KEY];
    }
    [errorDict setObject:[@(_errorCode) stringValue] forKey:ERROR_CODE_KEY];

    return  errorDict;
}

/**********************************************
 * returns JSON key corresponding to this.state
 *********************************************/
- (NSString *) stateToJsonKey {
    if (_state == Started) {
        return @"started";
    }
    else if (_state == Completed) {
        return @"completed";
    }
    else {
        return @"error";
    }
}

/********************************************
 * Helper: takes dict and returns JSON string
 ********************************************/
- (NSString *) toJsonString:(NSMutableDictionary *)resultsDict {

    NSError *jsonError;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:resultsDict options:NSJSONWritingPrettyPrinted error:&jsonError];

    if (jsonError != nil) {
        NSLog(@"Error converting to JSON. %@", jsonError.localizedDescription);
        return @"";
    }
    return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
}

@end
