-- Called by verified administrative account-deletion workflow, never from Android client.
-- On auth.users deletion all cascaded rows disappear. This SQL purposefully does
-- NOT expose a public RPC that could be invoked to delete another user's account.
-- Make retention rules explicit in the server before production launch; dashboards,
-- backups, backups retention and logs must follow the privacy policy.
comment on table public.ka_profiles is 'Account profile and consent. Owner may opt out; deletion must run via verified erasure workflow.';
