package com.kinchaku.stera.paymentapi

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import com.panasonic.smartpayment.android.api.*

class PaymentApiConnection {
    private var mPaymentApi: PaymentApi? = null
    private var mIPaymentApi: IPaymentApi? = null
    private var mIPaymentDeviceManager: IPaymentDeviceManager? = null
    private var mListener: IPaymentApiInitializationListener? = null
    private val mCallbackHandler = Handler(Looper.getMainLooper())

    // Set listener of IPaymentApiInitializationListener
    fun setIPaymentApiInitializationListener(listener: IPaymentApiInitializationListener?) {
        Log.d(TAG, "[in] setIPaymentApiInitializationListener()")
        mListener = listener
        Log.d(TAG, "[out] setIPaymentApiInitializationListener()")
    }

    // check if PaymentApi is connected
    private val mPaymentApiListener: IPaymentApiListener = object : IPaymentApiListener.Stub() {
        // PaymentApi is connected
        @Throws(RemoteException::class)
        override fun onApiConnected() {
            Log.d(TAG, "[in] onApiConnected()")
            mCallbackHandler.post {
                try {
                    mIPaymentApi = mPaymentApi!!.paymentApi
                    mIPaymentDeviceManager = mIPaymentApi!!.paymentDeviceManager
                    mListener!!.onApiConnected()
                } catch (e: TransactionException) {
                    e.printStackTrace()
                } catch (e: FatalException) {
                    e.printStackTrace()
                }
            }
            Log.d(TAG, "[out] onApiConnected()")
        }

        // PaymentApi isn't connected
        @Throws(RemoteException::class)
        override fun onApiDisconnected() {
            Log.d(TAG, "PaymentApi is disconnected")
        }
    }

    // Connect to PaymentAPI Service.
    fun initializePaymentApi(context: Context?) {
        Log.d(TAG, "[in] initializePaymentApi()")
        mPaymentApi = PaymentApi()
        mPaymentApi!!.init(context, mPaymentApiListener)
        Log.d(TAG, "[out] initializePaymentApi()")
    }

    // Get SDK Version
    val sdkVersion: String
        get() {
            Log.d(TAG, "[in] getSdkVersion()")
            val sdkVersion = mPaymentApi!!.sdkVersion
            Log.d(TAG, "SDK version =$sdkVersion")
            Log.d(TAG, "[out] getSdkVersion()")
            return sdkVersion
        }

    // Return PaymentDeviceManager instance.
    val iPaymentDeviceManager: IPaymentDeviceManager?
        get() {
            Log.d(TAG, "[in] getmIPaymentDeviceManager()")
            Log.d(TAG, "[out] getmIPaymentDeviceManager()")
            return mIPaymentDeviceManager
        }

    // Finish using PaymentAPI
    fun terminatePaymentApi(context: Context?) {
        Log.d(TAG, "[in] terminatePaymentApi()")
        mPaymentApi!!.term(context)
        Log.d(TAG, "[out] terminatePaymentApi()")
    }

    companion object {
        private const val TAG = "PaymentApiConnection"
    }
}