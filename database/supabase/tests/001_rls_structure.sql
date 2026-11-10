-- Read-only structural assertions; run against a local/staging Supabase database.
-- This checks RLS on every ka_* table. It does NOT demonstrate two-user isolation.
do $$
declare missing text;
begin
  select string_agg(s.tablename, ', ') into missing
  from pg_tables s
  left join pg_class c on c.relname=s.tablename and c.relnamespace='public'::regnamespace
  where s.schemaname='public' and s.tablename like 'ka_%' and (c.relrowsecurity is not true);
  if missing is not null then raise exception 'RLS disabled on: %', missing; end if;
  if (select count(*) from pg_tables where schemaname='public' and tablename like 'ka_%') <> 12 then
    raise exception 'Expected 12 ka_* tables';
  end if;
end $$;
-- Integration gate must create two REAL Supabase Auth test users, enable sync,
-- attempt cross-user SELECT/INSERT/UPDATE/DELETE via publishable key + each JWT,
-- assert zero foreign rows and denied foreign inserts. Do not use service_role
-- for RLS tests since it can bypass policies.
