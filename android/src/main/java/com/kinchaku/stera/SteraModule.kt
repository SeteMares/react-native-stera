package com.kinchaku.stera

import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import android.content.Context

class SteraModule(
    private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "Stera"

    @ReactMethod
    fun initialize(stringArgument: String, numberArgument: Int, callback: Callback) {
        // TODO: Implement some actually useful functionality
        callback.invoke("Received numberArgument: $numberArgument stringArgument: $stringArgument")
        val context: Context = reactApplicationContext
    }

}