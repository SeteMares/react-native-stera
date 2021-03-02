package com.kinchaku.stera

import android.content.ContentResolver
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.facebook.react.bridge.ReactApplicationContext
import com.kinchaku.stera.customerdisplay.CustomerDisplay
import com.kinchaku.stera.paymentapi.IPaymentApiInitializationListener
import com.kinchaku.stera.paymentapi.PaymentApiConnection
import java.io.File
import java.io.FileOutputStream
import java.util.*

object DisplaySingleton {

    private var contentResolver: ContentResolver? = null
    private var context: ReactApplicationContext? = null

    fun initialize(context: ReactApplicationContext) {
        contentResolver = context.contentResolver
        this.context = context
    }

    private var mPaymentApiConnection: PaymentApiConnection? = null
    private var mCustomerDisplay: CustomerDisplay? = null
    private const val TAG = "DisplaySingleton"

    private val mCallbackHandler: Handler = Handler(Looper.getMainLooper())
    private var mRunnable: Runnable? = null
    private var mUsingCustomerDisplay = false
    var mHasPermission = false

//    const val SUCCESS = 0
//    const val FAIL = 1
//    const val CANCEL = 2

    private fun downloadImage(url: String, onLoaded: (m: String) -> Unit) {
        val fileName = "IMG_" + System.currentTimeMillis().toString() + ".jpg"
        Log.d(TAG, "Downloading image. Filename: $fileName")

        Glide.with(context!!)
            .asBitmap()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap?>?
                ) {
                    write(fileName, resource)
                    onLoaded(fileName)
                }
            })
    }

    fun write(fileName: String, bitmap: Bitmap) {
        var outputStream: FileOutputStream? = null
        val imageFile = File(context!!.externalCacheDir, fileName)
        val savedImagePath = imageFile.absolutePath
        try {
            outputStream = context!!.openFileOutput(savedImagePath, MODE_PRIVATE)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Log.d(TAG, "Saved image: " + outputStream.fd.toString())
        } catch (error: Exception) {
            error.printStackTrace()
        } finally {
            outputStream?.close()
        }
    }

    fun onResume() {
        // Show QR code image on CustomerDisplay
        mPaymentApiConnection = PaymentApiConnection()
        mCustomerDisplay = CustomerDisplay(CustomerDisplay.IMAGE, context!!.packageName, context!!)

        // check if PaymentApi is connected
        mPaymentApiConnection!!.setIPaymentApiInitializationListener(object : IPaymentApiInitializationListener {
            // PaymentApi is connected
            override fun onApiConnected() {
                Log.d(TAG, "[in] onApiConnected")
                mCallbackHandler.post(Runnable {
                    mUsingCustomerDisplay = true
                    if (mHasPermission) {
                        downloadImage("https://dev.kinchaku.me/passkit/hSGQc8srYPSc8scd65ra7U/qrcode?size=480") { fileName ->
                            val imageFile = File(context!!.externalCacheDir, fileName)
                            val savedImagePath = imageFile.absolutePath
                            if (!imageFile.exists()) {
                                Log.d(TAG, "File does not exist!$savedImagePath")
                                return@downloadImage
                            }
                            Log.d(TAG, "Downloaded image $savedImagePath")

                            // Get PaymentDeviceManager instance
                            val iPaymentDeviceManager = mPaymentApiConnection!!.iPaymentDeviceManager
                            // Show QR code image on CustomerDisplay
                            mCustomerDisplay!!.initializeCustomerDisplay(iPaymentDeviceManager!!, savedImagePath)
                        }
                    } else {
                        Log.i(TAG, "Don't have storage permission, skipping image download")
                    }
                }.also { mRunnable = it })
                Log.d(TAG, "[out] onApiConnected")
            }

            // PaymentApi isn't connected
            override fun onApiDisconnected() {
                Log.d(TAG, "PaymentApi is disconnected")
            }
        })

        // Connect to PaymentAPI Service.
        mPaymentApiConnection!!.initializePaymentApi(context)

        // Get SDK Version
        val sdkVersion = mPaymentApiConnection!!.sdkVersion
        Log.d(TAG, "SDK version=$sdkVersion")

    }

    // Terminate CustomerDisplayApi and PaymentApi
    fun onPause() {
        Log.d(TAG, "[in] onPause()")
        if (mUsingCustomerDisplay) {
            Log.d(TAG, "Handler is running.")
            mCustomerDisplay?.terminateCustomerDisplay(true)
            mPaymentApiConnection!!.terminatePaymentApi(context!!)
        } else {
            Log.d(TAG, "Handler didn't run.")
        }
        if (mRunnable !== null) {
            mCallbackHandler.removeCallbacks(mRunnable!!)
        }
        mUsingCustomerDisplay = false
    }

}