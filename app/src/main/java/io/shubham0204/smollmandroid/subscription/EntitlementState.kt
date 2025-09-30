package io.shubham0204.smollmandroid.subscription

/**
 * Possible entitlement states for crisis AI subscription system.
 * SURVIVAL_MODE never blocks user even if subscription cannot be verified.
 */
enum class EntitlementState {
    ACTIVE,
    SURVIVAL_MODE,
    NOT_ENTITLED;

    fun label(): String = when (this) {
        ACTIVE -> "Active"
        SURVIVAL_MODE -> "Survival Mode"
        NOT_ENTITLED -> "Not Entitled"
    }
}
