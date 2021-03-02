package com.kinchaku.stera.customerdisplay

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.kinchaku.stera.R
import com.panasonic.smartpayment.android.api.*

class CustomerDisplay(val displayType: String, private val packageName: String, private val context: Context) {

    private var mICustomerDisplay: ICustomerDisplay? = null
    private val mCallbackHandler = Handler(Looper.getMainLooper())
    private var mListener: ISteraCustomerDisplayListener? = null
    private val mCustomerDisplayImage = CustomerDisplayImage()
    private val mCustomerDisplayCheckOut = CustomerDisplayCheckOut()
    private var imageSource: String? = null

    // Set listener of ICustomerDisplayListener (for buttons activity)
    fun setICustomerDisplayListener(listener: ISteraCustomerDisplayListener?) {
        mListener = listener
    }

    private val mICustomerDisplayListener: ICustomerDisplayListener = object : ICustomerDisplayListener.Stub() {
        // Check if CustomerDisplay is connected
        override fun onOpenComplete(result: Boolean) {
            Log.d(TAG, "[in] onOpenComplete()")
            mCallbackHandler.post {
                if (result) {
                    Log.d(TAG, "result=$imageSource")

                    // Set image for CustomerDisplay
                    setupImage(imageSource!!)
                    try {
                        // Show image on CustomerDisplay
                        showDisplay(displayType)
                    } catch (e: ArgumentException) {
                        e.printStackTrace()
                    } catch (e: FatalException) {
                        e.printStackTrace()
                    }
                } else {
                    Log.d(TAG, "CustomerDisplay was not opened")
                    Log.d(TAG, "result=false")
                }
            }
            Log.d(TAG, "[out] onOpenComplete()")
        }

        // Get kinds of touched button on CustomerDisplay
        override fun onDetectButton(button: Int) {
            Log.d(TAG, "[in] onDetectButton()")
            mCallbackHandler.post { // Button press event notification on CustomerDisplay device.
                Log.d(TAG, "button=$button")
                mListener!!.onDetectButton(button)
            }
            Log.d(TAG, "[out] onDetectButton()")
        }
    }

    // call api to initialize CustomerDisplay device.
    fun initializeCustomerDisplay(mIPaymentDeviceManager: IPaymentDeviceManager, imageSource: String) {
        Log.d(TAG, "[in] initializeCustomerDisplay()")
        try {
            Log.d(TAG, "Application PackageName =$packageName")
            Log.d(TAG, imageSource)
            this.imageSource = imageSource

            mICustomerDisplay = mIPaymentDeviceManager.customerDisplay
            mICustomerDisplay?.registerCustomerDisplayListeners(packageName, mICustomerDisplayListener)
            mICustomerDisplay?.openCustomerDisplay(packageName)
        } catch (eArgument: ArgumentException) {
            eArgument.printStackTrace()
        } catch (eFatal: FatalException) {
            eFatal.printStackTrace()
            if (eFatal.additionalInformation == "113") {
                val dialog = AlertDialog.Builder(context)
                dialog.setMessage(context.getString(R.string.errorShutDown))
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                Log.d(TAG, "Caught an exception, code=" + eFatal.additionalInformation)
            }
        }
        Log.d(TAG, "[out] initializeCustomerDisplay()")
    }

    // Set image for CustomerDisplay
    private fun setupImage(displayType: String) {
        Log.d(TAG, "[in] setupImage()")
        try {
            Log.d(TAG, "setup QR Code Image" + this.imageSource)
            mICustomerDisplay?.setCustomerImage(
                packageName,
                mCustomerDisplayImage.imageKind,
                mCustomerDisplayImage.imageNumber,
                this.imageSource
            )
        } catch (e: ArgumentException) {
            e.printStackTrace()
        } catch (e: FatalException) {
            e.printStackTrace()
        }
        Log.d(TAG, "[out] setupImage()")
    }

    // Show image on CustomerDisplay
    fun showDisplay(displayType: String) {
        Log.d(TAG, "[in] showDisplay()")
        Log.d(TAG, "displayType=$displayType")

        try {
            Log.d(TAG, "showDisplay QR Code Screen")
            mICustomerDisplay?.doDisplayScreen(packageName, mCustomerDisplayImage.xml)
        } catch (e: ArgumentException) {
            e.printStackTrace()
        } catch (e: FatalException) {
            e.printStackTrace()
        }
        Log.d(TAG, "[out] showDisplay()")
    }

    // Finish using CustomerDisplay
    fun terminateCustomerDisplay(displayOff: Boolean) {
        Log.d(TAG, "[in] terminateCustomerDisplay()")
        if (mICustomerDisplay == null) {
            return;
        }
        try {
            mICustomerDisplay?.closeCustomerDisplay(packageName, displayOff)
            mICustomerDisplay?.unregisterCustomerDisplayListeners(packageName)
        } catch (e: ArgumentException) {
            e.printStackTrace()
        } catch (e: FatalException) {
            e.printStackTrace()
        }
        Log.d(TAG, "[out] terminateCustomerDisplay()")
    }

    companion object {
        private const val TAG = "CustomerDisplay"
        const val IMAGE = "image"
        const val CHECKOUT = "checkOut"
    }
}