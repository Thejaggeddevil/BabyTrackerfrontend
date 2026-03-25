package com.example.babyparenting.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Free Trial Manager — NO payment required.
 *
 * Flow:
 *  1. User pehli baar login kare → 14-din ka free trial start hota hai
 *  2. Day 11, 12, 13 → reminder notification dikhao (2-3 din pehle warn karo)
 *  3. 14 din baad → Paywall dikhao, payment lo
 *
 * Usage:
 *   - Call activateTrial() right after successful login (only if trial not already started)
 *   - Check canAccessAdvice() before showing advice
 *   - Check shouldShowTrialReminder() to show in-app reminder banner
 */
class SubscriptionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)

    // ── Trial ─────────────────────────────────────────────────────────────────

    /**
     * Pehli baar login ke baad call karo.
     * Agar trial already start ho chuka hai toh kuch nahi karega.
     */
    fun activateTrial() {
        if (isTrialStarted()) return   // Already started — don't reset
        val trialEnd = System.currentTimeMillis() + TRIAL_DURATION_MS
        prefs.edit()
            .putBoolean(KEY_TRIAL_STARTED, true)
            .putLong(KEY_TRIAL_START, System.currentTimeMillis())
            .putLong(KEY_TRIAL_END, trialEnd)
            .apply()
    }

    fun isTrialStarted(): Boolean = prefs.getBoolean(KEY_TRIAL_STARTED, false)

    fun isTrialActive(): Boolean {
        if (!isTrialStarted()) return false
        val end = prefs.getLong(KEY_TRIAL_END, 0L)
        return System.currentTimeMillis() < end
    }

    /** Kitne din baaki hain trial mein (0 agar khatam) */
    fun trialDaysRemaining(): Int {
        val end = prefs.getLong(KEY_TRIAL_END, 0L)
        val remaining = end - System.currentTimeMillis()
        return if (remaining <= 0) 0 else (remaining / MS_PER_DAY).toInt()
    }

    /** Trial kab start hua (epoch ms) */
    fun trialStartTime(): Long = prefs.getLong(KEY_TRIAL_START, 0L)

    // ── Paid subscription (post-trial) ───────────────────────────────────────

    /** Trial khatam hone ke baad payment ke liye call karo */
    fun activateSubscription(razorpayPaymentId: String) {
        val validUntil = System.currentTimeMillis() + SUBSCRIPTION_DURATION_MS
        prefs.edit()
            .putBoolean(KEY_SUBSCRIBED, true)
            .putString(KEY_PAYMENT_ID, razorpayPaymentId)
            .putLong(KEY_VALID_UNTIL, validUntil)
            .apply()
    }

    fun isSubscriptionActive(): Boolean {
        val subscribed = prefs.getBoolean(KEY_SUBSCRIBED, false)
        val validUntil = prefs.getLong(KEY_VALID_UNTIL, 0L)
        return subscribed && System.currentTimeMillis() < validUntil
    }

    fun subscriptionDaysRemaining(): Int {
        val validUntil = prefs.getLong(KEY_VALID_UNTIL, 0L)
        val remaining  = validUntil - System.currentTimeMillis()
        return if (remaining <= 0) 0 else (remaining / MS_PER_DAY).toInt()
    }

    fun getLastPaymentId(): String? = prefs.getString(KEY_PAYMENT_ID, null)

    // ── Main gate ─────────────────────────────────────────────────────────────

    /** Advice screen yahi check karta hai */
    fun canAccessAdvice(): Boolean = isTrialActive() || isSubscriptionActive()

    fun getStatus(): SubscriptionStatus = when {
        isSubscriptionActive() -> SubscriptionStatus.SUBSCRIBED
        isTrialActive()        -> SubscriptionStatus.TRIAL_ACTIVE
        isTrialStarted()       -> SubscriptionStatus.TRIAL_EXPIRED
        else                   -> SubscriptionStatus.NOT_STARTED
    }

    // ── Reminder logic ────────────────────────────────────────────────────────

    /**
     * True if user ko reminder dikhana chahiye (2-3 din pehle trial khatam hone se).
     * Ye in-app banner ke liye hai — push notification alag se implement karo.
     */
    fun shouldShowTrialReminder(): Boolean {
        if (!isTrialActive()) return false
        val daysLeft = trialDaysRemaining()
        return daysLeft in 0..REMINDER_DAYS_BEFORE
    }

    /**
     * Reminder message — banner mein dikhao
     */
    fun reminderMessage(): String {
        val days = trialDaysRemaining()
        return when {
            days == 0  -> "⚠️ Aapka free trial aaj khatam ho raha hai! Subscribe karein."
            days == 1  -> "⏰ Sirf 1 din bacha hai free trial mein. Subscribe karein!"
            days <= 3  -> "🔔 Aapka free trial $days dinon mein khatam hoga. Subscribe karein!"
            else       -> ""
        }
    }

    // ── Testing ───────────────────────────────────────────────────────────────

    fun resetForTesting() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TRIAL_STARTED  = "trial_started"
        private const val KEY_TRIAL_START    = "trial_start_ms"
        private const val KEY_TRIAL_END      = "trial_end_ms"
        private const val KEY_SUBSCRIBED     = "is_subscribed"
        private const val KEY_PAYMENT_ID     = "razorpay_payment_id"
        private const val KEY_VALID_UNTIL    = "subscription_valid_until_ms"

        private const val TRIAL_DURATION_MS        = 14L * 24 * 60 * 60 * 1000   // 14 days
        private const val SUBSCRIPTION_DURATION_MS = 30L * 24 * 60 * 60 * 1000   // 30 days
        private const val MS_PER_DAY               = 24L * 60 * 60 * 1000

        /** Reminder dikhao jab itne ya kam din bachein */
        const val REMINDER_DAYS_BEFORE = 3
    }
}

enum class SubscriptionStatus {
    NOT_STARTED,    // User ne abhi login nahi kiya / trial activate nahi hua
    TRIAL_ACTIVE,   // 14-din ka free trial chal raha hai
    TRIAL_EXPIRED,  // Trial khatam, payment pending
    SUBSCRIBED      // Active paid subscription
}