"""Static PC02 schema check — no device required.

1. Extract the real MIGRATION_1_2 DDL from KnouxDatabase.kt (the string literals
   passed to execSQL), so this cannot drift from what the app actually runs.
2. Execute them in a real SQLite database that already contains the v1 tables.
3. Compare each created table with Room's exported 2.json expectation.
4. Prove the append-only key: the same content hash in two scans must coexist, and
   the (scanId, uri) unique index must allow the same URI in a different scan.

Exit code 0 = parity proven. Any mismatch exits 1.
"""

import json
import os
import re
import sqlite3
import sys

ROOT = r"D:\KNOUX-AI"
KT = os.path.join(ROOT, "app", "src", "main", "java", "com", "knoux", "ai", "data", "KnouxDatabase.kt")
SCHEMA = os.path.join(ROOT, "app", "schemas", "com.knoux.ai.data.KnouxDatabase", "2.json")


def extract_migration_sql(source: str) -> list[str]:
    start = source.index("MIGRATION_1_2")
    end = source.index("val MIGRATION_1_2", start) if source.count("val MIGRATION_1_2") > 1 else -1
    body = source[start:]
    statements = []
    for match in re.finditer(r"db\.execSQL\(", body):
        i = match.end()
        depth = 1
        literals = []
        while i < len(body) and depth > 0:
            ch = body[i]
            if ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
            elif ch == '"':
                j = i + 1
                buf = []
                while j < len(body):
                    if body[j] == "\\":
                        buf.append(body[j:j + 2])
                        j += 2
                        continue
                    if body[j] == '"':
                        break
                    buf.append(body[j])
                    j += 1
                literals.append("".join(buf))
                i = j
            i += 1
        text = "".join(literals)
        # Kotlin escaping of newlines is not used in this migration; nothing else to undo.
        statements.append(text.strip())
    return [s for s in statements if s]


def normalise(sql: str) -> str:
    out = sql.replace("IF NOT EXISTS ", "")
    out = out.replace("`", '"')
    out = re.sub(r"\s+", " ", out).strip()
    out = out.replace('"${TABLE_NAME}"', '"T"')
    out = out.replace("`${TABLE_NAME}`", '"T"')
    return out


def main() -> int:
    source = open(KT, encoding="utf-8").read()
    statements = extract_migration_sql(source)
    print(f"extracted {len(statements)} DDL statement(s) from MIGRATION_1_2")
    for s in statements:
        print("  DDL:", s[:110] + ("..." if len(s) > 110 else ""))
    if len(statements) != 5:
        print(f"FAIL: expected 5 statements (3 tables + 2 member indexes), found {len(statements)}")
        return 1

    db = sqlite3.connect(":memory:")
    # A v1 database already contains only the PC00/PC01 tables.
    db.execute("CREATE TABLE `scan_sessions` (`id` TEXT NOT NULL PRIMARY KEY)")
    for statement in statements:
        db.execute(statement)
    print("OK: all migration statements executed against SQLite")

    exported = json.load(open(SCHEMA, encoding="utf-8"))
    entities = {e["tableName"]: e for e in exported["database"]["entities"]
                if e["tableName"].startswith("duplicate_")}

    failures = []
    for name, entity in entities.items():
        expected = normalise(entity["createSql"].replace("${TABLE_NAME}", name))
        row = db.execute(
            "SELECT sql FROM sqlite_master WHERE type='table' AND name=?", (name,)
        ).fetchone()
        if row is None:
            failures.append(f"{name}: migration did not create the table")
            continue
        actual = normalise(row[0])
        if actual != expected:
            failures.append(f"{name} MISMATCH\n  expected: {expected}\n  actual  : {actual}")
        else:
            print(f"OK: {name} matches Room schema 2 exactly")

    for entity in entities.values():
        for index in entity.get("indices", []):
            row = db.execute(
                "SELECT sql FROM sqlite_master WHERE type='index' AND name=?", (index["name"],)
            ).fetchone()
            if row is None:
                failures.append(f"{index['name']}: migration did not create the index")
                continue
            expected = normalise(index["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
            actual = normalise(row[0])
            if actual != expected:
                failures.append(f"{index['name']} MISMATCH\n  expected: {expected}\n  actual  : {actual}")
            else:
                print(f"OK: index {index['name']} matches Room schema 2 exactly")

    # Append-only evidence: identical content hash in two scans must coexist.
    db.execute(
        "INSERT INTO duplicate_groups (scanId,id,contentHash,bytes,keeperUri,keeperName,"
        "keeperModified,memberCount,redundantBytes) VALUES ('scanA','h1','h1',10,'u1','a',1,2,10)"
    )
    db.execute(
        "INSERT INTO duplicate_groups (scanId,id,contentHash,bytes,keeperUri,keeperName,"
        "keeperModified,memberCount,redundantBytes) VALUES ('scanB','h1','h1',10,'u1','a',1,2,10)"
    )
    rows = db.execute("SELECT COUNT(*) FROM duplicate_groups WHERE id='h1'").fetchone()[0]
    if rows != 2:
        failures.append(f"append-only key broken: same hash across scans collapsed to {rows} row(s)")
    else:
        print("OK: same content hash in two scans is preserved as two rows")

    # The unique (scanId, uri) index must still reject a duplicate inside one scan.
    db.execute(
        "INSERT INTO duplicate_members (scanId,groupId,uri,displayName,bytes,lastModified,"
        "category,provenance,eligibility,reason,isKeeper) "
        "VALUES ('scanA','h1','u1','a',10,1,'IMAGE','MEDIASTORE','ELIGIBLE','r',1)"
    )
    db.execute(
        "INSERT INTO duplicate_members (scanId,groupId,uri,displayName,bytes,lastModified,"
        "category,provenance,eligibility,reason,isKeeper) "
        "VALUES ('scanB','h1','u1','a',10,1,'IMAGE','MEDIASTORE','ELIGIBLE','r',1)"
    )
    print("OK: the same URI is allowed once per scan")
    try:
        db.execute(
            "INSERT INTO duplicate_members (scanId,groupId,uri,displayName,bytes,lastModified,"
            "category,provenance,eligibility,reason,isKeeper) "
            "VALUES ('scanA','h1','u1','a',10,1,'IMAGE','MEDIASTORE','ELIGIBLE','r',0)"
        )
        failures.append("unique (scanId, uri) index did not reject a repeated URI in one scan")
    except sqlite3.IntegrityError:
        print("OK: a repeated URI inside one scan is rejected by the unique index")

    if failures:
        print("\nFAIL")
        for failure in failures:
            print(" -", failure)
        return 1
    print("\nPASS: migration 1->2 and Room schema 2 are identical; keys are append-only")
    return 0


if __name__ == "__main__":
    sys.exit(main())
