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

package com.macadamian.blinkup;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.electricimp.blinkup.BlinkupController;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

/*********************************************
 * execute() called from Javascript interface,
 * which saves the arguments and presents the
 * BlinkUpPlugin interface from the SDK
 ********************************************/
public class BlinkUpPlugin extends CordovaPlugin {
    private static final String TAG = "BlinkUpPlugin";

    // accessed from BlinkUpCompleteActivity and ClearCompleteActivity
    static int timeoutMs = 30000;
    static CallbackContext callbackContext;

    static final String PLAN_ID_CACHE_KEY = "planId";
    static final String PLAN_ID_CACHE_NAME = "DefaultPreferences";
    static boolean clearedCache = false;
    static String developerPlanId; // don't want to cache planId if development

    // only needed in this class
    private String apiKey;
    private Boolean generatePlanId = false;

    public enum StatusCodes {
        DEVICE_CONNECTED(0),
        GATHERING_INFO(200),
        CLEAR_WIFI_COMPLETE(201),
        CLEAR_WIFI_AND_CACHE_COMPLETE(202);

        private final int code;
        StatusCodes(int code) { this.code = code; }
        public int getCode() { return code; }
    }
    public enum ErrorCodes {
        INVALID_ARGUMENTS(100),
        PROCESS_TIMED_OUT(101),
        CANCELLED_BY_USER(102),
        INVALID_API_KEY(103),
        VERIFY_API_KEY_FAIL(301), // android only
        JSON_ERROR(302);          // android only

        private final int code;
        ErrorCodes(int code) { this.code = code; }
        public int getCode() { return code; }
    }

    // argument indexes from Cordova javascript
    private static final int BlinkUpArgumentApiKey = 0;
    private static final int BlinkUpArgumentDeveloperPlanId = 1;
    private static final int BlinkUpArgumentTimeOut = 2;
    private static final int BlinkUpArgumentGeneratePlanId = 3;

    /**********************************************************
     * method called by Cordova javascript
     *********************************************************/
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        BlinkUpPlugin.callbackContext = callbackContext;

        // onActivityResult called on MainActivity (i.e. cordova.getActivity()) when blinkup or clear
        // complete. It calls handleActivityResult on blinkupController, which initiates the following intents
        BlinkupController.getInstance().intentBlinkupComplete = new Intent(this.cordova.getActivity(), BlinkUpCompleteActivity.class);
        BlinkupController.getInstance().intentClearComplete = new Intent(this.cordova.getActivity(), ClearCompleteActivity.class);

        // starting blinkup
        if (action.equalsIgnoreCase("invokeBlinkUp")) {
            try {
                apiKey = data.getString(BlinkUpArgumentApiKey);
                developerPlanId = data.getString(BlinkUpArgumentDeveloperPlanId);
                timeoutMs = data.getInt(BlinkUpArgumentTimeOut);
                generatePlanId = data.getBoolean(BlinkUpArgumentGeneratePlanId);
            } catch (JSONException exc) {
                BlinkUpPluginResult argErrorResult = new BlinkUpPluginResult();
                argErrorResult.setState(BlinkUpPluginResult.BlinkUpPluginState.Error);
                argErrorResult.setPluginError(ErrorCodes.INVALID_ARGUMENTS.getCode());
                argErrorResult.sendResultsToCallback();
                return false;
            }

            // if api key not valid, send error message and quit
            if (!apiKeyFormatValid()) {
                BlinkUpPluginResult pluginResult = new BlinkUpPluginResult();
                pluginResult.setState(BlinkUpPluginResult.BlinkUpPluginState.Error);
                pluginResult.setPluginError(ErrorCodes.INVALID_API_KEY.getCode());
                pluginResult.sendResultsToCallback();
                return false;
            }

            // default is to run on WebCore thread, we have UI so need UI thread
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    presentBlinkUp();
                }
            });
        }

        // abort blinkup
        else if (action.equalsIgnoreCase("abortBlinkUp")) {
            BlinkupController.getInstance().cancelTokenStatusPolling();

            BlinkUpPluginResult pluginResult = new BlinkUpPluginResult();
            pluginResult.setState(BlinkUpPluginResult.BlinkUpPluginState.Error);
            pluginResult.setPluginError(ErrorCodes.CANCELLED_BY_USER.getCode());
            pluginResult.sendResultsToCallback();
        }

        // clears wifi and removes cached planId
        else if (action.equalsIgnoreCase("clearBlinkUpData")) {
            SharedPreferences preferences = cordova.getActivity().getSharedPreferences(PLAN_ID_CACHE_NAME, Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PLAN_ID_CACHE_KEY, null);
            editor.apply();

            clearedCache = true;

            // default is to run on WebCore thread, clearing shows UI so needs UI thread
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BlinkupController.getInstance().clearDevice(cordova.getActivity());
                }
            });
        }

        return true;
    }

    /**********************************************************
     * shows BlinkUpPlugin activity and handles appropriate callbacks
     *********************************************************/
    private void presentBlinkUp() {

        // show toast if can't acquire token
        final BlinkupController.TokenAcquireCallback tokenAcquireCallback = new BlinkupController.TokenAcquireCallback() {
            @Override
            public void onSuccess(String planId, String id) { }

            @Override
            public void onError(String s) {
                Log.e(TAG, s);
            }
        };

        // send back error if connectivity issue
        BlinkupController.ServerErrorHandler serverErrorHandler= new BlinkupController.ServerErrorHandler() {
            @Override
            public void onError(String s) {
                BlinkUpPluginResult serverErrorResult = new BlinkUpPluginResult();
                serverErrorResult.setState(BlinkUpPluginResult.BlinkUpPluginState.Error);
                serverErrorResult.setStatusCode(ErrorCodes.VERIFY_API_KEY_FAIL.getCode());
                serverErrorResult.sendResultsToCallback();
            }
        };

        // load cached planId if available. Otherwise, SDK generates new one automatically
        if (!generatePlanId) {
            SharedPreferences preferences = cordova.getActivity().getSharedPreferences(PLAN_ID_CACHE_NAME, Activity.MODE_PRIVATE);
            String planId = preferences.getString(PLAN_ID_CACHE_KEY, null);
            BlinkupController.getInstance().setPlanID(planId);
        }

        // see electricimp.com/docs/manufacturing/planids/ for info about planIDs
        if (org.apache.cordova.BuildConfig.DEBUG && !generatePlanId) {
            BlinkupController.getInstance().setPlanID(developerPlanId);
        }

        BlinkupController.getInstance().acquireSetupToken(cordova.getActivity(), apiKey, tokenAcquireCallback);
        BlinkupController.getInstance().selectWifiAndSetupDevice(cordova.getActivity(), apiKey, serverErrorHandler);
    }

    /**********************************************************
     * @return true if apiKey is 32 alpha-numeric characters
     *********************************************************/
    private boolean apiKeyFormatValid() {
        if (TextUtils.isEmpty(apiKey) || TextUtils.getTrimmedLength(apiKey) != 32) {
            return false;
        }

        String isAlphaNumericPattern = "^[a-zA-Z0-9]*$";
        return apiKey.matches(isAlphaNumericPattern);
    }
}
