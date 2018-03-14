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
 * Created by Stuart Douglas (sdouglas@macadamian.com) on June 11, 2015.
 * Copyright (c) 2015 Macadamian. All rights reserved.
 */

/*global cordova, module*/

module.exports = {
    /** startBlinkUp - starts the blinkup process
    * @param {apiKey}: your blinkup api key
    * @param {developerPlanId}: your development plan Id. Will be disregarded when {isInDevelopment} is set to false
    * @param {isInDevelopment}: TRUE if you are connecting to development devices. when you are moving to production devices, this must be set to FALSE.
    * @param {timeoutMS}: Amount of second before the application times out. Default & Maximum value is 60000.
    */
    startBlinkUp: function (apiKey, developerPlanId, isInDevelopment, timeoutMs, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "cordova-blinkup-plugin", "startBlinkUp", [apiKey, developerPlanId, isInDevelopment, timeoutMs]);
    },
    abortBlinkUp: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "cordova-blinkup-plugin", "abortBlinkUp", []);
    },
    clearBlinkUpData: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "cordova-blinkup-plugin", "clearBlinkUpData", []);
    }
};
