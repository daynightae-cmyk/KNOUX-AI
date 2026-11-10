-- NOTE: Apply AFTER 001_core.sql. RLS does not replace backend input validation.
-- Production use: handle user deletion/erasure with authenticated server side workflow.
-- Do not revoke permissions for unrelated public tables in a shared Supabase project.

do $$
declare t text;
begin
  foreach t in array ARRAY[
   'ka_profiles','ka_devices','ka_scan_sessions','ka_scan_modules','ka_finding_summaries',
   'ka_cleanup_actions','ka_privacy_assessments','ka_app_aggregates','ka_battery_measurements',
   'ka_recommendations','ka_reports','ka_sync_events'
  ] loop
    execute format('revoke all on public.%I from anon, authenticated',t);
    execute format('alter table public.%I enable row level security',t);
    execute format('alter table public.%I force row level security',t);
    execute format('grant select, insert on public.%I to authenticated',t);
    execute format('drop policy if exists %I on public.%I', 'ka_'+t+'_select', t);
    execute format('drop policy if exists %I on public.%I', 'ka_'+t+'_insert', t);
    execute format('create policy %I on public.%I for select to authenticated using ((select auth.uid()) = owner_id)', 'ka_'+t+'_select', t);
    execute format('create policy %I on public.%I for insert to authenticated with check ((select auth.uid()) = owner_id)', 'ka_'+t+'_insert', t);
  end loop;
end $$;
-- Editable core tables; append-only evidence logs intentionally excluded.
do $$
declare t text;
begin
  foreach t in array ARRAY['ka_profiles','ka_devices','ka_scan_sessions','ka_scan_modules',
        'ka_finding_summaries','ka_privacy_assessments','ka_app_aggregates',
        'ka_battery_measurements','ka_recommendations','ka_reports'] loop
    execute format('grant update, delete on public.%I to authenticated',t);
    execute format('drop policy if exists %I on public.%I', 'ka_'+t+'_update', t);
    execute format('drop policy if exists %I on public.%I', 'ka_'+t+'_delete', t);
    execute format('create policy %I on public.%I for update to authenticated using ((select auth.uid()) = owner_id) with check ((select auth.uid()) = owner_id)', 'ka_'+t+'_update', t);
    execute format('create policy %I on public.%I for delete to authenticated using ((select auth.uid()) = owner_id)', 'ka_'+t+'_delete', t);
  end loop;
end $$;
-- Extra guard: cloud sync must have been explicitly enabled by the signed-in user
-- before inserting telemetry summaries. Server-side proof of actual device consent
-- still requires the Android consent log and tests; SQL alone cannot prove informed consent.
create or replace function public.ka_sync_allowed(p_owner uuid) returns boolean
language sql stable security invoker set search_path = '' as $$
  select exists (select 1 from public.ka_profiles p
    where p.owner_id = p_owner and p.cloud_sync_enabled = true
      and p.consent_at is not null and p.consent_version is not null)
$$;
-- REPLACE permissive INSERT policies on telemetry objects (RLS policies are OR'ed).
do $$
declare t text;
begin
  foreach t in array ARRAY[
     'ka_devices','ka_scan_sessions','ka_scan_modules','ka_finding_summaries',
     'ka_cleanup_actions','ka_privacy_assessments','ka_app_aggregates',
     'ka_battery_measurements','ka_recommendations','ka_reports','ka_sync_events'] loop
    execute format('drop policy if exists %I on public.%I','ka_'+t+'_insert',t);
    execute format('create policy %I on public.%I for insert to authenticated with check ((select auth.uid()) = owner_id and public.ka_sync_allowed(owner_id))','ka_'+t+'_insert',t);
  end loop;
end $$;
-- NOTE: Some users may later withdraw consent; reads and deletion still allowed.
-- Enforce consent in UPDATE policy for telemetry modifications as well (the app must stop sync).
revoke execute on function public.ka_sync_allowed(uuid) from public;
grant execute on function public.ka_sync_allowed(uuid) to authenticated;
