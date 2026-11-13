package com.knoux.ai

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.ai.data.KnouxDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Room 3 → 4 on-device proof.
 *
 * Checkpointed residue and operation history is inserted into a real version-3
 * database first, then the PC04 migration runs and Room validates the resulting
 * schema against the exported 4.json. A mistyped or destructive migration fails
 * here rather than on a phone that already holds the customer's history.
 */
@RunWith(AndroidJUnit4::class)
class PrivacyMigrationTest {

    private val databaseName = "knoux-privacy-migration-${UUID.randomUUID()}"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KnouxDatabase::class.java,
    )

    @After fun deleteOnlyTheTemporaryMigrationDatabase() {
        assertTrue(
            "the temporary migration database must be removable",
            InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(databaseName),
        )
    }

    @Test fun versionThreeHistorySurvivesAndPrivacyTablesAppear() {
        helper.createDatabase(databaseName, 3).use { db ->
            db.execSQL(
                "INSERT INTO scan_sessions (id, startedAt, finishedAt, status, scopes, itemCount, totalBytes) " +
                    "VALUES ('legacy-scan', 100, 200, 'COMPLETED', 'APP_CACHE', 3, 4096)"
            )
            db.execSQL(
                "INSERT INTO residue_scans (id, startedAt, finishedAt, status, scopes, minAgeDays, " +
                    "fileCount, itemCount, bytes, note) " +
                    "VALUES ('legacy-residue', 1, 2, 'MEASURED', 'APP_CACHE', 3, 9, 2, 8192, NULL)"
            )
            db.execSQL(
                "INSERT INTO residue_decisions (id, scanId, action, itemCount, bytes, createdAt, note) " +
                    "VALUES ('legacy-decision', 'legacy-residue', 'VERIFIED', 1, 8192, 3, NULL)"
            )
        }

        helper.runMigrationsAndValidate(databaseName, 4, true, KnouxDatabase.MIGRATION_3_4).use { db ->
            db.query("SELECT id, itemCount FROM scan_sessions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("legacy-scan", cursor.getString(0))
                assertEquals(3, cursor.getInt(1))
            }
            db.query("SELECT id, bytes FROM residue_scans").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("legacy-residue", cursor.getString(0))
                assertEquals(8192L, cursor.getLong(1))
            }
            db.query("SELECT action, bytes FROM residue_decisions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("VERIFIED", cursor.getString(0))
                assertEquals(8192L, cursor.getLong(1))
            }
            listOf("privacy_scans", "privacy_findings", "privacy_actions").forEach { table ->
                db.query("SELECT COUNT(*) FROM $table").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("$table must exist and start empty", 0, cursor.getInt(0))
                }
            }
            db.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(4, cursor.getInt(0))
            }
            db.query("PRAGMA integrity_check").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("ok", cursor.getString(0))
            }
        }
    }
}
