package com.knoux.ai.visual

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM proof of the Abyss Sonar byte label, which lives inside an Arabic
 * paragraph and previously rendered "0 B" as "B 0".
 *
 * The instrument may only change the *order* of the label, never its measured
 * content, so these tests pin both: the exact decimal text and the bidi
 * isolation that protects it from paragraph reordering.
 */
class AbyssSonarBytesTest {

    private val lri = '\u2066'
    private val pdi = '\u2069'

    @Test
    fun `zero is isolated and never reversed`() {
        assertEquals("${lri}0 B$pdi", formatBytes(0))
    }

    @Test
    fun `raw units stay integral below one kilobyte`() {
        assertEquals("${lri}1 B$pdi", formatBytes(1))
        assertEquals("${lri}999 B$pdi", formatBytes(999))
    }

    @Test
    fun `decimal units match the instrument scale`() {
        assertEquals("${lri}1.0 KB$pdi", formatBytes(1_000))
        assertEquals("${lri}1.0 KB$pdi", formatBytes(1023))
        assertEquals("${lri}1.50 MB$pdi", formatBytes(1_500_000))
        assertEquals("${lri}2.50 GB$pdi", formatBytes(2_500_000_000))
        assertEquals("${lri}1.25 TB$pdi", formatBytes(1_250_000_000_000))
    }

    @Test
    fun `label always starts with a digit and ends with the unit`() {
        val samples = longArrayOf(0, 512, 1_000, 1_500_000, 2_500_000_000, 1_250_000_000_000)
        val order = Regex("""^\d[\d.]* (B|KB|MB|GB|TB)$""")
        for (bytes in samples) {
            val inner = formatBytes(bytes).removeSurrounding("$lri", "$pdi")
            assertTrue("'$inner' lost its digit-first order for $bytes", order.matches(inner))
        }
    }

    @Test
    fun `exactly one isolate pair wraps the label`() {
        for (bytes in longArrayOf(0, 1_000, 1_500_000, 2_500_000_000)) {
            val value = formatBytes(bytes)
            assertEquals("exactly one LRI for $bytes", 1, value.count { it == lri })
            assertEquals("exactly one PDI for $bytes", 1, value.count { it == pdi })
            assertEquals("LRI must open the label for $bytes", lri, value.first())
            assertEquals("PDI must close the label for $bytes", pdi, value.last())
        }
    }

    @Test
    fun `arabic system locale cannot switch the digits`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("${lri}1.50 MB$pdi", formatBytes(1_500_000))
            assertEquals("${lri}0 B$pdi", formatBytes(0))
        } finally {
            Locale.setDefault(original)
        }
    }
}
