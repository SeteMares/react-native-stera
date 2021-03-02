package com.kinchaku.stera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener


class SteraModule(
    private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext),
    LifecycleEventListener,
    PermissionListener {

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private lateinit var eventEmitter: DeviceEventManagerModule.RCTDeviceEventEmitter

    private val TAG = "SteraModule"

    override fun getName() = "Stera"

    override fun initialize() {
        super.initialize()
        eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        reactContext.addLifecycleEventListener(this)
        val permission = ContextCompat.checkSelfPermission(reactContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            DisplaySingleton.mHasPermission = false
            // We don't have permission so prompt the user
            requestPermission()
        } else {
            DisplaySingleton.mHasPermission = true
        }
    }

    override fun onHostResume() {
        DisplaySingleton.onResume()
    }

    override fun onHostPause() {
        // disconnect customer display here
        DisplaySingleton.onPause()
    }

    override fun onHostDestroy() {
//        try {
//            DisplaySingleton.apply {

//            }
//        } finally {
//            DisplaySingleton.contentResolver = null
//            DisplaySingleton.jobs.forEach {
//                it.cancel()
//            }
//        }
    }

    //Request the external storage permission
    private fun requestPermission() {
        val activity = currentActivity as PermissionAwareActivity?
        activity?.requestPermissions(PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE, this)
    }

    //Get the result of requesting permission by callback
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "We get the external storage permission")
                // We have permission
                DisplaySingleton.mHasPermission = true
                return true
            }
            Log.d(TAG, "We wasn't able to get the external storage permission")
            // We don't have permission so prompt the user again
            DisplaySingleton.mHasPermission = false
            requestPermission()
            return true
        }

        return true
    }

//    @ReactMethod
//    fun initialize(stringArgument: String, numberArgument: Int, callback: Callback) {
//        // TODO: Implement some actually useful functionality
//        callback.invoke("Received numberArgument: $numberArgument stringArgument: $stringArgument")
//        val context: Context = reactApplicationContext
//    }

    @ReactMethod
    fun getDeviceName(cb: Callback) {
        try {
            cb.invoke(null, Build.MODEL)
        } catch (e: Exception) {
            cb.invoke(e.toString(), null)
        }
    }

}
