package com.kinchaku.stera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.kinchaku.stera.customerdisplay.CustomerDisplay
import com.kinchaku.stera.paymentapi.IPaymentApiInitializationListener
import com.kinchaku.stera.paymentapi.PaymentApiConnection
import java.io.File
import java.io.FileOutputStream
import java.util.*


class Display : AppCompatActivity() {

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var mPaymentApiConnection: PaymentApiConnection? = null
    private var mCustomerDisplay: CustomerDisplay? = null
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if we have the permission to access the terminal storage
        // Check if we have the permission to access the terminal storage
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            mHasPermission = false
            // We don't have permission so prompt the user
            requestPermission()
        } else {
            mHasPermission = true
        }
    }

    private val mCallbackHandler: Handler = Handler(Looper.getMainLooper())
    private var mRunnable: Runnable? = null
    private var mUsingCustomerDisplay = false
    private var mHasPermission = false

    companion object {
        const val SUCCESS = 0
        const val FAIL = 1
        const val CANCEL = 2
    }

    private fun downloadImage(url: String, onLoaded: (m: String) -> Unit) {
        val fileName = "IMG_" + System.currentTimeMillis().toString() + ".jpg"
        Log.d(TAG, "Downloading image:$fileName")

        Glide.with(this)
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

    fun write(fileName: String?, bitmap: Bitmap) {
        var outputStream: FileOutputStream? = null
        try {
            outputStream = this.openFileOutput(fileName, MODE_PRIVATE)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Log.d(TAG, "Saved image")
        } catch (error: Exception) {
            error.printStackTrace()
        } finally {
            outputStream?.close()
        }
    }

    override fun onResume() {
        super.onResume()
        setContentView(R.layout.activity_main)

        // Show QR code image on CustomerDisplay
        mPaymentApiConnection = PaymentApiConnection()
        mCustomerDisplay = CustomerDisplay(CustomerDisplay.IMAGE, packageName, this@MainActivity)

        // check if PaymentApi is connected
        mPaymentApiConnection!!.setIPaymentApiInitializationListener(object : IPaymentApiInitializationListener {
            // PaymentApi is connected
            override fun onApiConnected() {
                Log.d(TAG, "[in] onApiConnected")
                mCallbackHandler.post(Runnable {
                    mUsingCustomerDisplay = true
                    downloadImage("https://dev.kinchaku.me/passkit/hSGQc8srYPSc8scd65ra7U/qrcode?size=480") { fileName ->
                        val imageFile = File(applicationContext.filesDir, fileName)
                        val savedImagePath = imageFile.absolutePath
                        if (!imageFile.exists()) {
                            Log.d(TAG, "File does not exist!$savedImagePath")
                            return@downloadImage
                        }
                        Log.d(TAG, "Downloaded image $savedImagePath")
                        val myBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                        val myImage: ImageView = findViewById(R.id.imageView)
                        myImage.setImageBitmap(myBitmap)

                        // Get PaymentDeviceManager instance
                        val iPaymentDeviceManager = mPaymentApiConnection!!.iPaymentDeviceManager
                        // Show QR code image on CustomerDisplay
                        mCustomerDisplay!!.initializeCustomerDisplay(iPaymentDeviceManager!!, savedImagePath)
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
        mPaymentApiConnection!!.initializePaymentApi(this)

        // Get SDK Version
        val sdkVersion = mPaymentApiConnection!!.sdkVersion
        Log.d(TAG, "SDK version=$sdkVersion")

    }

    // Terminate CustomerDisplayApi and PaymentApi
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "[in] onPause()")
        if (mUsingCustomerDisplay) {
            Log.d(TAG, "Handler is running.")
            mCustomerDisplay?.terminateCustomerDisplay(true)
            mPaymentApiConnection!!.terminatePaymentApi(this@MainActivity)
        } else {
            Log.d(TAG, "Handler didn't run.")
        }
        if (mRunnable !== null) {
            mCallbackHandler.removeCallbacks(mRunnable!!)
        }
        mUsingCustomerDisplay = false
    }

    /* Get result of transaction
     * Send it to ResultActivity
     * requestCode : 1=transaction, 2=reprint, 3=dailybalance
     * resultCode : 0=success, 1=fail, 2=cancel
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Log.d(TAG, "[in] onActivityResult()")
        Log.d(TAG, "requestCode=$requestCode")
        Log.d(TAG, "resultCode=$resultCode")
        val intentResult = Intent(this@MainActivity, ResultActivity::class.java)
        intentResult.putExtra("ResultCode", resultCode)
        intentResult.putExtra("TransactionType", requestCode.toString())
//        if (requestCode == REPRINT) {
//            Log.d(TAG, "PaymentType=" + intent.getStringExtra("PaymentType"))
//            if (resultCode == SUCCESS) {
//                intentResult.putExtra("Result", getString(R.string.judgereprintOK))
//            } else if (resultCode == FAIL) {
//                Log.d(TAG, "ErrorCodeReprint=" + intent.getStringExtra("ErrorCode"))
//                intentResult.putExtra("Result", getString(R.string.judgereprintNG))
//            } else if (resultCode == CANCEL) {
//                intentResult.putExtra("Result", getString(R.string.judgereprintCancel))
//            } else {
//                Log.d(TAG, "MainActivity gets incorrect resultCode. resultCode=$resultCode")
//            }
//        } else if (requestCode == DAILYBALANCE) {
//            if (resultCode == SUCCESS) {
//                intentResult.putExtra("Result", getString(R.string.judgedailybalanceOK))
//            } else if (resultCode == FAIL) {
//                Log.d(TAG, "PaymentType=" + Arrays.toString(intent.getStringArrayExtra("PaymentType")))
//                Log.d(TAG, "ErrorCodeSummary=" + Arrays.toString(intent.getStringArrayExtra("ErrorCode")))
//                intentResult.putExtra("Result", getString(R.string.judgedailybalanceNG))
//            } else if (resultCode == CANCEL) {
//                intentResult.putExtra("Result", getString(R.string.judgedailybalanceCancel))
//            } else {
//                Log.d(TAG, "MainActivity got incorrect resultCode. resultCode=$resultCode")
//            }
//        }
        startActivity(intentResult)
        Log.d(TAG, "[out] onActivityResult()")
    }

    override fun onBackPressed() {
        // Finish this Activity
        finishAndRemoveTask()
    }

    //Request the external storage permission
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            PERMISSIONS_STORAGE,
            REQUEST_EXTERNAL_STORAGE
        )
    }

    //Get the result of requesting permission by callback
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "We get the external storage permission")
                // We have permission
                mHasPermission = true
            } else {
                Log.d(TAG, "We wasn't able to get the external storage permission")
                // We don't have permission so prompt the user again
                mHasPermission = false
                requestPermission()
            }
        }
    }

}