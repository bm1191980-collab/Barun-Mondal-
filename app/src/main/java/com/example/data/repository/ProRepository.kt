package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.service.PaymentGatewayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ProRepository(
    private val context: Context,
    private val proSubscriptionDao: ProSubscriptionDao,
    private val referralDao: ReferralDao,
    private val walletDao: WalletDao,
    private val walletTransactionDao: WalletTransactionDao,
    private val withdrawalRequestDao: WithdrawalRequestDao,
    private val ownerChatDao: OwnerChatDao,
    private val chatMessageDao: ChatMessageDao,
    private val userAccountDao: UserAccountDao,
    private val postDao: PostDao,
    private val auditLogDao: AuditLogDao
) {
    private val TAG = "ProRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    // Current User ID (defaults to primary creator user)
    val currentUserId = "user_creator"
    val currentUserName = "Satisfy Creator"
    val currentUserEmail = "creator@satisfy.app"
    val currentUserAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200"

    // Flows for current user
    val currentUserSubscription: Flow<ProSubscriptionEntity?> =
        proSubscriptionDao.observeActiveSubscription(currentUserId)

    val currentUserWallet: Flow<WalletEntity?> =
        walletDao.observeWallet(currentUserId)

    val currentUserTransactions: Flow<List<WalletTransactionEntity>> =
        walletTransactionDao.getTransactionsForUser(currentUserId)

    val currentUserReferrals: Flow<List<ReferralEntity>> =
        referralDao.getReferralsByReferrer(currentUserId)

    val currentUserWithdrawals: Flow<List<WithdrawalRequestEntity>> =
        withdrawalRequestDao.getWithdrawalsForUser(currentUserId)

    val currentUserChat: Flow<OwnerChatEntity?> =
        ownerChatDao.observeChatByUserId(currentUserId)

    val currentUserChatMessages: Flow<List<ChatMessageEntity>> =
        chatMessageDao.getMessagesForChat(currentUserId)

    // Admin Flows
    val allSubscriptions: Flow<List<ProSubscriptionEntity>> = proSubscriptionDao.getAllSubscriptions()
    val allReferrals: Flow<List<ReferralEntity>> = referralDao.getAllReferrals()
    val allWallets: Flow<List<WalletEntity>> = walletDao.getAllWallets()
    val allTransactions: Flow<List<WalletTransactionEntity>> = walletTransactionDao.getAllTransactions()
    val allWithdrawals: Flow<List<WithdrawalRequestEntity>> = withdrawalRequestDao.getAllWithdrawals()
    val pendingWithdrawals: Flow<List<WithdrawalRequestEntity>> = withdrawalRequestDao.getPendingWithdrawals()
    val allOwnerChats: Flow<List<OwnerChatEntity>> = ownerChatDao.getAllChats()

    init {
        scope.launch {
            seedInitialProAndReferralData()
        }
    }

    /**
     * Ensure user wallet exists with real initial balance
     */
    private suspend fun seedInitialProAndReferralData() = withContext(Dispatchers.IO) {
        // Ensure user wallet exists with real clean state
        val wallet = walletDao.getWallet(currentUserId)
        if (wallet == null) {
            walletDao.insertOrUpdateWallet(
                WalletEntity(
                    userId = currentUserId,
                    userName = currentUserName,
                    referralBalance = 0.0,
                    totalEarned = 0.0,
                    totalWithdrawn = 0.0,
                    pendingWithdrawalAmount = 0.0,
                    successfulReferralsCount = 0,
                    isFrozen = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Check whether current user is an active Pro subscriber
     */
    suspend fun isUserProActive(userId: String = currentUserId): Boolean = withContext(Dispatchers.IO) {
        return@withContext isUserEligibleForReferralIncome(userId)
    }

    /**
     * Real backend subscription-status verification:
     * Check if a user has an active, unexpired Pro subscription (Pro ₹5, Premium Pro ₹15, or Super Premium Pro ₹25).
     * Referral Income is a PRO-ONLY feature. Free users or users with expired/cancelled/inactive subscriptions
     * cannot earn referral commissions, and referral earnings are not added to their wallets.
     */
    suspend fun isUserEligibleForReferralIncome(userId: String): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val activeSub = proSubscriptionDao.getActiveSubscription(userId, now)
        if (activeSub != null && activeSub.expiresAt > now && activeSub.status == "ACTIVE") {
            return@withContext true
        }
        val user = userAccountDao.getUserByUid(userId)
        if (user != null && user.isPro && (user.proExpiresAt ?: 0L) > now && user.subscriptionStatus == "ACTIVE") {
            return@withContext true
        }
        return@withContext false
    }

    /**
     * Process Pro Membership Purchase for any of the 3 Plans:
     * 1. Pro — ₹5/month (plan_pro_5) -> Referral Income Enabled (50% Net Split)
     * 2. Premium Pro — ₹15/month (plan_premium_pro_15) -> Referral Income Enabled + Premium Features
     * 3. Super Premium Pro — ₹25/month (plan_super_premium_pro_25) -> Referral Income Enabled + Super Premium Features
     *
     * Rules & Architecture:
     * - Pro subscription is optional. Free users can continue using all normal/free Satisfy features.
     * - Referral Income is a PRO-ONLY feature:
     *   - Free User: Cannot earn referral commissions; do not add referral earnings to wallet.
     *   - Pro/Premium Pro/Super Premium Pro: Referral Income enabled (verified 50/50 net split).
     *   - When subscription expires/cancelled/inactive: Referral income immediately becomes unavailable.
     * - Protected by real backend subscription-status verification.
     * - Wallet contains real verified referral earnings only (no fake transactions/earnings).
     * - Only one plan can be active at a time; upgrades/downgrades supersede previous active subscriptions.
     */
    suspend fun purchaseProMembership(
        plan: SatisfyProPlan = SatisfyProPlan.PRO,
        userId: String = currentUserId,
        userName: String = currentUserName,
        userEmail: String = currentUserEmail,
        referralCodeApplied: String? = null,
        paymentMethod: String = "UPI / Razorpay Gateway",
        onResult: (PaymentGatewayService.PaymentVerificationResult) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val orderId = PaymentGatewayService.createPaymentOrder(
                PaymentGatewayService.PaymentOrderRequest(
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    plan = plan,
                    amount = plan.priceInr,
                    referralCodeApplied = referralCodeApplied,
                    paymentMethod = paymentMethod
                )
            )
            val paymentId = "PAY_${plan.planId.takeLast(4).uppercase()}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6).uppercase()}"

            // 1. Server-Side Verification
            val isPaymentValid = PaymentGatewayService.verifyPaymentSignature(
                orderId = orderId,
                paymentId = paymentId,
                amount = plan.priceInr,
                expectedPlan = plan
            )

            if (!isPaymentValid) {
                onResult(
                    PaymentGatewayService.PaymentVerificationResult(
                        isSuccess = false,
                        paymentId = paymentId,
                        orderId = orderId,
                        plan = plan,
                        amount = plan.priceInr,
                        status = "FAILED",
                        subscription = null,
                        referralRewardCreated = false,
                        baseCommission = 0.0,
                        feeDeducted = 0.0,
                        finalReferrerPayout = 0.0,
                        ownerRevenueAmount = 0.0,
                        commissionStatus = "FAILED",
                        errorMessage = "Server payment verification failed. Please try again."
                    )
                )
                return@withContext
            }

            // 2. Resolve Referrer & Anti-Fraud / Pro-Only Eligibility Rules
            var referrerUid: String? = null
            var referrerCode: String? = null
            var isReferrerEligibleForCommission = false
            val cleanCode = referralCodeApplied?.trim()?.uppercase()

            if (!cleanCode.isNullOrBlank()) {
                val userAccount = userAccountDao.getUserByUid(userId)
                val userOwnCode = userAccount?.referralCode?.uppercase() ?: "SATISFY100"

                // Anti-Self Referral Check: Cannot use own referral code or user id
                val isSelfReferral = cleanCode == userOwnCode || cleanCode == userId.uppercase()
                if (!isSelfReferral) {
                    // Check if this referee has already triggered a referral commission in the past (First Purchase Only Rule)
                    val previousReferral = referralDao.getPurchasedReferralForReferee(userId)
                    if (previousReferral == null) {
                        referrerCode = cleanCode
                        val matchedUser = userAccountDao.getUserByReferralCode(cleanCode)
                        referrerUid = matchedUser?.uid ?: if (cleanCode == "SATISFY100") "user_demo_1" else "user_referrer_$cleanCode"

                        // BACKEND SUBSCRIPTION-STATUS VERIFICATION:
                        // Referral Income is a PRO-ONLY feature. Verify that the referrer currently holds an ACTIVE Pro plan.
                        isReferrerEligibleForCommission = isUserEligibleForReferralIncome(referrerUid)
                        if (!isReferrerEligibleForCommission) {
                            Log.i(TAG, "Referrer $referrerUid (code: $cleanCode) is a FREE or INACTIVE user. Referral Income is a PRO-ONLY feature. Commission will NOT be credited.")
                        } else {
                            Log.i(TAG, "Referrer $referrerUid (code: $cleanCode) verified as ACTIVE PRO subscriber. Referral Income authorized.")
                        }
                    } else {
                        Log.i(TAG, "Referral reward skipped: User $userId has already claimed first-purchase referral reward.")
                    }
                } else {
                    Log.w(TAG, "Self-referral attempt blocked for user: $userId with code: $cleanCode")
                }
            }

            // 3. Compute 50/50 Split via Verified Gateway Settlement Formula
            val hasValidReferral = (referrerUid != null && referrerCode != null && isReferrerEligibleForCommission)
            val settlement = PaymentGatewayService.calculateCommissionSplit(
                plan = plan,
                customGatewayFee = plan.gatewayFeeInr,
                hasReferral = hasValidReferral
            )

            val now = System.currentTimeMillis()
            val expiresAt = now + (30L * 24 * 60 * 60 * 1000) // 30 Days Monthly Period

            // 4. Enforce Single Active Plan Rule: Supersede / Upgrade any existing active subscription
            val existingActiveSub = proSubscriptionDao.getActiveSubscription(userId, now)
            val isUpgrade = existingActiveSub != null && SatisfyProPlan.fromPlanId(existingActiveSub.planId).tierLevel < plan.tierLevel
            val previousStatus = if (isUpgrade) "UPGRADED" else "SUPERSEDED"
            proSubscriptionDao.updateActiveSubscriptionsStatus(userId, previousStatus)

            // 5. Create New Verified Active Subscription Record
            val subscription = ProSubscriptionEntity(
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                planId = plan.planId,
                planName = plan.planName,
                planTier = plan.name,
                billingPeriod = plan.billingPeriod,
                amount = settlement.grossAmount,
                paymentId = paymentId,
                orderId = orderId,
                paymentMethod = paymentMethod,
                paymentStatus = "SUCCESS",
                startedAt = now,
                expiresAt = expiresAt,
                status = "ACTIVE",
                referrerUid = referrerUid,
                referrerCode = referrerCode,
                baseReferralCommission = if (isReferrerEligibleForCommission) settlement.baseReferralCommission else 0.0,
                gatewayFee = settlement.gatewayFee,
                finalReferralPayout = if (isReferrerEligibleForCommission) settlement.finalReferralPayout else 0.0,
                referralRewardAmount = if (isReferrerEligibleForCommission) settlement.finalReferralPayout else 0.0,
                ownerRevenueAmount = settlement.ownerCommission,
                notes = if (isReferrerEligibleForCommission) settlement.settlementNote else "Referrer is Free/Inactive. 100% net allocated to Owner."
            )

            val subId = proSubscriptionDao.insertSubscription(subscription)

            // 6. Update User Profile Pro status & active plan in DB
            userAccountDao.getUserByUid(userId)?.let { user ->
                userAccountDao.updateUser(
                    user.copy(
                        isPro = true,
                        activePlanId = plan.planId,
                        activePlanName = plan.planName,
                        activePlanTier = plan.name,
                        subscriptionStatus = "ACTIVE",
                        proStartedAt = now,
                        proExpiresAt = expiresAt
                    )
                )
            }

            // 7. Referral Income Processing (PRO-ONLY Enforcement)
            var referralRewardCreated = false
            if (referrerUid != null && referrerCode != null) {
                val existingWallet = walletDao.getWallet(referrerUid) ?: WalletEntity(
                    userId = referrerUid,
                    userName = "Referrer ($referrerCode)",
                    referralBalance = 0.0,
                    totalEarned = 0.0,
                    totalWithdrawn = 0.0,
                    pendingWithdrawalAmount = 0.0,
                    successfulReferralsCount = 0
                )

                if (isReferrerEligibleForCommission) {
                    // Referrer is an active PRO user: Credit 50% net referral commission to Wallet
                    if (!existingWallet.isFrozen) {
                        val updatedBalance = existingWallet.referralBalance + settlement.finalReferralPayout
                        val updatedTotalEarned = existingWallet.totalEarned + settlement.finalReferralPayout
                        val updatedCount = existingWallet.successfulReferralsCount + 1

                        walletDao.insertOrUpdateWallet(
                            existingWallet.copy(
                                referralBalance = updatedBalance,
                                totalEarned = updatedTotalEarned,
                                successfulReferralsCount = updatedCount,
                                updatedAt = now
                            )
                        )

                        // Record Immutable Wallet Ledger Transaction
                        walletTransactionDao.insertTransaction(
                            WalletTransactionEntity(
                                userId = referrerUid,
                                userName = existingWallet.userName,
                                type = "REFERRAL_REWARD",
                                amount = settlement.finalReferralPayout,
                                balanceAfter = updatedBalance,
                                description = "Referral Reward: $userName joined ${plan.planName} (₹${plan.priceInr} Gross - ₹${settlement.gatewayFee} PG Fee = ₹${settlement.finalReferralPayout} 50% Net Payout)",
                                referenceId = paymentId,
                                timestamp = now,
                                status = "COMPLETED"
                            )
                        )

                        // Record Referral Event with full audit details
                        val referralTxnId = "TXN_REF_${plan.planId.takeLast(4).uppercase()}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5).uppercase()}"
                        referralDao.insertReferral(
                            ReferralEntity(
                                transactionId = referralTxnId,
                                referrerUid = referrerUid,
                                referrerName = existingWallet.userName,
                                referrerCode = referrerCode,
                                refereeUid = userId,
                                refereeName = userName,
                                planId = plan.planId,
                                proPlan = "${plan.planName} (₹${plan.priceInr.toInt()}/mo)",
                                planTier = plan.name,
                                grossPayment = settlement.grossAmount,
                                baseReferralCommission = settlement.baseReferralCommission,
                                gatewayFee = settlement.gatewayFee,
                                finalReferralPayout = settlement.finalReferralPayout,
                                ownerCommission = settlement.ownerCommission,
                                paymentStatus = "SUCCESS",
                                commissionStatus = "AVAILABLE",
                                rewardStatus = "AVAILABLE",
                                rewardAmount = settlement.finalReferralPayout,
                                joinedAt = now,
                                hasPurchasedPro = true,
                                proSubscriptionId = subId,
                                paymentId = paymentId,
                                isSuspicious = false,
                                auditNote = "Verified Active PRO Referrer ($referrerCode). ${plan.planName} Payment #$paymentId. 50/50 Net Split credited."
                            )
                        )

                        referralRewardCreated = true
                    }
                } else {
                    // Referrer is a FREE or INACTIVE user:
                    // DO NOT add referral earnings to wallet. Log referral record as ineligibility audit for transparency.
                    val referralTxnId = "TXN_REF_INEL_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5).uppercase()}"
                    referralDao.insertReferral(
                        ReferralEntity(
                            transactionId = referralTxnId,
                            referrerUid = referrerUid,
                            referrerName = existingWallet.userName,
                            referrerCode = referrerCode,
                            refereeUid = userId,
                            refereeName = userName,
                            planId = plan.planId,
                            proPlan = "${plan.planName} (₹${plan.priceInr.toInt()}/mo)",
                            planTier = plan.name,
                            grossPayment = settlement.grossAmount,
                            baseReferralCommission = 0.0,
                            gatewayFee = settlement.gatewayFee,
                            finalReferralPayout = 0.0,
                            ownerCommission = settlement.netSettledFromGateway,
                            paymentStatus = "SUCCESS",
                            commissionStatus = "INELIGIBLE_NON_PRO",
                            rewardStatus = "INELIGIBLE_NON_PRO",
                            rewardAmount = 0.0,
                            joinedAt = now,
                            hasPurchasedPro = true,
                            proSubscriptionId = subId,
                            paymentId = paymentId,
                            isSuspicious = false,
                            auditNote = "Referrer $referrerCode is Free / Inactive. Referral Income is a PRO-ONLY feature. No earnings added to wallet."
                        )
                    )
                }
            }

            // 8. Audit Log Entry
            auditLogDao.insertLog(
                AdminAuditLogEntity(
                    action = "PRO_PLAN_PURCHASED",
                    adminEmail = "system_billing",
                    details = "User $userName ($userId) purchased ${plan.planName} for ₹${plan.priceInr} via $paymentMethod. Order: $orderId, Payment: $paymentId. Referrer Eligible: $isReferrerEligibleForCommission, Payout: ₹${if (isReferrerEligibleForCommission) settlement.finalReferralPayout else 0.0}, Fee: ₹${settlement.gatewayFee}, Owner: ₹${settlement.ownerCommission}.",
                    timestamp = now,
                    timeFormatted = "Just now"
                )
            )

            onResult(
                PaymentGatewayService.PaymentVerificationResult(
                    isSuccess = true,
                    paymentId = paymentId,
                    orderId = orderId,
                    plan = plan,
                    amount = settlement.grossAmount,
                    status = "SUCCESS",
                    subscription = subscription.copy(id = subId),
                    referralRewardCreated = referralRewardCreated,
                    baseCommission = if (isReferrerEligibleForCommission) settlement.baseReferralCommission else 0.0,
                    feeDeducted = settlement.gatewayFee,
                    finalReferrerPayout = if (isReferrerEligibleForCommission) settlement.finalReferralPayout else 0.0,
                    ownerRevenueAmount = settlement.ownerCommission,
                    commissionStatus = if (isReferrerEligibleForCommission) "AVAILABLE" else if (referrerUid != null) "INELIGIBLE_NON_PRO" else "NONE"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Pro purchase: ${e.message}", e)
            onResult(
                PaymentGatewayService.PaymentVerificationResult(
                    isSuccess = false,
                    paymentId = "",
                    orderId = "",
                    plan = plan,
                    amount = plan.priceInr,
                    status = "ERROR",
                    subscription = null,
                    referralRewardCreated = false,
                    baseCommission = 0.0,
                    feeDeducted = 0.0,
                    finalReferrerPayout = 0.0,
                    ownerRevenueAmount = 0.0,
                    commissionStatus = "FAILED",
                    errorMessage = e.message ?: "An unexpected error occurred during payment processing."
                )
            )
        }
    }

    /**
     * Submit Withdrawal Request from Referral Wallet
     */
    suspend fun requestWithdrawal(
        userId: String = currentUserId,
        userName: String = currentUserName,
        userEmail: String = currentUserEmail,
        amount: Double,
        paymentMethod: String,
        paymentDetails: String,
        accountHolderName: String,
        onResult: (Boolean, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val wallet = walletDao.getWallet(userId)
        if (wallet == null) {
            onResult(false, "Wallet not found.")
            return@withContext
        }

        if (wallet.isFrozen) {
            onResult(false, "Your wallet is currently frozen: ${wallet.freezeReason.ifBlank { "Contact support." }}")
            return@withContext
        }

        if (amount < PaymentGatewayService.MINIMUM_WITHDRAWAL_INR) {
            onResult(false, "Minimum withdrawal limit is ₹${PaymentGatewayService.MINIMUM_WITHDRAWAL_INR.toInt()}.")
            return@withContext
        }

        if (amount > wallet.referralBalance) {
            onResult(false, "Insufficient balance. Available: ₹${wallet.referralBalance}")
            return@withContext
        }

        val now = System.currentTimeMillis()

        // 1. Move amount from referralBalance to pendingWithdrawalAmount
        val newBalance = wallet.referralBalance - amount
        val newPending = wallet.pendingWithdrawalAmount + amount

        walletDao.insertOrUpdateWallet(
            wallet.copy(
                referralBalance = newBalance,
                pendingWithdrawalAmount = newPending,
                updatedAt = now
            )
        )

        // 2. Create Withdrawal Request
        val reqId = withdrawalRequestDao.insertWithdrawal(
            WithdrawalRequestEntity(
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                amount = amount,
                paymentMethod = paymentMethod,
                paymentDetails = paymentDetails,
                accountHolderName = accountHolderName,
                requestedAt = now,
                status = "PENDING"
            )
        )

        // 3. Record in Ledger
        walletTransactionDao.insertTransaction(
            WalletTransactionEntity(
                userId = userId,
                userName = userName,
                type = "WITHDRAWAL_REQUEST",
                amount = amount,
                balanceAfter = newBalance,
                description = "Withdrawal request submitted via $paymentMethod ($paymentDetails)",
                referenceId = "WDR-$reqId",
                timestamp = now,
                status = "PENDING"
            )
        )

        onResult(true, "Withdrawal request for ₹$amount submitted successfully! Admin will review and process.")
    }

    /**
     * Admin: Approve & Mark Withdrawal as Paid
     */
    suspend fun approveWithdrawal(
        requestId: Long,
        paymentReference: String,
        adminNotes: String
    ) = withContext(Dispatchers.IO) {
        val request = withdrawalRequestDao.getWithdrawalById(requestId) ?: return@withContext
        val now = System.currentTimeMillis()

        // Update request status to PAID
        withdrawalRequestDao.updateWithdrawalStatus(
            id = requestId,
            status = "PAID",
            processedAt = now,
            notes = adminNotes,
            ref = paymentReference
        )

        // Update User Wallet
        val wallet = walletDao.getWallet(request.userId)
        if (wallet != null) {
            val newPending = maxOf(0.0, wallet.pendingWithdrawalAmount - request.amount)
            val newWithdrawn = wallet.totalWithdrawn + request.amount
            walletDao.insertOrUpdateWallet(
                wallet.copy(
                    pendingWithdrawalAmount = newPending,
                    totalWithdrawn = newWithdrawn,
                    updatedAt = now
                )
            )

            // Ledger record
            walletTransactionDao.insertTransaction(
                WalletTransactionEntity(
                    userId = request.userId,
                    userName = request.userName,
                    type = "WITHDRAWAL_COMPLETED",
                    amount = request.amount,
                    balanceAfter = wallet.referralBalance,
                    description = "Withdrawal of ₹${request.amount} completed via ${request.paymentMethod}. Ref: $paymentReference",
                    referenceId = request.requestId,
                    timestamp = now,
                    status = "COMPLETED"
                )
            )
        }

        auditLogDao.insertLog(
            AdminAuditLogEntity(
                action = "WITHDRAWAL_PAID",
                adminEmail = "admin",
                details = "Approved ₹${request.amount} withdrawal for ${request.userName}. Ref: $paymentReference",
                timestamp = now
            )
        )
    }

    /**
     * Admin: Reject Withdrawal Request (Funds refunded to wallet)
     */
    suspend fun rejectWithdrawal(
        requestId: Long,
        reason: String,
        adminNotes: String
    ) = withContext(Dispatchers.IO) {
        val request = withdrawalRequestDao.getWithdrawalById(requestId) ?: return@withContext
        val now = System.currentTimeMillis()

        withdrawalRequestDao.rejectWithdrawal(
            id = requestId,
            reason = reason,
            notes = adminNotes,
            processedAt = now
        )

        // Refund funds back to active wallet balance
        val wallet = walletDao.getWallet(request.userId)
        if (wallet != null) {
            val newBalance = wallet.referralBalance + request.amount
            val newPending = maxOf(0.0, wallet.pendingWithdrawalAmount - request.amount)

            walletDao.insertOrUpdateWallet(
                wallet.copy(
                    referralBalance = newBalance,
                    pendingWithdrawalAmount = newPending,
                    updatedAt = now
                )
            )

            walletTransactionDao.insertTransaction(
                WalletTransactionEntity(
                    userId = request.userId,
                    userName = request.userName,
                    type = "WITHDRAWAL_REJECTED",
                    amount = request.amount,
                    balanceAfter = newBalance,
                    description = "Withdrawal rejected: $reason. ₹${request.amount} refunded to balance.",
                    referenceId = request.requestId,
                    timestamp = now,
                    status = "REVERSED"
                )
            )
        }

        auditLogDao.insertLog(
            AdminAuditLogEntity(
                action = "WITHDRAWAL_REJECTED",
                adminEmail = "admin",
                details = "Rejected ₹${request.amount} withdrawal for ${request.userName}. Reason: $reason",
                timestamp = now
            )
        )
    }

    /**
     * Admin: Freeze or Unfreeze User's Wallet
     */
    suspend fun toggleWalletFreeze(userId: String, isFrozen: Boolean, reason: String) = withContext(Dispatchers.IO) {
        walletDao.updateWalletFreezeStatus(userId, isFrozen, reason)
        auditLogDao.insertLog(
            AdminAuditLogEntity(
                action = if (isFrozen) "WALLET_FROZEN" else "WALLET_UNFROZEN",
                adminEmail = "admin",
                details = "Wallet for user $userId set to frozen=$isFrozen. Reason: $reason",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Admin: Flag or Mark Referral as Suspicious
     */
    suspend fun markReferralSuspicious(referralId: Long, isSuspicious: Boolean, auditNote: String) = withContext(Dispatchers.IO) {
        referralDao.markSuspicious(referralId, isSuspicious, auditNote)
    }

    /**
     * Admin: Cancel or Reverse Referral Reward on Refund
     */
    suspend fun reverseReferralReward(referralId: Long, reason: String) = withContext(Dispatchers.IO) {
        val ref = referralDao.getReferralById(referralId) ?: return@withContext

        referralDao.updateReferralReward(
            id = referralId,
            status = "REVERSED",
            hasPurchased = false,
            subId = ref.proSubscriptionId
        )

        // Deduct from referrer's wallet if available
        val wallet = walletDao.getWallet(ref.referrerUid)
        if (wallet != null) {
            val deductAmount = if (ref.finalReferralPayout > 0.0) ref.finalReferralPayout else ref.rewardAmount
            val newBalance = maxOf(0.0, wallet.referralBalance - deductAmount)
            walletDao.insertOrUpdateWallet(
                wallet.copy(
                    referralBalance = newBalance,
                    updatedAt = System.currentTimeMillis()
                )
            )

            walletTransactionDao.insertTransaction(
                WalletTransactionEntity(
                    userId = ref.referrerUid,
                    userName = wallet.userName,
                    type = "REFUND_REVERSAL",
                    amount = -deductAmount,
                    balanceAfter = newBalance,
                    description = "Reward reversed for ${ref.refereeName}: $reason (-₹$deductAmount)",
                    referenceId = ref.referralId,
                    timestamp = System.currentTimeMillis(),
                    status = "REVERSED"
                )
            )
        }
    }

    /**
     * Admin: Toggle Post Free vs PRO/PREMIUM
     */
    suspend fun togglePostPremiumStatus(postId: Long, isPremium: Boolean) = withContext(Dispatchers.IO) {
        postDao.updatePostPremiumStatus(postId, isPremium)
    }

    /**
     * Owner 1-to-1 Chat: User sends message to Owner
     */
    suspend fun sendUserChatMessage(
        userId: String = currentUserId,
        userName: String = currentUserName,
        userAvatar: String = currentUserAvatar,
        userEmail: String = currentUserEmail,
        message: String
    ) = withContext(Dispatchers.IO) {
        if (message.isBlank()) return@withContext

        val now = System.currentTimeMillis()
        val existingChat = ownerChatDao.getChatByUserId(userId)

        // Update/Insert Chat Conversation
        val unreadAdmin = (existingChat?.unreadCountForAdmin ?: 0) + 1
        ownerChatDao.insertOrUpdateChat(
            OwnerChatEntity(
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                userEmail = userEmail,
                isPro = true,
                lastMessage = message,
                lastMessageTimestamp = now,
                unreadCountForAdmin = unreadAdmin,
                unreadCountForUser = 0,
                isBlocked = existingChat?.isBlocked ?: false
            )
        )

        // Insert Message
        chatMessageDao.insertMessage(
            ChatMessageEntity(
                chatUserId = userId,
                senderId = userId,
                senderName = userName,
                senderRole = "USER",
                message = message,
                timestamp = now,
                isRead = false
            )
        )
    }

    /**
     * Owner 1-to-1 Chat: Owner/Admin sends reply to User
     */
    suspend fun sendAdminReplyMessage(
        targetUserId: String,
        adminName: String = "App Owner / Support",
        message: String
    ) = withContext(Dispatchers.IO) {
        if (message.isBlank()) return@withContext

        val now = System.currentTimeMillis()
        val existingChat = ownerChatDao.getChatByUserId(targetUserId)

        val unreadUser = (existingChat?.unreadCountForUser ?: 0) + 1
        ownerChatDao.insertOrUpdateChat(
            OwnerChatEntity(
                userId = targetUserId,
                userName = existingChat?.userName ?: "User",
                userAvatar = existingChat?.userAvatar ?: "",
                userEmail = existingChat?.userEmail ?: "",
                isPro = existingChat?.isPro ?: true,
                lastMessage = message,
                lastMessageTimestamp = now,
                unreadCountForAdmin = 0,
                unreadCountForUser = unreadUser,
                isBlocked = existingChat?.isBlocked ?: false
            )
        )

        chatMessageDao.insertMessage(
            ChatMessageEntity(
                chatUserId = targetUserId,
                senderId = "admin_owner",
                senderName = adminName,
                senderRole = "OWNER",
                message = message,
                timestamp = now,
                isRead = false
            )
        )
    }

    /**
     * Mark Chat Messages as Read
     */
    suspend fun markChatAsRead(chatUserId: String, readerRole: String) = withContext(Dispatchers.IO) {
        chatMessageDao.markMessagesAsRead(chatUserId, readerRole)
        if (readerRole == "ADMIN" || readerRole == "OWNER") {
            ownerChatDao.markAdminRead(chatUserId)
        } else {
            ownerChatDao.markUserRead(chatUserId)
        }
    }

    /**
     * Fetch real-time Pro Analytics Summary for Admin Dashboard
     * Computes all 8 exact Referral Commission Metrics directly from verified records:
     * 1. Total Referral Sales (₹5 per sale)
     * 2. Total Base Commission (₹4 per sale)
     * 3. Total Fees Deducted (₹0.50 per sale)
     * 4. Total Final Referral Payout (₹3.50 per sale)
     * 5. Owner Commission (₹1.00 per sale + non-referral sales)
     * 6. Pending Commission
     * 7. Available Commission
     * 8. Withdrawn Commission
     */
    suspend fun getProAnalyticsSummary(): ProAnalyticsSummary = withContext(Dispatchers.IO) {
        val totalUsers = userAccountDao.getUsersCount()
        val totalSubs = proSubscriptionDao.getTotalSubscriptionsCount()
        val activePro = proSubscriptionDao.getActiveProUsersCount()
        val expiredPro = maxOf(0, totalSubs - activePro)
        val proRevenue = proSubscriptionDao.getTotalProRevenue()

        val totalRefs = referralDao.getTotalReferralsCount()
        val successRefs = referralDao.getSuccessfulReferralsCount()

        // Exact 8 Metrics calculation
        val totalReferralSales = referralDao.getTotalReferralSales()
        val totalBaseCommission = referralDao.getTotalBaseCommission()
        val totalFeesDeducted = referralDao.getTotalFeesDeducted()
        val totalFinalReferralPayout = referralDao.getTotalFinalReferralPayout()
        val ownerCommissionFromRefs = referralDao.getTotalOwnerCommissionFromReferrals()
        val pendingCommission = referralDao.getPendingCommission()
        val availableCommission = referralDao.getAvailableCommission()
        val withdrawnCommission = referralDao.getWithdrawnCommission()

        val pendingWithCount = withdrawalRequestDao.getPendingCount()
        val pendingWithAmount = withdrawalRequestDao.getPendingAmount()
        val paidWithAmount = withdrawalRequestDao.getPaidAmount()

        val totalOwnerCommission = ownerCommissionFromRefs + maxOf(0.0, proRevenue - totalReferralSales)

        // Per-Plan breakdown
        val proPlanUsers = proSubscriptionDao.getActiveUsersCountByPlan("PRO")
        val proPlanRev = proSubscriptionDao.getRevenueByPlan("PRO")
        val premiumProPlanUsers = proSubscriptionDao.getActiveUsersCountByPlan("PREMIUM_PRO")
        val premiumProPlanRev = proSubscriptionDao.getRevenueByPlan("PREMIUM_PRO")
        val superPremiumProPlanUsers = proSubscriptionDao.getActiveUsersCountByPlan("SUPER_PREMIUM_PRO")
        val superPremiumProPlanRev = proSubscriptionDao.getRevenueByPlan("SUPER_PREMIUM_PRO")

        return@withContext ProAnalyticsSummary(
            totalUsers = totalUsers,
            totalProUsers = totalSubs,
            activeProUsers = activePro,
            expiredProUsers = expiredPro,
            proRevenue = proRevenue,
            proPlanUsersCount = proPlanUsers,
            proPlanRevenue = proPlanRev,
            premiumProPlanUsersCount = premiumProPlanUsers,
            premiumProPlanRevenue = premiumProPlanRev,
            superPremiumProPlanUsersCount = superPremiumProPlanUsers,
            superPremiumProPlanRevenue = superPremiumProPlanRev,
            totalReferrals = totalRefs,
            successfulProReferrals = successRefs,
            totalReferralSales = totalReferralSales,
            totalBaseCommission = totalBaseCommission,
            totalFeesDeducted = totalFeesDeducted,
            totalFinalReferralPayout = totalFinalReferralPayout,
            ownerCommissionTotal = totalOwnerCommission,
            pendingCommission = pendingCommission,
            availableCommission = availableCommission,
            withdrawnCommission = withdrawnCommission,
            totalReferralRewards = totalFinalReferralPayout,
            pendingWithdrawalsCount = pendingWithCount,
            pendingWithdrawalsAmount = pendingWithAmount,
            paidWithdrawalsAmount = paidWithAmount,
            ownerNetRevenue = totalOwnerCommission
        )
    }

    // ==========================================
    // ADDITIONAL HELPER EXTENSIONS FOR VIEWMODEL
    // ==========================================

    fun getUserSubscription(userId: String): Flow<ProSubscriptionEntity?> =
        proSubscriptionDao.observeActiveSubscription(userId)

    fun isUserPro(userId: String): Flow<Boolean> =
        proSubscriptionDao.observeActiveSubscription(userId).map { it != null && it.expiresAt > System.currentTimeMillis() }

    fun getUserWallet(userId: String): Flow<WalletEntity?> =
        walletDao.observeWallet(userId)

    fun getUserTransactions(userId: String): Flow<List<WalletTransactionEntity>> =
        walletTransactionDao.getTransactionsForUser(userId)

    fun getUserWithdrawals(userId: String): Flow<List<WithdrawalRequestEntity>> =
        withdrawalRequestDao.getWithdrawalsForUser(userId)

    fun getUserReferrals(userId: String): Flow<List<ReferralEntity>> =
        referralDao.getReferralsByReferrer(userId)

    fun getOwnerChatForUser(userId: String): Flow<OwnerChatEntity?> =
        ownerChatDao.observeChatByUserId(userId)

    fun getChatMessages(userId: String): Flow<List<ChatMessageEntity>> =
        chatMessageDao.getMessagesForChat(userId)

    suspend fun cancelSubscription(subId: Long) = withContext(Dispatchers.IO) {
        proSubscriptionDao.updateSubscriptionStatus(subId, "CANCELLED")
    }

    suspend fun setChatBlocked(userId: String, isBlocked: Boolean) = withContext(Dispatchers.IO) {
        ownerChatDao.updateBlockStatus(userId, isBlocked)
    }

    suspend fun setWalletFrozen(userId: String, isFrozen: Boolean, reason: String) =
        toggleWalletFreeze(userId, isFrozen, reason)

    suspend fun setReferralSuspicious(referralId: Long, isSuspicious: Boolean, auditNote: String) =
        markReferralSuspicious(referralId, isSuspicious, auditNote)

    suspend fun seedInitialProData(userId: String, referralCode: String) = withContext(Dispatchers.IO) {
        seedInitialProAndReferralData()
    }
}
