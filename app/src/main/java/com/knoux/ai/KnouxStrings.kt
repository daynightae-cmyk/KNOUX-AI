package com.knoux.ai

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import java.text.NumberFormat
import java.util.Locale

/**
 * Resolves every customer-visible string from resources.
 *
 * The app keeps its own language switch, so the requested language is applied to
 * the resources explicitly instead of trusting the system locale. English is the
 * default resource set; Arabic lives in `values-ar`.
 */
internal object KnouxStrings {
    private var english: Resources? = null
    private var arabic: Resources? = null

    fun install(context: Context) {
        if (english != null && arabic != null) return
        val app = context.applicationContext
        english = resourcesFor(app, Locale.ENGLISH)
        arabic = resourcesFor(app, Locale.forLanguageTag("ar"))
    }

    private fun resourcesFor(context: Context, locale: Locale): Resources {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config).resources
    }

    fun get(@StringRes id: Int, arabicWanted: Boolean, args: Array<out Any?> = emptyArray()): String {
        val resources = (if (arabicWanted) arabic else english) ?: english ?: return ""
        return if (args.isEmpty()) resources.getString(id) else resources.getString(id, *args)
    }
}

/**
 * A customer-visible string.
 *
 * Production copy is a resource id, optionally with positional arguments. The
 * `(en, ar)` constructor exists only for text that arrives from the platform or
 * the data layer (an exception message, an OS status label); authored interface
 * copy must always be a resource.
 */
internal class Copy private constructor(
    @StringRes private val id: Int,
    private val directEn: String?,
    private val directAr: String?,
    private val formatArgs: Array<out Any?>,
) {
    constructor(en: String, ar: String) : this(0, en, ar, emptyArray())

    constructor(@StringRes id: Int) : this(id, null, null, emptyArray())

    fun text(arabicWanted: Boolean): String =
        if (id != 0) KnouxStrings.get(id, arabicWanted, formatArgs)
        else (if (arabicWanted) directAr ?: directEn else directEn).orEmpty()

    override fun equals(other: Any?): Boolean =
        other is Copy && other.text(false) == text(false) && other.text(true) == text(true)

    override fun hashCode(): Int = 31 * text(false).hashCode() + text(true).hashCode()

    override fun toString(): String = text(false)

    companion object {
        /** A resource whose placeholders are filled with already-formatted values. */
        fun args(@StringRes id: Int, vararg args: Any?): Copy = Copy(id, null, null, args)

        /** Platform text that is already in the customer's language. */
        fun runtime(value: String?): Copy = Copy(0, value.orEmpty(), value.orEmpty(), emptyArray())
    }
}

/**
 * One formatter for every measured value.
 *
 * Digits are Latin in both languages so a screen can never mix Arabic-Indic and
 * Latin numerals. A non-breaking space keeps the unit attached to its value, and
 * an LTR isolate stops bidi from reordering "128 GB" inside an Arabic sentence.
 */
internal object KnouxFormat {
    private val latin = Locale.US
    private const val NBSP = '\u00A0'

    fun number(value: Long): String = NumberFormat.getIntegerInstance(latin).format(value)

    fun number(value: Int): String = number(value.toLong())

    fun decimal(value: Double, digits: Int = 1): String =
        NumberFormat.getNumberInstance(latin).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = digits
            isGroupingUsed = false
        }.format(value)

    /** Isolate a measured token so bidi never swaps its sign or unit. */
    fun isolate(value: String): String = "\u2066$value\u2069"

    /** "128 GB" with a non-breaking space, isolated. */
    fun unit(value: String, unit: String): String = isolate("$value$NBSP$unit")

    fun bytes(bytes: Long): String {
        if (bytes <= 0) return unit("0", "B")
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        val rendered = if (index == 0 || value >= 100) number(value.toLong()) else decimal(value, 1)
        return unit(rendered, units[index])
    }

    fun percent(value: Int): String = unit(number(value), "%")

    fun signed(value: String): String = isolate("\u2212$value")
}

/**
 * Files that a service created for QA carry machine names. Customers must never
 * see `knoux_qa_*`, a UUID or an internal cache path.
 */
internal object KnouxNames {
    private val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private val machine = Regex("(?i)(knoux[_-](qa|residue|dup|scan|test)[^/\\\\]*|knoux_[0-9a-f]{8,})")

    fun friendly(raw: String?, arabicWanted: Boolean): String {
        val name = raw?.substringAfterLast('/')?.substringAfterLast('\\')?.trim().orEmpty()
        if (name.isEmpty() || name == "." || name == "..") {
            return KnouxStrings.get(R.string.file_name_temporary, arabicWanted)
        }
        if (uuid.containsMatchIn(name) || machine.containsMatchIn(name)) {
            return KnouxStrings.get(R.string.file_name_temporary, arabicWanted)
        }
        return name
    }
}

/** The readable name of one source the customer allowed KNOUX to read. */
internal fun scopeLabel(scope: com.knoux.ai.data.AccessScope): Copy = when (scope) {
    com.knoux.ai.data.AccessScope.APP_CACHE -> Copy(R.string.k_knoux_cache)
    com.knoux.ai.data.AccessScope.MEDIA_IMAGES -> Copy(R.string.k_photos_2)
    com.knoux.ai.data.AccessScope.MEDIA_VIDEO -> Copy(R.string.k_videos_3)
    com.knoux.ai.data.AccessScope.SAF_TREE -> Copy(R.string.k_granted_folders)
}

/**
 * How a stored measurement describes itself. The enum is honest data, but a
 * customer reads a word in their own language, never `PARTIAL` in Latin caps.
 */
internal fun dataStatusLabel(status: com.knoux.ai.data.DataStatus): Copy = when (status) {
    com.knoux.ai.data.DataStatus.MEASURED -> Copy(R.string.status_measured)
    com.knoux.ai.data.DataStatus.ESTIMATED -> Copy(R.string.status_estimated)
    com.knoux.ai.data.DataStatus.PREVIEW -> Copy(R.string.status_preview)
    com.knoux.ai.data.DataStatus.UNAVAILABLE -> Copy(R.string.status_unavailable)
    com.knoux.ai.data.DataStatus.PARTIAL -> Copy(R.string.status_partial)
    com.knoux.ai.data.DataStatus.VERIFIED -> Copy(R.string.status_verified)
}

/** How one cleanup or restore operation ended, in the customer's language. */
internal fun operationStatusLabel(status: com.knoux.ai.data.OperationStatus): Copy = when (status) {
    com.knoux.ai.data.OperationStatus.PLANNED -> Copy(R.string.op_planned)
    com.knoux.ai.data.OperationStatus.AWAITING_USER_CONFIRMATION -> Copy(R.string.op_awaiting)
    com.knoux.ai.data.OperationStatus.RUNNING -> Copy(R.string.op_running)
    com.knoux.ai.data.OperationStatus.SUCCEEDED -> Copy(R.string.op_succeeded)
    com.knoux.ai.data.OperationStatus.PARTIAL -> Copy(R.string.status_partial)
    com.knoux.ai.data.OperationStatus.FAILED -> Copy(R.string.op_failed)
    com.knoux.ai.data.OperationStatus.CANCELLED -> Copy(R.string.op_cancelled)
    com.knoux.ai.data.OperationStatus.REJECTED_BY_OS -> Copy(R.string.op_rejected_os)
}

/** Why a file looked like residue: the filename pattern, in plain words. */
internal fun residuePatternLabel(pattern: com.knoux.ai.data.ResiduePattern): Copy = when (pattern) {
    com.knoux.ai.data.ResiduePattern.CACHE_FILE -> Copy(R.string.residue_pattern_cache_file)
    com.knoux.ai.data.ResiduePattern.TEMP_FILE -> Copy(R.string.residue_pattern_temp_file)
    com.knoux.ai.data.ResiduePattern.PARTIAL_DOWNLOAD -> Copy(R.string.residue_pattern_partial_download)
    com.knoux.ai.data.ResiduePattern.INTERRUPTED_TRASH -> Copy(R.string.residue_pattern_interrupted_trash)
    com.knoux.ai.data.ResiduePattern.PENDING_DELETE -> Copy(R.string.residue_pattern_pending_delete)
    com.knoux.ai.data.ResiduePattern.EDITOR_BACKUP -> Copy(R.string.residue_pattern_editor_backup)
    com.knoux.ai.data.ResiduePattern.LOG_FILE -> Copy(R.string.residue_pattern_log_file)
    com.knoux.ai.data.ResiduePattern.THUMBNAIL -> Copy(R.string.residue_pattern_thumbnail)
}

/** How much the filename evidence actually proves. */
internal fun residueConfidenceLabel(confidence: com.knoux.ai.data.ResidueConfidence): Copy = when (confidence) {
    com.knoux.ai.data.ResidueConfidence.OWNED -> Copy(R.string.residue_confidence_owned)
    com.knoux.ai.data.ResidueConfidence.STRONG -> Copy(R.string.residue_confidence_strong)
    com.knoux.ai.data.ResidueConfidence.WEAK -> Copy(R.string.residue_confidence_weak)
}

/** What the customer did with a residue item. */
internal fun residueActionLabel(action: com.knoux.ai.data.ResidueAction): Copy = when (action) {
    com.knoux.ai.data.ResidueAction.REVIEWED -> Copy(R.string.residue_action_reviewed)
    com.knoux.ai.data.ResidueAction.DRY_RUN -> Copy(R.string.residue_action_dry_run)
    com.knoux.ai.data.ResidueAction.SKIPPED -> Copy(R.string.residue_action_skipped)
    com.knoux.ai.data.ResidueAction.ATTEMPTED -> Copy(R.string.residue_action_attempted)
    com.knoux.ai.data.ResidueAction.VERIFIED -> Copy(R.string.residue_action_verified)
}
