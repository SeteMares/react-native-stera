package com.kinchaku.stera

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap

/**
 * A [Promise] that records how it was settled.
 *
 * Every defect these tests cover ends the same way -- a promise that is never settled,
 * or settled twice -- so [settlements] is the assertion that matters, not the value.
 */
internal class RecordingPromise : Promise {
    var resolvedWith: Any? = null
        private set
    var rejectedCode: String? = null
        private set
    var settlements = 0
        private set

    override fun resolve(value: Any?) {
        resolvedWith = value
        settlements++
    }

    override fun reject(code: String, message: String?) = record(code)

    override fun reject(code: String, throwable: Throwable?) = record(code)

    override fun reject(code: String, message: String?, throwable: Throwable?) = record(code)

    override fun reject(throwable: Throwable) = record(throwable.javaClass.name)

    override fun reject(throwable: Throwable, userInfo: WritableMap) = record(throwable.javaClass.name)

    override fun reject(code: String, userInfo: WritableMap) = record(code)

    override fun reject(code: String, throwable: Throwable?, userInfo: WritableMap) = record(code)

    override fun reject(code: String, message: String?, userInfo: WritableMap) = record(code)

    override fun reject(
        code: String?,
        message: String?,
        throwable: Throwable?,
        userInfo: WritableMap?,
    ) = record(code)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun reject(message: String) = record(message)

    private fun record(code: String?) {
        rejectedCode = code
        settlements++
    }
}
