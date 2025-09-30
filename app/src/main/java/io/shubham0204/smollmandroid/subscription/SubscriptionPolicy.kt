package io.shubham0204.smollmandroid.subscription

/** Policy constants governing subscription handling & survival ethics.
 * Periodic background verification removed – verification now only on explicit user action (purchase/restore) or app logic calling syncFromBilling() explicitly.
 */
object SubscriptionPolicy {
    // Time constants in ms
    // const val VERIFY_INTERVAL_MIN_MS removed (manual only now)
    const val VERIFICATION_DEBT_AFTER_MS: Long = 14L * 24 * 60 * 60 * 1000 // 14 days (still used for UI hinting if desired)
    const val SURVIVAL_MODE_AFTER_EXPIRY_OFFLINE_MS: Long = 0L // immediate

    const val CLOCK_SUSPICIOUS_DRIFT_BACK_MS: Long = 3L * 24 * 60 * 60 * 1000
    const val CLOCK_SUSPICIOUS_DRIFT_FORWARD_MS: Long = 60L * 24 * 60 * 60 * 1000

    // Product details
    const val PRODUCT_ID_ANNUAL = "crisis_ai_annual"

    // Debug override flag key (not persisted across reinstalls intentionally)
    const val DEBUG_FORCE_SURVIVAL = false

    const val YEAR_MS: Long = 365L * 24 * 60 * 60 * 1000
}