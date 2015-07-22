package com.macadamian.blinkup.util;


import org.apache.cordova.BuildConfig;

public class DebugUtils {
    public static void checkAssert(boolean condition) {
        if (BuildConfig.DEBUG && !condition) {
            throw new AssertionError();
        }
    }
}
