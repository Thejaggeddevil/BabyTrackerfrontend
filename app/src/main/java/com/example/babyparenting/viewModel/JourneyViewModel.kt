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

// ── Payment state ─────────────────────────────────────────────────────────────

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

    // ── Subscription / Payment ────────────────────────────────────────────────

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private val _subscriptionStatus = MutableStateFlow(subscriptionManager.getStatus())
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    // Razorpay ko Activity chahiye
    private var currentActivity: Activity? = null

    init {
        viewModelScope.launch {
            combine(milestoneRepo.milestones, _activeFilter) { all, filter ->
                if (filter == null) all else all.filter { it.source == filter }
            }.collect { filtered ->
                _filteredMilestones.value = filtered
                _visibleMilestones.value  = computeVisible(filtered)
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

    // ── Completion — ONE WAY, no undo ─────────────────────────────────────────

    fun markComplete(id: String)     = milestoneRepo.markComplete(id)
    fun toggleCompletion(id: String) = milestoneRepo.markComplete(id)

    // ── Visible logic — same age group, 4-4 ──────────────────────────────────

    /**
     * Logic same hai — 4-4 batch mein milestones dikhao.
     *
     * FIX: Pehle findActiveGroupId() null return karta tha jab ek poora
     * group complete ho jaata tha lekin next group abhi load nahi hua tha.
     * Us case mein "return all" ho jaata tha — jo galat tha.
     *
     * Ab: agar activeGroupId null hai (matlab sab complete) toh
     * sabse zyada ageGroupId wala group dikhao — next group load hone
     * tak blank screen nahi aayegi.
     */
    private fun computeVisible(all: List<Milestone>): List<Milestone> {
        if (all.isEmpty()) return emptyList()

        val activeGroupId = findActiveGroupId(all)

        // Agar sab complete hain (activeGroupId == null) — last group dikhao
        // taaki next group load hone tak screen blank na rahe
        if (activeGroupId == null) {
            val lastGroupId = all.maxOf { it.ageGroupId }
            return all.filter { it.ageGroupId <= lastGroupId }
        }

        val previousDone  = all.filter { it.ageGroupId < activeGroupId }
        val activeGroupMs = all.filter { it.ageGroupId == activeGroupId }

        val visibleFromActive = mutableListOf<Milestone>()
        var batchStart = 0
        while (batchStart < activeGroupMs.size) {
            val end   = minOf(batchStart + 4, activeGroupMs.size)
            val batch = activeGroupMs.subList(batchStart, end)
            visibleFromActive.addAll(batch)
            if (!batch.all { it.isCompleted }) break
            batchStart += 4
        }

        return previousDone + visibleFromActive
    }

    private fun findActiveGroupId(all: List<Milestone>): Int? =
        all.map { it.ageGroupId }
            .distinct()
            .sorted()
            .firstOrNull { groupId ->
                all.filter { it.ageGroupId == groupId }.any { !it.isCompleted }
            }

    /**
     * FIX: Pehle agar activeGroupId null tha (sab complete), toh early return
     * ho jaata tha aur next group kabhi load nahi hota tha.
     *
     * Ab: null case mein bhi maxGroup + 1 load karte hain.
     */
    private fun checkAndLoadNextGroup(all: List<Milestone>) {
        val activeGroupId = findActiveGroupId(all)

        // Agar koi active group nahi (sab complete) — next group load karo
        if (activeGroupId == null) {
            val maxGroup = all.maxOfOrNull { it.ageGroupId } ?: return
            viewModelScope.launch {
                milestoneRepo.loadNextGroupIfNeeded(maxGroup + 1)
            }
            return
        }

        // Active group ke saare milestones complete hain — next load karo
        val activeGroupMs = all.filter { it.ageGroupId == activeGroupId }
        if (activeGroupMs.any { !it.isCompleted }) return

        viewModelScope.launch {
            milestoneRepo.loadNextGroupIfNeeded(activeGroupId + 1)
        }
    }

    // ── Lock logic ────────────────────────────────────────────────────────────

    fun isLocked(milestone: Milestone): Boolean {
        val visible       = _visibleMilestones.value
        val activeGroupId = findActiveGroupId(visible)
        if (milestone.ageGroupId != activeGroupId) return false

        val groupVisible = visible.filter { it.ageGroupId == activeGroupId }
        val idxInGroup   = groupVisible.indexOf(milestone)
        if (idxInGroup <= 0) return false

        val indexInBatch = idxInGroup % 4
        if (indexInBatch == 0) return false

        val prev = groupVisible.getOrNull(idxInGroup - 1) ?: return false
        return !prev.isCompleted
    }

    // ── Milestone tap — paywall on first tap if not subscribed ────────────────

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

    // ── Razorpay ──────────────────────────────────────────────────────────────

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
                put("amount", 100)   // 100 paise = ₹1
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

    /** Called from MainActivity after Razorpay success */
    fun onPaymentSuccess(razorpayPaymentId: String) {
        subscriptionManager.activateSubscription(razorpayPaymentId)
        _paymentState.value       = PaymentState.Success(razorpayPaymentId)
        _subscriptionStatus.value = subscriptionManager.getStatus()
        _showPaywall.value        = false
        _selectedMilestone.value?.let { fetchAdvice(it) }
    }

    /** Called from MainActivity after Razorpay failure */
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

    fun setChildAge(months: Int) {
        hasLoaded = false   // FIX: age change pe fresh load allow karo
        milestoneRepo.setChildAge(months)
        viewModelScope.launch { milestoneRepo.initialLoad() }
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
        hasLoaded = false   // FIX: manual reload bhi fresh load kare
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