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
    private val residueRepositoryRef = lazy { ResidueRepository(requireContext(), permissions) }
    private val residueControllerRef = lazy {
        ResidueController(residueRepository, database, settings, permissions, smartClean)
    }
    private val privacyRepositoryRef = lazy { PrivacyRepository(requireContext(), permissions) }
    private val privacyControllerRef = lazy {
        PrivacyController(privacyRepository, database, settings)
    }
    private val deepScanRepositoryRef = lazy {
        DeepScanRepository(permissions, repository, duplicateRepository)
    }
    private val deepScanControllerRef = lazy {
        DeepScanController(deepScanRepository, database, settings, smartClean)
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
    val residueRepository: ResidueRepository get() = residueRepositoryRef.value
    val residue: ResidueController get() = residueControllerRef.value
    val privacyRepository: PrivacyRepository get() = privacyRepositoryRef.value
    val privacy: PrivacyController get() = privacyControllerRef.value
    internal val deepScanRepository: DeepScanRepository get() = deepScanRepositoryRef.value
    internal val deepScan: DeepScanController get() = deepScanControllerRef.value
}
