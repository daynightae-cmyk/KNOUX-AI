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
