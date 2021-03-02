package com.kinchaku.stera.paymentapi

interface IPaymentApiInitializationListener {
    fun onApiConnected()
    fun onApiDisconnected()
}