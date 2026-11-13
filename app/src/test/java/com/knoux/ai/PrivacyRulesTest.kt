package com.knoux.ai

import com.knoux.ai.data.PrivacyCheck
import com.knoux.ai.data.PrivacyObservation
import com.knoux.ai.data.PrivacyRemedy
import com.knoux.ai.data.PrivacyRules
import com.knoux.ai.data.PrivacySeverity
import com.knoux.ai.data.PrivacyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PC04 — pure proof of the privacy contract.
 *
 * These tests exist to make the dishonest versions of the screen impossible: a
 * pass with no observation, a score over checks Android never exposed, a "fix"
 * we cannot verify, or a 100 that ignores an attention item.
 */
class PrivacyRulesTest {

    private fun pass(
        check: PrivacyCheck,
        severity: PrivacySeverity = PrivacySeverity.HIGH,
        observed: String = "observed ${check.name}",
    ) = PrivacyRules.pass(check, severity, observed, "detail ${check.name}")

    private fun attention(
        check: PrivacyCheck = PrivacyCheck.SCREENSHOT_PROTECTION,
        severity: PrivacySeverity = PrivacySeverity.LOW,
        remedy: PrivacyRemedy = PrivacyRemedy.APP_SETTING,
    ) = PrivacyRules.attention(check, severity, "observed ${check.name}", "detail ${check.name}", remedy)

    private fun unmeasurable(check: PrivacyCheck = PrivacyCheck.OTHER_APP_PRIVACY) =
        PrivacyRules.unmeasurable(check, "Android does not expose this")

    private val scoredPasses = listOf(
        pass(PrivacyCheck.NETWORK_EGRESS),
        pass(PrivacyCheck.ALL_FILES_ACCESS),
        pass(PrivacyCheck.SENSITIVE_PERMISSIONS),
    )

    @Test fun scoreIsNullWhenNothingScorableWasMeasured() {
        val rows = listOf(
            pass(PrivacyCheck.MEDIA_ACCESS, PrivacySeverity.INFO),
            unmeasurable(PrivacyCheck.OTHER_APP_PRIVACY),
            unmeasurable(PrivacyCheck.ANDROID_PRIVACY_DASHBOARD),
        )
        assertNull("no scored check means no score", PrivacyRules.score(rows))
        val summary = PrivacyRules.summarize(rows)
        assertNull(summary.score)
        assertEquals(0, summary.scoredCount)
        assertEquals(2, summary.unmeasurableCount)
        assertEquals("Not measured", summary.label)
    }

    @Test fun aScoredAttentionItemKeepsTheScoreBelowOneHundred() {
        val rows = scoredPasses + attention()
        assertEquals(92, PrivacyRules.score(rows))
        assertEquals(1, PrivacyRules.summarize(rows).attentionCount)
    }

    @Test fun oneHundredRequiresEveryScoredCheckToPass() {
        val rows = scoredPasses + pass(PrivacyCheck.SCREENSHOT_PROTECTION, PrivacySeverity.LOW)
        assertEquals(100, PrivacyRules.score(rows))
        assertEquals(0, PrivacyRules.summarize(rows).attentionCount)
    }

    @Test fun aHighAttentionItemCostsMoreThanALowOne() {
        val high = scoredPasses + attention(PrivacyCheck.NETWORK_EGRESS, PrivacySeverity.HIGH)
        val low = scoredPasses + attention(PrivacyCheck.NETWORK_EGRESS, PrivacySeverity.LOW)
        assertEquals(64, PrivacyRules.score(high))
        assertEquals(92, PrivacyRules.score(low))
        assertTrue(PrivacyRules.score(high)!! < PrivacyRules.score(low)!!)
    }

    @Test fun informationalRowsNeverChangeTheScore() {
        val base = scoredPasses + attention()
        val withInfo = base + listOf(
            pass(PrivacyCheck.MEDIA_ACCESS, PrivacySeverity.INFO),
            pass(PrivacyCheck.SAF_WRITE_GRANTS, PrivacySeverity.INFO),
            pass(PrivacyCheck.NOTIFICATIONS, PrivacySeverity.INFO),
        )
        assertEquals(PrivacyRules.score(base), PrivacyRules.score(withInfo))
        assertEquals(PrivacyRules.summarize(base).scoredCount, PrivacyRules.summarize(withInfo).scoredCount)
    }

    @Test fun unmeasurableRowsNeverCountAsAPassOrAsAPenalty() {
        val base = scoredPasses + attention()
        val withUnmeasurable = base + unmeasurable(PrivacyCheck.OTHER_APP_PRIVACY)
        assertEquals(PrivacyRules.score(base), PrivacyRules.score(withUnmeasurable))
        val rows = PrivacyRules.sanitize(withUnmeasurable)
        val other = rows.first { it.check == PrivacyCheck.OTHER_APP_PRIVACY }
        assertEquals(PrivacyStatus.UNMEASURABLE, other.status)
        assertTrue(other.remedy == PrivacyRemedy.NOT_POSSIBLE)
        assertFalse(PrivacyRules.isAppOwned(other))
    }

    @Test fun aBlankObservationIsDowngradedInsteadOfShownAsAPass() {
        val forged = PrivacyObservation(
            check = PrivacyCheck.NETWORK_EGRESS,
            status = PrivacyStatus.PASS,
            severity = PrivacySeverity.HIGH,
            observed = "   ",
            detail = "forged pass",
            remedy = PrivacyRemedy.NONE,
        )
        val clean = PrivacyRules.sanitize(listOf(forged)).single()
        assertEquals(PrivacyStatus.UNMEASURABLE, clean.status)
        assertEquals(PrivacySeverity.INFO, clean.severity)
        assertEquals(PrivacyRemedy.NOT_POSSIBLE, clean.remedy)
        assertNull("a blank observation must not produce a score", PrivacyRules.score(listOf(forged)))
    }

    @Test fun anAttentionRowWithoutARealRemedyIsNotPresentedAsActionable() {
        val forged = PrivacyObservation(
            check = PrivacyCheck.SENSITIVE_PERMISSIONS,
            status = PrivacyStatus.ATTENTION,
            severity = PrivacySeverity.HIGH,
            observed = "CAMERA",
            detail = "forged",
            remedy = PrivacyRemedy.NOT_POSSIBLE,
        )
        val clean = PrivacyRules.sanitize(listOf(forged)).single()
        assertEquals(PrivacyStatus.UNMEASURABLE, clean.status)
        assertFalse(PrivacyRules.isAppOwned(clean))
    }

    @Test fun anUnmeasurableRowCannotBeMarkedAsFixable() {
        val forged = PrivacyObservation(
            check = PrivacyCheck.OTHER_APP_PRIVACY,
            status = PrivacyStatus.UNMEASURABLE,
            severity = PrivacySeverity.INFO,
            observed = "not observable",
            detail = "forged",
            remedy = PrivacyRemedy.OS_SCREEN,
        )
        val clean = PrivacyRules.sanitize(listOf(forged)).single()
        assertEquals(PrivacyRemedy.NOT_POSSIBLE, clean.remedy)
        assertFalse(PrivacyRules.isAppOwned(clean))
    }

    @Test fun aDuplicateCheckIsCountedOnce() {
        val rows = scoredPasses + attention() + attention()
        val clean = PrivacyRules.sanitize(rows)
        assertEquals(clean.size, clean.map { it.check }.distinct().size)
        assertEquals(92, PrivacyRules.score(rows))
    }

    @Test fun summaryCountsDescribeOnlyObservedRows() {
        val rows = scoredPasses + attention() + unmeasurable() + pass(PrivacyCheck.MEDIA_ACCESS, PrivacySeverity.INFO)
        val summary = PrivacyRules.summarize(rows)
        assertEquals(4, summary.scoredCount)
        assertEquals(1, summary.attentionCount)
        assertEquals(1, summary.unmeasurableCount)
        assertEquals(5, summary.measuredCount)
        assertEquals(PrivacySeverity.LOW, summary.worst)
    }

    @Test fun validateRejectsAnIncoherentPass() {
        val rows = listOf(
            PrivacyObservation(
                PrivacyCheck.NETWORK_EGRESS, PrivacyStatus.PASS, PrivacySeverity.HIGH,
                "", "blank", PrivacyRemedy.NONE,
            ),
            PrivacyObservation(
                PrivacyCheck.OTHER_APP_PRIVACY, PrivacyStatus.UNMEASURABLE, PrivacySeverity.HIGH,
                "not observable", "wrong severity", PrivacyRemedy.NOT_POSSIBLE,
            ),
            PrivacyObservation(
                PrivacyCheck.SENSITIVE_PERMISSIONS, PrivacyStatus.ATTENTION, PrivacySeverity.HIGH,
                "CAMERA", "no remedy", PrivacyRemedy.NOT_POSSIBLE,
            ),
            unmeasurable(PrivacyCheck.NETWORK_EGRESS),
        )
        val problems = PrivacyRules.validate(rows)
        assertTrue(problems.any { it.contains("blank observation") })
        assertTrue(problems.any { it.contains("non-informational severity") })
        assertTrue(problems.any { it.contains("attention without a remedy") })
        assertTrue(problems.any { it.contains("duplicate checks") })
    }

    @Test fun theScoreStaysInsideItsBoundsWithManyFindings() {
        val many = PrivacyCheck.entries
            .filter { it != PrivacyCheck.MEDIA_ACCESS }
            .map { attention(it, PrivacySeverity.HIGH) }
        val score = PrivacyRules.score(many)
        assertEquals(0, score)
        assertTrue(score!! in 0..PrivacyRules.MAX_SCORE)
    }

    @Test fun aDebugBuildIsInformationalAndNeverChangesTheScore() {
        val base = scoredPasses + attention()
        val debug = PrivacyObservation(
            check = PrivacyCheck.DEBUGGABLE_BUILD,
            status = PrivacyStatus.ATTENTION,
            severity = PrivacySeverity.INFO,
            observed = "debuggable=true (test build)",
            detail = "Test build",
            remedy = PrivacyRemedy.NONE,
        )
        assertEquals(PrivacyRules.score(base), PrivacyRules.score(base + debug))
        assertEquals(PrivacyRules.summarize(base).attentionCount, PrivacyRules.summarize(base + debug).attentionCount)
    }

    @Test fun onlyAnAppOwnedSettingIsOfferedAsAKnouxAction() {
        val setting = attention(PrivacyCheck.SCREENSHOT_PROTECTION, PrivacySeverity.LOW)
        val osOwned = attention(PrivacyCheck.SENSITIVE_PERMISSIONS, PrivacySeverity.HIGH, PrivacyRemedy.OS_SCREEN)
        assertTrue(PrivacyRules.isAppOwned(setting))
        assertFalse(PrivacyRules.isAppOwned(osOwned))
        assertFalse(PrivacyRules.isAppOwned(unmeasurable()))
    }

    @Test fun anEmptyObservationListIsHonestRatherThanPerfect() {
        val summary = PrivacyRules.summarize(emptyList())
        assertNull(summary.score)
        assertEquals(0, summary.scoredCount)
        assertEquals(0, summary.measuredCount)
        assertTrue(PrivacyRules.validate(emptyList()).isEmpty())
    }
}
