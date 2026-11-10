package ai.knoux.integration

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import android.content.Intent

/** Usage Access is a Settings grant, not an ordinary permission dialog. */
class AppUsageScope(private val context: Context) {
    fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestThroughSettings(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun usageForLastSevenDays(): Map<String, Long>? {
        if (!hasUsageAccess()) return null // Explicit unavailable, not zero use.
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 7L * 24 * 60 * 60 * 1000
        return manager.queryAndAggregateUsageStats(start, end)
            .mapValues { (_, value) -> value.totalTimeInForeground }
    }
}
