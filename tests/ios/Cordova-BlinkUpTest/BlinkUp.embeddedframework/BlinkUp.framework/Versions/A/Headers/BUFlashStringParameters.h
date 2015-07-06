//
//  BUFlashStringParameters.h
//  BlinkUp
//
//  Created by Brett Park on 2014-12-12.
//  Copyright (c) 2014 Electric Imp, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

/*!
   Runtime string parameters

   Collection of arrays that can be used for parameters based on NSString format
   in the BlinkUpSDK.strings localization file. See the [Text Customization](../docs/text%20customization.html) guide.

     // Example .strings text
     "PreflashText" = @"Point the screen at your %@";

     // Example setting of parameter for the format string
     flashController.stringParams.preflashText = @[@"Device Type 3"];
 */
@interface BUFlashStringParameters : NSObject

/// Parameters for button text on the interstitial screen if shown
@property (nonatomic, copy, nullable) NSArray *interstitialContinue;

/// Parameters for the preflash screen if it contains text
@property (nonatomic, copy, nullable) NSArray *preflashText;
@end
