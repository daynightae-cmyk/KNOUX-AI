package com.knoux.ai

import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.knoux.ai.data.OperationKind
import com.knoux.ai.data.OperationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
 * PC02 end-to-end UI proof on the authorized phone.
 *
 * The scan, the selected copy and the deletion all target newly created UUID cache
 * files. The preserved original and the same-size different-content file are
 * asserted to survive.
 */
@RunWith(AndroidJUnit4::class)
class DuplicateRealScreenTest {
    private val ui = createAndroidComposeRule<MainActivity>()

    @get:Rule val rules: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() { SplashGate.consumed = true }
        })
        .around(ui)

    private val prefix = "knoux_dup_ui_${UUID.randomUUID()}_"
    private lateinit var seedDirectory: File
    private val seeds = mutableListOf<File>()
    private var originalScopes: Set<AccessScope>? = null
    private var historyBefore = emptySet<String>()

    // Derived, never stored: an uninitialised @Before can never yield a half-built path.
    private val original get() = File(seedDirectory, prefix + "original.bin")
    private val copy get() = File(seedDirectory, prefix + "copy.bin")
    private val lookalike get() = File(seedDirectory, prefix + "same-size-different.bin")

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
    private fun exists(tag: String) = ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun node(tag: String): SemanticsNodeInteraction = ui.onAllNodesWithTag(tag).onLast()

    private fun click(tag: String, inSheet: Boolean = false) {
        val target = node(tag)
        if (inSheet) target.performScrollTo() else runCatching { target.performScrollTo() }
        target.assertIsDisplayed().performClick()
    }

    private fun reveal(tag: String) {
        runCatching { ui.onNodeWithTag("duplicate-list").performScrollToNode(hasTestTag(tag)) }
    }

    private fun toggleBySemantics(tag: String) {
        val target = node(tag)
        reveal(tag)
        target.assertIsDisplayed()
        target.performSemanticsAction(SemanticsActions.OnClick)
    }

    @Before fun seedFixturesAndOpenDuplicateFinder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        KnouxData.init(context)
        originalScopes = io { KnouxData.settings.selectedScopes.first() }
        historyBefore = io { KnouxData.database.operationDao().observeOperations().first().map { it.id }.toSet() }
        seedDirectory = File(context.cacheDir, "KNOUX_QA_${UUID.randomUUID()}")
        assertTrue("UUID cache directory must be newly created", seedDirectory.mkdir())

        val bytes = ByteArray(8_192) { (it % 31).toByte() }
        val different = ByteArray(8_192) { (it % 43).toByte() }
        val now = System.currentTimeMillis()
        seed("original.bin", bytes, now - 600_000L)
        seed("copy.bin", bytes, now - 300_000L)
        seed("same-size-different.bin", different, now - 100_000L)
        io { KnouxData.settings.setSelectedScopes(setOf(AccessScope.APP_CACHE)) }

        ui.waitUntil(20_000) { exists("tab-home") || exists("auth-guest") }
        if (!exists("tab-home")) click("auth-guest")
        ui.waitUntil(20_000) { exists("tab-home") }
        click("tab-clean")
        ui.waitUntil(10_000) { exists("open-duplicates") }
        click("open-duplicates")
        ui.waitUntil(10_000) { exists("screen-duplicates") }
        node("screen-duplicates").assertIsDisplayed()
    }

    @After fun deleteOnlyExactFixturesAndRestoreScopes() {
        try {
            if (::seedDirectory.isInitialized) {
                seeds.forEach { file ->
                    assertEquals(seedDirectory.canonicalPath, file.canonicalFile.parentFile?.canonicalPath)
                    assertTrue("could not remove exact fixture ${file.name}", !file.exists() || file.delete())
                }
                assertTrue("fixture directory is not empty", !seedDirectory.exists() || seedDirectory.delete())
            }
        } finally {
            originalScopes?.let { scopes -> io { KnouxData.settings.setSelectedScopes(scopes) } }
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

    @Test fun realFinderShowsTheProtectedOriginalAndDeletesOnlyTheSelectedCopy() {
        val previousScanId = io { KnouxData.duplicates.latest()?.scanId }

        // Force a fresh measured pass through the real sources sheet.
        click("review-action")
        ui.waitUntil(20_000) { exists("dup-scope-scan") }
        click("dup-scope-scan", inSheet = true)
        ui.waitUntil(60_000) {
            io {
                val latest = KnouxData.duplicates.latest()
                latest != null && latest.scanId != previousScanId
            }
        }
        if (exists("dup-scope-scan")) click("close-sheet", inSheet = true)

        // The displayed total must equal the measured total that was just persisted.
        val measuredRedundant = io {
            KnouxData.duplicates.latest()?.redundantBytes
                ?: error("a completed pass must persist its measured bytes")
        }
        assertTrue("the fixtures must contribute real measured bytes", measuredRedundant >= 8_192L)
        ui.waitUntil(20_000) { exists("duplicate-redundant") }
        runCatching { node("duplicate-redundant").performScrollTo() }
        assertEquals(
            "the summary must show the measured total, not a fixture",
            humanBytes(measuredRedundant),
            node("duplicate-redundant").fetchSemanticsNode()
                .config[SemanticsProperties.Text].first().text,
        )

        ui.waitUntil(20_000) { exists("dup-member-${prefix}copy.bin") }
        reveal("dup-keeper-${prefix}original.bin")
        node("dup-keeper-${prefix}original.bin").assertIsDisplayed()
        reveal("dup-member-${prefix}copy.bin")
        node("dup-member-${prefix}copy.bin").assertIsOff()

        // The preserved original exposes no checkbox at all.
        val keeperToggle = runCatching {
            node("dup-keeper-${prefix}original.bin").fetchSemanticsNode()
                .config[SemanticsProperties.ToggleableState]
        }
        assertTrue("the keeper must not be toggleable", keeperToggle.isFailure)

        toggleBySemantics("dup-member-${prefix}copy.bin")
        ui.waitUntil(10_000) {
            runCatching { node("dup-member-${prefix}copy.bin").assertIsOn() }.isSuccess
        }

        click("primary-action")
        ui.waitUntil(10_000) { exists("duplicate-action-delete") }
        node("duplicate-action-trash").performScrollTo().assertIsNotEnabled()
        node("duplicate-action-delete").performScrollTo().assertIsNotEnabled()
        toggleBySemantics("duplicate-ack-irreversible")
        ui.waitUntil(5_000) { runCatching { node("duplicate-ack-irreversible").assertIsOn() }.isSuccess }
        click("duplicate-action-delete", inSheet = true)

        val copyUri = Uri.fromFile(copy).toString()
        val originalUri = Uri.fromFile(original).toString()
        ui.waitUntil(30_000) {
            !copy.exists() && io {
                KnouxData.database.operationDao().observeOperations().first().any { row ->
                    row.id !in historyBefore && row.status == OperationStatus.SUCCEEDED.name &&
                        KnouxData.database.operationDao().items(row.id).map { it.uri }.toSet() == setOf(copyUri)
                }
            }
        }
        val operation = io {
            KnouxData.database.operationDao().observeOperations().first().single { row ->
                row.id !in historyBefore &&
                    KnouxData.database.operationDao().items(row.id).map { it.uri }.toSet() == setOf(copyUri)
            }
        }
        assertEquals(OperationKind.PERMANENT_DELETE.name, operation.kind)
        assertEquals(1, operation.itemCount)
        assertTrue("the preserved original must survive", original.isFile && original.length() == 8_192L)
        assertTrue("the different-content lookalike must survive", lookalike.isFile)
        assertTrue("no keeper may be hidden in the plan", io {
            KnouxData.database.operationDao().items(operation.id).none { it.uri == originalUri }
        })

        click("close-sheet", inSheet = true)
        ui.waitUntil(20_000) {
            ui.onAllNodesWithTag("dup-member-${prefix}copy.bin").fetchSemanticsNodes().isEmpty()
        }
        assertTrue("the keeper remains on disk and in the scan", original.isFile)
        assertTrue("the dissolved group must not remain stored", io {
            KnouxData.duplicates.latest()?.groups?.none { group ->
                (listOf(group.keeper) + group.duplicates).any { it.displayName.startsWith(prefix) }
            } ?: true
        })
    }
}
