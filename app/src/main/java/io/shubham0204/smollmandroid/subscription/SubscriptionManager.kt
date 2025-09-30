package io.shubham0204.smollmandroid.subscription

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.android.billingclient.api.Purchase
import io.shubham0204.smollmandroid.llm.ModelsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class SubscriptionManager(
    private val store: SubscriptionLocalStore,
    private val billingInteractor: BillingInteractor,
    private val modelsRepository: ModelsRepository,
    context: Context,
) : EntitlementProvider {
    @Volatile private var state: EntitlementState = EntitlementState.NOT_ENTITLED
    private val _stateFlow = MutableStateFlow(state)
    val stateFlow: StateFlow<EntitlementState> = _stateFlow
    @Volatile private var cachedRecord: SubscriptionRecord? = null

    private val metaPrefs: SharedPreferences = context.getSharedPreferences("subscription_meta", Context.MODE_PRIVATE)
    private var lastSyncAttemptUtc: Long = 0L

    init {
        billingInteractor.startConnection { syncFromBilling() }
        CoroutineScope(Dispatchers.IO).launch {
            billingInteractor.lastPurchaseResult.collectLatest { p -> p?.let { applyPurchase(it) } }
        }
    }

    // EntitlementProvider API
    override fun isFeatureAccessAllowed(): Boolean = state == EntitlementState.ACTIVE || state == EntitlementState.SURVIVAL_MODE
    override fun currentState(): EntitlementState = state
    override fun accessModeLabel(): String = state.label()
    override fun refreshIfNeeded(force: Boolean) { if (force || shouldAttemptSync()) syncFromBilling() else evaluateState() }
    override fun beginPurchase(activity: Activity, onResult: (PurchaseOutcome) -> Unit) {
        billingInteractor.launchPurchase(activity) { outcome -> onResult(outcome) }
    }

    fun shouldAttemptSync(nowUtc: Long = System.currentTimeMillis()): Boolean = (nowUtc - lastSyncAttemptUtc) > SubscriptionPolicy.VERIFY_INTERVAL_MIN_MS
    private fun markSyncAttempt(nowUtc: Long = System.currentTimeMillis()) { lastSyncAttemptUtc = nowUtc }
    fun getTransitionLog(): String = metaPrefs.getString("transition_log", "") ?: ""

    fun shouldShowRenewalPrompt(nowUtc: Long = System.currentTimeMillis()): Boolean {
        if (state != EntitlementState.NOT_ENTITLED && state != EntitlementState.SURVIVAL_MODE) return false
        val rec = cachedRecord ?: return false
        if (rec.survivalModeActivatedAtUtc == null) return false
        if (nowUtc < rec.lastKnownExpiryUtc) return false
        val suppressUntil = metaPrefs.getLong("renew_prompt_suppress_until", 0L)
        return nowUtc >= suppressUntil
    }

    fun suppressRenewalPrompt(days: Int = 1, nowUtc: Long = System.currentTimeMillis()) {
        metaPrefs.edit().putLong("renew_prompt_suppress_until", nowUtc + days * 24L * 60 * 60 * 1000).apply()
    }

    fun evaluateState(nowUtc: Long = System.currentTimeMillis(), elapsed: Long = SystemClock.elapsedRealtime()) {
        var record = store.read()
        cachedRecord = record
        if (SubscriptionPolicy.DEBUG_FORCE_SURVIVAL) { updateState(EntitlementState.SURVIVAL_MODE); return }
        if (record == null) {
            val hasModels = runCatching { modelsRepository.getAvailableModelsList().isNotEmpty() }.getOrDefault(false)
            updateState(if (hasModels) EntitlementState.SURVIVAL_MODE else EntitlementState.NOT_ENTITLED)
            return
        }
        val lastElapsed = record.systemElapsedRealtimeAtVerification
        val elapsedDelta = if (lastElapsed == 0L) 0L else (elapsed - lastElapsed)
        val wallDelta = nowUtc - record.lastVerificationUtc
        val clockSuspicious = (wallDelta < -SubscriptionPolicy.CLOCK_SUSPICIOUS_DRIFT_BACK_MS) || (wallDelta - elapsedDelta > SubscriptionPolicy.CLOCK_SUSPICIOUS_DRIFT_FORWARD_MS)
        if (clockSuspicious != record.clockSuspicious) {
            record = record.copy(clockSuspicious = clockSuspicious)
            store.write(record)
            cachedRecord = record
        }
        if (nowUtc < record.lastKnownExpiryUtc) {
            updateState(EntitlementState.ACTIVE)
        } else {
            if (record.survivalModeActivatedAtUtc == null) {
                record = record.copy(survivalModeActivatedAtUtc = nowUtc)
                store.write(record)
                cachedRecord = record
            }
            updateState(EntitlementState.SURVIVAL_MODE)
        }
    }

    fun syncFromBilling(nowUtc: Long = System.currentTimeMillis(), elapsed: Long = SystemClock.elapsedRealtime()) {
        markSyncAttempt(nowUtc)
        billingInteractor.queryActivePurchase { purchase ->
            if (purchase != null) {
                applyPurchase(purchase, nowUtc, elapsed)
            } else {
                val rec = cachedRecord ?: store.read()?.also { cachedRecord = it }
                if (rec != null) {
                    val expired = nowUtc >= rec.lastKnownExpiryUtc
                    val revokedEarly = !expired
                    if (expired || revokedEarly) {
                        if (state == EntitlementState.SURVIVAL_MODE && expired) updateState(EntitlementState.SURVIVAL_MODE) else updateState(EntitlementState.NOT_ENTITLED)
                    }
                }
            }
        }
    }

    fun applyPurchase(purchase: Purchase, nowUtc: Long = System.currentTimeMillis(), elapsed: Long = SystemClock.elapsedRealtime()) {
        if (!purchase.products.contains(SubscriptionPolicy.PRODUCT_ID_ANNUAL)) return
        val start = purchase.purchaseTime.takeIf { it > 0 } ?: nowUtc
        val expiry = start + SubscriptionPolicy.YEAR_MS
        val record = SubscriptionRecord(
            purchaseStartUtc = start,
            lastKnownExpiryUtc = expiry,
            lastVerificationUtc = nowUtc,
            purchaseToken = purchase.purchaseToken,
            entitlementState = EntitlementState.ACTIVE,
            survivalModeActivatedAtUtc = null,
            verificationDebt = false,
            clockSuspicious = false,
            systemElapsedRealtimeAtVerification = elapsed,
            autoRenewing = purchase.isAutoRenewing,
        )
        store.write(record)
        cachedRecord = record
        updateState(EntitlementState.ACTIVE)
    }

    private fun updateState(new: EntitlementState) {
        if (new != state) {
            val old = state
            state = new
            // Persist entitlementState if we have a record
            cachedRecord?.let { rec ->
                val updated = rec.copy(entitlementState = new)
                store.write(updated)
                cachedRecord = updated
            }
            appendTransition(old, new)
            _stateFlow.value = new
        }
    }

    private fun appendTransition(from: EntitlementState, to: EntitlementState) {
        val key = "transition_log"
        val existing = metaPrefs.getString(key, "") ?: ""
        val line = "${System.currentTimeMillis()}:$from->$to\n"
        val updated = (existing + line).split('\n').filter { it.isNotBlank() }.takeLast(50).joinToString("\n")
        metaPrefs.edit().putString(key, updated).apply()
    }

    companion object {
        data class EvalResult(val state: EntitlementState, val updated: SubscriptionRecord?)
        fun evaluatePure(
            record: SubscriptionRecord?,
            nowUtc: Long,
            elapsed: Long,
            hasModels: Boolean,
        ): EvalResult {
            if (record == null) {
                return EvalResult(if (hasModels) EntitlementState.SURVIVAL_MODE else EntitlementState.NOT_ENTITLED, null)
            }
            val lastElapsed = record.systemElapsedRealtimeAtVerification
            val elapsedDelta = if (lastElapsed == 0L) 0L else (elapsed - lastElapsed)
            val wallDelta = nowUtc - record.lastVerificationUtc
            val clockSuspicious = (wallDelta < -SubscriptionPolicy.CLOCK_SUSPICIOUS_DRIFT_BACK_MS) || (wallDelta - elapsedDelta > SubscriptionPolicy.CLOCK_SUSPICIOUS_DRIFT_FORWARD_MS)
            var updated = if (clockSuspicious != record.clockSuspicious) {
                record.copy(clockSuspicious = clockSuspicious)
            } else record
            return if (nowUtc < updated.lastKnownExpiryUtc) {
                EvalResult(EntitlementState.ACTIVE, updated)
            } else {
                if (updated.survivalModeActivatedAtUtc == null) {
                    updated = updated.copy(survivalModeActivatedAtUtc = nowUtc)
                }
                EvalResult(EntitlementState.SURVIVAL_MODE, updated)
            }
        }
    }
}