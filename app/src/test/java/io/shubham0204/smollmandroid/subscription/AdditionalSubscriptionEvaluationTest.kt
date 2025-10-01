package io.shubham0204.smollmandroid.subscription

import org.junit.Assert.*
import org.junit.Test

class AdditionalSubscriptionEvaluationTest {

    private fun record(
        start: Long,
        expiry: Long,
        lastVerification: Long,
        state: EntitlementState,
        survivalAt: Long? = null,
        clockSuspicious: Boolean = false,
        elapsedAt: Long = 1000L,
        autoRenew: Boolean = true,
    ) = SubscriptionRecord(
        purchaseStartUtc = start,
        lastKnownExpiryUtc = expiry,
        lastVerificationUtc = lastVerification,
        purchaseToken = "tok",
        entitlementState = state,
        survivalModeActivatedAtUtc = survivalAt,
        clockSuspicious = clockSuspicious,
        systemElapsedRealtimeAtVerification = elapsedAt,
        autoRenewing = autoRenew,
    )

    @Test
    fun survival_activation_timestamp_stable_across_re_evaluation() {
        val now1 = 5_000_000L
        val expiredRec = record(start = 0L, expiry = now1 - 1_000L, lastVerification = now1 - 10_000L, state = EntitlementState.ACTIVE)
        val eval1 = SubscriptionManager.evaluatePure(expiredRec, now1, elapsed = 50_000L, hasModels = true)
        assertEquals(EntitlementState.SURVIVAL_MODE, eval1.state)
        val ts = eval1.updated!!.survivalModeActivatedAtUtc
        assertNotNull(ts)
        // Advance time, re-evaluate with updated record – timestamp must not change
        val now2 = now1 + 100_000L
        val eval2 = SubscriptionManager.evaluatePure(eval1.updated, now2, elapsed = 60_000L, hasModels = true)
        assertEquals(EntitlementState.SURVIVAL_MODE, eval2.state)
        assertEquals(ts, eval2.updated!!.survivalModeActivatedAtUtc)
    }

    @Test
    fun backward_clock_drift_sets_flag() {
        val now = 10_000_000L
        // lastVerification in the future relative to now (simulate user moved clock backward > threshold)
        val futureVerification = now + SubscriptionPolicy.CLOCK_SUSPICIOUS_DRIFT_BACK_MS + 10_000L
        val rec = record(start = 0L, expiry = now + 500_000L, lastVerification = futureVerification, state = EntitlementState.ACTIVE)
        val eval = SubscriptionManager.evaluatePure(rec, now, elapsed = 20_000L, hasModels = true)
        assertTrue("Backward clock drift should set suspicious flag", eval.updated!!.clockSuspicious)
        assertEquals(EntitlementState.ACTIVE, eval.state)
    }

    @Test
    fun survival_record_becomes_active_when_future_expiry_present() {
        val now = 20_000_000L
        val futureExpiry = now + 200_000L
        val rec = record(
            start = now - 100_000L,
            expiry = futureExpiry,
            lastVerification = now - 50_000L,
            state = EntitlementState.SURVIVAL_MODE,
            survivalAt = now - 60_000L,
        )
        val eval = SubscriptionManager.evaluatePure(rec, now, elapsed = 90_000L, hasModels = true)
        assertEquals(EntitlementState.ACTIVE, eval.state)
        // survival timestamp should remain (we don't clear, acceptable for current design)
        assertEquals(rec.survivalModeActivatedAtUtc, eval.updated!!.survivalModeActivatedAtUtc)
    }
}
