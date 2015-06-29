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

/*****************************************************
 * When the clearing BlinkUpPlugin process completes, it
 * executes the BlinkUpClearIntent set in BlinkUpPlugin.java,
 * starting this activity, which tells the callback
 * that clearing is complete, then dismisses
 ******************************************************/
public class ClearCompleteActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // send callback that we've cleared device
        BlinkUpPluginResult clearResult = new BlinkUpPluginResult();
        clearResult.setState(BlinkUpPluginResult.BlinkUpPluginState.Completed);

        // set the status code depending if we just cleared the cache
        if (BlinkUpPlugin.clearedCache) {
            clearResult.setStatusCode(BlinkUpPlugin.StatusCodes.CLEAR_WIFI_AND_CACHE_COMPLETE.getCode());
            BlinkUpPlugin.clearedCache = false;
        }
        else {
            clearResult.setStatusCode(BlinkUpPlugin.StatusCodes.CLEAR_WIFI_COMPLETE.getCode());
        }

        clearResult.sendResultsToCallback();

        this.finish();
    }
}
