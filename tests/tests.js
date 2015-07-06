/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

exports.defineAutoTests = function() {
    describe('BlinkUp plugin', function () {
        
        // check that plugin exists
        it("should be defined", function () {
            expect(window.blinkup).toBeDefined();
        });
        
         // valid API key should not call callback
        it("should give no error if apiKey valid", function (done) {
            var callbackCalled = false;
            var callback = function (message) {
                // ignore error if it's "unable to verify api key". You can avoid this by using a real API key below
                if (message !== "") {
                    try {
                        var errCode = JSON.parse(message).error.errorCode;
                        if (errCode !== "31" && errCode != "300") {
                            callbackCalled = true;
                        }
                    } catch (exception) {
                        console.log("Error parsing JSON in blinkUpCallback");
                    }
                }            
            };

            // start blinkup with pseudo-valid (32 alpha-numeric chars) api key
            window.blinkup.invokeBlinkUp("abcdefghijklmnopqrstuvwxyz123456", "", 10000, false, callback, callback);

            setTimeout(function () {
                expect(callbackCalled).toBe(false);
                done();
            }, 1000);
        });

        // invalid API key check
        // NOTE: this will fail if using older versions of plugin
        it("should give error 103 when invalid API key", function (done) {
            
            // raise timeout limit
            window.jasmine.DEFAULT_TIMEOUT_INTERVAL = 20000;
            
            var jsonData;
            var callback = function (message) {
                try {
                    jsonData = JSON.parse(message);
                    expect(jsonData).toBeDefined();
                    expect(jsonData.state).toBe("error");
                    expect(jsonData.error.errorType).toBe("plugin");
                    expect(jsonData.error.errorCode).toBe("103");
                    done();
                } catch (exception) {
                    console.log("Error parsing JSON in blinkUpCallback:" + exception);
                }
            };

            // start blinkup with empty api key
            window.blinkup.invokeBlinkUp("", "", 10000, false, callback, callback);
        });

        // clearBlinkUp status code
        it("should give status code 202 when clearBlinkUpData called", function (done) {
            var jsonData;
            var callback = function (message) {
                try {
                    jsonData = JSON.parse(message);
                    expect(jsonData).toBeDefined();
                    expect(jsonData.state).toBe("completed");
                    expect(jsonData.statusCode).toBe("202");
                    done();
                } catch (exception) {
                    console.log("Error parsing JSON in blinkUpCallback:" + exception);
                }
            };

            // start blinkUp to clear data and cache
            window.blinkup.clearBlinkUpData(callback, callback);
        });
    });
};