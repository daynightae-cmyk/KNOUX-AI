package ai.knoux.integration

/** Use measured, completed, non-preview actions only; never sum potential and actual together. */
data class CompletedActionEvidence(
    val id: String,
    val source: String,
    val status: String,
    val bytesVerified: Long?
)
object VerifiedReportMath {
    fun bytesReclaimed(actions: List<CompletedActionEvidence>): Long =
        actions.asSequence().filter { it.source == "measured" && it.status == "completed" }
            .mapNotNull { it.bytesVerified?.takeIf { size -> size >= 0L } }
            .sum()
}
