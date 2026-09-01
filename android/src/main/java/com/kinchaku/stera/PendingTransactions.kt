package com.kinchaku.stera

import com.facebook.react.bridge.Promise

/**
 * The promises waiting on an activity result, keyed by request code.
 *
 * Its one rule is that a request code can be settled exactly once: [take] hands the
 * promise over and forgets it in the same step, so a second result for the same code
 * finds nothing rather than calling back into an already-settled promise.
 *
 * A plain map rather than a SparseArray. This holds one entry per in-flight
 * transaction -- there is nothing to optimise -- and SparseArray is an Android
 * framework class, which is a no-op stub in unit tests, so bookkeeping built on it
 * could not be tested off a device. That is how the settle-once bug shipped.
 */
internal class PendingTransactions {
    private val promises = HashMap<Int, Promise?>()

    val size: Int
        get() = promises.size

    /** Register [promise] as awaiting the result of [requestCode]. */
    fun await(requestCode: Int, promise: Promise?) {
        promises[requestCode] = promise
    }

    /** True while [requestCode] is still expecting a result. */
    fun isAwaiting(requestCode: Int): Boolean = promises.containsKey(requestCode)

    /**
     * Hand back the promise waiting on [requestCode] and forget it. Returns null when
     * nothing is waiting -- and also when a null promise was registered, which is why
     * [isAwaiting] exists.
     */
    fun take(requestCode: Int): Promise? = promises.remove(requestCode)
}
