package com.kinchaku.stera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.SparseArray
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener

class SteraModule(
    private val reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext),
    LifecycleEventListener,
    ActivityEventListener,
    PermissionListener {

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private lateinit var eventEmitter: DeviceEventManagerModule.RCTDeviceEventEmitter
    private var mPromises: SparseArray<Promise?>? = null

    companion object {
        const val TAG = "SteraModule"
        const val SUCCESS = 0
        const val FAIL = 1
        const val CANCEL = 2
        const val TRANSACTION = 1801
    }

    override fun getName() = "Stera"

    override fun initialize() {
        super.initialize()
        Log.d(TAG, "DEVICE=" + Build.MODEL)
        if (Build.MODEL != "JT-C60" && Build.MODEL != "JT-VT10") {
            Log.i(TAG, "Skipping init. Not a Panasonic device: " + Build.MODEL)
            return
        }
        reactContext.addActivityEventListener(this)

        eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        reactContext.addLifecycleEventListener(this)
        val permission = ContextCompat.checkSelfPermission(reactContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            SteraSingleton.mHasPermission = false
            // We don't have permission so prompt the user
            requestPermission()
        } else {
            SteraSingleton.mHasPermission = true
        }
        mPromises = SparseArray()
    }

    @Nullable
    override fun getConstants(): Map<String, Any>? {
        val constants = HashMap<String, Any>()
        constants["OK"] = Activity.RESULT_OK
        constants["CANCELED"] = Activity.RESULT_CANCELED
        return constants
    }

    override fun onHostResume() {
        SteraSingleton.onResume()
    }

    override fun onHostPause() {
        // disconnect customer display here
        SteraSingleton.onPause()
    }

    override fun onHostDestroy() {

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
        grantResults: IntArray,
    ): Boolean {
        Log.d(TAG, "onRequestPermissionResult: $requestCode")
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "We've got the external storage permission")
                // We have permission
                SteraSingleton.mHasPermission = true
                return true
            }
            Log.d(TAG, "We weren't able to get the external storage permission")
            // We don't have permission so prompt the user again
            SteraSingleton.mHasPermission = false
            requestPermission()
            return true
        }

        return true
    }

    @ReactMethod
    fun getPayment(production: Boolean, subtotal: Int, tax: Int, promise: Promise?) {
        val salesIntent = SaleIntent()
        val intRequestCode = TRANSACTION
        val mTransactionMode: String = if (production) "1" else "2"
        val mTransactionType = "1"

        val intent: Intent = salesIntent.createSalesIntent(
            mTransactionMode, // 1 prod, 2 sandbox
            mTransactionType, // sales "1", cancel ”2”, returns ”3”
            subtotal.toString(),
            tax.toString()
        )
        val activity = reactApplicationContext.currentActivity
        activity!!.startActivityForResult(intent, intRequestCode)
        mPromises!!.put(intRequestCode, promise)

        Log.d(TAG, "TransactionMode=$mTransactionMode")
        Log.d(TAG, "TransactionType=$mTransactionType")
        Log.d(TAG, "Amount=$subtotal")
        Log.d(TAG, "Tax=$tax")
        Log.d(TAG, "RequestCode=$intRequestCode")
    }

    @ReactMethod
    fun getDeviceName(cb: Callback) {
        try {
            cb.invoke(null, Build.MODEL)
        } catch (e: Exception) {
            cb.invoke(e.toString(), null)
        }
    }

    @ReactMethod
    fun displayImage(url: String, promise: Promise?) {
        if (!SteraSingleton.mHasPermission) {
            promise?.reject("no_permisson", "No storage permission")
            return
        }
        SteraSingleton.showImage(url, promise)
    }

    @ReactMethod
    fun showMessage(headers: ReadableMap, url: String, promise: Promise?) {
        if (!SteraSingleton.mHasPermission) {
            promise?.reject("no_permisson", "No storage permission")
            return
        }
        SteraSingleton.showMessage(headers, url, promise)
    }

    @ReactMethod
    fun hideImage(promise: Promise?) {
        if (!SteraSingleton.mHasPermission) {
            promise?.reject("no_permisson", "No storage permission")
            return
        }
        SteraSingleton.hideImage()
        promise?.resolve(true)
    }

    @ReactMethod
    fun isSupported(promise: Promise?) {
        promise?.resolve(Build.MODEL == "JT-C60" || Build.MODEL == "JT-VT10")
    }

    @ReactMethod
    fun printTicket(
        ticket: ReadableMap,
        str: String?,
        promise: Promise?,
    ) {
        if (!SteraSingleton.mHasPermission) {
            promise?.reject("no_permisson", "No storage permission")
            return
        }
        SteraSingleton.printTicket(ticket, str, promise)
    }

    @ReactMethod
    fun printXML(xml: String, str: String?, promise: Promise?) {
        if (!SteraSingleton.mHasPermission) {
            promise?.reject("no_permisson", "No storage permission")
            return
        }
        SteraSingleton.printXML(xml, str, promise)
    }

    /* Get result of transaction
     * Send it to ResultActivity
     * requestCode : 1=transaction, 2=reprint, 3=dailybalance
     * resultCode : 0=success, 1=fail, 2=cancel
     */
    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "[in] onActivityResult()")
        Log.d(TAG, "requestCode=$requestCode")
        Log.d(TAG, "resultCode=$resultCode")

        val promise = mPromises!![requestCode]
        if (promise != null) {
            when (resultCode) {
                SUCCESS -> {
                    Log.d(TAG, "SUCCESS")
                }
                FAIL -> {
                    Log.d(TAG, "ErrorCodeSales=" + data!!.getStringExtra("ErrorCode"))
                    Log.d(TAG, "FAIL")
                }
                CANCEL -> {
                    Log.d(TAG, "CANCEL")
                }
                else -> {
                    Log.d(TAG, "Incorrect resultCode. resultCode=$resultCode")
                }
            }
            val result: WritableMap = WritableNativeMap()
            with(promise) {
                result.putInt("resultCode", resultCode)
                result.putMap("data", Arguments.makeNativeMap(data!!.extras))
                resolve(result)
            }
            Log.d(TAG, "[out] onActivityResult()")
            return
        }
    }

    override fun onNewIntent(intent: Intent?) {
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        reactContext.removeActivityEventListener(this)
    }
}
