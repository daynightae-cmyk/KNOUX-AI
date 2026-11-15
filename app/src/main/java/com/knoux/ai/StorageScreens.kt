package com.knoux.ai

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.DeviceInsights
import com.knoux.ai.data.KnouxData
import com.knoux.ai.data.PermissionGrant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Storage identity — Terrain.
 *
 * Reads the real data partition and the real MediaStore aggregates. Anything the
 * app cannot see (documents, other apps' private data, system files) is drawn as
 * an unmeasured remainder instead of being assigned a guess.
 */

private val storageDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)

@Composable
internal fun RealStorageDetailScreen(open: (Page) -> Unit) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<DeviceInsights.StorageSnapshot?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refresh++ }

    LaunchedEffect(refresh) {
        snapshot = withContext(Dispatchers.IO) { DeviceInsights.storage(context) }
    }

    val data = snapshot
    val world = World.TERRAIN
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            IdentityHeader(
                title = Copy(R.string.idn_storage_title),
                caption = Copy(R.string.idn_storage_caption),
                tag = "screen-storage",
                state = if (data == null) Copy(R.string.idn_measuring)
                else Copy(R.string.idn_measured_now),
                stateTint = world.primary,
            )
            if (data == null) {
                IdentityHero(
                    world = world, mark = Mark.DISK,
                    title = Copy(R.string.idn_storage_summary),
                    caption = Copy(R.string.idn_measuring),
                    metrics = listOf(
                        IdentityMetric("—", Copy(R.string.idn_used), world.primary),
                        IdentityMetric("—", Copy(R.string.idn_free), Identity.cyan),
                        IdentityMetric("—", Copy(R.string.idn_total), Identity.muted),
                    ),
                    ring = IdentityRing(null, null, Copy(R.string.idn_used)),
                    tag = "storage-hero",
                )
                IdentityLoading(Copy(R.string.idn_measuring))
            } else {
                StorageHero(data, world)
                StorageBreakdown(data, world, onGrant = {
                    permissionLauncher.launch(KnouxData.permissions.mediaPermissions())
                })
                IdentitySection(Copy(R.string.idn_distribution_title), world.primary)
                SurfaceCard {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DistributionBars(storageBars(data))
                        val measured = data.measuredBytes.coerceAtLeast(0L)
                        val other = (data.usedBytes - measured).coerceAtLeast(0L)
                        if (other > 0 && data.usedBytes > 0) {
                            IdentityNotice(
                                Copy.args(
                                    R.string.idn_storage_other_bytes,
                                    KnouxFormat.bytes(other),
                                ),
                                Identity.muted,
                                Mark.WARN,
                            )
                        }
                        IdentityNotice(Copy(R.string.idn_storage_note), world.primary, Mark.SHIELD)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentButton(
                        Action(Copy(R.string.idn_open_large_files), Mark.LAYERS),
                        Modifier.weight(1f), primary = false, tag = "storage-open-large", labelSize = 12,
                    ) { open(Page.LARGE_FILES) }
                    AccentButton(
                        Action(Copy(R.string.idn_review_clean), Mark.BROOM),
                        Modifier.weight(1f), primary = false, tag = "storage-open-clean", labelSize = 12,
                    ) { open(Page.CLEAN) }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        if (data != null) {
            IdentityActionBar(
                primary = Action(Copy(R.string.idn_storage_analyze), Mark.RADAR),
                secondary = Action(Copy(R.string.idn_open_large_files), Mark.LAYERS),
                onPrimary = { refresh++ },
                onSecondary = { open(Page.LARGE_FILES) },
                primaryTag = "storage-refresh",
            )
        }
    }
}

@Composable
private fun StorageHero(data: DeviceInsights.StorageSnapshot, world: World) {
    val total = data.totalBytes
    val used = data.usedBytes
    val free = data.freeBytes
    IdentityHero(
        world = world,
        mark = Mark.DISK,
        title = Copy(R.string.idn_storage_summary),
        caption = Copy(R.string.idn_storage_caption_short),
        metrics = listOf(
            IdentityMetric(KnouxFormat.bytes(used), Copy(R.string.idn_used), world.primary),
            IdentityMetric(KnouxFormat.bytes(free), Copy(R.string.idn_free), Identity.cyan),
            IdentityMetric(KnouxFormat.bytes(total), Copy(R.string.idn_total), Identity.muted),
        ),
        ring = IdentityRing(
            fraction = if (total > 0) used.toFloat() / total else null,
            text = if (total > 0) KnouxFormat.percent(data.usedPercent) else null,
            label = Copy(R.string.idn_used),
        ),
        footer = IdentityFooter(
            Copy.args(R.string.idn_storage_free_footer, KnouxFormat.bytes(free)),
            Mark.CHECK, world.primary,
        ),
        tag = "storage-hero",
    )
}

@Composable
private fun StorageBreakdown(data: DeviceInsights.StorageSnapshot, world: World, onGrant: () -> Unit) {
    val images = data.categories.firstOrNull { it.key == "images" }
    val video = data.categories.firstOrNull { it.key == "video" }
    val cache = data.categories.firstOrNull { it.key == "cache" }
    IdentitySection(Copy(R.string.idn_storage_categories), world.primary)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (images != null) {
            IdentityRow(Mark.PICTURES, Identity.violet, Copy(R.string.idn_cat_images),
                Copy.args(R.string.idn_files_count, KnouxFormat.number(images.count)),
                KnouxFormat.bytes(images.bytes), tag = "storage-row-images")
        } else {
            IdentityRow(Mark.PICTURES, Identity.muted, Copy(R.string.idn_cat_images),
                Copy(R.string.idn_media_not_measured), null, enabled = false)
        }
        if (video != null) {
            IdentityRow(Mark.VIDEO, Identity.cyan, Copy(R.string.idn_cat_video),
                Copy.args(R.string.idn_files_count, KnouxFormat.number(video.count)),
                KnouxFormat.bytes(video.bytes), tag = "storage-row-video")
        } else {
            IdentityRow(Mark.VIDEO, Identity.muted, Copy(R.string.idn_cat_video),
                Copy(R.string.idn_media_not_measured), null, enabled = false)
        }
        if (cache != null && cache.bytes > 0) {
            IdentityRow(Mark.LAYERS, world.primary, Copy(R.string.idn_cat_app_cache),
                Copy(R.string.idn_cat_app_cache_note), KnouxFormat.bytes(cache.bytes))
        }
        IdentityRow(
            Mark.FOLDER, Identity.amber, Copy(R.string.idn_cat_unmeasured),
            Copy(R.string.idn_cat_unmeasured_note), null, enabled = false,
        )
        if (images == null || video == null) {
            AccentButton(
                Action(Copy(R.string.idn_grant_access), Mark.SHIELD),
                Modifier.fillMaxWidth(), primary = false, tag = "storage-grant-media", labelSize = 12,
                onClick = onGrant,
            )
        }
    }
}

private fun storageBars(data: DeviceInsights.StorageSnapshot): List<IdentityBar> {
    val used = data.usedBytes.coerceAtLeast(1L)
    val labels = mapOf(
        "images" to (Copy(R.string.idn_cat_images) to Identity.violet),
        "video" to (Copy(R.string.idn_cat_video) to Identity.cyan),
        "cache" to (Copy(R.string.idn_cat_app_cache) to Identity.mint),
    )
    val measured = data.categories.filter { it.measured && it.bytes > 0 }.map { category ->
        val (label, tint) = labels[category.key] ?: (Copy.runtime(category.key) to Identity.cyan)
        IdentityBar(label, KnouxFormat.bytes(category.bytes), category.bytes.toFloat() / used, tint, true)
    }
    val accounted = data.categories.filter { it.measured }.sumOf { it.bytes }
    val other = (data.usedBytes - accounted).coerceAtLeast(0L)
    val list = measured.toMutableList()
    if (other > 0) {
        list += IdentityBar(Copy(R.string.idn_cat_other), KnouxFormat.bytes(other), other.toFloat() / used, Identity.muted, false)
    }
    return list
}

/**
 * Large files identity — Strata/Layers.
 *
 * Only visual media is measurable without a folder grant, so only visual media
 * is listed. Deletion is an OS-mediated request: this screen never deletes by
 * itself and never reports a deletion the provider did not confirm.
 */
@Composable
internal fun RealLargeFilesScreen(open: (Page) -> Unit) {
    val context = LocalContext.current
    var minMegabytes by remember { mutableStateOf(10) }
    var onlyVideos by remember { mutableStateOf(false) }
    var onlyPhotos by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<DeviceInsights.MediaResult?>(null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var storage by remember { mutableStateOf<DeviceInsights.StorageSnapshot?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refresh++ }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { response ->
        if (response.resultCode == android.app.Activity.RESULT_OK) {
            selected = emptySet()
            refresh++
        }
    }

    LaunchedEffect(minMegabytes, refresh) {
        result = withContext(Dispatchers.IO) {
            DeviceInsights.largeMedia(context, minMegabytes.toLong() * 1024L * 1024L)
        }
        storage = withContext(Dispatchers.IO) { DeviceInsights.storage(context) }
    }

    val world = World.STRATA
    val files = (result as? DeviceInsights.MediaResult.Measured)?.files.orEmpty()
        .filter { file ->
            when {
                onlyVideos -> file.isVideo
                onlyPhotos -> !file.isVideo
                else -> true
            }
        }
    val selectedBytes = files.filter { it.uri in selected }.sumOf { it.bytes }
    val canDelete = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            IdentityHeader(
                title = Copy(R.string.idn_large_title),
                caption = Copy(R.string.idn_large_caption),
                tag = "screen-large-files",
                state = when (result) {
                    null -> Copy(R.string.idn_measuring)
                    is DeviceInsights.MediaResult.Denied -> Copy(R.string.idn_media_not_measured)
                    is DeviceInsights.MediaResult.Failed -> Copy(R.string.idn_measure_failed)
                    else -> Copy(R.string.idn_measured_now)
                },
                stateTint = world.primary,
            )
            val measured = result as? DeviceInsights.MediaResult.Measured
            val used = storage?.usedBytes ?: 0L
            val total = files.sumOf { it.bytes }
            IdentityHero(
                world = world,
                mark = Mark.LAYERS,
                title = Copy(R.string.idn_large_summary),
                caption = Copy(R.string.idn_large_summary_short),
                metrics = listOf(
                    IdentityMetric(KnouxFormat.number(files.size), Copy(R.string.idn_files_word), world.primary),
                    IdentityMetric(KnouxFormat.bytes(total), Copy(R.string.idn_total), Identity.cyan),
                    IdentityMetric(KnouxFormat.percent(if (used > 0) ((total * 100) / used).toInt() else 0), Copy(R.string.idn_of_used), Identity.muted),
                ),
                ring = IdentityRing(
                    fraction = if (used > 0) total.toFloat() / used else null,
                    text = if (used > 0) KnouxFormat.percent(((total * 100) / used).toInt()) else null,
                    label = Copy(R.string.idn_of_storage),
                ),
                footer = IdentityFooter(Copy(R.string.idn_large_footer), Mark.SHIELD, world.primary),
                tag = "large-hero",
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 50, 100, 500).forEach { option ->
                    IdentityChip(
                        label = Copy.args(R.string.idn_min_mb, KnouxFormat.number(option)),
                        selected = minMegabytes == option,
                        tint = world.primary,
                        onClick = { minMegabytes = option; selected = emptySet() },
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IdentityChip(Copy(R.string.idn_all_types), !onlyVideos && !onlyPhotos, Identity.cyan) {
                    onlyPhotos = false; onlyVideos = false
                }
                IdentityChip(Copy(R.string.idn_photos), onlyPhotos, Identity.violet) {
                    onlyPhotos = true; onlyVideos = false
                }
                IdentityChip(Copy(R.string.idn_videos), onlyVideos, Identity.magenta) {
                    onlyVideos = true; onlyPhotos = false
                }
            }
            IdentitySection(Copy(R.string.idn_large_list), world.primary, count = KnouxFormat.number(files.size))
            when (result) {
                null -> IdentityLoading(Copy(R.string.idn_measuring))
                is DeviceInsights.MediaResult.Denied -> IdentityEmpty(
                    Mark.LOCK, Copy(R.string.idn_media_denied_title), Copy(R.string.idn_media_denied_body), Identity.gold,
                ) {
                    AccentButton(Action(Copy(R.string.idn_grant_access), Mark.SHIELD), primary = false, tag = "large-grant") {
                        permissionLauncher.launch(KnouxData.permissions.mediaPermissions())
                    }
                }
                is DeviceInsights.MediaResult.Failed -> IdentityEmpty(
                    Mark.WARN, Copy(R.string.idn_measure_failed), Copy(R.string.idn_measure_failed_body), Identity.rose,
                ) {
                    AccentButton(Action(Copy(R.string.action_retry), Mark.SYNC), primary = false, tag = "large-retry") { refresh++ }
                }
                is DeviceInsights.MediaResult.Measured -> if (files.isEmpty()) {
                    IdentityEmpty(Mark.LAYERS, Copy(R.string.idn_no_large_title), Copy(R.string.idn_no_large_body), Identity.muted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        files.forEach { file ->
                            LargeFileRow(
                                file = file,
                                tint = world.primary,
                                checked = file.uri in selected,
                                onToggle = {
                                    selected = if (file.uri in selected) selected - file.uri else selected + file.uri
                                },
                                onOpen = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(file.uri))
                                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
            IdentityNotice(Copy(R.string.idn_large_note), world.primary, Mark.SHIELD)
            if (!canDelete) IdentityNotice(Copy(R.string.idn_old_android_note), Identity.gold, Mark.WARN)
            Spacer(Modifier.height(10.dp))
        }
        IdentityActionBar(
            primary = Action(Copy(R.string.idn_move_to_trash), Mark.TRASH),
            secondary = Action(
                if (selected.isNotEmpty() && selected.size == files.size) Copy(R.string.idn_clear_selection)
                else Copy(R.string.idn_select_all),
                Mark.CHECK,
            ),
            enabled = selected.isNotEmpty() && canDelete,
            onPrimary = {
                if (canDelete && selected.isNotEmpty()) {
                    runCatching {
                        val uris = selected.map { Uri.parse(it) }
                        val pending = MediaStore.createDeleteRequest(context.contentResolver, uris)
                        deleteLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    }
                }
            },
            onSecondary = {
                selected = if (selected.size == files.size) emptySet() else files.map { it.uri }.toSet()
            },
            primaryTag = "large-delete",
            secondaryTag = "large-select-all",
        )
    }
}

@Composable
private fun LargeFileRow(
    file: DeviceInsights.MediaFile,
    tint: Color,
    checked: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(listOf(Identity.panelTop, Identity.panelBottom.copy(alpha = .55f))))
            .clickable(onClick = onToggle)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaThumb(file, 46, if (checked) tint else Identity.line)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(KnouxNames.friendly(file.name, LocalArabic.current), 12, Identity.text, FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Label(
                if (file.folder.isBlank()) Copy.runtime(storageDateFormat.format(Date(file.modifiedAtMillis)))
                else Copy.runtime("${file.folder} · ${storageDateFormat.format(Date(file.modifiedAtMillis))}"),
                10, Identity.muted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(KnouxFormat.bytes(file.bytes), 13, tint, FontWeight.Bold, maxLines = 1)
            Box(
                Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                    .background(Identity.panelBottom)
                    .clickable(onClick = onOpen),
                contentAlignment = Alignment.Center,
            ) { Glyph(if (file.isVideo) Mark.PLAY else Mark.PICTURES, Modifier.size(13.dp), Identity.muted) }
        }
        IdentityCheck(checked, onChange = { onToggle() })
    }
}

/** Thumbnails are decoded off the main thread and cached per URI. */
private val thumbnailCache = object : android.util.LruCache<String, Bitmap>(48) {}

@Composable
private fun MediaThumb(file: DeviceInsights.MediaFile, sizeDp: Int, rim: Color) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val pixels = with(density) { (sizeDp * 2).dp.roundToPx() }
    val bitmap by produceState<Bitmap?>(initialValue = thumbnailCache.get(file.uri), file.uri) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val cached = thumbnailCache.get(file.uri)
                    if (cached != null) return@runCatching cached
                    val loaded = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(
                            Uri.parse(file.uri), android.util.Size(pixels, pixels), null,
                        )
                    } else {
                        context.contentResolver.openInputStream(Uri.parse(file.uri))?.use { stream ->
                            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                            BitmapFactory.decodeStream(stream, null, options)
                        }
                    }
                    loaded?.also { thumbnailCache.put(file.uri, it) }
                }.getOrNull()
            }
        }
    }
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.size(sizeDp.dp).clip(shape).background(Identity.panelBottom)
            .then(Modifier.padding(0.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Glyph(if (file.isVideo) Mark.VIDEO else Mark.PICTURES, Modifier.size((sizeDp * .5f).dp), Identity.muted)
        }
        Box(Modifier.fillMaxSize().clip(shape).background(Color.Transparent).border(1.dp, rim.copy(alpha = .6f), shape))
        if (file.isVideo) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(3.dp).size(16.dp)
                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = .55f)),
                contentAlignment = Alignment.Center,
            ) { Glyph(Mark.PLAY, Modifier.size(9.dp), Color.White) }
        }
    }
}

/** Media permission summary shared by the storage surfaces. */
internal fun mediaGrantLabel(grant: PermissionGrant): Copy = when (grant) {
    PermissionGrant.GRANTED -> Copy(R.string.grant_granted)
    PermissionGrant.PARTIAL_SELECTED -> Copy(R.string.grant_partial)
    PermissionGrant.REVOKED -> Copy(R.string.grant_revoked)
    else -> Copy(R.string.grant_denied)
}

/** Kept for the empty category path: media exists but its scope is not granted. */
internal fun mediaScope(scope: AccessScope): Copy = scopeLabel(scope)
