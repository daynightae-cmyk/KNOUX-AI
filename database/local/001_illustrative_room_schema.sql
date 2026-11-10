-- SQLite/Room reference: local-only sensitive data, not uploaded to Supabase.
-- Room entity definitions in android/integration/kotlin/LocalAuditStore.kt are source of truth.
PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS local_scans (
  id TEXT NOT NULL PRIMARY KEY,
  created_at_ms INTEGER NOT NULL,
  status TEXT NOT NULL CHECK(status IN ('queued','running','paused','completed','failed','cancelled')),
  source TEXT NOT NULL CHECK(source IN ('real','preview'))
);
CREATE TABLE IF NOT EXISTS local_media (
  id TEXT NOT NULL PRIMARY KEY,
  scan_id TEXT NOT NULL REFERENCES local_scans(id) ON DELETE CASCADE,
  content_uri TEXT NOT NULL,
  media_kind TEXT NOT NULL,
  bytes INTEGER NOT NULL CHECK(bytes>=0),
  sha256 TEXT,
  included_in_cleanup INTEGER NOT NULL DEFAULT 0,
  CHECK(length(sha256)=64 OR sha256 IS NULL)
);
CREATE INDEX IF NOT EXISTS idx_local_media_scan ON local_media(scan_id);
CREATE TABLE IF NOT EXISTS local_duplicate_groups (
  group_id TEXT NOT NULL,
  media_id TEXT NOT NULL REFERENCES local_media(id) ON DELETE CASCADE,
  PRIMARY KEY(group_id,media_id)
);
CREATE TABLE IF NOT EXISTS local_actions (
  id TEXT NOT NULL PRIMARY KEY,
  scan_id TEXT REFERENCES local_scans(id),
  status TEXT NOT NULL,
  consent_at_ms INTEGER,
  actual_reclaimed_bytes INTEGER CHECK(actual_reclaimed_bytes>=0),
  reversible INTEGER NOT NULL DEFAULT 0
);
