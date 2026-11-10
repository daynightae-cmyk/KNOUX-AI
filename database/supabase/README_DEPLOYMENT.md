# Optional Supabase setup, NOT Phase 1

No Supabase project or credentials are bundled or connected. To use these files after UI approval, the owner creates/chooses an authorized project and validates in **local/staging first**.

1. Install Supabase CLI and Docker as required, start local Supabase, enable email sign-in/test users. `supabase start` must succeed.
2. Check project is isolated or migrate existing schema carefully. In order apply `001_core.sql`, `002_rls.sql`, `003_account_erasure.sql`. These are reference migrations, not proof of deployability.
3. `001_core.sql` assumes Supabase extension namespace `extensions` exists; modify only after confirming platform schema. `gen_random_uuid()` is available on contemporary PostgreSQL.
4. Run `tests/001_rls_structure.sql` with a privileged staging SQL user; then execute the **two-real-user RLS test** described there through authenticated *client JWTs* and publishable key. Test different devices, cross-owner `device_id`/`scan_id`, `auth.uid`, no-consent insert, session expiry. RLS policies must deny foreign reads/inserts/updates/deletes.
5. Verify `ka_profiles` owner self-insertion, cloud_sync_enabled false by default, explicit consent UI before flipping true, retry behavior, withdrawal, user erasure and retention.
6. For Kotlin install supported `supabase-kt` BOM + `auth-kt` + `postgrest-kt` + Ktor engine and serialization; keep only URL and publishable key in app BuildConfig/environment. No service role key in app, screenshots, CI logs or repository.
7. Verify migration syntax and RLS tests on the actual Postgres version. This handoff did **not** connect to any database; no SQL execution in Supabase has occurred.
8. If an Edge Function is wanted for authenticated aggregate reports, use the optional `functions/report-summary/` with platform JWT verification and `ctx.supabase` RLS client. Never route raw files through the cloud.

Details: https://supabase.com/docs/guides/database/postgres/row-level-security and https://supabase.com/docs/guides/functions/auth
