package com.kinchaku.stera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The settle-once rule. The registry entry used to be left in place after a result was
 * delivered, so an abandoned transaction kept a stale promise under the same request
 * code and the next result for that code called back into an already-settled promise.
 */
class PendingTransactionsTest {

    private val transaction = SteraModule.TRANSACTION

    @Test
    fun `take hands back the promise that was registered`() {
        val promise = RecordingPromise()
        val pending = PendingTransactions()

        pending.await(transaction, promise)

        assertSame(promise, pending.take(transaction))
    }

    @Test
    fun `take forgets the request code, so only the first result settles`() {
        val pending = PendingTransactions()
        pending.await(transaction, RecordingPromise())

        pending.take(transaction)

        assertFalse(pending.isAwaiting(transaction))
        assertNull(pending.take(transaction))
        assertEquals(0, pending.size)
    }

    @Test
    fun `a result for an unknown request code is simply absent`() {
        val pending = PendingTransactions()

        assertFalse(pending.isAwaiting(transaction))
        assertNull(pending.take(transaction))
    }

    @Test
    fun `a null promise is still tracked, and still cleared`() {
        // getPayment's promise parameter is nullable, so absent and present-but-null
        // have to stay distinguishable or the entry leaks.
        val pending = PendingTransactions()
        pending.await(transaction, null)

        assertTrue(pending.isAwaiting(transaction))
        assertEquals(1, pending.size)

        pending.take(transaction)

        assertFalse(pending.isAwaiting(transaction))
    }

    @Test
    fun `transactions are independent`() {
        val sale = RecordingPromise()
        val reprint = RecordingPromise()
        val pending = PendingTransactions()

        pending.await(transaction, sale)
        pending.await(transaction + 1, reprint)
        pending.take(transaction)

        assertTrue(pending.isAwaiting(transaction + 1))
        assertSame(reprint, pending.take(transaction + 1))
    }
}
