package com.kinchaku.stera.printer

import com.panasonic.smartpayment.android.api.Result

interface PrinterListener {
    fun onPrintReceipt(result: Result?)
}