package com.knoux.ai.data

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Read-only device observations used by the identity surfaces (storage, large
 * files, apps, battery).
 *
 * Nothing here invents a value. Every measurement is either read from the OS in
 * this call, or returned as `null` so the UI can draw the honest unknown state.
 * No cache is persisted: a permission can be revoked between two visits.
 */
object DeviceInsights {

    // ---------------------------------------------------------------- storage

    data class CategoryUsage(
        val key: String,
        val bytes: Long,
        val count: Int,
        val measured: Boolean,
    )

    data class StorageSnapshot(
        val totalBytes: Long,
        val freeBytes: Long,
        val usedBytes: Long,
        val categories: List<CategoryUsage>,
        val appCacheBytes: Long,
        val measured: Boolean,
        val note: String?,
    ) {
        val usedPercent: Int
            get() = if (totalBytes <= 0L) 0 else ((usedBytes * 100) / totalBytes).toInt()
        val measuredBytes: Long
            get() = categories.filter { it.measured }.sumOf { it.bytes } + appCacheBytes
    }

    fun storage(context: Context): StorageSnapshot {
        val stat = runCatching { StatFs(Environment.getDataDirectory().absolutePath) }.getOrNull()
        val total = stat?.blockCount?.times(stat.blockSizeLong) ?: 0L
        val free = stat?.availableBlocks?.times(stat.blockSizeLong) ?: 0L
        val used = (total - free).coerceAtLeast(0L)
        val categories = mutableListOf<CategoryUsage>()
        var note: String? = null

        val images = mediaAggregate(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val video = mediaAggregate(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        if (images != null) categories += CategoryUsage("images", images.first, images.second, true)
        if (video != null) categories += CategoryUsage("video", video.first, video.second, true)
        if (images == null && video == null) note = "media_unmeasured"

        val cache = appCacheBytes(context)
        categories += CategoryUsage("cache", cache, 0, true)

        return StorageSnapshot(
            totalBytes = total,
            freeBytes = free,
            usedBytes = used,
            categories = categories.sortedByDescending { it.bytes },
            appCacheBytes = cache,
            measured = total > 0 && stat != null,
            note = note,
        )
    }

    /** SUM(_size), COUNT(*) of one MediaStore collection, or null when refused. */
    private fun mediaAggregate(context: Context, uri: android.net.Uri): Pair<Long, Int>? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf("SUM(${MediaStore.MediaColumns.SIZE})", "COUNT(*)"),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val bytes = cursor.getLong(0).coerceAtLeast(0L)
            val count = cursor.getInt(1)
            if (bytes <= 0L && count <= 0) return@use null
            bytes to count
        }
    }.getOrNull()

    /** Our own cache only: no permission and no other app's private storage. */
    fun appCacheBytes(context: Context): Long {
        fun size(dir: File?): Long {
            if (dir == null || !dir.exists()) return 0L
            return runCatching {
                dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }.getOrDefault(0L)
        }
        return size(context.cacheDir) + size(context.codeCacheDir) + size(context.externalCacheDir) +
            size(File(context.filesDir.parentFile, "no_backup"))
    }

    // ----------------------------------------------------------- large media

    data class MediaFile(
        val uri: String,
        val name: String,
        val bytes: Long,
        val modifiedAtMillis: Long,
        val isVideo: Boolean,
        val folder: String,
        val mimeType: String?,
        val durationMillis: Long,
    )

    /**
     * Visual media at or above [minBytes], largest first.
     * A `denied` result is distinct from "nothing found": the UI must not draw
     * an empty library when it simply has no permission.
     */
    sealed interface MediaResult {
        data class Measured(val files: List<MediaFile>) : MediaResult
        data class Denied(val scope: AccessScope) : MediaResult
        data class Failed(val reason: String) : MediaResult
    }

    fun largeMedia(context: Context, minBytes: Long, limit: Int = 200): MediaResult {
        val permissions = PermissionState(context)
        val images = permissions.mediaGrant(AccessScope.MEDIA_IMAGES)
        val video = permissions.mediaGrant(AccessScope.MEDIA_VIDEO)
        val imagesReadable = images == PermissionGrant.GRANTED || images == PermissionGrant.PARTIAL_SELECTED
        val videoReadable = video == PermissionGrant.GRANTED || video == PermissionGrant.PARTIAL_SELECTED
        if (!imagesReadable && !videoReadable) return MediaResult.Denied(AccessScope.MEDIA_IMAGES)

        val files = mutableListOf<MediaFile>()
        if (imagesReadable) {
            files += mediaPage(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, minBytes, limit)
        }
        if (videoReadable) {
            files += mediaPage(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, minBytes, limit)
        }
        return MediaResult.Measured(files.sortedByDescending { it.bytes }.take(limit))
    }

    /**
     * One MediaStore collection at a time.
     *
     * The generic `Files` table is avoided on purpose: its MEDIA_TYPE column only
     * exists from API 29, and this app supports API 26.
     */
    private fun mediaPage(
        context: Context,
        collection: android.net.Uri,
        isVideo: Boolean,
        minBytes: Long,
        limit: Int,
    ): List<MediaFile> = runCatching {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            if (isVideo) add(MediaStore.Video.Media.DURATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.MediaColumns.RELATIVE_PATH)
        }.toTypedArray()
        val selection = buildString {
            append(MediaStore.MediaColumns.SIZE).append(" >= ?")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append(" AND ").append(MediaStore.MediaColumns.IS_PENDING).append(" = 0")
            }
        }
        val out = mutableListOf<MediaFile>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            arrayOf(minBytes.toString()),
            "${MediaStore.MediaColumns.SIZE} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val bucketCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val durationCol = if (isVideo) cursor.getColumnIndex(MediaStore.Video.Media.DURATION) else -1
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
            while (cursor.moveToNext() && out.size < limit) {
                val id = cursor.getLong(idCol)
                val folder = sequenceOf(
                    if (pathCol >= 0) cursor.getString(pathCol) else null,
                    if (bucketCol >= 0) cursor.getString(bucketCol) else null,
                ).firstOrNull { !it.isNullOrBlank() }.orEmpty().trimEnd('/')
                out += MediaFile(
                    uri = android.content.ContentUris.withAppendedId(collection, id).toString(),
                    name = cursor.getString(nameCol) ?: "",
                    bytes = cursor.getLong(sizeCol),
                    modifiedAtMillis = TimeUnit.SECONDS.toMillis(cursor.getLong(dateCol)),
                    isVideo = isVideo,
                    folder = folder,
                    mimeType = cursor.getString(mimeCol),
                    durationMillis = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                )
            }
        }
        out
    }.getOrDefault(emptyList())

    // ------------------------------------------------------------------- apps

    data class AppEntry(
        val packageName: String,
        val label: String,
        val isSystem: Boolean,
        val versionName: String?,
        val bytes: Long?,
    )

    /** Launchable apps only, which the platform exposes without QUERY_ALL_PACKAGES. */
    fun launchableApps(context: Context): List<AppEntry> = runCatching {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        infos.asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { info ->
                AppEntry(
                    packageName = info.packageName,
                    label = runCatching { pm.getApplicationLabel(info).toString() }
                        .getOrDefault(info.packageName),
                    isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    versionName = runCatching {
                        pm.getPackageInfo(info.packageName, 0).versionName
                    }.getOrNull(),
                    bytes = appBytes(context, info.packageName),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }.getOrDefault(emptyList())

    data class AppDetail(
        val packageName: String,
        val label: String,
        val versionName: String?,
        val versionCode: Long,
        val isSystem: Boolean,
        val enabled: Boolean,
        val targetSdk: Int,
        val minSdk: Int,
        val installTimeMillis: Long,
        val updateTimeMillis: Long,
        val bytes: Long?,
        val launchCount: Int?,
        val declaredPermissions: List<String>,
        val launchable: Boolean,
    )

    fun appDetail(context: Context, packageName: String): AppDetail? = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }
        val app = info.applicationInfo ?: return@runCatching null
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        AppDetail(
            packageName = packageName,
            label = runCatching { pm.getApplicationLabel(app).toString() }.getOrDefault(packageName),
            versionName = info.versionName,
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong(),
            isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            enabled = app.enabled,
            targetSdk = app.targetSdkVersion,
            minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) app.minSdkVersion else 0,
            installTimeMillis = info.firstInstallTime,
            updateTimeMillis = info.lastUpdateTime,
            bytes = appBytes(context, packageName),
            launchCount = if (hasUsageAccess(context)) launchCount(context, packageName) else null,
            declaredPermissions = info.requestedPermissions?.toList().orEmpty(),
            launchable = launchIntent != null,
        )
    }.getOrNull()

    fun appIcon(context: Context, packageName: String): Drawable? = runCatching {
        context.packageManager.getApplicationIcon(packageName)
    }.getOrNull()

    /**
     * Package size needs the user-granted usage-access app-op on modern Android.
     * Without it the platform throws SecurityException; the caller receives null
     * and must render "not measured", never 0 B.
     */
    fun appBytes(context: Context, packageName: String): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return runCatching {
            val stats = context.getSystemService(StorageStatsManager::class.java) ?: return null
            val uid = context.packageManager.getApplicationInfo(packageName, 0).uid
            val measured = stats.queryStatsForUid(android.os.storage.StorageManager.UUID_DEFAULT, uid)
            measured.appBytes + measured.dataBytes + measured.cacheBytes
        }.getOrNull()
    }

    fun hasUsageAccess(context: Context): Boolean = runCatching {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /** Real launch events in the window, or null when usage access is missing. */
    fun launchCount(context: Context, packageName: String, days: Int = 7): Int? {
        if (!hasUsageAccess(context)) return null
        return runCatching {
            val usage = context.getSystemService(UsageStatsManager::class.java) ?: return null
            val end = System.currentTimeMillis()
            val start = end - TimeUnit.DAYS.toMillis(days.toLong())
            @Suppress("DEPRECATION")
            val events = usage.queryEvents(start, end) ?: return null
            val event = android.app.usage.UsageEvents.Event()
            var count = 0
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND &&
                    event.packageName == packageName
                ) count++
            }
            count
        }.getOrNull()
    }

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    // ---------------------------------------------------------------- battery

    data class BatterySnapshot(
        val levelPercent: Int?,
        val charging: Boolean,
        val plugged: String,
        val health: String,
        val temperatureCelsius: Float?,
        val voltageMillivolts: Int?,
        val technology: String?,
        val chargeRemainingMillis: Long?,
        val measuredAtMillis: Long,
    )

    fun battery(context: Context): BatterySnapshot {
        val manager = context.getSystemService(BatteryManager::class.java)
        val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else null
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "ac"
            BatteryManager.BATTERY_PLUGGED_USB -> "usb"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            else -> "none"
        }
        val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "failure"
            else -> "unknown"
        }
        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val remaining = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { manager?.computeChargeTimeRemaining() }.getOrNull()?.takeIf { it > 0 }
        } else null
        return BatterySnapshot(
            levelPercent = percent,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            plugged = plugged,
            health = health,
            temperatureCelsius = tempTenths?.takeIf { it != Int.MIN_VALUE }?.div(10f),
            voltageMillivolts = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 },
            technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
            chargeRemainingMillis = remaining,
            measuredAtMillis = System.currentTimeMillis(),
        )
    }

    // ------------------------------------------------------ app-local prefs

    /** The one thing the app itself can change: which guidance profile is on. */
    enum class BatteryProfile { ECO, BALANCED, PERFORMANCE }

    /** Open the OS screen that actually owns each permission family. */
    fun appSettingsIntent(packageName: String): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.fromParts("package", packageName, null),
    )

    fun batterySettingsIntent(): Intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)

    fun displaySettingsIntent(): Intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)

    /** True when the app may hand a real uninstall to the OS for this package. */
    fun canUninstall(context: Context, packageName: String): Boolean = runCatching {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && info.packageName != context.packageName
    }.getOrDefault(false)
}
