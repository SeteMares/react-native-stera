package com.kinchaku.stera

import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Android delivers a null result Intent on some cancel paths. This used to read
 * `data!!.extras`, which threw inside onActivityResult -- so the promise was never
 * settled and the till sat on a spinner with no error and no way forward.
 */
class SteraResultExtrasTest {

    @Test
    fun `a null result Intent yields empty extras rather than throwing`() {
        assertNotNull(SteraModule.resultExtras(null))
    }
}
