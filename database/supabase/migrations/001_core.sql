-- KNOUX AI | optional opt-in cloud summaries, NEVER raw user media, paths, app lists or file hashes.
-- Apply ONLY to an owned Supabase project after review. No Supabase project is created by this kit.
-- All tenant-owned rows carry owner_id and composite ownership foreign keys.
create schema if not exists extensions;
create extension if not exists pgcrypto with schema extensions;

create table if not exists public.ka_profiles (
  owner_id uuid primary key references auth.users(id) on delete cascade,
  locale text not null default 'en' check (locale in ('en','ar')),
  cloud_sync_enabled boolean not null default false,
  consent_version text,
  consent_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint consent_required_for_sync check (not cloud_sync_enabled or (consent_version is not null and consent_at is not null))
);
create table if not exists public.ka_devices (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.ka_profiles(owner_id) on delete cascade,
  nickname text check (char_length(nickname) <= 80),
  platform text not null default 'android' check (platform='android'),
  app_version text,
  sdk_int integer check (sdk_int between 26 and 100),
  created_at timestamptz not null default now(),
  last_seen_at timestamptz,
  unique (id, owner_id)
);
comment on table public.ka_devices is 'Rotatable UUID; never collect IMEI, Android ID, advertising ID or phone number.';

create table if not exists public.ka_scan_sessions (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.ka_profiles(owner_id) on delete cascade,
  device_id uuid not null,
  status text not null default 'queued' check (status in ('queued','running','paused','completed','failed','cancelled')),
  started_at timestamptz,
  finished_at timestamptz,
  modules_total smallint not null default 0 check (modules_total between 0 and 20),
  modules_completed smallint not null default 0 check (modules_completed between 0 and modules_total),
  findings_count integer not null default 0 check (findings_count >= 0),
  measured_bytes_reclaimable bigint check (measured_bytes_reclaimable >= 0),
  scan_engine_version text not null,
  created_at timestamptz not null default now(),
  unique(id, owner_id),
  foreign key(device_id,owner_id) references public.ka_devices(id,owner_id) on delete cascade,
  check (finished_at is null or started_at is null or finished_at >= started_at)
);
create table if not exists public.ka_scan_modules (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  scan_id uuid not null,
  module_key text not null check(module_key in ('storage','duplicates','privacy','apps','battery')),
  status text not null default 'queued' check(status in ('queued','running','completed','failed','skipped')),
  percent smallint check (percent between 0 and 100),
  duration_ms bigint check (duration_ms >= 0),
  unique(scan_id,module_key),
  unique(id,owner_id),
  foreign key(scan_id,owner_id) references public.ka_scan_sessions(id,owner_id) on delete cascade
);
create table if not exists public.ka_finding_summaries (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  scan_id uuid not null,
  category text not null check(category in ('duplicate','storage','privacy','app','battery','other')),
  severity text not null check (severity in ('info','low','medium','high')),
  count integer not null check(count >= 0),
  bytes_total bigint check(bytes_total >= 0),
  risk_code text check(risk_code ~ '^[A-Z0-9_]{1,64}$'),
  created_at timestamptz not null default now(),
  foreign key(scan_id,owner_id) references public.ka_scan_sessions(id,owner_id) on delete cascade
);
comment on table public.ka_finding_summaries is 'Only aggregate count/byte metrics. NEVER paths, file titles, content, hashes, contact info, installed package names.';

create table if not exists public.ka_cleanup_actions (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  device_id uuid not null,
  scan_id uuid,
  action_type text not null check(action_type in ('media_trash','saf_delete','own_cache_cleanup','manual_guidance')),
  status text not null check(status in ('requested','user_approved','completed','failed','cancelled')),
  selected_item_count integer not null default 0 check(selected_item_count >= 0),
  measured_reclaimed_bytes bigint check(measured_reclaimed_bytes >= 0),
  reversible boolean not null default false,
  user_confirmed_at timestamptz,
  occurred_at timestamptz not null default now(),
  foreign key(device_id,owner_id) references public.ka_devices(id,owner_id) on delete cascade,
  foreign key(scan_id,owner_id) references public.ka_scan_sessions(id,owner_id) on delete set null (scan_id),
  check(status not in ('user_approved','completed') or user_confirmed_at is not null)
);
comment on table public.ka_cleanup_actions is 'Append-only consent/action evidence. Never store underlying file URIs or names.';

create table if not exists public.ka_privacy_assessments (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  scan_id uuid not null,
  exposure_category text not null check(exposure_category in ('permissions','overlays','notification_access','usage_access','sdk_inventory','other')),
  observed_count integer not null check(observed_count >= 0),
  confidence text not null check(confidence in ('measured','estimated','unknown')),
  collected_at timestamptz not null default now(),
  foreign key(scan_id,owner_id) references public.ka_scan_sessions(id,owner_id) on delete cascade
);
create table if not exists public.ka_app_aggregates (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  scan_id uuid not null,
  category_code text not null check (category_code in ('games','media','social','utilities','messaging','other','unknown')),
  visible_app_count integer not null check(visible_app_count >= 0),
  total_estimated_bytes bigint check(total_estimated_bytes >= 0),
  source_scope text not null default 'filtered' check(source_scope in ('filtered','user_selected','authorized')),
  foreign key(scan_id,owner_id) references public.ka_scan_sessions(id,owner_id) on delete cascade
);
create table if not exists public.ka_battery_measurements (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  device_id uuid not null,
  battery_level_percent smallint check(battery_level_percent between 0 and 100),
  charging boolean,
  thermal_status smallint check(thermal_status between 0 and 6),
  measured_at timestamptz not null default now(),
  source text not null default 'android_battery_manager' check(source in ('android_battery_manager','android_power_manager')),
  foreign key(device_id,owner_id) references public.ka_devices(id,owner_id) on delete cascade
);
create table if not exists public.ka_recommendations (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  device_id uuid not null,
  rule_code text not null check(rule_code ~ '^[A-Z0-9_]{1,64}$'),
  explanation_code text not null check(explanation_code ~ '^[A-Z0-9_]{1,64}$'),
  status text not null default 'suggested' check(status in ('suggested','dismissed','user_applied','unavailable')),
  confidence text not null check(confidence in ('measured','estimated','unknown')),
  created_at timestamptz not null default now(),
  foreign key(device_id,owner_id) references public.ka_devices(id,owner_id) on delete cascade
);
create table if not exists public.ka_reports (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  device_id uuid not null,
  range_start timestamptz not null,
  range_end timestamptz not null,
  actual_bytes_reclaimed bigint not null default 0 check(actual_bytes_reclaimed >= 0),
  actual_cleanup_count integer not null default 0 check(actual_cleanup_count >= 0),
  has_verified_measurements boolean not null default false,
  generated_at timestamptz not null default now(),
  check(range_end >= range_start),
  foreign key(device_id,owner_id) references public.ka_devices(id,owner_id) on delete cascade
);
create table if not exists public.ka_sync_events (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.ka_profiles(owner_id) on delete cascade,
  device_id uuid not null,
  event_type text not null check(event_type in ('summary_uploaded','summary_deleted','sync_error','consent_changed')),
  event_at timestamptz not null default now(),
  foreign key(device_id,owner_id) references public.ka_devices(id,owner_id) on delete cascade
);

create index if not exists ka_devices_owner_idx on public.ka_devices(owner_id);
create index if not exists ka_scan_sessions_owner_date_idx on public.ka_scan_sessions(owner_id,created_at desc);
create index if not exists ka_scan_modules_owner_scan_idx on public.ka_scan_modules(owner_id,scan_id);
create index if not exists ka_findings_owner_scan_idx on public.ka_finding_summaries(owner_id,scan_id);
create index if not exists ka_actions_owner_date_idx on public.ka_cleanup_actions(owner_id,occurred_at desc);
create index if not exists ka_privacy_owner_scan_idx on public.ka_privacy_assessments(owner_id,scan_id);
create index if not exists ka_apps_owner_scan_idx on public.ka_app_aggregates(owner_id,scan_id);
create index if not exists ka_battery_owner_date_idx on public.ka_battery_measurements(owner_id,measured_at desc);
create index if not exists ka_recommendations_owner_idx on public.ka_recommendations(owner_id,created_at desc);
create index if not exists ka_reports_owner_date_idx on public.ka_reports(owner_id,generated_at desc);
create index if not exists ka_sync_owner_idx on public.ka_sync_events(owner_id,event_at desc);

create or replace function public.ka_set_updated_at() returns trigger
language plpgsql set search_path = '' as $$
begin new.updated_at := now(); return new; end $$;
drop trigger if exists ka_profiles_updated on public.ka_profiles;
create trigger ka_profiles_updated before update on public.ka_profiles
for each row execute function public.ka_set_updated_at();
