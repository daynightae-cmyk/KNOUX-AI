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
 * Room 2 → 3 on-device proof.
 *
 * PC02 history is inserted into a real version-2 database first, then the PC03
 * migration runs and Room validates the resulting schema against the exported
 * schema. A destructive or mistyped migration fails here instead of on a phone
 * that already holds the customer's history.
 */
@RunWith(AndroidJUnit4::class)
class ResidueMigrationTest {

    private val databaseName = "knoux-migration-${UUID.randomUUID()}"

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

    @Test fun versionTwoHistorySurvivesAndResidueTablesAppear() {
        helper.createDatabase(databaseName, 2).use { db ->
            db.execSQL(
                "INSERT INTO scan_sessions (id, startedAt, finishedAt, status, scopes, itemCount, totalBytes) " +
                    "VALUES ('legacy-scan', 100, 200, 'COMPLETED', 'APP_CACHE', 3, 4096)"
            )
            db.execSQL(
                "INSERT INTO duplicate_scans (id, startedAt, finishedAt, status, scopes, fileCount, hashedCount, " +
                    "bytesHashed, groupCount, redundantBytes, statusNote) " +
                    "VALUES ('legacy-dupes', 1, 2, 'MEASURED', 'APP_CACHE', 10, 4, 4096, 1, 2048, NULL)"
            )
        }

        helper.runMigrationsAndValidate(databaseName, 3, true, KnouxDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT id, status, itemCount FROM scan_sessions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("legacy-scan", cursor.getString(0))
                assertEquals("COMPLETED", cursor.getString(1))
                assertEquals(3, cursor.getInt(2))
            }
            db.query("SELECT id, groupCount, redundantBytes FROM duplicate_scans").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("legacy-dupes", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
                assertEquals(2048, cursor.getInt(2))
            }
            db.query("SELECT COUNT(*) FROM residue_scans").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM residue_items").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM residue_decisions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM residue_profiles").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(3, cursor.getInt(0))
            }
        }
    }
}
