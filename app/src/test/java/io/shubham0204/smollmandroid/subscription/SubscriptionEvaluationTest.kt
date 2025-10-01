package io.shubham0204.smollmandroid.subscription

import org.junit.Assert.*
import org.junit.Test

class SubscriptionEvaluationTest {

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
    fun active_when_not_expired() {
        val now = 2_000_000L
        val rec = record(start = 0L, expiry = now + 86_400_000L, lastVerification = now - 1_000L, state = EntitlementState.ACTIVE)
        val eval = SubscriptionManager.evaluatePure(rec, now, elapsed = 10_000L, hasModels = true)
        assertEquals(EntitlementState.ACTIVE, eval.state)
        assertNull("No survival activation expected", eval.updated?.survivalModeActivatedAtUtc)
    }

    @Test
    fun survival_mode_when_expired() {
        val now = 2_000_000L
        val rec = record(start = 0L, expiry = now - 10_000L, lastVerification = now - 20_000L, state = EntitlementState.ACTIVE)
        val eval = SubscriptionManager.evaluatePure(rec, now, elapsed = 10_000L, hasModels = true)
        assertEquals(EntitlementState.SURVIVAL_MODE, eval.state)
        assertNotNull("Survival activation timestamp set", eval.updated?.survivalModeActivatedAtUtc)
    }

    @Test
    fun not_entitled_when_no_record_and_no_models() {
        val now = 1_000L
        val eval = SubscriptionManager.evaluatePure(null, now, elapsed = 0L, hasModels = false)
        assertEquals(EntitlementState.NOT_ENTITLED, eval.state)
    }

    @Test
    fun survival_mode_when_no_record_and_models_present() {
        val now = 1_000L
        val eval = SubscriptionManager.evaluatePure(null, now, elapsed = 0L, hasModels = true)
        assertEquals(EntitlementState.SURVIVAL_MODE, eval.state)
    }

    @Test
    fun clock_suspicious_forward_sets_flag() {
        val now = 2_000_000L
        // Simulate large forward wall clock jump beyond forward drift threshold (60 days)
        val sixtyOneDaysMs = 61L * 24 * 60 * 60 * 1000
        val rec = record(start = 0L, expiry = now + 100_000L, lastVerification = now - sixtyOneDaysMs, state = EntitlementState.ACTIVE, elapsedAt = 10_000L)
        val eval = SubscriptionManager.evaluatePure(rec, now, elapsed = 10_000L + 5_000L, hasModels = true)
        assertEquals(EntitlementState.ACTIVE, eval.state)
        assertTrue("Clock suspicious flag expected", eval.updated?.clockSuspicious == true)
    }
}
