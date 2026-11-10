package ai.knoux.integration

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudReportDto(
    @SerialName("owner_id") val ownerId: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("range_start") val rangeStart: String,
    @SerialName("range_end") val rangeEnd: String,
    @SerialName("actual_bytes_reclaimed") val actualBytesReclaimed: Long,
    @SerialName("actual_cleanup_count") val actualCleanupCount: Int,
    @SerialName("has_verified_measurements") val hasVerifiedMeasurements: Boolean
)

/** OPTIONAL future phase only; publishable key is not a secret. Never ship service_role or private keys. */
class CloudSummaryClient(url: String, publishableKey: String) {
    private val client: SupabaseClient = createSupabaseClient(url, publishableKey) {
        install(Auth)
        install(Postgrest)
    }

    suspend fun uploadVerifiedSummary(userConsented: Boolean, dto: CloudReportDto) {
        require(userConsented) { "Explicit cloud-sync consent required" }
        require(dto.hasVerifiedMeasurements) { "Preview / unverified data cannot be uploaded" }
        // Caller must also enforce current authenticated session and same owner as JWT;
        // DB RLS is authoritative, but client checks improve UX and logging.
        client.from("ka_reports").insert(dto)
    }
}
