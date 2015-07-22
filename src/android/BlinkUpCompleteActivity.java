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
import android.os.Bundle;
import android.text.TextUtils;

import com.electricimp.blinkup.BlinkupController;
import com.macadamian.blinkup.util.PreferencesHelper;

import org.json.JSONException;
import org.json.JSONObject;

/*****************************************************
 * When the BlinkUpPlugin process completes, it executes the
 * BlinkUpCompleteIntent set in BlinkUpPlugin.java, starting
 * this activity, which requests the setup info from
 * the Electric Imp server, dismisses itself, and
 * sends the info back to the callback when received.
 *****************************************************/
public class BlinkUpCompleteActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String developerPlanId = getIntent().getStringExtra(Extras.EXTRA_DEVELOPER_PLAN_ID);
        int timeoutMs = getIntent().getIntExtra(Extras.EXTRA_TIMEOUT_MS, 30000);
        getDeviceInfo(developerPlanId, timeoutMs);

        BlinkUpPluginResult pluginResult = new BlinkUpPluginResult();
        pluginResult.setState(BlinkUpPluginResult.STATE_STARTED);
        pluginResult.setStatusCode(BlinkUpPlugin.STATUS_GATHERING_INFO);
        pluginResult.sendResultsToCallback();

        finish();
    }

    private void getDeviceInfo(final String developerPlanId, int timeoutMs) {
        final BlinkupController.TokenStatusCallback tokenStatusCallback= new BlinkupController.TokenStatusCallback() {

            //---------------------------------
            // give connection info to Cordova
            //---------------------------------
            @Override public void onSuccess(JSONObject json) {
                BlinkUpPluginResult successResult = new BlinkUpPluginResult();
                successResult.setState(BlinkUpPluginResult.STATE_COMPLETED);
                successResult.setStatusCode(BlinkUpPlugin.STATUS_DEVICE_CONNECTED);
                successResult.setDeviceInfoFromJson(json);
                successResult.sendResultsToCallback();

                // cache planID if not development ID (see electricimp.com/docs/manufacturing/planids/)
                try {
                    String planId = json.getString(BlinkUpPluginResult.SDK_PLAN_ID_KEY);
                    if (!TextUtils.equals(planId, developerPlanId)) {
                        PreferencesHelper.setPlanIdKey(BlinkUpCompleteActivity.this, planId);
                    }
                } catch (JSONException e) {
                    BlinkUpPluginResult.sendPluginErrorToCallback(BlinkUpPlugin.ERROR_JSON_ERROR);
                }
            }

            //---------------------------------
            // give error msg to Cordova
            //---------------------------------
            @Override public void onError(String errorMsg) {
                // can't use "sendPluginErrorToCallback" since this is an SDK error
                BlinkUpPluginResult errorResult = new BlinkUpPluginResult();
                errorResult.setState(BlinkUpPluginResult.STATE_ERROR);
                errorResult.setBlinkUpError(errorMsg);
                errorResult.sendResultsToCallback();
            }

            //---------------------------------
            // give timeout message to Cordova
            //---------------------------------
            @Override public void onTimeout() {
                BlinkUpPluginResult.sendPluginErrorToCallback(BlinkUpPlugin.ERROR_PROCESS_TIMED_OUT);
            }
        };

        // request the device info from the server
        BlinkupController.getInstance().getTokenStatus(tokenStatusCallback, timeoutMs);
    }
}
