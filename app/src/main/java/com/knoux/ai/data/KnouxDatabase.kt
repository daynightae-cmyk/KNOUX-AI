package com.knoux.ai.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * PC00 durable store.
 *
 * Enums are persisted as their `name` strings rather than through ordinal
 * converters: an ordinal would silently change meaning if an enum constant were
 * ever reordered, which is exactly the kind of quiet corruption a migration
 * cannot repair.
 *
 * Version 1 is the first schema. No destructive fallback is configured anywhere,
 * so an unexpected downgrade crashes loudly instead of wiping customer history.
 */

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val status: String,
    /** Comma-separated AccessScope names actually included in this scan. */
    val scopes: String,
    val itemCount: Int,
    val totalBytes: Long,
)

@Entity(
    tableName = "source_snapshots",
    indices = [Index("sessionId")]
)
data class SourceSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sessionId: String,
    val scope: String,
    val provenance: String,
    val grant: String,
    val itemCount: Int,
    val totalBytes: Long,
    val capturedAt: Long,
    val status: String,
    val detail: String?,
)

@Entity(
    tableName = "candidates",
    indices = [Index(value = ["sessionId", "uri"], unique = true)]
)
data class CandidateEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sessionId: String,
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val bytes: Long,
    val lastModified: Long,
    val category: String,
    val provenance: String,
    val eligibility: String,
    val reason: String,
)

@Entity(tableName = "operations", indices = [Index("startedAt")])
data class OperationEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val status: String,
    val startedAt: Long,
    val completedAt: Long?,
    val itemCount: Int,
    val bytes: Long,
    val volumeFreeBefore: Long?,
    val volumeFreeAfter: Long?,
    val note: String?,
)

@Entity(tableName = "operation_items", indices = [Index("operationId")])
data class OperationItemEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val operationId: String,
    val uri: String,
    val displayName: String,
    val bytes: Long,
    val outcome: String,
    val error: String?,
)

// ── PC02 duplicate evidence ──────────────────────────────────────────────────

/**
 * One duplicate pass. Counts are the measured outcome of that pass, so a stale
 * row is still labelled with when it was captured rather than treated as live.
 */
@Entity(tableName = "duplicate_scans")
data class DuplicateScanEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val status: String,
    /** Comma-separated AccessScope names actually included. */
    val scopes: String,
    val fileCount: Int,
    val hashedCount: Int,
    val bytesHashed: Long,
    val groupCount: Int,
    val redundantBytes: Long,
    val statusNote: String?,
)

/**
 * One SHA-256 group. The primary key is (scanId, id) so a later pass can never
 * overwrite an earlier pass's evidence: history is append-only by construction.
 */
@Entity(tableName = "duplicate_groups", primaryKeys = ["scanId", "id"])
data class DuplicateGroupEntity(
    val scanId: String,
    /** Content hash — unique within one scan. */
    val id: String,
    val contentHash: String,
    val bytes: Long,
    val keeperUri: String,
    val keeperName: String,
    val keeperModified: Long,
    val memberCount: Int,
    val redundantBytes: Long,
)

@Entity(
    tableName = "duplicate_members",
    indices = [Index(value = ["scanId", "uri"], unique = true), Index("groupId")]
)
data class DuplicateMemberEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val scanId: String,
    val groupId: String,
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val bytes: Long,
    val lastModified: Long,
    val category: String,
    val provenance: String,
    val eligibility: String,
    val reason: String,
    val isKeeper: Boolean,
)

// ── PC03 residue evidence ────────────────────────────────────────────────────

/**
 * One residue pass. [fileCount] is how many files were actually inspected, so a
 * pass that hit its entry ceiling cannot be mistaken for a complete library read.
 */
@Entity(tableName = "residue_scans")
data class ResidueScanEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val status: String,
    /** Comma-separated AccessScope names actually included. */
    val scopes: String,
    /** Age threshold applied to shared folders/media; own cache ignores it. */
    val minAgeDays: Int,
    val fileCount: Int,
    val itemCount: Int,
    val bytes: Long,
    val note: String?,
)

/**
 * One measured residue candidate. The composite key keeps every pass append-only:
 * a later pass cannot overwrite the evidence of an earlier one.
 */
@Entity(tableName = "residue_items", primaryKeys = ["scanId", "uri"])
data class ResidueItemEntity(
    val scanId: String,
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val bytes: Long,
    val lastModified: Long,
    val category: String,
    val provenance: String,
    val eligibility: String,
    val pattern: String,
    val confidence: String,
    val matchedOn: String,
    val reason: String,
)

/**
 * What the customer decided, including the decisions that removed nothing.
 *
 * A "reviewed, kept everything" entry is evidence too: without it the history
 * would imply that only removals ever happened.
 */
@Entity(tableName = "residue_decisions", indices = [Index("createdAt")])
data class ResidueDecisionEntity(
    @PrimaryKey val id: String,
    val scanId: String?,
    val action: String,
    val itemCount: Int,
    val bytes: Long,
    val createdAt: Long,
    val note: String?,
)

/**
 * A saved rule for one granted folder.
 *
 * The SAF grant itself lives in the OS; this row stores only the customer's rules
 * for it, and is deleted when the folder stops being granted.
 */
@Entity(tableName = "residue_profiles")
data class ResidueProfileEntity(
    @PrimaryKey val treeUri: String,
    val label: String,
    val minAgeDays: Int,
    /** Comma-separated ResiduePattern names; empty means "every pattern". */
    val patterns: String,
    val createdAt: Long,
    val updatedAt: Long,
)

// ── PC04 privacy evidence ────────────────────────────────────────────────────

/**
 * One privacy sweep. [score] is nullable on purpose: a pass that measured only
 * informational checks has no score, and storing 0 would read as a failure.
 */
@Entity(tableName = "privacy_scans")
data class PrivacyScanEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val status: String,
    val score: Int?,
    val scoredCount: Int,
    val measuredCount: Int,
    val attentionCount: Int,
    val unmeasurableCount: Int,
    val note: String?,
)

/** One observed row. Composite key keeps every pass append-only. */
@Entity(tableName = "privacy_findings", primaryKeys = ["scanId", "checkId"])
data class PrivacyFindingEntity(
    val scanId: String,
    val checkId: String,
    val status: String,
    val severity: String,
    val observed: String,
    val detail: String,
    val remedy: String,
    val recordedAt: Long,
)

/**
 * What the customer (or the sweep) did about a row.
 *
 * [outcome] is `OPENED` for an Android screen KNOUX merely launched, and
 * `VERIFIED` only after the platform was re-read and confirmed the change.
 */
@Entity(tableName = "privacy_actions", indices = [Index("createdAt")])
data class PrivacyActionEntity(
    @PrimaryKey val id: String,
    val scanId: String?,
    val checkId: String,
    val action: String,
    val outcome: String,
    val createdAt: Long,
    val note: String?,
)

/** Items the customer asked us to keep, so a later scan cannot re-offer them. */
@Entity(tableName = "exclusions")
data class ExclusionEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val rule: String,
    val createdAt: Long,
)

/** A user-configured scan schedule. Scan only — it never deletes on its own. */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val enabled: Boolean,
    val intervalMinutes: Int,
    val updatedAt: Long,
)

@Dao
interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ScanSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<SourceSnapshotEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCandidates(candidates: List<CandidateEntity>)

    @Query("SELECT * FROM scan_sessions ORDER BY startedAt DESC LIMIT 20")
    fun observeSessions(): Flow<List<ScanSessionEntity>>

    @Query("SELECT * FROM scan_sessions WHERE id = :id")
    suspend fun session(id: String): ScanSessionEntity?

    @Query("SELECT * FROM candidates WHERE sessionId = :sessionId ORDER BY bytes DESC")
    suspend fun candidates(sessionId: String): List<CandidateEntity>

    @Query("SELECT * FROM candidates WHERE sessionId = :sessionId ORDER BY bytes DESC")
    fun observeCandidates(sessionId: String): Flow<List<CandidateEntity>>

    @Query("SELECT * FROM source_snapshots WHERE sessionId = :sessionId")
    suspend fun snapshots(sessionId: String): List<SourceSnapshotEntity>

    @Query("DELETE FROM candidates WHERE sessionId = :sessionId")
    suspend fun clearCandidates(sessionId: String)

    @Query("SELECT * FROM scan_sessions WHERE status IN ('COMPLETED', 'PARTIAL') ORDER BY startedAt DESC, id DESC LIMIT 1")
    suspend fun latestFinishedSession(): ScanSessionEntity?

    @Query("DELETE FROM source_snapshots WHERE sessionId = :sessionId")
    suspend fun clearSnapshots(sessionId: String)

    @Query("DELETE FROM candidates WHERE uri = :uri")
    suspend fun removeCandidateUri(uri: String)

    /** Counts describe the retained snapshot, not a new observation of the source. */
    @Query("""
        UPDATE scan_sessions SET
            itemCount = (SELECT COUNT(*) FROM candidates WHERE sessionId = scan_sessions.id),
            totalBytes = COALESCE((SELECT SUM(bytes) FROM candidates WHERE sessionId = scan_sessions.id), 0)
    """)
    suspend fun refreshSessionTotals()

    @Query("""
        UPDATE source_snapshots SET
            itemCount = (SELECT COUNT(*) FROM candidates c WHERE c.sessionId = source_snapshots.sessionId
                AND ((source_snapshots.scope = 'APP_CACHE' AND c.provenance = 'APP_OWNED_CACHE')
                  OR (source_snapshots.scope = 'SAF_TREE' AND c.provenance = 'SAF_DOCUMENT')
                  OR (source_snapshots.scope = 'MEDIA_IMAGES' AND c.provenance = 'MEDIASTORE' AND c.category = 'IMAGE')
                  OR (source_snapshots.scope = 'MEDIA_VIDEO' AND c.provenance = 'MEDIASTORE' AND c.category = 'VIDEO'))),
            totalBytes = COALESCE((SELECT SUM(c.bytes) FROM candidates c WHERE c.sessionId = source_snapshots.sessionId
                AND ((source_snapshots.scope = 'APP_CACHE' AND c.provenance = 'APP_OWNED_CACHE')
                  OR (source_snapshots.scope = 'SAF_TREE' AND c.provenance = 'SAF_DOCUMENT')
                  OR (source_snapshots.scope = 'MEDIA_IMAGES' AND c.provenance = 'MEDIASTORE' AND c.category = 'IMAGE')
                  OR (source_snapshots.scope = 'MEDIA_VIDEO' AND c.provenance = 'MEDIASTORE' AND c.category = 'VIDEO'))), 0)
    """)
    suspend fun refreshSnapshotTotals()
}

@Dao
interface OperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOperation(operation: OperationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<OperationItemEntity>)

    @Query("SELECT * FROM operations ORDER BY startedAt DESC LIMIT 50")
    fun observeOperations(): Flow<List<OperationEntity>>

    @Query("SELECT * FROM operations WHERE id = :id")
    suspend fun operation(id: String): OperationEntity?

    @Query("SELECT * FROM operation_items WHERE operationId = :operationId")
    suspend fun items(operationId: String): List<OperationItemEntity>

    /** Legacy read API; use claimOperation in a transaction, never check-then-act. */
    @Query("SELECT COUNT(*) FROM operations WHERE id = :id")
    suspend fun operationCount(id: String): Int

    /** Returns -1 when another caller already owns this durable plan id. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun claimOperation(operation: OperationEntity): Long

    @Query("UPDATE operations SET status = :nextStatus WHERE id = :id AND status = :expectedStatus")
    suspend fun transition(id: String, expectedStatus: String, nextStatus: String): Int

    @Query("""
        SELECT * FROM operations WHERE status IN ('RUNNING', 'AWAITING_USER_CONFIRMATION')
        ORDER BY startedAt, id
    """)
    suspend fun interrupted(): List<OperationEntity>

    @Query("""
        SELECT * FROM operations WHERE kind = 'MOVE_TO_TRASH' AND status IN ('SUCCEEDED', 'PARTIAL')
        AND EXISTS (SELECT 1 FROM operation_items WHERE operationId = operations.id AND outcome = 'TRASHED_VERIFIED')
        ORDER BY startedAt DESC, id DESC LIMIT 1
    """)
    suspend fun latestVerifiedTrash(): OperationEntity?

    @Query("""
        SELECT DISTINCT i.uri FROM operation_items i JOIN operations o ON o.id = i.operationId
        WHERE o.kind = 'RESTORE_FROM_TRASH' AND i.outcome = 'RESTORED_VERIFIED'
        AND o.completedAt >= :since
    """)
    suspend fun restoredUrisSince(since: Long): List<String>
}

@Dao
interface ExclusionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(exclusion: ExclusionEntity)

    @Query("DELETE FROM exclusions WHERE uri = :uri")
    suspend fun remove(uri: String)

    @Query("SELECT uri FROM exclusions")
    fun observeUris(): Flow<List<String>>

    @Query("SELECT uri FROM exclusions")
    suspend fun uris(): List<String>

    @Query("SELECT * FROM exclusions ORDER BY createdAt DESC, uri")
    fun observeAll(): Flow<List<ExclusionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM exclusions WHERE uri = :uri)")
    suspend fun contains(uri: String): Boolean
}

@Dao
interface DuplicateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScan(scan: DuplicateScanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<DuplicateGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<DuplicateMemberEntity>)

    @Query("SELECT * FROM duplicate_scans WHERE status IN ('MEASURED', 'PARTIAL') ORDER BY startedAt DESC, id DESC LIMIT 1")
    suspend fun latestFinishedScan(): DuplicateScanEntity?

    @Query("SELECT * FROM duplicate_scans ORDER BY startedAt DESC, id DESC LIMIT 20")
    fun observeScans(): Flow<List<DuplicateScanEntity>>

    @Query("SELECT * FROM duplicate_groups WHERE scanId = :scanId ORDER BY redundantBytes DESC, id")
    suspend fun groups(scanId: String): List<DuplicateGroupEntity>

    @Query("SELECT * FROM duplicate_members WHERE scanId = :scanId")
    suspend fun members(scanId: String): List<DuplicateMemberEntity>

    @Query("DELETE FROM duplicate_groups WHERE scanId = :scanId")
    suspend fun clearGroups(scanId: String)

    @Query("DELETE FROM duplicate_members WHERE scanId = :scanId")
    suspend fun clearMembers(scanId: String)

    @Query("""
        UPDATE duplicate_scans SET
            fileCount = :fileCount, groupCount = :groupCount, redundantBytes = :redundantBytes
        WHERE id = :scanId
    """)
    suspend fun updateSummary(scanId: String, fileCount: Int, groupCount: Int, redundantBytes: Long)
}

@Dao
interface ResidueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScan(scan: ResidueScanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ResidueItemEntity>)

    @Query("SELECT * FROM residue_items WHERE scanId = :scanId ORDER BY bytes DESC, displayName")
    suspend fun items(scanId: String): List<ResidueItemEntity>

    @Query("DELETE FROM residue_items WHERE scanId = :scanId")
    suspend fun clearItems(scanId: String)

    @Query("SELECT * FROM residue_scans WHERE status IN ('MEASURED', 'PARTIAL') ORDER BY startedAt DESC, id DESC LIMIT 1")
    suspend fun latestFinishedScan(): ResidueScanEntity?

    @Query("SELECT * FROM residue_scans ORDER BY startedAt DESC, id DESC LIMIT 20")
    fun observeScans(): Flow<List<ResidueScanEntity>>

    @Query("DELETE FROM residue_items WHERE uri = :uri")
    suspend fun removeItem(uri: String)

    @Query("""
        UPDATE residue_scans SET
            itemCount = (SELECT COUNT(*) FROM residue_items WHERE scanId = residue_scans.id),
            bytes = COALESCE((SELECT SUM(bytes) FROM residue_items WHERE scanId = residue_scans.id), 0)
        WHERE id = :scanId
    """)
    suspend fun refreshTotals(scanId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putDecision(decision: ResidueDecisionEntity)

    @Query("SELECT * FROM residue_decisions ORDER BY createdAt DESC LIMIT 50")
    fun observeDecisions(): Flow<List<ResidueDecisionEntity>>

    @Query("SELECT * FROM residue_decisions WHERE scanId = :scanId ORDER BY createdAt DESC")
    suspend fun decisions(scanId: String): List<ResidueDecisionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putProfile(profile: ResidueProfileEntity)

    @Query("DELETE FROM residue_profiles WHERE treeUri = :treeUri")
    suspend fun removeProfile(treeUri: String)

    @Query("SELECT * FROM residue_profiles ORDER BY updatedAt DESC, treeUri")
    fun observeProfiles(): Flow<List<ResidueProfileEntity>>

    @Query("SELECT * FROM residue_profiles ORDER BY updatedAt DESC, treeUri")
    suspend fun profiles(): List<ResidueProfileEntity>

    @Query("SELECT * FROM residue_profiles WHERE treeUri = :treeUri")
    suspend fun profile(treeUri: String): ResidueProfileEntity?
}

/**
 * PC04 privacy history. The sweep is read-only apart from KNOUX's own
 * screenshot-protection setting, so this DAO stores observations and actions,
 * never another app's data.
 */
@Dao
interface PrivacyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScan(scan: PrivacyScanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(findings: List<PrivacyFindingEntity>)

    @Query("SELECT * FROM privacy_findings WHERE scanId = :scanId ORDER BY checkId")
    suspend fun findings(scanId: String): List<PrivacyFindingEntity>

    @Query("DELETE FROM privacy_findings WHERE scanId = :scanId")
    suspend fun clearFindings(scanId: String)

    @Query("SELECT * FROM privacy_scans WHERE status IN ('MEASURED', 'PARTIAL') ORDER BY startedAt DESC, id DESC LIMIT 1")
    suspend fun latestFinishedScan(): PrivacyScanEntity?

    @Query("SELECT * FROM privacy_scans ORDER BY startedAt DESC, id DESC LIMIT 20")
    fun observeScans(): Flow<List<PrivacyScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAction(action: PrivacyActionEntity)

    @Query("SELECT * FROM privacy_actions ORDER BY createdAt DESC LIMIT 50")
    fun observeActions(): Flow<List<PrivacyActionEntity>>

    @Query("SELECT * FROM privacy_actions WHERE scanId = :scanId ORDER BY createdAt DESC")
    suspend fun actions(scanId: String): List<PrivacyActionEntity>
}

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun get(id: String): ScheduleEntity?

    @Query("SELECT * FROM schedules")
    fun observeAll(): Flow<List<ScheduleEntity>>
}

@Database(
    entities = [
        ScanSessionEntity::class,
        SourceSnapshotEntity::class,
        CandidateEntity::class,
        OperationEntity::class,
        OperationItemEntity::class,
        DuplicateScanEntity::class,
        DuplicateGroupEntity::class,
        DuplicateMemberEntity::class,
        ExclusionEntity::class,
        ScheduleEntity::class,
        ResidueScanEntity::class,
        ResidueItemEntity::class,
        ResidueDecisionEntity::class,
        ResidueProfileEntity::class,
        PrivacyScanEntity::class,
        PrivacyFindingEntity::class,
        PrivacyActionEntity::class,
    ],
    version = 4,
    exportSchema = true
)
abstract class KnouxDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun operationDao(): OperationDao
    abstract fun duplicateDao(): DuplicateDao
    abstract fun exclusionDao(): ExclusionDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun residueDao(): ResidueDao
    abstract fun privacyDao(): PrivacyDao

    companion object {
        /** PC02 duplicates: new tables only, proven PC00/PC01 tables untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `duplicate_scans` (`id` TEXT NOT NULL, " +
                        "`startedAt` INTEGER NOT NULL, `finishedAt` INTEGER, `status` TEXT NOT NULL, " +
                        "`scopes` TEXT NOT NULL, `fileCount` INTEGER NOT NULL, `hashedCount` INTEGER NOT NULL, " +
                        "`bytesHashed` INTEGER NOT NULL, `groupCount` INTEGER NOT NULL, " +
                        "`redundantBytes` INTEGER NOT NULL, `statusNote` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `duplicate_groups` (`scanId` TEXT NOT NULL, " +
                        "`id` TEXT NOT NULL, `contentHash` TEXT NOT NULL, `bytes` INTEGER NOT NULL, " +
                        "`keeperUri` TEXT NOT NULL, `keeperName` TEXT NOT NULL, `keeperModified` INTEGER NOT NULL, " +
                        "`memberCount` INTEGER NOT NULL, `redundantBytes` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`scanId`, `id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `duplicate_members` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`scanId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `uri` TEXT NOT NULL, " +
                        "`displayName` TEXT NOT NULL, `mimeType` TEXT, `bytes` INTEGER NOT NULL, " +
                        "`lastModified` INTEGER NOT NULL, `category` TEXT NOT NULL, `provenance` TEXT NOT NULL, " +
                        "`eligibility` TEXT NOT NULL, `reason` TEXT NOT NULL, `isKeeper` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_duplicate_members_scanId_uri` " +
                        "ON `duplicate_members` (`scanId`, `uri`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_duplicate_members_groupId` ON `duplicate_members` (`groupId`)"
                )
            }
        }

        /**
         * PC03 residue: four new tables, no change to any existing column.
         *
         * A residue profile is a customer rule for one SAF tree, not a copy of the
         * OS grant, so deleting the app's rows never revives a revoked folder.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `residue_scans` (`id` TEXT NOT NULL, " +
                        "`startedAt` INTEGER NOT NULL, `finishedAt` INTEGER, `status` TEXT NOT NULL, " +
                        "`scopes` TEXT NOT NULL, `minAgeDays` INTEGER NOT NULL, `fileCount` INTEGER NOT NULL, " +
                        "`itemCount` INTEGER NOT NULL, `bytes` INTEGER NOT NULL, `note` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `residue_items` (`scanId` TEXT NOT NULL, `uri` TEXT NOT NULL, " +
                        "`displayName` TEXT NOT NULL, `mimeType` TEXT, `bytes` INTEGER NOT NULL, " +
                        "`lastModified` INTEGER NOT NULL, `category` TEXT NOT NULL, `provenance` TEXT NOT NULL, " +
                        "`eligibility` TEXT NOT NULL, `pattern` TEXT NOT NULL, `confidence` TEXT NOT NULL, " +
                        "`matchedOn` TEXT NOT NULL, `reason` TEXT NOT NULL, PRIMARY KEY(`scanId`, `uri`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `residue_decisions` (`id` TEXT NOT NULL, `scanId` TEXT, " +
                        "`action` TEXT NOT NULL, `itemCount` INTEGER NOT NULL, `bytes` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `note` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_residue_decisions_createdAt` " +
                        "ON `residue_decisions` (`createdAt`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `residue_profiles` (`treeUri` TEXT NOT NULL, `label` TEXT NOT NULL, " +
                        "`minAgeDays` INTEGER NOT NULL, `patterns` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`treeUri`))"
                )
            }
        }
        /**
         * PC04 privacy: three new tables, no change to any existing column.
         *
         * Observations are append-only by composite key, exactly like the residue
         * evidence, so a later sweep cannot rewrite what an earlier one saw.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `privacy_scans` (`id` TEXT NOT NULL, " +
                        "`startedAt` INTEGER NOT NULL, `finishedAt` INTEGER, `status` TEXT NOT NULL, " +
                        "`score` INTEGER, `scoredCount` INTEGER NOT NULL, `measuredCount` INTEGER NOT NULL, " +
                        "`attentionCount` INTEGER NOT NULL, `unmeasurableCount` INTEGER NOT NULL, " +
                        "`note` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `privacy_findings` (`scanId` TEXT NOT NULL, " +
                        "`checkId` TEXT NOT NULL, `status` TEXT NOT NULL, `severity` TEXT NOT NULL, " +
                        "`observed` TEXT NOT NULL, `detail` TEXT NOT NULL, `remedy` TEXT NOT NULL, " +
                        "`recordedAt` INTEGER NOT NULL, PRIMARY KEY(`scanId`, `checkId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `privacy_actions` (`id` TEXT NOT NULL, `scanId` TEXT, " +
                        "`checkId` TEXT NOT NULL, `action` TEXT NOT NULL, `outcome` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `note` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_privacy_actions_createdAt` " +
                        "ON `privacy_actions` (`createdAt`)"
                )
            }
        }
        @Volatile
        private var instance: KnouxDatabase? = null
        /**
         * Single process-wide instance. Reusing it is non-negotiable here: two
         * Room instances over the same file would each keep their own connection
         * pool and invalidation tracker, and history written by one would not
         * refresh the other's flows.
         */
        fun get(context: Context): KnouxDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KnouxDatabase::class.java,
                    "knoux.db"
                )
                    // No fallbackToDestructiveMigration(): losing customer history
                    // is never an acceptable response to a schema change.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
