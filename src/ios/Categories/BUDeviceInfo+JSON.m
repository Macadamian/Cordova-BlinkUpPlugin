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
 * Created by Daniel Lanthier (dlanthier@macadamian.com) on June 11, 2015.
 * Copyright (c) 2015 Macadamian. All rights reserved.
 */

#import <BlinkUp/BlinkUp.h>
#import "BUDeviceInfo+JSON.h"
#import "BlinkUpPlugin.h"

NSString * const DEVICE_ID_KEY = @"deviceId";
NSString * const PLAN_ID_KEY = @"planId";
NSString * const AGENT_URL_KEY = @"agentURL";
NSString * const VERIFICATION_DATE_KEY = @"verificationDate";

@implementation BUDeviceInfo (JSON)

-(NSDictionary*)toDictionary {
    NSMutableDictionary *deviceInfo = [[NSMutableDictionary alloc] init];
    [deviceInfo setValue:self.planId forKey:PLAN_ID_KEY];
    [deviceInfo setValue:self.deviceId forKey:DEVICE_ID_KEY];
    [deviceInfo setValue:self.agentURL.description forKey:AGENT_URL_KEY];
    [deviceInfo setValue:self.verificationDate.description forKey:VERIFICATION_DATE_KEY];
    return deviceInfo;
}

@end
