package com.kinchaku.stera

import android.content.Intent
import android.util.Log

class SaleIntent {
    companion object {
        private const val TAG = "SalesIntent"
    }

    /*  This method is creating an intent of sales transaction for sales menu
     *  transactionMode : "1"=Normal, "2"=Training
     *  transactionType : "1"=sales, "2"=cancel, "3"=return, "4"=reprint, "5"=daily balance
     *  subtotal (except tax) : Max length = 8
     *  tax : Max length = 8
     */
    fun createSalesIntent(
        transactionMode: String?,
        transactionType: String?,
        subtotal: String?,
        tax: String?
    ): Intent {
        Log.d(TAG, "[in] createSalesIntent()")
        val intentResult = Intent()
        intentResult.setClassName(
            "com.panasonic.smartpayment.android.salesmenu",
            "com.panasonic.smartpayment.android.salesmenu.MainActivity"
        )
        intentResult.putExtra("TransactionMode", transactionMode)
        intentResult.putExtra("TransactionType", transactionType)
        intentResult.putExtra("Amount", subtotal)
        intentResult.putExtra("Tax", tax)
        Log.d(TAG, "[out] createSalesIntent()")
        return intentResult
    }
}