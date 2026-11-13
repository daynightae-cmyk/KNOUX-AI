package com.knoux.ai

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.KnouxData
import com.knoux.ai.data.ResidueAction
import com.knoux.ai.data.ResidueRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * PC03 end-to-end UI proof on the authorized phone.
 *
 * The scan, the selection and the removal target newly created UUID cache files
 * only. A file that was not selected must still exist afterwards, and the audit row
 * is read back from the real production database this screen writes to.
 */
@RunWith(AndroidJUnit4::class)
class ResidueRealScreenTest {
    private val ui = createAndroidComposeRule<MainActivity>()

    @get:Rule val rules: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() { SplashGate.consumed = true }
        })
        .around(ui)

    private val prefix = "knoux_residue_ui_${UUID.randomUUID()}_"
    private lateinit var seedDirectory: File
    private val seeds = mutableListOf<File>()
    private var originalScopes: Set<AccessScope>? = null
    private var originalAge: Int? = null

    private val leftover get() = File(seedDirectory, prefix + "leftover.tmp")
    private val keeper get() = File(seedDirectory, prefix + "keep-me.bin")

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
    private fun exists(tag: String) = ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun node(tag: String): SemanticsNodeInteraction = ui.onAllNodesWithTag(tag).onLast()

    private fun click(tag: String, inSheet: Boolean = false) {
        val target = node(tag)
        if (inSheet) target.performScrollTo() else runCatching { target.performScrollTo() }
        target.assertIsDisplayed().performClick()
    }

    private fun reveal(tag: String) {
        runCatching { ui.onNodeWithTag("residue-list").performScrollToNode(hasTestTag(tag)) }
    }

    private fun toggleBySemantics(tag: String) {
        val target = node(tag)
        reveal(tag)
        runCatching { target.performScrollTo() }
        target.assertIsDisplayed()
        target.performSemanticsAction(SemanticsActions.OnClick)
    }

    @Before fun seedFixturesAndOpenResidueScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        KnouxData.init(context)
        originalScopes = io { KnouxData.settings.residueScopes.first() }
        originalAge = io { KnouxData.settings.residueMinAgeDays.first() }
        seedDirectory = File(context.cacheDir, "KNOUX_QA_${UUID.randomUUID()}")
        assertTrue("UUID cache directory must be newly created", seedDirectory.mkdir())

        val now = System.currentTimeMillis()
        seed("leftover.tmp", ByteArray(8_192) { (it % 29).toByte() }, now - 5L * 86_400_000L)
        seed("keep-me.bin", ByteArray(4_096) { (it % 41).toByte() }, now - 2L * 86_400_000L)
        io {
            KnouxData.settings.setResidueScopes(setOf(AccessScope.APP_CACHE))
            KnouxData.settings.setResidueMinAgeDays(ResidueRules.DEFAULT_MIN_AGE_DAYS)
        }

        ui.waitUntil(20_000) { exists("tab-home") || exists("auth-guest") }
        if (!exists("tab-home")) click("auth-guest")
        ui.waitUntil(20_000) { exists("tab-home") }
        click("tab-clean")
        ui.waitUntil(10_000) { exists("open-residue") }
        click("open-residue")
        ui.waitUntil(10_000) { exists("screen-residue") }
        node("screen-residue").assertIsDisplayed()
    }

    @After fun deleteOnlyExactFixturesAndRestoreRules() {
        try {
            if (::seedDirectory.isInitialized) {
                seeds.forEach { file ->
                    assertEquals(seedDirectory.canonicalPath, file.canonicalFile.parentFile?.canonicalPath)
                    assertTrue("could not remove exact fixture ${file.name}", !file.exists() || file.delete())
                }
                assertTrue("fixture directory is not empty", !seedDirectory.exists() || seedDirectory.delete())
            }
        } finally {
            originalScopes?.let { scopes -> io { KnouxData.settings.setResidueScopes(scopes) } }
            originalAge?.let { days -> io { KnouxData.settings.setResidueMinAgeDays(days) } }
        }
    }

    private fun seed(name: String, content: ByteArray, modified: Long): File {
        val file = File(seedDirectory, prefix + name)
        assertTrue("fixture must not replace an existing file", file.createNewFile())
        seeds += file
        file.writeBytes(content)
        assertTrue(file.setLastModified(modified))
        return file
    }

    @Test fun realScreenMeasuresResidueAndDeletesOnlyTheSelectedItem() {
        val previousScanId = io { KnouxData.residue.latest()?.scanId }
        val auditsBefore = io { KnouxData.residue.observeAudit().first().map { it.id }.toSet() }

        // Explicit pass through the real sources sheet. The screen may be showing a
        // restored previous pass, so the scan must be requested, not assumed.
        click("review-action")
        ui.waitUntil(10_000) { exists("residue-scope-scan") }
        click("residue-scope-scan", inSheet = true)
        ui.waitUntil(120_000) {
            io {
                val latest = KnouxData.residue.latest()
                latest != null && latest.scanId != previousScanId
            }
        }

        val measured = io { KnouxData.residue.latest()!! }
        assertTrue("the fixtures must contribute real measured bytes", measured.bytes >= 12_288L)
        ui.waitUntil(20_000) { exists("residue-total") }
        runCatching { node("residue-total").performScrollTo() }
        assertEquals(
            "the summary must show the measured total, not a fixture",
            humanBytes(measured.bytes),
            node("residue-total").fetchSemanticsNode().config[SemanticsProperties.Text].first().text,
        )

        val leftoverTag = "residue-item-${leftover.name}"
        // A LazyColumn only composes what is near the viewport, so the fixture row has
        // to be scrolled to before its nodes exist at all.
        ui.waitUntil(30_000) {
            runCatching {
                ui.onNodeWithTag("residue-list").performScrollToNode(hasTestTag(leftoverTag))
            }.isSuccess
        }
        node(leftoverTag).assertIsDisplayed()
        // Nothing is pre-selected, not even our own cache.
        reveal("residue-select-${leftover.name}")
        node("residue-select-${leftover.name}").assertIsOff()

        toggleBySemantics("residue-select-${leftover.name}")
        ui.waitUntil(10_000) {
            runCatching { node("residue-select-${leftover.name}").assertIsOn() }.isSuccess
        }

        click("primary-action")
        ui.waitUntil(10_000) { exists("residue-action-delete") }
        ui.waitUntil(10_000) { exists("residue-dry-run") }
        val dryRun = node("residue-dry-run").fetchSemanticsNode().config[SemanticsProperties.Text]
            .joinToString(" ") { it.text }
        assertTrue("the dry run must describe the selected kind of leftover", dryRun.contains("temp_file"))

        toggleBySemantics("residue-ack-irreversible")
        ui.waitUntil(10_000) {
            runCatching { node("residue-action-delete").assertIsDisplayed() }.isSuccess
        }
        click("residue-action-delete", inSheet = true)

        ui.waitUntil(30_000) { !leftover.exists() }
        ui.waitUntil(30_000) {
            io {
                KnouxData.residue.observeAudit().first().any { entry ->
                    entry.id !in auditsBefore && entry.action == ResidueAction.VERIFIED
                }
            }
        }
        assertTrue("an unselected cache file must survive", keeper.exists())

        val after = io { KnouxData.residue.latest()!! }
        assertFalse(
            "a verified removal must leave the stored pass",
            after.items.any { it.candidate.uri == measured.items.first { item -> item.candidate.displayName == leftover.name }.candidate.uri },
        )
        assertEquals(
            "the persisted total must drop by exactly the removed size",
            measured.bytes - 8_192L,
            after.bytes,
        )
    }
}
