//
//  BUErrors.h
//  BlinkUp
//
//  Created by BrettPark on 2014-08-12.
//  Copyright (c) 2014 Electric Imp, Inc. All rights reserved.
//

#pragma once

/*!
 *  @brief  String used to determine if error was created from BlinkUp
 */
extern NSString *const BlinkUpErrorDomain;

/*!
 *  Possible error codes
 *
 *  Please use the enumerations for error matching.
 *  The comment string listed after each error is the .string key for the
 *  NSLocalizedDescription of the error if you would like to override the message
 *  Conversion from older error codes is provided in the [Migration documentation](../docs/Version%20Migration.html)
 *  that can be found with the SDK
 *
 */
typedef NS_ENUM (NSInteger, BlinkUpError){
  /// NetworkErrorDuringImpPol
  BlinkUpErrorNetworkError = 10,

  /// BadFlashPacket
  BlinkUpErrorFlashPacketInvalid = 20, /// Test2

  /// SaveWifiConfigPasswordError
  BlinkUpErrorPasswordAlreadySaved = 51,

  /// BadPlanId
  BlinkUpErrorPlanIDInvalid = 30,

  /// FailedGettingPlanIdFromServer
  BlinkUpErrorPlanIDRetrievalFailed = 31,

  /// BadSetupToken
  BlinkUpErrorSetupTokenInvalid = 41,

  /// SetupTokenFailure
  BlinkUpErrorSetupTokenRetrievalFailed = 42,

  /// EmptySsid
  BlinkUpErrorSSIDNotSet = 60,

  /// BadMimeType
  BlinkUpErrorMIMETypeInvalid = 80,

  /// Security issue when saving password
  BlinkUpErrorPasswordSaveFailed = 52,

  /// The BlinkUp.bundle was not included in your target
  BlinkUpErrorBundleNotCopied = 110,

  /// The ObjC flag was not set in your project
  BlinkUpErrorObjCFlagNotSet = 111,

  /// A configId was not passed in to the flash controller
  BlinkUpErrorConfigIdIsNil = 112,

  /// A non-active configId was passed in to the flash controller
  BlinkUpErrorConfigIdIsNotActive = 113,

  /// The ApiKey cannot be nil
  BlinkUpErrorApiKeyIsNil = 114,

  /// The ApiKey is not long enough
  BlinkUpErrorApiKeyIsToShort= 115,
};
