package io.shubham0204.smollmandroid.subscription

import android.app.Activity

/** Public facing abstraction for entitlement checks used by UI / other layers. */
interface EntitlementProvider {
    /** True when user may access full app functionality (ACTIVE or SURVIVAL_MODE). */
    fun isFeatureAccessAllowed(): Boolean
    /** Current raw entitlement state. */
    fun currentState(): EntitlementState
    /** Human readable label for diagnostics / UI. */
    fun accessModeLabel(): String
    /** Triggers background refresh if interval elapsed, or forces when force=true. */
    fun refreshIfNeeded(force: Boolean = false)
    /** Starts purchase flow, result handled internally; callback only on error. */
    fun beginPurchase(activity: Activity, onResult: (PurchaseOutcome) -> Unit)
}