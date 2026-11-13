package com.knoux.ai

import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
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
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.DataProvenance
import com.knoux.ai.data.KnouxData
import com.knoux.ai.data.OperationKind
import com.knoux.ai.data.OperationStatus
import com.knoux.ai.data.PermissionGrant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Run only on an explicitly verified emulator. All destructive targets are fresh cache fixtures. */
@RunWith(AndroidJUnit4::class)
class SmartCleanRealTest {
    private val ui = createAndroidComposeRule<MainActivity>()

    // The cinematic splash finishes its first-run flow by routing into onboarding,
    // which replaces the whole app shell mid-test. Consuming the gate before the
    // activity launches keeps every case on the real, already-onboarded dashboard.
    @get:Rule val rules: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() { SplashGate.consumed = true }
        })
        .around(ui)

    private val prefix = "knoux_qa_${UUID.randomUUID()}_"
    private val seeds = mutableListOf<File>()
    private lateinit var seedDirectory: File
    private var originalScopes: Set<AccessScope>? = null
    private var originalScanAt: Long? = null
    private var historyBefore = emptySet<String>()

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
    private fun exists(tag: String) = ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    // Shared actions can occur in the background screen and foreground sheet.
    // The foreground instance is last; file rows are never addressed by an index.
    private fun node(tag: String): SemanticsNodeInteraction = ui.onAllNodesWithTag(tag).onLast()
    private fun click(tag: String, inSheet: Boolean = false) {
        val target = node(tag)
        // Sheets and long forms legitimately scroll; reveal the target before asserting.
        if (inSheet) target.performScrollTo() else runCatching { target.performScrollTo() }
        target.assertIsDisplayed().performClick()
    }

    // Toggleable nodes can be visually covered by a row overlay on some devices.
    // Invoke the exported semantics action instead of injecting a pointer tap.
    private fun toggleBySemantics(tag: String) {
        val target = node(tag)
        runCatching { target.performScrollTo() }
        target.assertIsDisplayed()
        target.performSemanticsAction(SemanticsActions.OnClick)
    }

    @Before fun seedSyntheticFilesAndEnterSmartClean() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        KnouxData.init(context)
        originalScopes = io { KnouxData.settings.selectedScopes.first() }
        originalScanAt = io { KnouxData.settings.lastScanAt.first() }
        historyBefore = io { KnouxData.database.operationDao().observeOperations().first().map { it.id }.toSet() }
        seedDirectory = File(context.cacheDir, "KNOUX_QA_${UUID.randomUUID()}")
        assertTrue("UUID cache directory must be newly created", seedDirectory.mkdir())
        listOf("alpha.tmp", "bravo.tmp", "charlie.tmp").forEach { suffix ->
            val file = File(seedDirectory, prefix + suffix)
            assertTrue("fixture must not replace an existing file", file.createNewFile())
            seeds += file
            file.writeBytes(ByteArray(4096) { (it % 11).toByte() })
        }
        assertFixtureFilesPresent()

        ui.waitUntil(20_000) { exists("tab-home") || exists("auth-guest") }
        if (!exists("tab-home")) click("auth-guest")
        ui.waitUntil(20_000) { exists("tab-home") }
        click("tab-clean")
        node("screen-clean").assertIsDisplayed()
    }

    @After fun removeOnlyKnownLeftoverSeedsAndRestorePreferences() {
        try {
            // Remove only this test's exclusions; never clear Room or app data.
            seeds.forEach { seed ->
                io { KnouxData.smartClean.removeExclusion(Uri.fromFile(seed).toString()) }
                assertEquals(seedDirectory.canonicalPath, seed.canonicalFile.parentFile?.canonicalPath)
                assertTrue("could not remove exact fixture ${seed.name}", !seed.exists() || seed.delete())
            }
            if (::seedDirectory.isInitialized) {
                // Nonrecursive: any unexpected contents are preserved.
                assertTrue("UUID fixture directory is not empty", !seedDirectory.exists() || seedDirectory.delete())
            }
        } finally {
            originalScopes?.let { scopes -> io { KnouxData.settings.setSelectedScopes(scopes) } }
            originalScanAt?.let { stamp -> io { KnouxData.settings.recordScanAt(stamp) } }
        }
    }

    private fun assertFixtureFilesPresent() {
        assertEquals(3, seeds.size)
        seeds.forEach {
            assertEquals(seedDirectory.canonicalPath, it.canonicalFile.parentFile?.canonicalPath)
            assertTrue("exact fixture must survive: ${it.name}", it.isFile && it.length() == 4096L)
        }
    }

    private fun includeScope(scope: AccessScope, wanted: Boolean) {
        val target = node("include-${scope.name}").performScrollTo()
        val actual = runCatching { target.fetchSemanticsNode().config[SemanticsProperties.ToggleableState] }.getOrNull()
        assertTrue("scope must expose checkbox semantics", actual != null)
        if ((actual == ToggleableState.On) != wanted) {
            target.performSemanticsAction(SemanticsActions.OnClick)
        }
        if (wanted) target.assertIsOn() else target.assertIsOff()
    }

    private fun runScanAndOpenReview() {
        val previousId = io { KnouxData.smartClean.latestScan()?.session?.id }
        click("review-action")
        ui.waitUntil(20_000) { exists("scope-scan") }
        AccessScope.entries.forEach { includeScope(it, it == AccessScope.APP_CACHE) }
        click("scope-scan", inSheet = true)
        ui.waitUntil(30_000) {
            io { KnouxData.smartClean.latestScan()?.session?.id?.let { it != previousId } == true }
        }
        assertEquals(setOf(AccessScope.APP_CACHE), io { KnouxData.smartClean.latestScan()!!.session.scopes })
        if (exists("scope-scan")) click("close-sheet", inSheet = true)
        if (!exists("clean-search")) click("primary-action")
        ui.waitUntil(20_000) { exists("clean-search") }
    }

    private fun assertExactScannedFixtureScope(expected: List<File> = seeds) {
        val outcome = io { KnouxData.smartClean.latestScan() }!!
        assertEquals(setOf(AccessScope.APP_CACHE), outcome.session.scopes)
        val fixtureRows = outcome.candidates.filter { it.displayName.startsWith(prefix) }
        assertEquals(expected.size, fixtureRows.size)
        assertEquals(expected.map { it.name }.toSet(), fixtureRows.map { it.displayName }.toSet())
        assertEquals(expected.map { Uri.fromFile(it).toString() }.toSet(), fixtureRows.map { it.uri }.toSet())
        fixtureRows.forEach { row ->
            assertEquals(DataProvenance.APP_OWNED_CACHE, row.provenance)
            assertEquals(seedDirectory.canonicalPath, File(Uri.parse(row.uri).path!!).canonicalFile.parentFile?.canonicalPath)
            assertEquals(4096L, row.bytes)
        }
    }

    private fun filterToExactFixtures(expected: List<File> = seeds) {
        assertExactScannedFixtureScope(expected)
        node("clean-search").performScrollTo().performTextReplacement(prefix)
        listOf("filter-ALL", "size-ANY", "age-ANY").forEach { click(it, inSheet = true) }
        expected.forEach { seed ->
            node("clean-list").performScrollTo().performScrollToNode(hasTestTag("clean-select-${seed.name}"))
            ui.onNodeWithTag("clean-select-${seed.name}").assertIsDisplayed()
            ui.onAllNodesWithTag("clean-select-${seed.name}").assertCountEquals(1)
        }
        val rowTag = SemanticsMatcher("named candidate selector") {
            runCatching { it.config[SemanticsProperties.TestTag] }.getOrNull()?.startsWith("clean-select-") == true
        }
        val names = ui.onAllNodes(rowTag).fetchSemanticsNodes().map {
            it.config[SemanticsProperties.TestTag].removePrefix("clean-select-")
        }
        assertEquals(expected.map { it.name }.toSet(), names.toSet())
        assertEquals(expected.size, names.size)
    }

    private fun selectExactFixtures() {
        filterToExactFixtures()
        if (exists("clear-selection")) click("clear-selection", inSheet = true)
        seeds.forEach { seed ->
            node("clean-list").performScrollTo().performScrollToNode(hasTestTag("clean-select-${seed.name}"))
            val tag = "clean-select-${seed.name}"
            ui.onNodeWithTag(tag).assertIsOff()
            toggleBySemantics(tag)
            // Selection is persisted asynchronously; wait for the row to report On
            // instead of asserting against a state that has not recomposed yet.
            ui.waitUntil(10_000) { runCatching { ui.onNodeWithTag(tag).assertIsOn() }.isSuccess }
        }
        assertExactScannedFixtureScope()
        assertFixtureFilesPresent()
    }

    @Test fun realScanFindsExactSyntheticAppCacheAndCacheCannotBeTrashed() {
        runScanAndOpenReview()
        selectExactFixtures()
        node("action-trash").performScrollTo().assertIsNotEnabled()
        assertFixtureFilesPresent()
    }

    @Test fun permanentDeleteOfExactSyntheticCacheIsVerifiedAndWrittenToHistory() {
        runScanAndOpenReview()
        selectExactFixtures()
        click("action-delete", inSheet = true)
        node("confirm-delete").performScrollTo().assertIsNotEnabled()
        node("ack-irreversible").performScrollTo().assertIsOff()
        toggleBySemantics("ack-irreversible")
        ui.waitUntil(5_000) { runCatching { node("ack-irreversible").assertIsOn() }.isSuccess }
        assertExactScannedFixtureScope()
        click("confirm-delete", inSheet = true)
        val uris = seeds.map { Uri.fromFile(it).toString() }.toSet()
        ui.waitUntil(20_000) {
            seeds.none { it.exists() } && io {
                KnouxData.database.operationDao().observeOperations().first().any { row ->
                    row.id !in historyBefore && row.status == OperationStatus.SUCCEEDED.name &&
                        KnouxData.database.operationDao().items(row.id).map { it.uri }.toSet() == uris
                }
            }
        }
        val operation = io {
            KnouxData.database.operationDao().observeOperations().first().single { row ->
                row.id !in historyBefore && KnouxData.database.operationDao().items(row.id).map { it.uri }.toSet() == uris
            }
        }
        assertEquals(OperationKind.PERMANENT_DELETE.name, operation.kind)
        assertEquals(OperationStatus.SUCCEEDED.name, operation.status)
        assertEquals(3, operation.itemCount)
        assertEquals(12_288L, operation.bytes)
        assertEquals(uris, io { KnouxData.smartClean.verifiedRemovedUris(operation.id).toSet() })
        assertTrue("every exact synthetic seed must be gone", seeds.none { it.exists() })
        click("close-sheet", inSheet = true)
        click("review-action")
        ui.waitUntil(20_000) { exists("clean-history") }
        node("clean-history").performScrollTo().assertIsDisplayed()
        assertTrue(io { KnouxData.smartClean.observeHistory().first().any { it.id == operation.id } })
    }

    @Test fun cancelledPermanentConfirmationPreservesEveryFixture() {
        runScanAndOpenReview()
        selectExactFixtures()
        click("action-delete", inSheet = true)
        node("ack-irreversible").performScrollTo().assertIsOff()
        node("confirm-delete").performScrollTo().assertIsNotEnabled()
        click("close-sheet", inSheet = true)
        assertFixtureFilesPresent()
        assertEquals(historyBefore, io { KnouxData.database.operationDao().observeOperations().first().map { it.id }.toSet() })
    }

    @Test fun keepExactFixturePersistsExclusionAndPreventsItBeingOfferedAgain() {
        runScanAndOpenReview()
        filterToExactFixtures()
        val kept = seeds.first { it.name == prefix + "bravo.tmp" }
        node("clean-list").performScrollTo().performScrollToNode(hasTestTag("clean-keep-${kept.name}"))
        ui.onNodeWithTag("clean-keep-${kept.name}").performClick()
        val keptUri = Uri.fromFile(kept).toString()
        ui.waitUntil(20_000) { io { KnouxData.database.exclusionDao().contains(keptUri) } }
        assertFixtureFilesPresent()
        click("close-sheet", inSheet = true)
        runScanAndOpenReview()
        filterToExactFixtures(seeds.filterNot { it == kept })
        ui.onAllNodesWithTag("clean-select-${kept.name}").assertCountEquals(0)
        assertFixtureFilesPresent()
    }

    @Test fun actuallyDeniedMediaScopesHaveNoMeasuredRowsAndAccessRemovesNothing() {
        val denied = setOf(AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO).filter {
            KnouxData.permissions.mediaGrant(it) in setOf(PermissionGrant.DENIED, PermissionGrant.REVOKED)
        }.toSet()
        assumeTrue("No denied visual-media scope; do not revoke customer permissions", denied.isNotEmpty())
        val scan = io { KnouxData.smartClean.scan(denied).getOrThrow() }
        assertTrue(scan.candidates.isEmpty())
        assertEquals(denied, scan.session.snapshots.map { it.scope }.toSet())
        scan.session.snapshots.forEach {
            assertTrue(it.grant in setOf(PermissionGrant.DENIED, PermissionGrant.REVOKED))
            assertEquals(0, it.itemCount)
            assertEquals(0L, it.totalBytes)
        }
        click("review-action")
        ui.waitUntil(20_000) { exists("scope-media") }
        node("scope-media").performScrollTo().assertIsDisplayed()
        click("close-sheet", inSheet = true)
        assertFixtureFilesPresent()
    }
}
