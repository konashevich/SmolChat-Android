package io.shubham0204.smollmandroid.subscription

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Periodic background worker that attempts silent subscription verification when online. */
class SubscriptionVerifyWorker(
    appContext: Context,
    params: WorkerParameters,
): CoroutineWorker(appContext, params), KoinComponent {
    private val subscriptionManager: SubscriptionManager by inject()

    override suspend fun doWork(): Result {
        // Only attempt if interval makes sense (lightweight guard)
        return try {
            subscriptionManager.refreshIfNeeded(force = false)
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
    companion object { const val UNIQUE_NAME = "subscription_verify_periodic" }
}