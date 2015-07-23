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

import android.text.TextUtils;
import android.util.Log;

import com.macadamian.blinkup.util.DebugUtils;

import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

// see README.md for format of JSON string to be sent to callback

public class BlinkUpPluginResult {
    private static final String TAG = "BlinkUpPluginResult";

    // the JSON keys are from the Android BlinkUp SDK, documented at:
    // https://electricimp.com/docs/manufacturing/sdkdocs/android/callbacks/
    private static final String SDK_IMPEE_ID_KEY = "impee_id";
    static final String SDK_PLAN_ID_KEY = "plan_id";
    private static final String SDK_AGENT_URL_KEY = "agent_url";
    private static final String SDK_CLAIMED_AT_KEY = "claimed_at";

    // possible states
    static final String STATE_STARTED = "started";
    static final String STATE_COMPLETED = "completed";
    static final String STATE_ERROR = "error";

    // possible error types
    private static final String ERROR_TYPE_BLINK_UP_SDK_ERROR = "blinkup";
    private static final String ERROR_TYPE_PLUGIN_ERROR = "plugin";

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
    private String mState;
    private int mStatusCode;
    private String mErrorType;
    private int mErrorCode;
    private String mErrorMsg;

    private String mDeviceId;
    private String mPlanId;
    private String mAgentURL;
    private String mVerificationDate;
    private boolean mHasDeviceInfo = false;

    /*************************************
     * Setters for our Results
     *************************************/
    public void setState(String state) {
        DebugUtils.checkAssert(TextUtils.equals(state, STATE_COMPLETED)
                || TextUtils.equals(state, STATE_ERROR)
                || TextUtils.equals(state, STATE_STARTED));
        mState = state;
    }
    public void setStatusCode(int statusCode) {
        mStatusCode = statusCode;
    }
    public void setPluginError(int errorCode) {
        mState = STATE_ERROR;
        mErrorType = ERROR_TYPE_PLUGIN_ERROR;
        mErrorCode = errorCode;
    }
    public void setBlinkUpError(String errorMsg) {
        mState = STATE_ERROR;
        mErrorType = ERROR_TYPE_BLINK_UP_SDK_ERROR;
        mErrorCode = 1; // set generic error code
        mErrorMsg = errorMsg;
    }
    public void setDeviceInfoFromJson(JSONObject deviceInfo) {
        try {
            mDeviceId = (deviceInfo.getString(SDK_IMPEE_ID_KEY) != null) ? deviceInfo.getString(SDK_IMPEE_ID_KEY).trim() : null;
            mPlanId = deviceInfo.getString(SDK_PLAN_ID_KEY);
            mAgentURL = deviceInfo.getString(SDK_AGENT_URL_KEY);
            mVerificationDate = deviceInfo.getString(SDK_CLAIMED_AT_KEY).replace("Z", "+0:00"); // match date format to iOS
            mHasDeviceInfo = true;
        } catch (JSONException e) {
            mState = STATE_ERROR;
            setPluginError(BlinkUpPlugin.ERROR_JSON_ERROR);
            sendResultsToCallback();
        }
    }

    static void sendPluginErrorToCallback(int error) {
        BlinkUpPluginResult argErrorResult = new BlinkUpPluginResult();
        argErrorResult.setState(STATE_ERROR);
        argErrorResult.setPluginError(error);
        argErrorResult.sendResultsToCallback();
    }

    /*************************************
     * Generates JSON of our plugin results
     * and sends back to the callback
     *************************************/
    public void sendResultsToCallback() {
        JSONObject resultJSON = new JSONObject();

        // set result status
        PluginResult.Status cordovaResultStatus;
        if (TextUtils.equals(mState, STATE_ERROR)) {
            cordovaResultStatus = PluginResult.Status.ERROR;
        }
        else {
            cordovaResultStatus = PluginResult.Status.OK;
        }

        try {
            resultJSON.put(ResultKeys.STATE.getKey(), mState);

            if (TextUtils.equals(mState, STATE_ERROR)) {
                resultJSON.put(ResultKeys.ERROR.getKey(), generateErrorJson());
            }
            else {
                resultJSON.put(ResultKeys.STATUS_CODE.getKey(), ("" + mStatusCode));
                if (mHasDeviceInfo) {
                    resultJSON.put(ResultKeys.DEVICE_INFO.getKey(), generateDeviceInfoJson());
                }
            }
        } catch (JSONException e) {
            // don't want endless loop calling ourselves so just log error (don't send to callback)
            Log.e(TAG, "", e);
        }

        PluginResult pluginResult = new PluginResult(cordovaResultStatus, resultJSON.toString());
        pluginResult.setKeepCallback(true); // uses same BlinkUpPlugin object across calls, so need to keep callback
        BlinkUpPlugin.getCallbackContext().sendPluginResult(pluginResult);
    }

    /*************************************
     * Returns JSON containing error
     *************************************/
    private JSONObject generateErrorJson() {
        JSONObject errorJson = new JSONObject();

        try {
            errorJson.put(ResultKeys.ERROR_TYPE.getKey(), mErrorType);
            errorJson.put(ResultKeys.ERROR_CODE.getKey(), "" + mErrorCode);

            if (TextUtils.equals(mErrorType, ERROR_TYPE_BLINK_UP_SDK_ERROR)) {
                errorJson.put(ResultKeys.ERROR_MSG.getKey(), mErrorMsg);
            }
        } catch (JSONException e) {
            mState = STATE_ERROR;
            setPluginError(BlinkUpPlugin.ERROR_JSON_ERROR);
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
            deviceInfoJson.put(ResultKeys.DEVICE_ID.getKey(), mDeviceId);
            deviceInfoJson.put(ResultKeys.PLAN_ID.getKey(), mPlanId);
            deviceInfoJson.put(ResultKeys.AGENT_URL.getKey(), mAgentURL);
            deviceInfoJson.put(ResultKeys.VERIFICATION_DATE.getKey(), mVerificationDate);
        } catch (JSONException e) {
            mState = STATE_ERROR;
            setPluginError(BlinkUpPlugin.ERROR_JSON_ERROR);
            sendResultsToCallback();
        }

        return deviceInfoJson;
    }
}
