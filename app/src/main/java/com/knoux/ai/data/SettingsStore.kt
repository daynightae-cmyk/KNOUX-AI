package com.knoux.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// File-level singleton — MUST only ever be accessed with applicationContext.
// Use SettingsStore.create(context) or SettingsStore(dataStore) directly.
private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "knoux_settings")

/**
 * DataStore holds *preferences and consent* only.
 *
 * It deliberately holds no entitlement: a boolean in local storage can be flipped
 * on a rooted device, so it can never prove a paid subscription. Pro rights come
 * from [com.knoux.ai.ProSystem] and, when a backend exists, a verified entitlement.
 *
 * Production callers use [SettingsStore.create]; tests inject a DataStore directly
 * via the primary constructor to avoid the applicationContext restriction of the
 * file-level singleton delegate.
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    companion object {
        /**
         * Production factory. Always resolves to applicationContext so the
         * [preferencesDataStore] singleton delegate is backed by the Application.
         */
        fun create(context: Context): SettingsStore =
            SettingsStore(context.applicationContext.appSettingsDataStore)
    }

    private object Keys {
        val SCOPES = stringSetPreferencesKey("scan_scopes")
        val LAST_SCAN_AT = longPreferencesKey("last_scan_at")
        val CONSENT_RECORDED = booleanPreferencesKey("history_consent")
        val SCHEDULE_ENABLED = booleanPreferencesKey("schedule_enabled")
        val SCHEDULE_MINUTES = longPreferencesKey("schedule_minutes")
        val RESIDUE_SCOPES = stringSetPreferencesKey("residue_scopes")
        val RESIDUE_MIN_AGE = longPreferencesKey("residue_min_age_days")
        val PRIVACY_SECURE_WINDOW = booleanPreferencesKey("privacy_secure_window")
    }

    /** Scopes the customer last chose. Defaults to the only always-available one. */
    val selectedScopes: Flow<Set<AccessScope>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.SCOPES]
            ?: return@map setOf(AccessScope.APP_CACHE)
        raw.mapNotNull { name -> AccessScope.entries.firstOrNull { it.name == name } }.toSet()
            .ifEmpty { setOf(AccessScope.APP_CACHE) }
    }

    suspend fun setSelectedScopes(scopes: Set<AccessScope>) {
        dataStore.edit { prefs ->
            prefs[Keys.SCOPES] = scopes.map { it.name }.toSet()
        }
    }

    val lastScanAt: Flow<Long> = dataStore.data.map { it[Keys.LAST_SCAN_AT] ?: 0L }

    suspend fun recordScanAt(millis: Long) {
        dataStore.edit { it[Keys.LAST_SCAN_AT] = millis }
    }

    val historyConsent: Flow<Boolean> = dataStore.data.map { it[Keys.CONSENT_RECORDED] ?: false }

    suspend fun setHistoryConsent(granted: Boolean) {
        dataStore.edit { it[Keys.CONSENT_RECORDED] = granted }
    }

    val scheduleEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.SCHEDULE_ENABLED] ?: false }

    val scheduleMinutes: Flow<Long> = dataStore.data.map { it[Keys.SCHEDULE_MINUTES] ?: 0L }

    suspend fun setSchedule(enabled: Boolean, minutes: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.SCHEDULE_ENABLED] = enabled
            prefs[Keys.SCHEDULE_MINUTES] = minutes
        }
    }

    /**
     * Residue scopes are kept separate from the Smart Clean scopes: turning on a
     * folder for leftover review must not silently widen the next Smart Clean pass.
     */
    val residueScopes: Flow<Set<AccessScope>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.RESIDUE_SCOPES] ?: return@map setOf(AccessScope.APP_CACHE)
        raw.mapNotNull { name -> AccessScope.entries.firstOrNull { it.name == name } }.toSet()
            .ifEmpty { setOf(AccessScope.APP_CACHE) }
    }

    suspend fun setResidueScopes(scopes: Set<AccessScope>) {
        dataStore.edit { prefs -> prefs[Keys.RESIDUE_SCOPES] = scopes.map { it.name }.toSet() }
    }

    /** Shared-folder residue age gate; own cache is always in scope regardless. */
    val residueMinAgeDays: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[Keys.RESIDUE_MIN_AGE] ?: ResidueRules.DEFAULT_MIN_AGE_DAYS.toLong()).toInt()
            .takeIf { it in ResidueRules.AGE_CHOICES } ?: ResidueRules.DEFAULT_MIN_AGE_DAYS
    }

    suspend fun setResidueMinAgeDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.RESIDUE_MIN_AGE] = (days.takeIf { it in ResidueRules.AGE_CHOICES }
                ?: ResidueRules.DEFAULT_MIN_AGE_DAYS).toLong()
        }
    }

    /**
     * PC04 — the one privacy setting KNOUX really owns.
     *
     * `true` makes MainActivity apply FLAG_SECURE to its window, which the OS
     * enforces by excluding the app from screenshots and the recents thumbnail.
     * It is a preference, not a claim: the screen re-reads the actual window flag
     * after applying it and records a mismatch as such.
     */
    val privacySecureWindow: Flow<Boolean> = dataStore.data.map { it[Keys.PRIVACY_SECURE_WINDOW] ?: false }

    suspend fun setPrivacySecureWindow(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PRIVACY_SECURE_WINDOW] = enabled }
    }
}
