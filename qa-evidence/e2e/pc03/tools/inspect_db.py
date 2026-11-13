"""Read-only inspection of the copied production DB. Writes nothing to the device."""
import os
import shutil
import sqlite3

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "db", "knoux-prod-after.db")
WORK = os.path.join(ROOT, "db", "knoux-prod-readonly-copy.db")

for ext in ("", "-wal", "-shm"):
    if os.path.exists(SRC + ext):
        shutil.copyfile(SRC + ext, WORK + ext)

conn = sqlite3.connect(WORK)
print("user_version", conn.execute("PRAGMA user_version").fetchone()[0])
tables = [r[0] for r in conn.execute(
    "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'residue%' ORDER BY name")]
print("residue_tables", tables)
print("residue_scans", conn.execute("SELECT COUNT(*) FROM residue_scans").fetchone()[0])
print("residue_items", conn.execute("SELECT COUNT(*) FROM residue_items").fetchone()[0])
print("residue_decisions", conn.execute("SELECT COUNT(*) FROM residue_decisions").fetchone()[0])
print("residue_profiles", conn.execute("SELECT COUNT(*) FROM residue_profiles").fetchone()[0])
print("integrity", conn.execute("PRAGMA integrity_check").fetchone()[0])
for row in conn.execute(
        "SELECT status, minAgeDays, fileCount, itemCount, bytes, note FROM residue_scans "
        "ORDER BY startedAt DESC LIMIT 3"):
    print("scan", row)
for row in conn.execute(
        "SELECT action, itemCount, bytes, note FROM residue_decisions ORDER BY createdAt DESC LIMIT 4"):
    print("decision", row)
print("latest_scan_items", [r[0] for r in conn.execute(
    "SELECT displayName FROM residue_items WHERE scanId = "
    "(SELECT id FROM residue_scans ORDER BY startedAt DESC LIMIT 1) ORDER BY bytes DESC LIMIT 20")])
conn.close()
