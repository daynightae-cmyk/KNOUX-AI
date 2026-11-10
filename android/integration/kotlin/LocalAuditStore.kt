package ai.knoux.integration

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Store URI and SHA-256 only in app-private local Room DB; default exclude from cloud sync. */
@Entity(tableName = "scan_sessions")
data class ScanRow(
    @PrimaryKey val id: String,
    val createdAtMs: Long,
    val state: String,
    val origin: String // "measured" or "preview"; NEVER allow preview to enter production reports.
)

@Entity(
    tableName = "media_candidates",
    foreignKeys = [ForeignKey(entity = ScanRow::class, parentColumns = ["id"],
        childColumns = ["scanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("scanId")]
)
data class MediaRow(
    @PrimaryKey val id: String,
    val scanId: String,
    val contentUri: String,
    val sizeBytes: Long,
    val sha256: String?,
    val selected: Boolean = false
)

@Entity(tableName = "action_journal", indices = [Index("scanId")])
data class ActionRow(
    @PrimaryKey val id: String,
    val scanId: String,
    val actionCode: String,
    val status: String,
    val userConsentAtMs: Long?,
    val verifiedReclaimedBytes: Long?
)

@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun addScan(row: ScanRow)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun addMedia(rows: List<MediaRow>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun addAction(row: ActionRow)
    @Query("SELECT * FROM scan_sessions ORDER BY createdAtMs DESC") fun scanHistory(): Flow<List<ScanRow>>
    @Query("SELECT * FROM media_candidates WHERE scanId = :id") suspend fun mediaForScan(id: String): List<MediaRow>
    @Query("SELECT * FROM action_journal WHERE scanId = :id") suspend fun actionsForScan(id: String): List<ActionRow>
}

@Database(entities = [ScanRow::class, MediaRow::class, ActionRow::class], version = 1, exportSchema = true)
abstract class AuditDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
}
