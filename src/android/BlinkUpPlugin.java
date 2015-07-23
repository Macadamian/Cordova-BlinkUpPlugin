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
import android.text.TextUtils;
import android.util.Log;

import com.electricimp.blinkup.BlinkupController;
import com.macadamian.blinkup.util.PreferencesHelper;

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

    private static final String INVOKE_BLINKUP = "invokeBlinkUp";
    private static final String ABORT_BLINKUP = "abortBlinkUp";
    private static final String CLEAR_BLINKUP_DATA = "clearBlinkUpData";

    private static CallbackContext sCallbackContext;
    private static boolean sClearCache = false;

    // only needed in this class
    private String mApiKey;
    private Boolean mGeneratePlanId = false;
    private String mDeveloperPlanId;

    static final int STATUS_DEVICE_CONNECTED = 0;
    static final int STATUS_GATHERING_INFO = 200;
    static final int STATUS_CLEAR_WIFI_COMPLETE = 201;
    static final int STATUS_CLEAR_WIFI_AND_CACHE_COMPLETE = 202;

    static final int ERROR_INVALID_ARGUMENTS = 100;
    static final int ERROR_PROCESS_TIMED_OUT = 101;
    static final int ERROR_CANCELLED_BY_USER = 102;
    static final int ERROR_INVALID_API_KEY = 300;
    static final int ERROR_VERIFY_API_KEY_FAIL = 301; // android only
    static final int ERROR_JSON_ERROR = 302;          // android only

    // argument indexes from BlinkUp.js, the plugin's JS interface to Cordova
    private static final int BLINKUP_ARG_API_KEY = 0;
    private static final int BLINKUP_ARG_DEVELOPER_PLAN_ID = 1;
    private static final int BLINKUP_ARG_TIMEOUT_MS = 2;
    private static final int BLINKUP_ARG_GENERATE_PLAN_ID = 3;

    /**********************************************************
     * method called by Cordova javascript
     *********************************************************/
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        sCallbackContext = callbackContext;
        final Activity activity = cordova.getActivity();
        final BlinkupController controller = BlinkupController.getInstance();

        if (INVOKE_BLINKUP.equalsIgnoreCase(action)) {
            return invokeBlinkup(activity, controller, data);
        } else if (ABORT_BLINKUP.equalsIgnoreCase(action)) {
            return abortBlinkup(controller);
        } else if (CLEAR_BLINKUP_DATA.equalsIgnoreCase(action)) {
            return clearBlinkupData(activity, controller);
        }
        return false;
    }

    private boolean invokeBlinkup(final Activity activity, final BlinkupController controller, JSONArray data) {
        int timeoutMs;
        try {
            mApiKey = data.getString(BLINKUP_ARG_API_KEY);
            mDeveloperPlanId = data.getString(BLINKUP_ARG_DEVELOPER_PLAN_ID);
            timeoutMs = data.getInt(BLINKUP_ARG_TIMEOUT_MS);
            mGeneratePlanId = data.getBoolean(BLINKUP_ARG_GENERATE_PLAN_ID);
        } catch (JSONException exc) {
            BlinkUpPluginResult.sendPluginErrorToCallback(ERROR_INVALID_ARGUMENTS);
            return false;
        }

        // if api key not valid, send error message and quit
        if (!apiKeyFormatValid()) {
            BlinkUpPluginResult.sendPluginErrorToCallback(ERROR_INVALID_API_KEY);
            return false;
        }

        Intent blinkupCompleteIntent = new Intent(activity, BlinkUpCompleteActivity.class);
        blinkupCompleteIntent.putExtra(Extras.EXTRA_DEVELOPER_PLAN_ID, mDeveloperPlanId);
        blinkupCompleteIntent.putExtra(Extras.EXTRA_TIMEOUT_MS, timeoutMs);
        controller.intentBlinkupComplete = blinkupCompleteIntent;

        // default is to run on WebCore thread, we have UI so need UI thread
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                presentBlinkUp(activity, controller);
            }
        });
        return true;
    }

    private boolean abortBlinkup(BlinkupController controller) {
        controller.cancelTokenStatusPolling();
        BlinkUpPluginResult.sendPluginErrorToCallback(ERROR_CANCELLED_BY_USER);
        return true;
    }

    private boolean clearBlinkupData(final Activity activity, final BlinkupController controller) {
        PreferencesHelper.setPlanId(activity, null);
        sClearCache = true;
        controller.intentClearComplete = new Intent(activity, ClearCompleteActivity.class);

        // default is to run on WebCore thread, clearing shows UI so needs UI thread
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                controller.clearDevice(activity);
            }
        });
        return true;
    }

    /**********************************************************
     * shows BlinkUpPlugin activity and handles appropriate callbacks
     **********************************************************/
    private void presentBlinkUp(Activity activity, BlinkupController controller) {

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
                BlinkUpPluginResult.sendPluginErrorToCallback(ERROR_VERIFY_API_KEY_FAIL);
            }
        };

        // load cached planId if available. Otherwise, SDK generates new one automatically
        // see electricimp.com/docs/manufacturing/planids/ for info about planIDs
        if (!mGeneratePlanId) {
            if (org.apache.cordova.BuildConfig.DEBUG && !TextUtils.isEmpty(mDeveloperPlanId)) {
                controller.setPlanID(mDeveloperPlanId);
            } else {
                String planId = PreferencesHelper.getPlanId(activity);
                controller.setPlanID(planId);
            }
        }

        controller.acquireSetupToken(activity, mApiKey, tokenAcquireCallback);
        controller.selectWifiAndSetupDevice(activity, mApiKey, serverErrorHandler);
    }

    /**********************************************************
     * @return true if mApiKey is 32 alpha-numeric characters
     *********************************************************/
    private boolean apiKeyFormatValid() {
        if (TextUtils.isEmpty(mApiKey) || TextUtils.getTrimmedLength(mApiKey) != 32) {
            return false;
        }

        String isAlphaNumericPattern = "^[a-zA-Z0-9]*$";
        return mApiKey.matches(isAlphaNumericPattern);
    }

    static boolean getClearCache() {
        return sClearCache;
    }

    static void setClearCache(boolean val) {
        sClearCache = val;
    }

    static CallbackContext getCallbackContext() {
        return sCallbackContext;
    }

}
