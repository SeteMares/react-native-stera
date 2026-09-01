package com.kinchaku.stera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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

    /**
     * Built at construction rather than in initialize(). initialize() returns early on
     * non-stera hardware, so this used to stay null there while getPayment() still
     * force-unwrapped it -- an off-terminal call threw inside the @ReactMethod and the
     * JS await never settled.
     */
    private val mPromises = PendingTransactions()

    companion object {
        const val TAG = "SteraModule"
        const val SUCCESS = 0
        const val FAIL = 1
        const val CANCEL = 2
        const val TRANSACTION = 1801

        // The service PaymentApi.init() binds to. Resolving it is what actually
        // proves this is a stera terminal.
        private const val PAYMENT_API_ACTION =
            "com.panasonic.smartpayment.android.api.service.IPaymentApiService"
        private const val PAYMENT_API_PACKAGE = "com.panasonic.smartpayment.android.api"

        // JT-C60, JT-C61 and JT-VT10 all match. Deliberately a family pattern and not
        // an exact list: an exact list of "JT-C60"/"JT-VT10" silently disabled this
        // entire module on the JT-C61, and the Panasonic app development guideline
        // (JT-C60/C61 v2.02 §3.3.3.3) warns the model name changes between
        // generations. This is only a cheap pre-filter -- see isSteraTerminal().
        private val MODEL_PATTERN = Regex("^JT-[A-Z]+[0-9]+$")

        /**
         * Whether [model] names a device in the stera/tance family.
         *
         * Split out from isSteraTerminal() so it can be tested without a device: this
         * predicate is the whole reason the module was dead on the JT-C61.
         */
        internal fun isSupportedModel(model: String?): Boolean =
            model != null && MODEL_PATTERN.matches(model)

        /**
         * The extras Android delivered with an activity result, or an empty Bundle.
         *
         * Android delivers a null Intent on some cancel paths, and this used to be
         * `data!!.extras` -- which threw inside onActivityResult and left the promise
         * unsettled, hanging the caller.
         */
        internal fun resultExtras(data: Intent?): Bundle = data?.extras ?: Bundle()

        /**
         * True when this device is a stera terminal we can drive.
         *
         * The model pattern alone would happily claim support on a future JT-* device
         * that ships without the payment service, so it is confirmed by resolving the
         * PaymentApi service. Resolution is synchronous and side-effect free -- no
         * bind, so it is safe to call from initialize() and getConstants().
         */
        fun isSteraTerminal(context: Context): Boolean {
            if (!isSupportedModel(Build.MODEL)) return false
            val intent = Intent(PAYMENT_API_ACTION).setPackage(PAYMENT_API_PACKAGE)
            return context.packageManager.resolveService(intent, 0) != null
        }
    }

    override fun getName() = "Stera"

    override fun initialize() {
        super.initialize()
        Log.d(TAG, "DEVICE=" + Build.MODEL)
        if (!isSteraTerminal(reactContext)) {
            Log.i(TAG, "Skipping init. Not a stera terminal: " + Build.MODEL)
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
    }

    @Nullable
    override fun getConstants(): Map<String, Any>? {
        val constants = HashMap<String, Any>()
        constants["OK"] = Activity.RESULT_OK
        constants["CANCELED"] = Activity.RESULT_CANCELED
        // Exposed as a constant so JS can branch on it during its first render.
        // isSupported() is a promise, so every consumer of it sees `undefined`
        // until it resolves and takes the non-terminal path in the meantime.
        constants["isStera"] = isSteraTerminal(reactContext)
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
        permissions: Array<String>,
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
        if (activity == null) {
            // Force-unwrapping here threw while the app was backgrounded, and the
            // promise was then neither resolved nor rejected: the JS await never
            // returned and the till sat on a spinner.
            Log.w(TAG, "No current activity. Cannot start a transaction.")
            promise?.reject("no_activity", "No current activity to start the transaction from.")
            return
        }

        // One transaction at a time. Every call uses the same request code, so a second
        // one would overwrite the first entry: the first promise would never settle, and
        // whichever result arrived first would settle the second call instead.
        if (mPromises.isAwaiting(intRequestCode)) {
            Log.w(TAG, "A transaction is already awaiting a result. RequestCode=$intRequestCode")
            promise?.reject("in_progress", "A transaction is already in progress.")
            return
        }

        // Register before launching so a result cannot arrive before its promise is
        // known -- then take the registration back if the launch throws, or a
        // transaction that never started would hold the request code forever. The
        // activity is explicit and vendor-supplied, so it can be absent, disabled or
        // permission-guarded; none of those may leave the promise unsettled.
        mPromises.await(intRequestCode, promise)
        try {
            activity.startActivityForResult(intent, intRequestCode)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not start the transaction activity", e)
            mPromises.take(intRequestCode)
            promise?.reject("no_payment_activity", "Could not start the stera transaction activity.", e)
            return
        }

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
        promise?.resolve(isSteraTerminal(reactContext))
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

        // Settle exactly once. The entry used to be left in place, so an abandoned
        // transaction kept a stale promise under the same request code and the next
        // result for it hit an already-settled callback.
        val promise = mPromises.take(requestCode)
        if (promise != null) {
            when (resultCode) {
                SUCCESS -> {
                    Log.d(TAG, "SUCCESS")
                }
                FAIL -> {
                    Log.d(TAG, "ErrorCodeSales=" + data?.getStringExtra("ErrorCode"))
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
            result.putInt("resultCode", resultCode)
            result.putMap("data", Arguments.makeNativeMap(resultExtras(data)))
            promise.resolve(result)

            Log.d(TAG, "[out] onActivityResult()")
            return
        }

        Log.d(TAG, "[out] onActivityResult(). No promise for requestCode=$requestCode")
    }

    override fun onNewIntent(intent: Intent?) {
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        reactContext.removeActivityEventListener(this)
    }
}
