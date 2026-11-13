package com.knoux.ai.data

import android.content.Context

/**
 * Minimal manual injection.
 *
 * The project has no DI framework and does not need one for four singletons;
 * adding Hilt here would mean a new Gradle plugin and KSP processor purely for
 * plumbing. Everything is created from the application context and shared.
 */
object KnouxData {

    @Volatile
    private var appContext: Context? = null

    private val permissionsRef = lazy { PermissionState(requireContext()) }
    private val databaseRef = lazy { KnouxDatabase.get(requireContext()) }
    private val settingsRef = lazy { SettingsStore.create(requireContext()) }
    private val repositoryRef = lazy { SmartCleanRepository(requireContext(), permissions) }
    private val controllerRef = lazy { SmartCleanController(repository, database, settings, permissions) }
    private val duplicateRepositoryRef = lazy { DuplicateRepository(requireContext(), permissions) }
    private val duplicateControllerRef = lazy {
        DuplicateController(duplicateRepository, database, settings, smartClean)
    }

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    private fun requireContext(): Context =
        appContext ?: error("KnouxData.init(context) must be called before use")

    /** Application context, needed by workers and scheduling helpers. */
    val application: Context get() = requireContext()

    val permissions: PermissionState get() = permissionsRef.value
    val database: KnouxDatabase get() = databaseRef.value
    val settings: SettingsStore get() = settingsRef.value
    val repository: SmartCleanRepository get() = repositoryRef.value
    val smartClean: SmartCleanController get() = controllerRef.value
    val duplicateRepository: DuplicateRepository get() = duplicateRepositoryRef.value
    val duplicates: DuplicateController get() = duplicateControllerRef.value
}
