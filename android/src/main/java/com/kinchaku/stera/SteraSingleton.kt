package com.kinchaku.stera

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.facebook.react.bridge.Promise
import com.kinchaku.stera.customerdisplay.CustomerDisplay
import com.kinchaku.stera.customerdisplay.ISteraCustomerDisplayListener
import com.kinchaku.stera.paymentapi.IPaymentApiInitializationListener
import com.kinchaku.stera.paymentapi.PaymentApiConnection
import com.kinchaku.stera.printer.PrintTicket
import com.kinchaku.stera.printer.PrintXML
import com.kinchaku.stera.printer.PrinterListener
import com.kinchaku.stera.printer.SaveToBMP
import com.kinchaku.stera.qrcode.Encoder
import com.panasonic.smartpayment.android.api.Result
import java.io.File
import java.io.FileOutputStream


@SuppressLint("StaticFieldLeak")
object SteraSingleton {

    private var contentResolver: ContentResolver? = null
    private var context: Context? = null

    fun initialize(context: Context) {
        contentResolver = context.contentResolver
        this.context = context.applicationContext
    }

    private var mPaymentApiConnection: PaymentApiConnection? = null
    private var mCustomerDisplay: CustomerDisplay? = null
    private const val TAG = "SteraSingleton"

    private val mCallbackHandler: Handler = Handler(Looper.getMainLooper())
    private var mRunnable: Runnable? = null
    private var mUsingCustomerDisplay = false
    var mHasPermission = false
    var imageURL: String? = null
    private var savedImagePath: String? = null

    // list of result code of printing error
    const val SUCCESS = 0x00

    private fun downloadImage(url: String, onLoaded: (m: String) -> Unit) {
        val fileName = "IMG_" + System.currentTimeMillis().toString() + ".jpg"
        Log.d(TAG, "Downloading image. Filename: $fileName")

        Glide.with(context!!)
            .asBitmap()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap?>?,
                ) {
                    write(fileName, resource)
                    onLoaded(fileName)
                }
            })
    }

    fun write(fileName: String, bitmap: Bitmap) {
        val imageFile = File(Environment.getExternalStorageDirectory().absolutePath, fileName)
        var outputStream = FileOutputStream(imageFile)
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 45, outputStream)
            Log.d(TAG, "Saved image")
        } catch (error: Exception) {
            error.printStackTrace()
        } finally {
            outputStream.close()
        }
    }

    fun onResume(promise: Promise? = null) {
        // Show QR code image on CustomerDisplay
        mPaymentApiConnection = PaymentApiConnection()
        mCustomerDisplay = CustomerDisplay(context!!.packageName, context!!)

        // check if PaymentApi is connected
        mPaymentApiConnection!!.setIPaymentApiInitializationListener(object : IPaymentApiInitializationListener {
            // PaymentApi is connected
            override fun onApiConnected() {
                Log.d(TAG, "[in] onApiConnected. URL: $imageURL")
                mCallbackHandler.post(Runnable {
                    mUsingCustomerDisplay = !imageURL.isNullOrBlank()
                    if (!mHasPermission) {
                        Log.i(TAG, "Don't have storage permission")
                        promise?.reject("no_permisson", "No storage permission")
                        return@Runnable
                    }
                    if (!mUsingCustomerDisplay) {
                        Log.d(TAG, "No image set. Skipping")
                        return@Runnable
                    }

                    downloadImage(imageURL!!) { fileName ->
                        val imageFile = File(Environment.getExternalStorageDirectory().absolutePath, fileName)
                        savedImagePath = imageFile.absolutePath
                        if (!imageFile.exists()) {
                            Log.d(TAG, "File does not exist: $savedImagePath")
                            savedImagePath = null
                            promise?.reject("download_failed", "Image saving failed")
                            return@downloadImage
                        }
                        val fileSize = imageFile.length()
                        Log.d(TAG, "Downloaded image $savedImagePath, size: " + fileSize / 1024.0)

                        // Get PaymentDeviceManager instance
                        val iPaymentDeviceManager = mPaymentApiConnection!!.iPaymentDeviceManager
                        // Show QR code image on CustomerDisplay
                        try {
                            mCustomerDisplay?.initializeCustomerDisplay(iPaymentDeviceManager!!, savedImagePath!!)
                        } catch (e: Exception) {
                            promise?.reject("init_failed", e.message)
                            return@downloadImage
                        }
                        if (promise !== null) {
                            mCustomerDisplay?.setListener(object : ISteraCustomerDisplayListener {
                                override fun onOpenComplete(result: Boolean) {
                                    Log.d(TAG, "[in] onOpenComplete()")
                                    mCallbackHandler.post {
                                        if (result) {
                                            promise?.resolve(true)
                                        } else {
                                            promise?.reject("open_display", "Display open failed")
                                        }
                                    }
                                    Log.d(TAG, "[out] onOpenComplete()")
                                }

                                override fun onDetectButton(button: Int) {
                                    //nothing to do here
                                }
                            })
                        }
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

    fun showImage(url: String, promise: Promise?) {
        imageURL = url
        onResume(promise)
    }

    fun hideImage() {
        if (!savedImagePath.isNullOrEmpty()) {
            val imageFile = File(savedImagePath!!)
            imageFile.delete()
        }
        imageURL = null
        savedImagePath = null
        onPause()
    }

    fun printTicket(
        line1: String,
        line2: String,
        line3: String,
        line4: String,
        str: String?,
        promise: Promise?,
    ) {
        val iPaymentDeviceManager = mPaymentApiConnection!!.iPaymentDeviceManager

        var imageSource: String? = null
        if (str != null) {
            val saver = SaveToBMP()
            val encoder = Encoder(200)
            val bm = encoder.encodeAsBitmap(str)
            val fileName = "IMG_" + System.currentTimeMillis().toString() + ".bmp"
            val imageFile = File(Environment.getExternalStorageDirectory().absolutePath, fileName)

            imageSource = imageFile.absolutePath
            saver.save(bm, imageSource)

            if (!imageFile.exists()) {
                Log.d(TAG, "File does not exist: $imageSource")
                promise?.reject("qr_fail", "QR code encoder failed to save image")
                return
            }
        }

        val printTicket = PrintTicket(
            line1,
            line2,
            line3,
            line4,
            imageSource)

        if (iPaymentDeviceManager == null) {
            Log.d(TAG, "No payment device manager")
            promise?.reject("not_initialized", "Payment API is not initialized")
            return
        }

        // Print ticket
        printTicket.print(iPaymentDeviceManager!!)

        // Check if printing is successful
        printTicket.setPrinterListener(object : PrinterListener {
            override fun onPrintReceipt(result: Result?) {
                if (!imageSource.isNullOrEmpty()) {
                    val imageFile = File(imageSource)
                    if (imageFile.exists()) {
                        imageFile.delete()
                    }
                }
                Log.d(TAG, "[in] onPrintReceipt")
                mCallbackHandler.post {
                    if (result?.resultCode == SUCCESS) {
                        Log.d(TAG, "Printing was successful")
                        promise?.resolve(true)
                    } else {
                        promise?.reject(result?.resultCode.toString(), result?.message)
                    }
                }
                Log.d(TAG, "[out] onPrintReceipt")
            }
        })
    }

    fun printXML(xml: String, str: String?, promise: Promise?) {
        val iPaymentDeviceManager = mPaymentApiConnection!!.iPaymentDeviceManager

        var imageSource: String? = null
        if (str != null) {
            val saver = SaveToBMP()
            val encoder = Encoder(200)
            val bm = encoder.encodeAsBitmap(str)
            val fileName = "IMG_" + System.currentTimeMillis().toString() + ".bmp"
            val imageFile = File(Environment.getExternalStorageDirectory().absolutePath, fileName)

            imageSource = imageFile.absolutePath
            saver.save(bm, imageSource)

            if (!imageFile.exists()) {
                Log.d(TAG, "File does not exist: $imageSource")
                promise?.reject("qr_fail", "QR code encoder failed to save image")
                return
            }
        }

        val printXML = PrintXML(xml, imageSource)

        if (iPaymentDeviceManager == null) {
            Log.d(TAG, "No payment device manager")
            promise?.reject("not_initialized", "Payment API is not initialized")
            return
        }

        // Print xml
        printXML.print(iPaymentDeviceManager!!)

        // Check if printing is successful
        printXML.setPrinterListener(object : PrinterListener {
            override fun onPrintReceipt(result: Result?) {
                if (!imageSource.isNullOrEmpty()) {
                    val imageFile = File(imageSource)
                    if (imageFile.exists()) {
                        imageFile.delete()
                    }
                }
                Log.d(TAG, "[in] onPrintReceipt")
                mCallbackHandler.post {
                    if (result?.resultCode == SUCCESS) {
                        Log.d(TAG, "Printing was successful")
                        promise?.resolve(true)
                    } else {
                        promise?.reject(result?.resultCode.toString(), result?.message)
                    }
                }
                Log.d(TAG, "[out] onPrintReceipt")
            }
        })
    }

}