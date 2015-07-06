//
//  BUNetworkSelectController.h
//  BlinkUp
//
//  Created by Brett Park on 2014-12-09.
//  Copyright (c) 2014 Electric Imp, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

@class BUWifiConfig;
@class BUWPSConfig;
@class BUNetworkSelectStringParameters;
@class BUNetworkConfig;

/*!
 *  Allow the user to select the network for configuration
 *
 *  The Network Select controller is used to present an interface to the user.
 *  The interface allows the user to select a variety of configuration options
 *  for the device such as Wifi network configurations, WPS network
 *  configurations, and clearing device configurations.
 *
 */
@interface BUNetworkSelectController : NSObject

/*!
 *  @brief  Gather network information from the user
 *
 *  Present the user with an interface to enter Wifi information. The results
 *  are returned in the completion handler.
 *
 *   If using Swift and want enumerated results, please use the Swift extension method
 *
 *  @param animated          Animate the presentation
 *  @param completionHandler The completion handler is executed immediately before control
 *    is returned to your program. If the user cancelled, networkConfig will be nil.
 */
- (void)presentInterfaceAnimated:(BOOL)animated completionHandler:(void (^__nullable)(BUNetworkConfig *__nullable networkConfig, BOOL userDidCancel))completionHandler;

/*!
 *  @brief  Dismiss the Network Select Controller
 *
 *  Dismiss the view controller and return control to the presenting view
 *  controller. If this method is called the presentInterfaceAnimated
 *  completion handler will not be called. This method only needs to be called
 *  if you wish to force the end of the user interaction.
 *
 *  @param completionHandler Block called after the view controller is dismissed
 */
- (void)forceDismissWithCompletionHandler:(void (^__nullable)())completionHandler;

#pragma mark Presentation options


/*!
 *  @brief List of strings that can be customized
 *
 *    On occasion it may be useful to change or append to it additional information about the
 *    state of the application for the user (such as the device they are about to BlinkUp,
 *    or their username). By adding objects into the various stringParams arrays it is possible
 *    to inject dynamic content using standard stringWithFormat notation in the localized file string
 *    for example "This is my footer with %\@ parameter" (remove the backslash if reading source)
 *
 *    Most of these strings can also be modified in the localization file
 */
@property (nonatomic, strong, readonly, nonnull) BUNetworkSelectStringParameters *stringParams;


/*!
 *   For added security, setting this flag to true will not allow users to see the ssid passwords
 *   that they are entering into the software
 */
@property (nonatomic, assign) BOOL disableWifiDetailShowPassword;

/*!
 *  If set to true, the status bar will be hidden
 */
@property (nonatomic, assign) BOOL hideStatusBar;

@end
