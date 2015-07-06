//
//  BUDevicePoller.h
//  BlinkUp
//
//  Created by Brett Park on 2014-12-10.
//  Copyright (c) 2014 Electric Imp, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

@class BUConfigId;
@class BUDeviceInfo;

/*!
 *  @brief  Block that is called on success or failure of a device connection
 *
 *  @param deviceInfo Information about the device if it connected succesfully
 *  @param timedOut True if no device information could be retrieved in the time
 *    allowed by the pollTimeout.
 *  @param error     Reason the imp did not connect on failure
 */
typedef void (^DevicePollingDidCompleteBlock)(BUDeviceInfo *__nullable deviceInfo, BOOL timedOut, NSError *__nullable error);

/*!
   Poll the Electric Imp server for device information

   The BUDevicePoller is used after configuring a device with a network configuration.
   The poller is created based on a BUConfigId and needs to be manually started.
   Once stated, the poller will poll the Electric Imp server until the device
   contacts the server or a timeout period has lapsed.
 */
@interface BUDevicePoller : NSObject

/*!
 *  @brief  Create a device poller to retrieve device information
 *
 *  @param configId ConfigId that was transmitted to the device
 *
 *  @return an intialized device poller
 */
- (nonnull instancetype)initWithConfigId:(nonnull BUConfigId *)configId NS_DESIGNATED_INITIALIZER;


/*!
 *   @brief  Stop the current status verification.
 *
 *   This will stop the SDK from polling and prevents the
 *   startPollingWithCompletionHandler completionHandler from being called.
 *   Typically done after the end user has blinked up and is waiting for status
 *   verification and the end user hits some kind of cancel or back button
 *   because the don't want to wait for verification to complete.
 */
- (void)stopPolling;

/*!
 *  @brief  Start polling for information about a device
 *
 *  Query the Electric Imp server to see if the device has connected
 *
 *   If using Swift and want enumerated results, please use the Swift extension method
 *
 *  @param completionHandler Block that is executed when the device connection
 *        attempt is complete (success or failure)
 */
- (void)startPollingWithCompletionHandler:(nullable DevicePollingDidCompleteBlock)completionHandler;

/*!
 *  @brief Time to wait before giving up on device connection
 *
 *   After the device is configured, it will take the imp some time to connect
 *    to the server and for the application to retrieve the AgentUrl. By default this time
 *    period will last 60 seconds. If you wish to override the timeout period, this property
 *    can be set with the number of seconds before the AgentUrl checking times out
 *
 */
@property (nonatomic) NSTimeInterval pollTimeout;

/*!
 *  @brief  The configId pass in during initialization to be used for reference
 */
@property (nonatomic, strong, readonly, nonnull) BUConfigId *configId;
@end
