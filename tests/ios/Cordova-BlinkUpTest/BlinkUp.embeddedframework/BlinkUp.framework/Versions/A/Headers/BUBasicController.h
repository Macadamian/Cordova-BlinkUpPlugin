//
//  BUBasicController.h
//  BlinkUp
//
//  Created by Brett Park on 2014-12-10.
//  Copyright (c) 2014 Electric Imp, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>
@class BUConfigId;
@class BUDevicePoller;
@class BUFlashController;
@class BUNetworkSelectController;
@class BUDeviceInfo;

/*!
 *  Block that is called when the BlinkUp interface reverts control
 *
 *  @param willRespond   True if the devicePollingDidComplete block will be called due
 *                       to device information retrieval
 *  @param userDidCancel True if the user did cancel out of the interface
 *  @param error         Reason for failure of BlinkUp
 */
typedef void (^BUResignActiveBlock)(BOOL willRespond, BOOL userDidCancel, NSError *__nullable error);

/*!
 *  @brief  Block that is called on success or failure of a device connection
 *
 *  @param deviceInfo Information about the device if it connected succesfully
 *  @param timedOut True if no device information could be retrieved in the time
 *    allowed by the pollTimeout.
 *  @param error     Reason the imp did not connect on failure
 */
typedef void (^BUDevicePollingDidCompleteBlock)(BUDeviceInfo *__nullable deviceInfo, BOOL timedOut, NSError *__nullable error);


/*!
   Use the device screen to perform a BlinkUp

   The BUBasicController is a convienance class that handles the normal operation
   of a BlinkUp using the standard BlinkUp user experience. A new BUBasicController
   should be created for each BlinkUp attempt.

   If you wish to have
   a more customized approach please create the BUNetworkSelectController,
   BUConfigId, BUFlashController, and the BUDevicePoller as individual objects.
 */
@interface BUBasicController : NSObject

#pragma mark ** Designated Initializer **

/*!
 *  @brief  Default Initializer
 *
 *  @param apiKey The APIKey assigned to you from Electric Imp
 *
 *  @return An initialized BlinkUpController
 */
- (nonnull instancetype)initWithApiKey:(nonnull NSString *)apiKey NS_DESIGNATED_INITIALIZER;

/*!
 *  @brief  Initial with an existing planId
 *
 *  @param apiKey The APIKey assigned to you from Electric Imp
 *  @param planId An existing planId to use for the configuration
 *
 *  @return An initialized BlinkUpController
 */
- (nonnull instancetype)initWithApiKey:(nonnull NSString *)apiKey planId:(nonnull NSString *)planId;

/*!
 *  @brief  Show standard network selection screen
 *
 *   if you want to use an existing PlanID, the BlinkUpController's planId property must be set.
 *   If planId is nil (or not set), this method will automatically fetch a new planId.
 *   Most developers won't need to reuse existing planIDs.  Please contact support if you have questions.
 *
 *   If using Swift and want enumerated results, please use the Swift extension method
 *
 *  @param animated        Should the presentation be animated
 *  @param resignActive    Block that is executed when the BlinkUp screen is
 *        dismissed and control is returned to the presenting screen
 *  @param devicePollingDidComplete Block that is executed when the device connection
 *        attempt is complete (success or failure)
 *
 */
- (void)presentInterfaceAnimated:(BOOL)animated
  resignActive:(nullable BUResignActiveBlock)resignActive
  devicePollingDidComplete:(nullable BUDevicePollingDidCompleteBlock)devicePollingDidComplete;

/*!
 *  @brief  Dismiss the BUBasicController
 *
 *  Dismiss the view controller and return control to the presenting view
 *  controller. If this method is called the resignActive and devicePollingDidComplete
 *  completion handlers will not be called. This method only needs to be called
 *  if you wish to force the end of the user interaction.
 *
 *  @param completionHandler Block called after the view controller is dismissed
 */
- (void)forceDismissWithCompletionHandler:(void (^__nullable)())completionHandler;

/*!
 *  @brief  An interface for network selection by the user
 *
 */
@property (strong, nonatomic, readonly, nonnull) BUNetworkSelectController *networkSelectController;

/*!
 *  @brief  An interface that performs the BlinkUp flashing operations
 */
@property (strong, nonatomic, readonly, nonnull) BUFlashController *flashController;

/*!
 *  @brief  Gather device metadata after a flash
 */
@property (strong, nonatomic, readonly, nullable) BUDevicePoller *devicePoller;

@end
