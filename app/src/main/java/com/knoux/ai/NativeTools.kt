package com.knoux.ai

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.knoux.ai.visual.ConstellationInstrument
import com.knoux.ai.visual.PulseInstrument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * The utilities whose instrument this build can genuinely measure.
 *
 * Battery, storage and the installed-app inventory are read from the platform
 * at the moment the screen opens: no permission, no fixture and no estimate.
 * Android does not expose per-app sizes, a battery-health percentage or every
 * installed package to a normal app, so where the system stays silent the screen
 * says so instead of filling the gap.
 */

// ---------------------------------------------------------------------------
// Battery
// ---------------------------------------------------------------------------

/**
 * One reading of the sticky `ACTION_BATTERY_CHANGED` broadcast.
 *
 * Every field is nullable on purpose: a device that does not report a value
 * leaves it null, and the screen draws a dash rather than a plausible default.
 * The broadcast is read with a null receiver, so nothing is registered, nothing
 * is retained and no permission is required.
 */
internal data class BatteryReading(
    val levelPercent: Int?,
    val status: Int?,
    val temperatureC: Float?,
    val voltageMv: Int?,
    val health: Int?,
    val technology: String?,
)

internal fun readBatteryReading(context: Context): BatteryReading {
    val intent = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull() ?: return BatteryReading(null, null, null, null, null, null)

    fun extraInt(key: String): Int? =
        intent.takeIf { it.hasExtra(key) }?.getIntExtra(key, Int.MIN_VALUE)
            ?.takeIf { it != Int.MIN_VALUE }

    val level = extraInt(BatteryManager.EXTRA_LEVEL)
    val scale = extraInt(BatteryManager.EXTRA_SCALE)
    val percent = if (level != null && scale != null && scale > 0) {
        ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
    } else null
    val tenths = extraInt(BatteryManager.EXTRA_TEMPERATURE)
    return BatteryReading(
        levelPercent = percent,
        status = extraInt(BatteryManager.EXTRA_STATUS),
        temperatureC = tenths?.let { it / 10f },
        voltageMv = extraInt(BatteryManager.EXTRA_VOLTAGE),
        health = extraInt(BatteryManager.EXTRA_HEALTH),
        technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() },
    )
}

/** The battery instrument: real level on a measured dial, facts underneath. */
@Composable
internal fun NativeBatteryScreen(open: (Page) -> Unit) {
    val context = LocalContext.current
    var reading by remember { mutableStateOf<BatteryReading?>(null) }
    fun refresh() {
        reading = readBatteryReading(context)
    }
    LaunchedEffect(context) { refresh() }

    val charge = reading
    val level = charge?.levelPercent

    Column(Modifier.fillMaxSize().testTag("tool-battery")) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            NativeScreenTitle(
                Copy(R.string.k_battery_diagnostics),
                Copy(R.string.battery_screen_subtitle),
                "screen-${Page.BATTERY_DIAGNOSTICS.id}",
                Modifier.fillMaxWidth()
                    .padding(start = NativeSpace.edge, top = 12.dp, end = NativeSpace.edge),
            )
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = NativeSpace.edge, vertical = NativeSpace.row),
                verticalArrangement = Arrangement.spacedBy(NativeSpace.row),
            ) {
                SurfaceCard(Modifier.enterRise(0), accent = BrandTone.cyan) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().testTag("battery-dial")) {
                            PulseInstrument(
                                levelPercent = level,
                                status = charge?.status,
                            )
                        }
                        ChargeStatePill(charge?.status)
                    }
                }

                SurfaceCard(Modifier.enterRise(1)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                        BatteryFactRow(Copy(R.string.battery_state), chargeStateCopy(charge?.status))
                        BatteryDivider()
                        BatteryFactRow(
                            Copy(R.string.battery_temp),
                            charge?.temperatureC?.let { Copy.runtime(KnouxFormat.unit(round1(it), "°C")) },
                        )
                        BatteryDivider()
                        BatteryFactRow(
                            Copy(R.string.battery_voltage),
                            charge?.voltageMv?.let { Copy.runtime(KnouxFormat.unit(round2(it / 1000f), "V")) },
                        )
                        BatteryDivider()
                        BatteryFactRow(Copy(R.string.battery_health_row), healthCopy(charge?.health))
                        BatteryDivider()
                        BatteryFactRow(Copy(R.string.battery_tech), charge?.technology?.let { Copy.runtime(it) })
                    }
                }

                StateNote(Copy(R.string.battery_honesty), Modifier.enterRise(2))
            }
            Spacer(Modifier.height(18.dp))
        }
        PinnedActions(
            first = Action(Copy(R.string.battery_back_tools), Mark.GRID),
            second = Action(Copy(R.string.battery_refresh), Mark.SYNC),
            onFirst = { open(Page.TOOLS) },
            onSecond = { refresh() },
        )
    }
}

/** The state line under the dial. It states only what Android reported. */
@Composable
private fun ChargeStatePill(status: Int?) {
    val text = chargeStateCopy(status)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val tone = if (charging) NativePalette.cyan else NativePalette.muted
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.clip(shape)
            .background(tone.copy(alpha = .12f))
            .drawBehind {
                // One 1dp tone line, not a filled badge.
                drawLine(
                    tone.copy(alpha = .45f), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.5f,
                )
            }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Glyph(
            if (charging) Mark.BOLT else Mark.BATTERY, Modifier.size(13.dp), tone,
        )
        Label(text, 12, if (charging) NativePalette.text else NativePalette.muted, FontWeight.SemiBold)
    }
}

private fun chargeStateCopy(status: Int?): Copy = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> Copy(R.string.battery_charging)
    BatteryManager.BATTERY_STATUS_FULL -> Copy(R.string.battery_full)
    BatteryManager.BATTERY_STATUS_DISCHARGING,
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> Copy(R.string.battery_not_charging)
    else -> Copy(R.string.battery_not_reported)
}

private fun healthCopy(health: Int?): Copy? = when (health) {
    BatteryManager.BATTERY_HEALTH_GOOD -> Copy(R.string.battery_health_good)
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> Copy(R.string.battery_health_overheat)
    BatteryManager.BATTERY_HEALTH_DEAD -> Copy(R.string.battery_health_replace)
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> Copy(R.string.battery_health_over_voltage)
    BatteryManager.BATTERY_HEALTH_COLD -> Copy(R.string.battery_health_cold)
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> Copy(R.string.battery_not_reported)
    else -> null
}

@Composable
private fun BatteryFactRow(label: Copy, value: Copy?) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(label, 13, NativePalette.muted, modifier = Modifier.weight(1f))
        if (value != null) {
            // Measured figures stay Latin and isolated in both languages, the
            // same rule the hub and the reports chart follow.
            Label(value, 14, NativePalette.text, FontWeight.SemiBold, maxLines = 1)
        } else {
            Label(Copy(R.string.battery_not_reported), 13, NativePalette.muted.copy(alpha = .75f))
        }
    }
}

@Composable
private fun BatteryDivider() {
    Box(
        Modifier.fillMaxWidth().height(1.dp)
            .background(NativePalette.border.copy(alpha = .45f))
    )
}

private fun round1(value: Float): String = String.format(java.util.Locale.US, "%.1f", value)
private fun round2(value: Float): String = String.format(java.util.Locale.US, "%.2f", value)

// ---------------------------------------------------------------------------
// Storage
// ---------------------------------------------------------------------------

/**
 * Storage Breakdown over the one figure Android gives a normal app: the shared
 * data partition. Per-category and per-app sizes need a real scan, so they are
 * stated as unavailable rather than divided out of the total by guesswork.
 */
@Composable
internal fun NativeStorageScreen(open: (Page) -> Unit) {
    val footprint = readStorageFootprint()
    val percent = footprint?.usedPercent
    val used = footprint?.let { KnouxFormat.bytes(it.usedBytes) } ?: "—"
    val free = footprint?.let { KnouxFormat.bytes(it.freeBytes) } ?: "—"
    val total = footprint?.let { KnouxFormat.bytes(it.totalBytes) } ?: "—"

    Column(Modifier.fillMaxSize().testTag("tool-storage")) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            NativeScreenTitle(
                Copy(R.string.k_storage_breakdown_2),
                Copy(R.string.storage_screen_subtitle),
                "screen-${Page.STORAGE_BREAKDOWN.id}",
                Modifier.fillMaxWidth()
                    .padding(start = NativeSpace.edge, top = 12.dp, end = NativeSpace.edge),
            )
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = NativeSpace.edge, vertical = NativeSpace.row),
                verticalArrangement = Arrangement.spacedBy(NativeSpace.row),
            ) {
                SurfaceCard(Modifier.enterRise(0), accent = BrandTone.blue) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NativeInstrumentDial(
                            value = percent,
                            display = percent?.let { KnouxFormat.isolate("$it%") },
                            caption = Copy(R.string.storage_used_share),
                            tones = listOf(NativePalette.blue, NativePalette.violet),
                            diameter = 168,
                            tag = "storage-dial",
                        )
                        if (percent != null) {
                            Row(
                                Modifier.fillMaxWidth(0.86f).height(6.dp)
                                    .clip(RoundedCornerShape(50)).background(NativePalette.raised)
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(percent / 100f).height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(NativePalette.blue, NativePalette.violet),
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                SurfaceCard(Modifier.enterRise(1)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                        BatteryFactRow(Copy(R.string.home_storage_used), Copy.runtime(used))
                        BatteryDivider()
                        BatteryFactRow(Copy(R.string.home_storage_free), Copy.runtime(free))
                        BatteryDivider()
                        BatteryFactRow(Copy(R.string.storage_total), Copy.runtime(total))
                    }
                }

                StateNote(Copy(R.string.storage_capacity_note), Modifier.enterRise(2))
            }
            Spacer(Modifier.height(18.dp))
        }
        PinnedActions(
            first = Action(Copy(R.string.battery_back_tools), Mark.GRID),
            second = Action(Copy(R.string.storage_scan), Mark.RADAR),
            onFirst = { open(Page.TOOLS) },
            onSecond = { open(Page.SCAN) },
        )
    }
}

// ---------------------------------------------------------------------------
// Shared instrument pieces
// ---------------------------------------------------------------------------

/**
 * The tactile dial of the native utility screens.
 *
 * One quiet track, one measured arc in the service's own logo tones and one
 * number that fits the ring. The sweep animates once, from empty to the measured
 * value, and only while the customer keeps motion on. With no reading the track
 * stays empty and the centre shows a dash.
 */
@Composable
internal fun NativeInstrumentDial(
    value: Int?,
    display: String?,
    caption: Copy,
    tones: List<Color>,
    diameter: Int,
    tag: String,
) {
    val motion = LocalPreviewMotion.current
    val target = value?.coerceIn(0, 100) ?: 0
    var entered by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) { entered = true }
    val sweep by animateFloatAsState(
        if (entered) 348f * target / 100f else 0f,
        tween(if (motion) 900 else 0, easing = FastOutSlowInEasing),
        label = "instrument sweep",
    )
    Box(Modifier.size(diameter.dp).testTag(tag), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val band = size.minDimension * .062f
            val inset = band / 2 + size.minDimension * .03f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(
                NativePalette.border, 0f, 360f, false, topLeft, arcSize,
                style = Stroke(band),
            )
            if (value != null && sweep > .5f) {
                drawArc(
                    Brush.sweepGradient(tones, center), -84f, sweep, false, topLeft, arcSize,
                    style = Stroke(band, cap = StrokeCap.Round),
                )
                // The pointer bead. At a small measured value the arc is a short
                // nub that can read as a stray dot, so the leading cap is drawn
                // as a deliberate bead on the track. At 100% the bead sits on the
                // gap and simply closes the ring.
                val angle = Math.toRadians((-84f + sweep).toDouble())
                val radius = size.minDimension / 2f - inset - band / 2f
                drawCircle(
                    Color.White.copy(alpha = .92f),
                    radius = band * .26f,
                    center = Offset(
                        center.x + radius * kotlin.math.cos(angle).toFloat(),
                        center.y + radius * kotlin.math.sin(angle).toFloat(),
                    ),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (value != null && display != null) {
                Label(display, 38, NativePalette.text, FontWeight.Bold, maxLines = 1)
                Label(caption, 11, NativePalette.muted, maxLines = 2, align = TextAlign.Center)
            } else {
                Label("—", 32, NativePalette.muted, FontWeight.Bold, maxLines = 1)
                Label(caption, 11, NativePalette.muted, maxLines = 2, align = TextAlign.Center)
            }
        }
    }
}

/**
 * The action pair every native utility keeps at the bottom.
 *
 * It is pinned and separated by one hairline, exactly like Smart Clean, so the
 * decision never scrolls away on a compact phone and a cut row reads as content
 * that continues rather than a card sliced in half.
 */
@Composable
internal fun PinnedActions(
    first: Action,
    second: Action,
    onFirst: () -> Unit,
    onSecond: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(NativePalette.ink)
            .drawBehind {
                drawLine(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            NativePalette.border.copy(alpha = .85f),
                            Color.Transparent,
                        ),
                    ),
                    Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f,
                )
            }
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        MasterActions(first, second, onFirst, onSecond)
    }
}

// ---------------------------------------------------------------------------
// App Manager
// ---------------------------------------------------------------------------

/**
 * One installed application as Android reports it.
 *
 * Only the label, the package and the launcher icon are read: `PackageManager`
 * exposes no per-app size or usage time to a normal app, so the screen never
 * pretends to know them.
 */
internal data class InstalledApp(
    val label: String,
    val packageName: String,
    val system: Boolean,
)

internal fun loadInstalledApps(context: Context): List<InstalledApp> = runCatching {
    val manager = context.packageManager
    val self = context.packageName
    // One launcher query resolves every openable package at once. Asking
    // `getLaunchIntentForPackage` per package costs a separate lookup for each
    // entry and stalls a cold start for a long time on a real device.
    val openable = manager
        .queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0,
        )
        .mapNotNull { it.activityInfo?.packageName }
        .toSet()
    manager.getInstalledApplications(0)
        .asSequence()
        .filter { it.packageName != self }
        // Only packages with something to open: a customer's App Manager is a
        // list of applications, not of every shared library the system ships.
        .filter { it.packageName in openable }
        .map { info ->
            InstalledApp(
                label = runCatching { manager.getApplicationLabel(info).toString() }
                    .getOrDefault(info.packageName),
                packageName = info.packageName,
                system = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
            )
        }
        // Customer-installed apps first, then the system image, each alphabetically.
        .sortedWith(compareBy({ it.system }, { it.label.lowercase() }))
        .toList()
}.getOrDefault(emptyList())

/**
 * One launcher icon, decoded for the row that is actually on screen.
 *
 * Decoding every icon up front stalls a long list on a cold process, so each
 * row asks for its own icon and falls back to the service plate if the package
 * exposes none.
 */
@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toImage(72)
            }.getOrNull()
        }
    }
    return icon
}

/** A launcher icon as a Compose bitmap, drawn at one fixed size. */
private fun Drawable.toImage(sizePx: Int): ImageBitmap? = runCatching {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    bitmap.asImageBitmap()
}.getOrNull()

/**
 * App Manager: the inventory Android lets this build read.
 *
 * Every row opens Android's own App info screen, and the pinned secondary opens
 * the system application list. KNOUX never uninstalls, freezes or clears another
 * application: the decision stays on the system screen the customer already
 * trusts.
 */
@Composable
internal fun NativeAppsScreen(open: (Page) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var focusedApp by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(context) {
        apps = withContext(Dispatchers.IO) { loadInstalledApps(context) }
    }

    Column(Modifier.fillMaxSize().testTag("tool-apps")) {
        NativeScreenTitle(
            Copy(R.string.k_app_manager),
            Copy(R.string.apps_screen_subtitle),
            "screen-${Page.APPS_REVIEW.id}",
            Modifier.fillMaxWidth()
                .padding(start = NativeSpace.edge, top = 12.dp, end = NativeSpace.edge),
        )
        SurfaceCard(
            Modifier.fillMaxWidth()
                .padding(start = NativeSpace.edge + 4.dp, end = NativeSpace.edge + 4.dp, top = 12.dp),
            accent = BrandTone.blue,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ServiceIconPlate(Mark.GROUP, BrandTone.blue, size = 40.dp, iconSize = 20.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Label(Copy(R.string.apps_visible_title), 12, NativePalette.muted, FontWeight.SemiBold)
                    if (apps == null) {
                        Label("—", 18, NativePalette.muted, FontWeight.Bold)
                    } else {
                        Label(
                            Copy.args(R.string.apps_visible_count, KnouxFormat.number(apps!!.size)), 18,
                            NativePalette.text, FontWeight.Bold,
                            modifier = Modifier.testTag("apps-count"),
                        )
                    }
                }
            }
        }
        // The installed apps as a readable starfield: two honest orbits (user
        // inner/bright, system outer/dim), tap a star to name it. Per-app size is
        // not readable here, so every star is the same size — no invented weight.
        val loaded = apps
        if (loaded != null && loaded.isNotEmpty()) {
            SurfaceCard(
                Modifier.fillMaxWidth()
                    .padding(start = NativeSpace.edge + 4.dp, end = NativeSpace.edge + 4.dp, top = 12.dp),
                accent = BrandTone.blue,
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    ConstellationInstrument(apps = loaded, focused = focusedApp, onFocus = { focusedApp = it })
                }
            }
        }
        // One honest line about what the rows can and cannot do. It is allowed
        // four lines: the policy sentence is the part that must never be cut.
        Label(
            Copy(R.string.apps_row_note), 11, NativePalette.muted,
            modifier = Modifier.padding(
                start = NativeSpace.edge, end = NativeSpace.edge, top = 10.dp,
            ),
            maxLines = 4,
        )
        if (apps == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Label(Copy(R.string.apps_reading), 13, NativePalette.muted)
            }
        } else if (apps!!.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Label(Copy(R.string.apps_empty), 13, NativePalette.muted, align = TextAlign.Center)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = NativeSpace.edge, end = NativeSpace.edge, top = 12.dp, bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(apps!!, key = { it.packageName }) { app ->
                    AppRow(app) { openAppInfo(context, app.packageName) }
                }
            }
        }
        PinnedActions(
            first = Action(Copy(R.string.battery_back_tools), Mark.GRID),
            second = Action(Copy(R.string.apps_system_settings), Mark.GEAR),
            onFirst = { open(Page.TOOLS) },
            onSecond = { openSystemApps(context) },
        )
    }
}

@Composable
private fun AppRow(app: InstalledApp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    SurfaceCard(
        Modifier.heightIn(min = 56.dp).clip(shape).testTag("app-${app.packageName}")
            .brandClickable(onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val icon = rememberAppIcon(app.packageName)
            if (icon != null) {
                Image(
                    icon, contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit,
                )
            } else {
                ServiceIconPlate(Mark.BOX, BrandTone.blue, size = 36.dp, iconSize = 18.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(
                    Copy.runtime(app.label), 14, NativePalette.text, FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Label(
                    Copy.runtime(app.packageName), 11, NativePalette.muted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (app.system) {
                Label(Copy(R.string.apps_system), 10, NativePalette.muted.copy(alpha = .8f), maxLines = 1)
            }
            Glyph(
                Mark.NEXT,
                Modifier.size(14.dp).rotate(if (LocalArabic.current) 180f else 0f),
                NativePalette.muted.copy(alpha = .6f),
            )
        }
    }
}

private fun openAppInfo(context: Context, packageName: String) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openSystemApps(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
