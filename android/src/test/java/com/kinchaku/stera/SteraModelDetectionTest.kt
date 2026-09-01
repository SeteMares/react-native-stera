package com.kinchaku.stera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The model predicate that decides whether this module does anything at all.
 *
 * It was an exact-match list of "JT-C60" and "JT-VT10", which silently disabled the
 * entire module on the JT-C61: no activity-result listener, no payments, and
 * isSupported() reporting false to JS. These are the cases that would have caught it.
 */
class SteraModelDetectionTest {

    @Test
    fun `accepts the stera family`() {
        assertTrue(SteraModule.isSupportedModel("JT-C60"))
        assertTrue(SteraModule.isSupportedModel("JT-C61"))
    }

    @Test
    fun `accepts the tance family`() {
        assertTrue(SteraModule.isSupportedModel("JT-VT10"))
    }

    @Test
    fun `accepts a model from a generation that does not exist yet`() {
        // The point of matching a family rather than listing members: the JT-C61 was
        // rejected because it was newer than the list, and the next one will be too.
        assertTrue(SteraModule.isSupportedModel("JT-C70"))
        assertTrue(SteraModule.isSupportedModel("JT-VT20"))
    }

    @Test
    fun `rejects an ordinary phone`() {
        assertFalse(SteraModule.isSupportedModel("Pixel 6"))
        assertFalse(SteraModule.isSupportedModel("SM-G991B"))
    }

    @Test
    fun `rejects near misses`() {
        assertFalse(SteraModule.isSupportedModel("JT-"))
        assertFalse(SteraModule.isSupportedModel("JTC61"))
        assertFalse(SteraModule.isSupportedModel("JT-C61X"))
        assertFalse(SteraModule.isSupportedModel("XJT-C61"))
        assertFalse(SteraModule.isSupportedModel("JT-C61 "))
    }

    @Test
    fun `rejects the wrong case rather than guessing`() {
        // Build.MODEL is vendor-supplied. Matching case-insensitively would widen the
        // pattern on a guess; if a device ever reports lowercase, add it deliberately.
        assertFalse(SteraModule.isSupportedModel("jt-c61"))
    }

    @Test
    fun `tolerates an absent model`() {
        // Build.MODEL is a platform type: null off device, and null is conceivable on
        // a badly provisioned build. The old code would have thrown here.
        assertFalse(SteraModule.isSupportedModel(null))
        assertFalse(SteraModule.isSupportedModel(""))
    }
}
