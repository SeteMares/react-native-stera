package com.kinchaku.stera.printer

import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import com.panasonic.smartpayment.android.api.*

class PrintXML(
    private var xml: String,
    private val imageSource: String?,
) {
    private val mCallbackHandler = Handler(Looper.getMainLooper())
    private var mListener: PrinterListener? = null

    fun setPrinterListener(listener: PrinterListener?) {
        mListener = listener
    }

    // Check if printing is success
    private val mIReceiptPrinterListener: IReceiptPrinterListener = object : IReceiptPrinterListener.Stub() {
        @Throws(RemoteException::class)
        override fun onPrintReceipt(result: Result) {
            Log.d(TAG, "[in] onPrintReceipt()")
            mCallbackHandler.post { // Set result to PrinterListener
                mListener!!.onPrintReceipt(result)
            }
            Log.d(TAG, "[out] onPrintReceipt()")
        }
    }

    fun print(paymentDeviceManager: IPaymentDeviceManager) {
        Log.d(TAG, "[in] print()")

        // Get PaymentDeviceManager instance
        val receiptPrinter = paymentDeviceManager.receiptPrinter
        if (imageSource != null) {
            xml = xml.replace("{image}", imageSource)
        }

        try {
            // Print receipt
            receiptPrinter.printReceipt(xml, mIReceiptPrinterListener)
        } catch (eTransaction: TransactionException) {
            eTransaction.printStackTrace()
        } catch (eFatal: FatalException) {
            val resultCode = String.format("0x%08X", eFatal.resultCode)
            val errorMessage = eFatal.message
            val additionalInformation = eFatal.additionalInformation

            // Show log of error information
            Log.d(TAG, "errorResultCode=$resultCode")
            Log.d(TAG, "errorMessage=$errorMessage")
            Log.d(TAG, "additionalInformation=$additionalInformation")
        }
        Log.d(TAG, "[out] printXml()")
    }

    companion object {
        private const val TAG = "PrintTicket"
    }
}