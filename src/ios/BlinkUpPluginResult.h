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

// see README.md for format of JSON string to be sent to callback

#import <Cordova/CDV.h>

@class BUDeviceInfo;

@interface BlinkUpPluginResult : NSObject

typedef NS_ENUM(NSInteger, BlinkUpPluginState) {
    Started,
    Completed,
    Error
};
typedef NS_ENUM(NSInteger, BlinkUpErrorType) {
    BlinkUpSDKError,
    PluginError
};

//*************************************
// Public methods
//*************************************
- (void) setBlinkUpError:(NSError *)error;
- (void) setPluginError:(NSInteger)errorCode;
- (NSString *)getResultsAsJsonString;
- (CDVCommandStatus) getCordovaStatus;
- (BOOL) getKeepCallback;

//=====================================
// BlinkUp result
//=====================================
@property BlinkUpPluginState state;
@property NSInteger statusCode;
@property BlinkUpErrorType errorType;
@property NSInteger errorCode;
@property NSString *errorMsg;
@property BUDeviceInfo *deviceInfo;

@end
