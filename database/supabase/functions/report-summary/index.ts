// OPTIONAL illustrative Supabase Edge Function. Deploy only after testing current @supabase/server API.
// Supabase config should keep verify_jwt=true for user endpoints.
import { withSupabase } from 'npm:@supabase/server@1'

export default {
  fetch: withSupabase({ auth: 'user' }, async (_request, ctx) => {
    // ctx.supabase is user-scoped; RLS enforces per-user isolation.
    // Only verified aggregate figures are returned. No files/hashes/package names.
    const { data, error } = await ctx.supabase.from('ka_reports')
      .select('id,generated_at,actual_bytes_reclaimed,actual_cleanup_count,has_verified_measurements')
      .eq('has_verified_measurements', true)
      .order('generated_at', { ascending: false })
      .limit(20)
    if (error) return Response.json({ error: 'Report unavailable' }, { status: 500 })
    return Response.json({ reports: data ?? [] }, { headers: { 'Cache-Control': 'no-store' } })
  })
}
