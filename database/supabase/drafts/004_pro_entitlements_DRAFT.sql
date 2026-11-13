-- KNOUX AI PC: PRO ENTITLEMENTS DRAFT — 2026-11-12
-- NOT DEPLOYED. NOT FOR AUTOMATIC EXECUTION. Staging preflight + owner approval required.
-- Additive to database/supabase/migrations/001_core.sql..003_account_erasure.sql.
-- Never use this draft to bypass Google Play Billing purchase verification.
begin;

create table public.ka_pro_plans (
  id text primary key,
  play_product_id text unique,
  tier text not null check (tier in ('pro','elite')),
  scope text not null check (scope in ('all','section')),
  section_id text,
  enabled boolean not null default false,
  created_at timestamptz not null default now(),
  check ((scope = 'all' and section_id is null) or (scope = 'section' and section_id is not null))
);
comment on table public.ka_pro_plans is 'Server-maintained product identity only. Prices must be rendered from Google Play ProductDetails, NOT trusted from this table or hardcoded.';

create table public.ka_pro_entitlements (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  feature_id text, -- null = whole plan/scope; explicit feature = single entitlement
  section_id text, -- null = all sections
  tier text not null check (tier in ('pro','elite')),
  source text not null check (source in ('google_play','verified_trial','manual_admin')),
  state text not null check (state in ('pending','active','grace','hold','expired','revoked')),
  plan_id text references public.ka_pro_plans(id) on delete set null,
  starts_at timestamptz not null default now(),
  expires_at timestamptz,
  checked_at timestamptz not null default now(),
  revoked_at timestamptz,
  origin_event_id uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (expires_at is null or expires_at > starts_at),
  check (revoked_at is null or state = 'revoked')
);
create index ka_pro_entitlements_owner_active_idx
  on public.ka_pro_entitlements(owner_id, state, expires_at);
create index ka_pro_entitlements_owner_section_idx
  on public.ka_pro_entitlements(owner_id, section_id, feature_id);
comment on table public.ka_pro_entitlements is 'AUTHORITATIVE SERVER WRITES ONLY; clients can SELECT own records. No client-granted premium access.';

create table public.ka_pro_purchase_events (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  provider text not null check (provider in ('google_play')),
  provider_event_id text not null,
  token_digest text not null, -- server HMAC/fingerprint; never a raw purchase token
  play_product_id text not null,
  event_type text not null,
  verified_state text not null check (verified_state in ('unverified','pending','verified','rejected')),
  verified_at timestamptz,
  received_at timestamptz not null default now(),
  unique(provider, provider_event_id)
);
create index ka_pro_purchase_owner_idx on public.ka_pro_purchase_events(owner_id, received_at desc);
comment on table public.ka_pro_purchase_events is 'Private server audit. No direct user read/write. RTDN alone is NOT purchase verification.';

create table public.ka_pro_trial_codes (
  code_digest text primary key, -- salted HMAC computed on server; never plaintext codes
  section_id text,
  tier text not null check (tier in ('pro','elite')),
  max_redemptions integer not null default 1 check (max_redemptions >= 1),
  redeemed_count integer not null default 0 check (redeemed_count >= 0 and redeemed_count <= max_redemptions),
  expires_at timestamptz not null,
  disabled_at timestamptz,
  created_at timestamptz not null default now()
);
comment on table public.ka_pro_trial_codes is 'Admin/server only: redeem atomically in verified backend. Never issue codes inside Android client.';

create table public.ka_pro_trial_requests (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  section_id text not null,
  requested_at timestamptz not null default now(),
  status text not null default 'pending' check (status in ('pending','approved','denied','cancelled')),
  reviewed_at timestamptz,
  unique(owner_id, section_id)
);
comment on table public.ka_pro_trial_requests is 'Non-entitling request. Approval must be server-side; this row alone grants nothing.';

create table public.ka_pro_access_audit (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  feature_id text not null,
  decision text not null check (decision in ('allowed','denied','unsupported','unknown')),
  reason_code text not null,
  entitlement_id uuid references public.ka_pro_entitlements(id) on delete set null,
  observed_at timestamptz not null default now()
);
create index ka_pro_access_audit_owner_date_idx on public.ka_pro_access_audit(owner_id, observed_at desc);
comment on table public.ka_pro_access_audit is 'Server-side entitlement decision evidence; no raw device file names, package inventory or personal media.';

alter table public.ka_pro_plans enable row level security;
alter table public.ka_pro_entitlements enable row level security;
alter table public.ka_pro_purchase_events enable row level security;
alter table public.ka_pro_trial_codes enable row level security;
alter table public.ka_pro_trial_requests enable row level security;
alter table public.ka_pro_access_audit enable row level security;

-- Explicitly deny writes to user-visible entitlement, billing, and audit tables.
revoke all on public.ka_pro_plans,
  public.ka_pro_entitlements,
  public.ka_pro_purchase_events,
  public.ka_pro_trial_codes,
  public.ka_pro_trial_requests,
  public.ka_pro_access_audit from anon, authenticated;

grant select on public.ka_pro_plans to authenticated;
grant select on public.ka_pro_entitlements to authenticated;
grant select on public.ka_pro_trial_requests to authenticated;
grant insert on public.ka_pro_trial_requests to authenticated;

create policy ka_pro_plans_read on public.ka_pro_plans
  for select to authenticated using (enabled = true);
create policy ka_pro_entitlements_owner_read on public.ka_pro_entitlements
  for select to authenticated using (owner_id = (select auth.uid()));
create policy ka_pro_trial_requests_owner_read on public.ka_pro_trial_requests
  for select to authenticated using (owner_id = (select auth.uid()));
create policy ka_pro_trial_requests_owner_request on public.ka_pro_trial_requests
  for insert to authenticated with check (
    owner_id = (select auth.uid()) and status = 'pending' and reviewed_at is null
  );

-- No client policies on purchase_events, trial_codes, access_audit: backend only.
-- Authenticated user INSERT into trial_requests cannot activate an entitlement.

commit;

-- MANDATORY BEFORE DEPLOYMENT:
-- 1. Confirm actual installed migration set / data / ownership / auth schema on isolated staging.
-- 2. Implement secure purchase verification via Google Play Developer API and Play Billing Library.
-- 3. Protect RTDN endpoint (Google Cloud Pub/Sub auth) and re-query subscriptionV2 state.
-- 4. Make granting/revocation idempotent; process pending/hold/grace/expiry/refunds.
-- 5. Atomic trial redemption under a server transaction; rate limit; never embed a verifier in client.
-- 6. Seed plan/product mapping only after actual Play Console setup; show locale prices from ProductDetails.
-- 7. Execute tests as two real JWT users: cross-user SELECT blocked, client INSERT entitlements denied,
--    client UPDATE/DELETE denied; trial requests cannot mutate status or grant access.
-- 8. Review old account-erasure function to ensure these owner_id tables cascade.
-- 9. Verify use of service-role only in backend secrets; no frontend service-role key.
-- 10. DO NOT execute on production automatically; this file is a candidate reference, not a migration applied.
