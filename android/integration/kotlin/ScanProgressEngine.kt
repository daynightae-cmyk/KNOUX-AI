package ai.knoux.integration

/** Pure state model. No fake timer-driven progress values. */
enum class ModuleState { QUEUED, RUNNING, COMPLETED, FAILED, SKIPPED, CANCELLED }
data class ModuleProgress(val key: String, val state: ModuleState, val completedUnits: Long, val totalUnits: Long?) {
    init { require(completedUnits >= 0L); require(totalUnits == null || (totalUnits >= 0L && completedUnits <= totalUnits)) }
}
data class ActualScanProgress(val completed: Int, val attempted: Int, val percent: Int?, val blocked: Int)

object ScanProgressEngine {
    fun aggregate(modules: List<ModuleProgress>): ActualScanProgress {
        val completed = modules.count { it.state == ModuleState.COMPLETED }
        val blocked = modules.count { it.state == ModuleState.FAILED || it.state == ModuleState.SKIPPED }
        val weighted = modules.filter { (it.totalUnits ?: 0L) > 0L }
        val total = weighted.sumOf { it.totalUnits ?: 0L }
        val units = weighted.sumOf { it.completedUnits }
        val percent = if (weighted.size == modules.size && total > 0L) (units * 100L / total).toInt() else null
        return ActualScanProgress(completed, modules.size, percent, blocked)
    }
}
