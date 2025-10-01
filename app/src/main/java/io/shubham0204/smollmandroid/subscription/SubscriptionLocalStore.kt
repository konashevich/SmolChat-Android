package io.shubham0204.smollmandroid.subscription

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import org.koin.core.annotation.Single

/** Handles persistence of subscription record in simple resilient JSON (plan section 5). */
@Single
class SubscriptionLocalStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("subscription_state", Context.MODE_PRIVATE)

    fun read(): SubscriptionRecord? = runCatching {
        val raw = prefs.getString(KEY_JSON, null) ?: return null
        parse(raw)
    }.getOrNull()

    fun write(record: SubscriptionRecord) = runCatching {
        prefs.edit().putString(KEY_JSON, serialize(record)).apply()
    }.getOrNull()

    private fun parse(json: String): SubscriptionRecord {
        val o = JSONObject(json)
        return SubscriptionRecord(
            purchaseStartUtc = o.optLong("purchaseStartUtc", 0L),
            lastKnownExpiryUtc = o.optLong("lastKnownExpiryUtc", 0L),
            lastVerificationUtc = o.optLong("lastVerificationUtc", 0L),
            purchaseToken = o.optString("purchaseToken", ""),
            entitlementState = o.optString("entitlementState", EntitlementState.NOT_ENTITLED.name).let { runCatching { EntitlementState.valueOf(it) }.getOrDefault(EntitlementState.NOT_ENTITLED) },
            survivalModeActivatedAtUtc = if (o.has("survivalModeActivatedAtUtc") && !o.isNull("survivalModeActivatedAtUtc")) o.getLong("survivalModeActivatedAtUtc") else null,
            clockSuspicious = o.optBoolean("clockSuspicious", false),
            systemElapsedRealtimeAtVerification = o.optLong("systemElapsedRealtimeAtVerification", 0L),
            autoRenewing = o.optBoolean("autoRenewing", true),
        )
    }

    private fun serialize(r: SubscriptionRecord): String = JSONObject().apply {
        put("purchaseStartUtc", r.purchaseStartUtc)
        put("lastKnownExpiryUtc", r.lastKnownExpiryUtc)
        put("lastVerificationUtc", r.lastVerificationUtc)
        put("purchaseToken", r.purchaseToken)
        put("entitlementState", r.entitlementState.name)
        if (r.survivalModeActivatedAtUtc != null) put("survivalModeActivatedAtUtc", r.survivalModeActivatedAtUtc) else put("survivalModeActivatedAtUtc", JSONObject.NULL)
        put("clockSuspicious", r.clockSuspicious)
        put("systemElapsedRealtimeAtVerification", r.systemElapsedRealtimeAtVerification)
        put("autoRenewing", r.autoRenewing)
    }.toString()

    companion object { private const val KEY_JSON = "state_json" }
}

// Persistent record of subscription & survival context (simplified – verificationDebt removed)
data class SubscriptionRecord(
    val purchaseStartUtc: Long,
    val lastKnownExpiryUtc: Long,
    val lastVerificationUtc: Long,
    val purchaseToken: String,
    val entitlementState: EntitlementState,
    val survivalModeActivatedAtUtc: Long?,
    val clockSuspicious: Boolean,
    val systemElapsedRealtimeAtVerification: Long,
    val autoRenewing: Boolean,
)