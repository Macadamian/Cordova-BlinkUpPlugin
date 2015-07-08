//
//  BlinkUpTest.m
//  Cordova-BlinkUpTest
//
//  Created by Stuart Douglas on 2015-07-08.
//  Copyright (c) 2015 Macadamian. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <XCTest/XCTest.h>

#import <BlinkUp/BlinkUp.h>
#import "BlinkUpPlugin.h"
#import "BUDeviceInfo+JSON.h"
#import "BlinkUpPluginResult.h"

//===========================================
// We need to access private methods, so make
// categories that declare them
//===========================================
@interface BlinkUpPlugin (Testing)
- (BOOL)isApiKeyFormatValid;
- (void)navigateToBlinkUpView;
- (void) deviceRequestDidCompleteWithDeviceInfo:(BUDeviceInfo*)deviceInfo timedOut:(BOOL)timedOut error:(NSError*)error;
@end

@interface BlinkUpPluginResult (Testing)
- (NSString *)toJsonString:(NSMutableDictionary *)resultsDict;
@end
//===========================================


@interface BlinkUpTest : XCTestCase
@end

@implementation BlinkUpTest

//===========================================
// Private instance variables
//===========================================
BUDeviceInfo *deviceInfo;


/********************************************
 * Initialize tester values
 ********************************************/
- (void)setUp {
    [super setUp];
    deviceInfo = [[BUDeviceInfo alloc] init];
    deviceInfo.deviceId = @"123456789";;
    deviceInfo.planId = @"987654321";
    deviceInfo.agentURL = [[NSURL alloc] initWithString:@"www.agenturl.com"];
    deviceInfo.verificationDate = [[NSDate alloc] initWithTimeIntervalSince1970:0];
}
- (void)tearDown {
    [super tearDown];
}

/********************************************
 * Makes sure that BlinkUpPlugin's method
 * for validating api keys works as expected
 ********************************************/
- (void)testApiKeyFormatCheck {
    BlinkUpPlugin *plugin = [[BlinkUpPlugin alloc] init];

    // api key is empty
    plugin.apiKey = @"";
    XCTAssert(![plugin isApiKeyFormatValid], @"Empty api key invalid");
    
    // not 32 characters
    plugin.apiKey = @"abcdefghijklmnopqrstuvwxyz";
    XCTAssert(![plugin isApiKeyFormatValid], @"Non-32 character api key invalid");

    plugin.apiKey = @"abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
    XCTAssert(![plugin isApiKeyFormatValid], @"Non-32 character api key invalid");

    // not alpha-numeric
    plugin.apiKey = @"*bcdefghijklmnopqrstuvwxyz123456";
    XCTAssert(![plugin isApiKeyFormatValid], @"Non-alphanumeric api key invalid, first char not alphanumeric");
   
    plugin.apiKey = @"abcdefghijklmn*pqrstuvwxyz123456";
    XCTAssert(![plugin isApiKeyFormatValid], @"Non-alphanumeric api key invalid, middle char not alphanumeric");
    
    plugin.apiKey = @"abcdefghijklmnopqrstuvwxyz12345*";
    XCTAssert(![plugin isApiKeyFormatValid], @"Non-alphanumeric api key invalid, last char not alphanumeric");

    // valid api key
    plugin.apiKey = @"abcdefghijklmnopqrstuvwxyz123456";
    XCTAssert([plugin isApiKeyFormatValid], @"32 character, alphanumeric api key valid");
}

/********************************************
 * Test that planID caches only when desired
 ********************************************/
- (void)testPlanIdCaching {
    // clear cache *****************
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"planId"];
    
    // planId is not developerId, so cache it
    BlinkUpPlugin *cachingPlugin = [[BlinkUpPlugin alloc] init];
    cachingPlugin.developerPlanId = @"";
    
    [cachingPlugin deviceRequestDidCompleteWithDeviceInfo:deviceInfo timedOut:NO error:nil];
    XCTAssertEqualObjects([[NSUserDefaults standardUserDefaults] objectForKey:@"planId"], deviceInfo.planId);
    
    // clear cache *****************
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"planId"];
    
    // planId is developerId, so don't cache it
    BlinkUpPlugin *noCachingPlugin = [[BlinkUpPlugin alloc] init];
    noCachingPlugin.developerPlanId = deviceInfo.planId;
    
    [noCachingPlugin deviceRequestDidCompleteWithDeviceInfo:deviceInfo timedOut:NO error:nil];
    XCTAssertNil([[NSUserDefaults standardUserDefaults] objectForKey:@"planId"]);
    
    // set plan id (to test clearing it) *****************
    [[NSUserDefaults standardUserDefaults] setObject:deviceInfo.planId forKey:@"planId"];

    BlinkUpPlugin *clearPlugin = [[BlinkUpPlugin alloc] init];
    [clearPlugin clearBlinkUpData:nil];

    // TODO: setting the timeout is done in a commandDelegate background thread
    // and commandDelegate is nil at this point. Need to solve this.
    //XCTAssertNil([[NSUserDefaults standardUserDefaults] objectForKey:@"planId"]);
}

/********************************************
 * Test that setting the timeoutMs variable
 * changes the devicePoller's pollTimeout, as
 * we had issues with this earlier.
 ********************************************/
- (void)testSettingTimeout {
    BlinkUpPlugin *plugin = [[BlinkUpPlugin alloc] init];
    plugin.timeoutMs = 10000;
    plugin.apiKey = @"abcdefghijklmnopqrstuvwxyz123456";
    [plugin navigateToBlinkUpView];

    // TODO: setting the timeout is done in a commandDelegate background thread
    // and commandDelegate is nil at this point. Need to solve this.
    //XCTAssert(plugin.blinkUpController.devicePoller.pollTimeout == 10000);
}

/********************************************
 * Tests that the BUDeviceInfo+JSON category
 * correctly converts device info to JSON
 ********************************************/
- (void)testDeviceInfoToDict {
    NSDictionary *deviceInfoDict = [deviceInfo toDictionary];
    NSDictionary *expectedDeviceInfoDict = @{
        @"deviceId" : deviceInfo.deviceId,
        @"planId": deviceInfo.planId,
        @"agentURL" : deviceInfo.agentURL.description,
        @"verificationDate" : deviceInfo.verificationDate.description
    };

    XCTAssertEqualObjects(deviceInfoDict, expectedDeviceInfoDict);
}

/********************************************
 * Test plugin should keep callback iff
 * the device polling has just started
 ********************************************/
- (void)testKeepCallbackBehaviour {
    // error
    BlinkUpPluginResult *errorResult = [[BlinkUpPluginResult alloc] init];
    errorResult.state = Error;
    XCTAssertEqual([errorResult getKeepCallback], NO);
    
    // started
    BlinkUpPluginResult *startedResult = [[BlinkUpPluginResult alloc] init];
    startedResult.state = Started;
    XCTAssertEqual([startedResult getKeepCallback], YES);
    
    // completed
    BlinkUpPluginResult *completedResult = [[BlinkUpPluginResult alloc] init];
    completedResult.state = Completed;
    XCTAssertEqual([completedResult getKeepCallback], NO);
}

/********************************************
 * Test that BlinkUpPluginResult gives the
 * correct Cordova command status
 ********************************************/
- (void)testCordovaCommandStatus {
    // error
    BlinkUpPluginResult *errorResult = [[BlinkUpPluginResult alloc] init];
    errorResult.state = Error;
    XCTAssertEqual([errorResult getCordovaStatus], CDVCommandStatus_ERROR);
    
    // started
    BlinkUpPluginResult *startedResult = [[BlinkUpPluginResult alloc] init];
    startedResult.state = Started;
    XCTAssertEqual([startedResult getCordovaStatus], CDVCommandStatus_OK);
    
    // completed
    BlinkUpPluginResult *completedResult = [[BlinkUpPluginResult alloc] init];
    completedResult.state = Completed;
    XCTAssertEqual([completedResult getCordovaStatus], CDVCommandStatus_OK);
}

/********************************************
 * Tests the BlinkUpPluginResult returns the
 * correct JSON string given different values
 ********************************************/
- (void)testGetResultsAsJsonString {
    // plugin error
    BlinkUpPluginResult *pluginError = [[BlinkUpPluginResult alloc] init];
    pluginError.state = Error;
    [pluginError setPluginError:100];
    NSString *pluginErrorJson = @"{\n  \"state\" : \"error\",\n  \"error\" : {\n    \"errorCode\" : \"100\",\n    \"errorType\" : \"plugin\"\n  }\n}";

    XCTAssertEqualObjects(pluginErrorJson, [pluginError getResultsAsJsonString]);

    // success - started
    BlinkUpPluginResult *pluginSuccessStarted = [[BlinkUpPluginResult alloc] init];
    pluginSuccessStarted.state = Started;
    pluginSuccessStarted.statusCode = 200;
    NSString *pluginSuccessStartedJson = @"{\n  \"state\" : \"started\",\n  \"statusCode\" : \"200\"\n}";
    
    XCTAssertEqualObjects(pluginSuccessStartedJson, [pluginSuccessStarted getResultsAsJsonString]);

    // success - with device info
    BlinkUpPluginResult *pluginSuccessCompleted = [[BlinkUpPluginResult alloc] init];
    pluginSuccessCompleted.state = Completed;
    pluginSuccessCompleted.statusCode = 0;
    pluginSuccessCompleted.deviceInfo = deviceInfo;
    NSString *pluginSuccessCompletedJson = [NSString stringWithFormat:@"{\n  \"state\" : \"completed\",\n  \"statusCode\" : \"0\",\n  \"deviceInfo\" : {\n    \"agentURL\" : \"%@\",\n    \"deviceId\" : \"%@\",\n    \"verificationDate\" : \"%@\",\n    \"planId\" : \"%@\"\n  }\n}",deviceInfo.agentURL.description, deviceInfo.deviceId, deviceInfo.verificationDate.description, deviceInfo.planId];
    
    XCTAssertEqualObjects(pluginSuccessCompletedJson, [pluginSuccessCompleted getResultsAsJsonString]);
}

@end
