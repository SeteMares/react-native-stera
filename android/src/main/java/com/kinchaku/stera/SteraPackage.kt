package com.kinchaku.stera

import com.kinchaku.stera.DisplaySingleton
import com.kinchaku.stera.SteraModule

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.JavaScriptModule
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import java.util.Arrays

class SteraPackage : ReactPackage {

    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        DisplaySingleton.initialize(reactContext)
        return listOf(SteraModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext) =
        emptyList<ViewManager<*, *>>()
}