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
 * Created by Stuart Douglas (sdouglas@macadamian.com) on June 18, 2015.
 * Copyright (c) 2015 Macadamian. All rights reserved.
 */

package com.macadamian.blinkup;

import android.util.Log;

import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

// see README.md for format of JSON string to be sent to callback

public class BlinkUpPluginResult {
    private static final String TAG = "BlinkUpPluginResult";

    // possible states
    public enum BlinkUpPluginState {
        Started("started"),
        Completed("completed"),
        Error("error");

        private final String state;
        BlinkUpPluginState(String state) { this.state = state; }
        public String getKey() { return this.state; }
    }

    // possible error types
    private enum BlinkUpErrorType {
        BlinkUpSDKError("blinkup"),
        PluginError("plugin");

        private final String type;
        BlinkUpErrorType(String type) { this.type = type; }
        public String getType() { return this.type; }
    }

    //=====================================
    // JSON keys for results
    //=====================================
    private enum ResultKeys {
        STATE("state"),
        STATUS_CODE("statusCode"),

        ERROR("error"),
        ERROR_TYPE("errorType"),
        ERROR_CODE("errorCode"),
        ERROR_MSG("errorMsg"),

        DEVICE_INFO("deviceInfo"),
        DEVICE_ID("deviceId"),
        PLAN_ID("planId"),
        AGENT_URL("agentURL"),
        VERIFICATION_DATE("verificationDate");

        private final String key;
        ResultKeys(String key) { this.key = key; }
        public String getKey() { return this.key; }
    }

    //====================================
    // BlinkUp Results
    //====================================
    private BlinkUpPluginState state;
    private int statusCode;
    private BlinkUpErrorType errorType;
    private int errorCode;
    private String errorMsg;

    private String deviceId;
    private String planId;
    private String agentURL;
    private String verificationDate;
    private boolean hasDeviceInfo = false;

    /*************************************
     * Setters for our Results
     *************************************/
    public void setState(BlinkUpPluginState state) {
        this.state = state;
    }
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    public void setPluginError(int errorCode) {
        this.state = BlinkUpPluginState.Error;
        this.errorType = BlinkUpErrorType.PluginError;
        this.errorCode = errorCode;
    }
    public void setBlinkUpError(String errorMsg) {
        this.state = BlinkUpPluginState.Error;
        this.errorType = BlinkUpErrorType.BlinkUpSDKError;
        this.errorCode = 1; // set generic error code
        this.errorMsg = errorMsg;
    }
    public void setDeviceInfoAsJson(JSONObject deviceInfo) {
        try {
            // the JSON keys are from the Android BlinkUp SDK, documented at:
            // https://electricimp.com/docs/manufacturing/sdkdocs/android/callbacks/
            this.deviceId = (deviceInfo.getString("impee_id") != null) ? deviceInfo.getString("impee_id").trim() : null;
            this.planId = deviceInfo.getString("plan_id");
            this.agentURL = deviceInfo.getString("agent_url");
            this.verificationDate = deviceInfo.getString("claimed_at").replace("Z", "+0:00"); // match date format to iOS
            this.hasDeviceInfo = true;
        } catch (JSONException e) {
            this.state = BlinkUpPluginState.Error;
            setPluginError(BlinkUpPlugin.ErrorCodes.JSON_ERROR.getCode());
            sendResultsToCallback();
        }
    }

    /*************************************
     * Generates JSON of our plugin results
     * and sends back to the callback
     *************************************/
    public void sendResultsToCallback() {
        JSONObject resultJSON = new JSONObject();

        // set result status
        PluginResult.Status cordovaResultStatus;
        if (this.state == BlinkUpPluginState.Error) {
            cordovaResultStatus = PluginResult.Status.ERROR;
        }
        else {
            cordovaResultStatus = PluginResult.Status.OK;
        }

        try {
            resultJSON.put(ResultKeys.STATE.getKey(), this.state.getKey());

            if (this.state == BlinkUpPluginState.Error) {
                resultJSON.put(ResultKeys.ERROR.getKey(), generateErrorJson());
            }
            else {
                resultJSON.put(ResultKeys.STATUS_CODE.getKey(), ("" + statusCode));
                if (this.hasDeviceInfo) {
                    resultJSON.put(ResultKeys.DEVICE_INFO.getKey(), generateDeviceInfoJson());
                }
            }
        } catch (JSONException e) {
            // don't want endless loop calling ourselves so just log error (don't send to callback)
            Log.e(TAG, "", e);
        }

        PluginResult pluginResult = new PluginResult(cordovaResultStatus, resultJSON.toString());
        pluginResult.setKeepCallback(true); // uses same BlinkUpPlugin object across calls, so need to keep callback
        BlinkUpPlugin.callbackContext.sendPluginResult(pluginResult);
    }

    /*************************************
     * Returns JSON containing error
     *************************************/
    private JSONObject generateErrorJson() {
        JSONObject errorJson = new JSONObject();

        try {
            errorJson.put(ResultKeys.ERROR_TYPE.getKey(), this.errorType.getType());
            errorJson.put(ResultKeys.ERROR_CODE.getKey(), ("" + this.errorCode));

            if (this.errorType == BlinkUpErrorType.BlinkUpSDKError) {
                errorJson.put(ResultKeys.ERROR_MSG.getKey(), this.errorMsg);
            }
        } catch (JSONException e) {
            this.state = BlinkUpPluginState.Error;
            setPluginError(BlinkUpPlugin.ErrorCodes.JSON_ERROR.getCode());
            sendResultsToCallback();
        }

        return errorJson;
    }

    /*************************************
     * Returns deviceInfo in JSON
     *************************************/
    private JSONObject generateDeviceInfoJson() {
        JSONObject deviceInfoJson = new JSONObject();

        try {
            deviceInfoJson.put(ResultKeys.DEVICE_ID.getKey(), this.deviceId);
            deviceInfoJson.put(ResultKeys.PLAN_ID.getKey(), this.planId);
            deviceInfoJson.put(ResultKeys.AGENT_URL.getKey(), this.agentURL);
            deviceInfoJson.put(ResultKeys.VERIFICATION_DATE.getKey(), this.verificationDate);
        } catch (JSONException e) {
            this.state = BlinkUpPluginState.Error;
            setPluginError(BlinkUpPlugin.ErrorCodes.JSON_ERROR.getCode());
            sendResultsToCallback();
        }

        return deviceInfoJson;
    }
}
