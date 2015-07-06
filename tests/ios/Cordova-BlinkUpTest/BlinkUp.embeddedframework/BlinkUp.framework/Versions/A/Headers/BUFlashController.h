//
//  BUFlashController.h
//  BlinkUp
//
//  Created by Brett Park on 2014-12-10.
//  Copyright (c) 2014 Electric Imp, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

@class BUDevicePoller;
@class BUWifiConfig;
@class BUWPSConfig;
@class BUNetworkConfig;
@class BUConfigId;
@class BUFlashStringParameters;

/*!
 *  @brief  Block that is called when the presenting screen resumes control
 *
 *  @param willRespond   If true, it is expected that the ImpeeDidConnectBlock
 *            will be called. If false, the ImpeeDidConnectBlock will not be called
 *  @param devicePoller      Nil if willRespond is true, otherwise it contains the
 *    device poller that will retrieve the device data from the server. It does
 *    not automatically start the data retrieval.
 *  @param error         Nil if no error or contains the error
 */
typedef void (^FlashResignActiveBlock)(BOOL willRespond, BUDevicePoller *__nullable devicePoller, NSError *__nullable error);

/*!
 *  Perform a BlinkUp using screen flashes
 *
 *  The flash controller is used to relay the BlinkUp information to the
 *  device using screen flashes.
 */
@interface BUFlashController : NSObject

/*!
 *  @brief  Perform a BlinkUp
 *
 *   If using Swift and want enumerated results, please use the Swift extension method
 *
 *  @param networkConfig   The WifiConfig, WpsConfig, or ClearConfig that is to
 *    be performed.
 *  @param configId        The single use configId for this flashing session. This
 *                         can be nil in the case of clearing a device
 *  @param animated        Should the presentation be animated
 *  @param resignActive    Block that is executed when the BlinkUp screen is
 *        dismissed and control is returned to the presenting screen
 */
- (void)presentFlashWithNetworkConfig:(nonnull BUNetworkConfig *)networkConfig
  configId:(nullable BUConfigId *)configId
  animated:(BOOL)animated
  resignActive:(nullable FlashResignActiveBlock)resignActive;

#pragma mark - Properties

/*!
 *  @brief Seconds before BlinkUp begins
 *
 *   Before the BlinkUp flashes occur there is a countdown to allow the user to
 *   prepare the device. The preFlashCountdownTime is the number of seconds that
 *   the countdown occurs for. The default is 3 seconds. This property cannot be
 *   set lower than 3 seconds, and not more than 10.
 */
@property (nonatomic, assign) NSInteger preFlashCountdownTime;


/*!
 *  @brief  Pre-flash interstitial (optional)
 *
 *  image should be 280x380 pixels \@1x and 560x760 pixels \@2x (retina)
 */
@property (nonatomic, strong, nullable) UIImage *interstitialImage;

/*!
 *  @brief  Brightness of the screen during the flash
 *
 *  A value from 0 ... 1 indicating brightness of the screen from 0 (darkest)
 *  to 1 (brightest). If your sensor is running hot, you can lower this number
 *
 *  Default value is 0.8
 */
@property (nonatomic, assign) float screenBrightness;

/*!
 *  @brief List of strings that can be customized
 *
 *    On occasion it may be useful to change or append to it additional information about the
 *    state of the application for the user (such as the device they are about to BlinkUp,
 *    or their username). By adding objects into the various stringParams arrays it is possible
 *    to inject dynamic content using standard stringWithFormat notation in the localized file string
 *    for example "Now connecting device %\@" (remove the backslash if reading source)
 *
 *    Most of these strings can also be modified in the localization file
 */
@property (nonatomic, readonly, strong, nonnull) BUFlashStringParameters *stringParams;

/*!
 *  If set to true, the status bar will be hidden after the Flash completes
 */
@property (nonatomic, assign) BOOL hideStatusBarAfterFlash;
@end




