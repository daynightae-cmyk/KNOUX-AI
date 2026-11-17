package com.knoux.ai.visual

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM proof of the service text budget (contract
 * `docs/design/KNOUX_SERVICE_VISUAL_IDENTITY_20261113.md` §2 and §9).
 *
 * The instrument is the interface; text is a caption layer. This test reads the
 * real EN and AR string resources and pins three rules so a future edit cannot
 * quietly turn a visual surface back into a paragraph:
 *  - an instrument name is micro-caps, at most two words;
 *  - an instrument state caption is at most four words;
 *  - no sentence in the instrument copy runs past eight words.
 *
 * Both languages are checked independently — Arabic is written natively, not
 * translated word-for-word, and must fit the same budget.
 */
class InstrumentCopyBudgetTest {

    private fun strings(locale: String): Map<String, String> {
        val dir = if (locale == "ar") "values-ar" else "values"
        val candidates = listOf(
            File("src/main/res/$dir/strings.xml"),
            File("app/src/main/res/$dir/strings.xml"),
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("strings.xml for $locale not found from ${File(".").absolutePath}")
        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }.newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val out = HashMap<String, String>()
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as org.w3c.dom.Element
            out[el.getAttribute("name")] = el.textContent.trim()
        }
        return out
    }

    private fun words(text: String): List<String> =
        text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun sentences(text: String): List<String> =
        text.split(Regex("[.!?؟…;؛—–]")).map { it.trim() }.filter { it.isNotEmpty() }

    private val nameKeys = listOf(
        "inst_tide", "inst_mirror", "inst_strata", "inst_vault_radar", "inst_pulse",
        "inst_constellation", "inst_terrain", "inst_prism", "inst_synapse",
    )

    private val captionKeys = listOf(
        "tide_listening", "tide_selected", "strata_digging",
        "vault_seal_verified", "vault_seal_open", "pulse_live",
        "synapse_empty_title", "prism_spectrum",
        "status_verified", "status_partial",
        "k_not_measured_yet", "k_measuring_device", "k_pass_failed",
    )

    private val sentenceKeys = listOf(
        "constellation_hint", "synapse_subtitle", "synapse_empty_body", "prism_empty",
    )

    @Test
    fun `instrument names are micro-caps in both languages`() {
        for (locale in listOf("en", "ar")) {
            val res = strings(locale)
            for (key in nameKeys) {
                val value = res[key] ?: error("missing $key in $locale")
                assertTrue(
                    "$locale/$key '$value' must be at most 2 words",
                    words(value).size <= 2,
                )
            }
        }
    }

    @Test
    fun `instrument state captions fit the four word budget in both languages`() {
        for (locale in listOf("en", "ar")) {
            val res = strings(locale)
            for (key in captionKeys) {
                val value = res[key] ?: error("missing $key in $locale")
                assertTrue(
                    "$locale/$key '$value' must be at most 4 words",
                    words(value).size <= 4,
                )
            }
        }
    }

    @Test
    fun `no instrument sentence exceeds eight words in either language`() {
        for (locale in listOf("en", "ar")) {
            val res = strings(locale)
            for (key in sentenceKeys) {
                val value = res[key] ?: error("missing $key in $locale")
                for (sentence in sentences(value)) {
                    assertTrue(
                        "$locale/$key sentence '$sentence' must be at most 8 words",
                        words(sentence).size <= 8,
                    )
                }
            }
        }
    }

    @Test
    fun `every instrument name and caption exists in both languages`() {
        val en = strings("en")
        val ar = strings("ar")
        for (key in nameKeys + captionKeys) {
            assertTrue("EN missing $key", en.containsKey(key))
            assertTrue("AR missing $key", ar.containsKey(key))
        }
    }
}