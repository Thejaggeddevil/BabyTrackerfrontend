package com.example.babyparenting.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.babyparenting.data.local.SubscriptionManager
import com.example.babyparenting.data.local.SubscriptionStatus
import com.example.babyparenting.data.model.AgeGroup
import com.example.babyparenting.data.model.DatasetSource
import com.example.babyparenting.data.model.JourneyProgress
import com.example.babyparenting.data.model.Milestone
import com.example.babyparenting.data.model.UiState
import com.example.babyparenting.data.repository.ApiRepository
import com.example.babyparenting.data.repository.MilestoneRepository
import com.example.babyparenting.network.api.RetrofitProvider
import com.example.babyparenting.network.model.AdviceResponse
import com.razorpay.Checkout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class PaymentState {
    object Idle    : PaymentState()
    object Loading : PaymentState()
    data class Success(val paymentId: String) : PaymentState()
    data class Error(val message: String)     : PaymentState()
}

class JourneyViewModel(app: Application) : AndroidViewModel(app) {

    private val milestoneRepo       = MilestoneRepository(app)
    private val apiRepo             = ApiRepository(RetrofitProvider.babyApi)
    private val subscriptionManager = SubscriptionManager(app)

    val milestones: StateFlow<List<Milestone>> = milestoneRepo.milestones
    val ageGroups:  StateFlow<List<AgeGroup>>  = milestoneRepo.ageGroups
    val progress:   StateFlow<JourneyProgress>  = milestoneRepo.progress
    val isLoading:  StateFlow<Boolean>           = milestoneRepo.isLoading
    val loadError:  StateFlow<String?>           = milestoneRepo.error

    private val _selectedMilestone = MutableStateFlow<Milestone?>(null)
    val selectedMilestone: StateFlow<Milestone?> = _selectedMilestone.asStateFlow()

    private val _adviceState = MutableStateFlow<UiState<AdviceResponse>>(UiState.Idle)
    val adviceState: StateFlow<UiState<AdviceResponse>> = _adviceState.asStateFlow()

    private val _activeFilter = MutableStateFlow<DatasetSource?>(null)
    val activeFilter: StateFlow<DatasetSource?> = _activeFilter.asStateFlow()

    private val _visibleMilestones = MutableStateFlow<List<Milestone>>(emptyList())
    val visibleMilestones: StateFlow<List<Milestone>> = _visibleMilestones.asStateFlow()

    private val _filteredMilestones = MutableStateFlow<List<Milestone>>(emptyList())
    val filteredMilestones: StateFlow<List<Milestone>> = _filteredMilestones.asStateFlow()

    // Payment
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private val _subscriptionStatus = MutableStateFlow(subscriptionManager.getStatus())
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    private var currentActivity: Activity? = null

    init {
        viewModelScope.launch {
            combine(milestoneRepo.milestones, _activeFilter) { all, filter ->
                if (filter == null) all else all.filter { it.source == filter }
            }.collect { filtered ->
                _filteredMilestones.value = filtered
                _visibleMilestones.value  = computeVisible(filtered)

                // ✅ CRITICAL: Trigger next group load when needed
                checkAndLoadNextGroup(filtered)
            }
        }
    }

    private var hasLoaded = false

    fun loadDataIfNeeded() {
        if (hasLoaded) return
        hasLoaded = true

        viewModelScope.launch {
            milestoneRepo.initialLoad()
        }
    }

    // ── Activity binding ──────────────────────────────────────────────────────

    fun bindActivity(activity: Activity) { currentActivity = activity }
    fun unbindActivity() { currentActivity = null }

    // ── Subscription helpers ──────────────────────────────────────────────────

    fun canAccessAdvice(): Boolean = subscriptionManager.canAccessAdvice()
    fun getDaysRemaining(): Int {
        return when {
            subscriptionManager.isSubscriptionActive() -> subscriptionManager.subscriptionDaysRemaining()
            subscriptionManager.isTrialActive()        -> subscriptionManager.trialDaysRemaining()
            else                                       -> 0
        }
    }
    fun refreshSubscriptionStatus() {
        _subscriptionStatus.value = subscriptionManager.getStatus()
    }

    // ── Completion ────────────────────────────────────────────────────────────

    fun markComplete(id: String)     = milestoneRepo.markComplete(id)
    fun toggleCompletion(id: String) = milestoneRepo.markComplete(id)

    // ── VISIBLE LOGIC: 4-4 unlock system ──────────────────────────────────────
    /**
     * ✅ FIXED: Now applies 4-4 unlock logic to EVERY age group.
     *
     * Behavior:
     * 1. Find the "active" (first incomplete) age group
     * 2. Show all completed groups' milestones
     * 3. In active group: show batches of 4 until hitting an incomplete card
     *
     * Example (User at group 3 with 12 cards):
     * - Cards 0-3: all done → show all 4
     * - Cards 4-7: 0-2 done, 3 incomplete → show these 4
     * - Cards 8-11: still locked (batch at 4-7 not all done)
     */
    private fun computeVisible(all: List<Milestone>): List<Milestone> {
        if (all.isEmpty()) return emptyList()

        val activeGroupId = findActiveGroupId(all) ?: return all

        // Show all milestones from completed groups
        val previousDone = all.filter { it.ageGroupId < activeGroupId }

        // Get milestones in the active group
        val activeGroupMs = all.filter { it.ageGroupId == activeGroupId }
        if (activeGroupMs.isEmpty()) return previousDone

        val visibleFromActive = mutableListOf<Milestone>()
        var batchStart = 0

        // Process in 4-card batches
        while (batchStart < activeGroupMs.size) {
            val batchEnd = minOf(batchStart + 4, activeGroupMs.size)
            val batch = activeGroupMs.subList(batchStart, batchEnd)

            visibleFromActive.addAll(batch)

            // Stop at first incomplete batch
            if (!batch.all { it.isCompleted }) {
                break
            }

            batchStart += 4
        }

        return previousDone + visibleFromActive
    }

    /**
     * Find the first age group that has incomplete milestones.
     */
    private fun findActiveGroupId(all: List<Milestone>): Int? =
        all.map { it.ageGroupId }
            .distinct()
            .sorted()
            .firstOrNull { groupId ->
                all.filter { it.ageGroupId == groupId }.any { !it.isCompleted }
            }

    /**
     * ✅ CRITICAL FIX: This function auto-loads the next group(s) when needed.
     *
     * Called every time milestones list changes.
     *
     * Detects if we need more data and loads it in background:
     * 1. User scrolls and sees last few cards → load next group
     * 2. User completes all cards in group → load next group
     * 3. New group loads → automatically added to milestones list
     *
     * This ensures seamless progression beyond 12 cards!
     */
    private fun checkAndLoadNextGroup(all: List<Milestone>) {
        if (all.isEmpty()) return

        val activeGroupId = findActiveGroupId(all) ?: return
        val activeGroupMs = all.filter { it.ageGroupId == activeGroupId }

        if (activeGroupMs.isEmpty()) return

        val visibleMs = _visibleMilestones.value

        // ✅ Strategy 1: If all active group cards are done, load next group
        if (activeGroupMs.all { it.isCompleted }) {
            val nextGroupId = activeGroupId + 1
            viewModelScope.launch {
                milestoneRepo.loadNextGroupIfNeeded(nextGroupId)
            }
            return
        }

        // ✅ Strategy 2: If user is seeing last card of current group, start loading next
        // This provides buffer time so next group is ready when needed
        val visibleAtEnd = visibleMs.lastOrNull()
        if (visibleAtEnd != null && visibleAtEnd.ageGroupId == activeGroupId) {
            val lastVisibleIdx = activeGroupMs.indexOf(visibleAtEnd)
            // If showing last card or last 2 cards, preload next group
            if (lastVisibleIdx >= activeGroupMs.size - 2) {
                val nextGroupId = activeGroupId + 1
                viewModelScope.launch {
                    milestoneRepo.loadNextGroupIfNeeded(nextGroupId)
                }
            }
        }
    }

    // ── LOCK LOGIC: Cards locked until previous batch complete ────────────────
    /**
     * ✅ FIXED: Works for ALL age groups.
     *
     * A card is LOCKED if:
     * 1. It's in the active group AND
     * 2. It's NOT in the first visible batch AND
     * 3. The previous card is incomplete
     *
     * This gates cards: complete 4 → next 4 unlock, etc.
     */
    fun isLocked(milestone: Milestone): Boolean {
        val visible = _visibleMilestones.value
        val activeGroupId = findActiveGroupId(visible)

        // Cards in completed groups are not locked
        if (milestone.ageGroupId != activeGroupId) return false

        val groupVisible = visible.filter { it.ageGroupId == activeGroupId }
        val idxInGroup   = groupVisible.indexOf(milestone)

        // First card is never locked
        if (idxInGroup <= 0) return false

        // Position within 4-card batch
        val indexInBatch = idxInGroup % 4
        if (indexInBatch == 0) return false

        // Card is locked if previous card is incomplete
        val prevCard = groupVisible.getOrNull(idxInGroup - 1) ?: return false
        return !prevCard.isCompleted
    }

    // ── Milestone interaction ─────────────────────────────────────────────────

    fun onMilestoneTapped(milestone: Milestone) {
        _selectedMilestone.value = milestone
        refreshSubscriptionStatus()

        if (subscriptionManager.canAccessAdvice()) {
            fetchAdvice(milestone)
            _showPaywall.value = false
        } else {
            _showPaywall.value = true
        }
    }

    fun dismissPaywall() { _showPaywall.value = false }

    // ── Payment / Razorpay ────────────────────────────────────────────────────

    fun startPayment() {
        val activity = currentActivity ?: run {
            _paymentState.value = PaymentState.Error("Payment cannot start. Please try again.")
            return
        }
        _paymentState.value = PaymentState.Loading
        try {
            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY_ID)
            val options = JSONObject().apply {
                put("name", "Baby Parenting Companion")
                put("description", "30 Days Access — ₹1")
                put("theme.color", "#FF8B94")
                put("currency", "INR")
                put("amount", 100)
                put("prefill", JSONObject().apply {
                    put("contact", "")
                    put("email", "")
                })
            }
            checkout.open(activity, options)
        } catch (e: Exception) {
            _paymentState.value = PaymentState.Error("Could not open payment. Please try again.")
        }
    }

    fun onPaymentSuccess(razorpayPaymentId: String) {
        subscriptionManager.activateSubscription(razorpayPaymentId)
        _paymentState.value       = PaymentState.Success(razorpayPaymentId)
        _subscriptionStatus.value = subscriptionManager.getStatus()
        _showPaywall.value        = false
        _selectedMilestone.value?.let { fetchAdvice(it) }
    }

    fun onPaymentError(errorCode: Int, description: String) {
        _paymentState.value = PaymentState.Error(
            when (errorCode) {
                Checkout.NETWORK_ERROR   -> "No internet connection."
                Checkout.INVALID_OPTIONS -> "Invalid payment options."
                else -> description.ifBlank { "Payment failed. Please try again." }
            }
        )
    }

    fun resetPaymentState() { _paymentState.value = PaymentState.Idle }

    // ── Other actions ─────────────────────────────────────────────────────────

    fun toggleFilter(source: DatasetSource) {
        _activeFilter.value = if (_activeFilter.value == source) null else source
    }

    fun clearFilter() { _activeFilter.value = null }

    /**
     * ✅ CRITICAL: When age changes, reset and reload from that age.
     */
    fun setChildAge(months: Int) {
        milestoneRepo.setChildAge(months)
        viewModelScope.launch {
            milestoneRepo.initialLoad()
        }
    }

    fun setChildName(name: String) = milestoneRepo.setChildName(name)
    fun getChildAgeMonths(): Int   = milestoneRepo.getChildAgeMonths()
    fun getChildName(): String     = milestoneRepo.getChildName()
    fun refreshAfterAdminEdit()    = milestoneRepo.refreshAdminMilestones()

    fun resetAdvice() {
        _adviceState.value       = UiState.Idle
        _selectedMilestone.value = null
    }

    fun retryAdvice() { _selectedMilestone.value?.let { fetchAdvice(it) } }

    fun reloadDatasets() {
        viewModelScope.launch { milestoneRepo.initialLoad() }
    }

    private fun fetchAdvice(milestone: Milestone) {
        viewModelScope.launch {
            _adviceState.value = UiState.Loading
            _adviceState.value = apiRepo.fetchAdvice(
                model = milestone.source.apiModel,
                query = milestone.apiQuery
            )
        }
    }

    companion object {
        private const val RAZORPAY_KEY_ID = "rzp_test_SHCQZMQFoBaboC"
    }
}