package com.kinchaku.stera.customerdisplay

interface ISteraCustomerDisplayListener {
    fun onOpenComplete(result: Boolean)
    fun onDetectButton(button: Int)
}