package ai.knoux.integration

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs

/** Measurements backed by public APIs. These are not health scores or promised savings. */
data class StorageSample(val volumeTotalBytes: Long, val volumeAvailableBytes: Long)
data class BatterySample(val levelPercent: Int?, val isCharging: Boolean?, val thermalStatus: Int?)

class DeviceMetrics(private val context: Context) {
    fun storage(): StorageSample {
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        return StorageSample(stat.totalBytes, stat.availableBytes)
    }

    fun battery(): BatterySample {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status?.let {
            it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL
        }
        val thermal = if (Build.VERSION.SDK_INT >= 29)
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus
        else null
        return BatterySample(level, charging, thermal)
    }
}
