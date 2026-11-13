package com.knoux.ai

import android.net.Uri
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.DataStatus
import com.knoux.ai.data.KnouxData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File

/**
 * PC03: a pass must describe the device as it is right now.
 *
 * This test creates no fixtures. It asks the real screen for a fresh pass over
 * whatever the phone currently holds and then checks the stored result against
 * the filesystem: every listed leftover has to exist, inside the measured scope,
 * with the measured size. That is the property a customer relies on when the
 * screen says "leftover", and it is the property that a cached or recycled pass
 * would silently break.
 */
@RunWith(AndroidJUnit4::class)
class ResidueFreshPassTest {
    private val ui = createAndroidComposeRule<MainActivity>()

    @get:Rule val rules: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() { SplashGate.consumed = true }
        })
        .around(ui)

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
    private fun exists(tag: String) = ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun node(tag: String) = ui.onAllNodesWithTag(tag).onLast()
    private fun click(tag: String, inSheet: Boolean = false) {
        val target = node(tag)
        if (inSheet) target.performScrollTo() else runCatching { target.performScrollTo() }
        target.assertIsDisplayed().performClick()
    }

    @Before fun openResidueScreen() {
        KnouxData.init(InstrumentationRegistry.getInstrumentation().targetContext)
        ui.waitUntil(20_000) { exists("tab-home") || exists("auth-guest") }
        if (!exists("tab-home")) click("auth-guest")
        ui.waitUntil(20_000) { exists("tab-home") }
        click("tab-clean")
        ui.waitUntil(10_000) { exists("open-residue") }
        click("open-residue")
        ui.waitUntil(10_000) { exists("screen-residue") }
        node("screen-residue").assertIsDisplayed()
    }

    @Test fun freshPassListsOnlyFilesThatExistInsideTheMeasuredScope() {
        val previous = io { KnouxData.residue.latest()?.scanId }

        // The sheet is the only way a pass is requested; nothing starts on its own.
        click("review-action")
        ui.waitUntil(10_000) { exists("residue-scope-scan") }
        click("residue-scope-scan", inSheet = true)
        ui.waitUntil(180_000) {
            io {
                val latest = KnouxData.residue.latest()
                latest != null && latest.scanId != previous
            }
        }

        val pass = io { KnouxData.residue.latest()!! }
        assertNotEquals("the screen must store a new pass, not reshuffle the old one", previous, pass.scanId)
        assertEquals("a completed pass is a measurement", DataStatus.MEASURED, pass.status)

        val scopes = io { KnouxData.settings.residueScopes.first() }
        val cacheRoot = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.canonicalPath

        pass.items.forEach { item ->
            val uri = Uri.parse(item.candidate.uri)
            if (uri.scheme == "file") {
                val file = File(requireNotNull(uri.path) { "file URI without a path: $uri" })
                assertTrue("a listed leftover must exist: ${item.candidate.displayName}", file.isFile)
                assertEquals(
                    "the stored size must match the file: ${item.candidate.displayName}",
                    file.length(),
                    item.candidate.bytes,
                )
                if (scopes == setOf(AccessScope.APP_CACHE)) {
                    assertTrue(
                        "an app-cache-only pass must stay inside our cache: ${file.canonicalPath}",
                        file.canonicalPath.startsWith(cacheRoot + File.separator),
                    )
                }
            }
            assertTrue("every row carries filename evidence", item.matchedOn.isNotBlank())
        }

        // Synthetic instrumentation names are not customer leftovers.
        val synthetic = pass.items.map { it.candidate.displayName }
            .filter { it.startsWith("KNOUX_QA_") || it.startsWith("knoux_residue_ui_") }
        assertTrue("test fixtures leaked into a customer measurement: $synthetic", synthetic.isEmpty())

        // The number on screen is the number that was stored, not a second estimate.
        ui.waitUntil(20_000) { exists("residue-total") }
        runCatching { node("residue-total").performScrollTo() }
        assertEquals(
            "the summary must show the freshly measured total",
            humanBytes(pass.bytes),
            node("residue-total").fetchSemanticsNode().config[SemanticsProperties.Text].first().text,
        )
    }
}
