//
//  BUErrorStringParameters.h
//  BlinkUp
//
//  Created by Brett Park on 2015-01-31.
//  Copyright (c) 2015 Electric Imp, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

/*!
   Runtime string parameters

   Collection of arrays that can be used for parameters based on NSString format
   in the BlinkUpError.strings localization file. See the [Text Customization](../docs/text%20customization.html) guide.

      // Example .strings text
      "FailedGettingPlanIdFromServer" = @"An error occured getting a PlanId for device: %@";

      // Example setting of parameter for the format string
      [BUErrorStringParameters shared].failedGettingPlanIdFromServer = @[@"Device 3"];
 */
@interface BUErrorStringParameters : NSObject
/*!
 *  @brief  The global error strings
 *
 *  @return A singleton where the string parameters can be set
 */
+ (nonnull instancetype)shared;

/// From BlinkUpError BlinkUpErrorFlashPacketInvalid
@property (nonatomic, copy, nullable) NSArray *badFlashPacket;

/// From BlinkUpError BlinkUpErrorPlanIDInvalid
@property (nonatomic, copy, nullable) NSArray *badPlanId;

/// From BlinkUpError BlinkUpErrorSetupTokenInvalid
@property (nonatomic, copy, nullable) NSArray *badSetupToken;

/// From BlinkUpError BlinkUpErrorSSIDNotSet
@property (nonatomic, copy, nullable) NSArray *emptySsid;

/// From BlinkUpError BlinkUpErrorPlanIDRetrievalFailed
@property (nonatomic, copy, nullable) NSArray *failedGettingPlanIdFromServer;

/// From BlinkUpError BlinkUpErrorNetworkError
@property (nonatomic, copy, nullable) NSArray *networkErrorDuringImpPoll;

/// From BlinkUpError BlinkUpErrorSetupTokenRetrievalFailed
@property (nonatomic, copy, nullable) NSArray *setupTokenFailure;

@end

