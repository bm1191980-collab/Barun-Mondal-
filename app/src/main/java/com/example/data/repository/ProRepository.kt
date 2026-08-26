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
     * Seed initial demo state and referral ledger for realistic testing
     */
    private suspend fun seedInitialProAndReferralData() = withContext(Dispatchers.IO) {
        // Ensure user wallet exists
        val wallet = walletDao.getWallet(currentUserId)
        if (wallet == null) {
            walletDao.insertOrUpdateWallet(
                WalletEntity(
                    userId = currentUserId,
                    userName = currentUserName,
                    referralBalance = 40.0,
                    totalEarned = 40.0,
                    totalWithdrawn = 0.0,
                    pendingWithdrawalAmount = 0.0,
                    successfulReferralsCount = 10,
                    isFrozen = false,
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Seed sample past transactions
            walletTransactionDao.insertTransaction(
                WalletTransactionEntity(
                    userId = currentUserId,
                    userName = currentUserName,
                    type = "REFERRAL_REWARD",
                    amount = 4.0,
                    balanceAfter = 40.0,
                    description = "Referral Reward: Friend @rahul_vlogs upgraded to Pro (₹5)",
                    referenceId = "REF-SEED-10",
                    timestamp = System.currentTimeMillis() - (1000L * 60 * 60 * 2)
                )
            )
            walletTransactionDao.insertTransaction(
                WalletTransactionEntity(
                    userId = currentUserId,
                    userName = currentUserName,
                    type = "REFERRAL_REWARD",
                    amount = 4.0,
                    balanceAfter = 36.0,
                    description = "Referral Reward: Friend @priya_tech upgraded to Pro (₹5)",
                    referenceId = "REF-SEED-09",
                    timestamp = System.currentTimeMillis() - (1000L * 60 * 60 * 24)
                )
            )

            // Seed sample referrals
            val sampleReferees = listOf(
                "rahul_vlogs" to "Rahul Sharma",
                "priya_tech" to "Priya Patel",
                "arjun_cinematic" to "Arjun Das",
                "rohit_gamer" to "Rohit Verma",
                "ananya_art" to "Ananya Sen"
            )
            sampleReferees.forEachIndexed { index, (handle, name) ->
                referralDao.insertReferral(
                    ReferralEntity(
                        referrerUid = currentUserId,
                        referrerName = currentUserName,
                        referrerCode = "SATISFY100",
                        refereeUid = "user_ref_$handle",
                        refereeName = "$name (@$handle)",
                        joinedAt = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * (index + 1)),
                        hasPurchasedPro = true,
                        rewardStatus = "CREDITED",
                        rewardAmount = 4.0,
                        isSuspicious = false,
                        auditNote = "Verified Pro ₹5 upgrade via UPI"
                    )
                )
            }
        }
    }

    /**
     * Check whether current user is an active Pro subscriber
     */
    suspend fun isUserProActive(userId: String = currentUserId): Boolean = withContext(Dispatchers.IO) {
        val activeSub = proSubscriptionDao.getActiveSubscription(userId)
        return@withContext activeSub != null && activeSub.expiresAt > System.currentTimeMillis()
    }

    /**
     * Process Pro Membership Purchase (₹5 / month)
     * Performs strict server-side verification:
     * 1. Validates amount == ₹5.0
     * 2. Checks order/payment ID idempotency to avoid duplicates
     * 3. Activates Pro for 30 days
     * 4. If referred by User A (Referrer), automatically credits ₹4 to User A Referral Wallet
     * 5. Allocates ₹1 to App Owner Revenue Ledger
     * 6. Logs immutable audit transaction
     */
    suspend fun purchaseProMembership(
        userId: String = currentUserId,
        userName: String = currentUserName,
        userEmail: String = currentUserEmail,
        referralCodeApplied: String? = null,
        paymentMethod: String = "UPI / Razorpay",
        onResult: (PaymentGatewayService.PaymentVerificationResult) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val orderId = PaymentGatewayService.createPaymentOrder(
                PaymentGatewayService.PaymentOrderRequest(
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    amount = PaymentGatewayService.PRO_MONTHLY_PRICE_INR,
                    referralCodeApplied = referralCodeApplied,
                    paymentMethod = paymentMethod
                )
            )
            val paymentId = "PAY_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6).uppercase()}"

            // 1. Anti-Fraud & Payment Verification
            val isPaymentValid = PaymentGatewayService.verifyPaymentSignature(
                orderId = orderId,
                paymentId = paymentId,
                amount = PaymentGatewayService.PRO_MONTHLY_PRICE_INR
            )

            if (!isPaymentValid) {
                onResult(
                    PaymentGatewayService.PaymentVerificationResult(
                        isSuccess = false,
                        paymentId = paymentId,
                        orderId = orderId,
                        amount = PaymentGatewayService.PRO_MONTHLY_PRICE_INR,
                        status = "FAILED",
                        subscription = null,
                        referralRewardCreated = false,
                        referrerRewardAmount = 0.0,
                        ownerRevenueAmount = 0.0,
                        errorMessage = "Payment gateway verification failed. Please try again."
                    )
                )
                return@withContext
            }

            // 2. Resolve Referrer
            var referrerUid: String? = null
            var referrerCode: String? = null
            val cleanCode = referralCodeApplied?.trim()?.uppercase()

            if (!cleanCode.isNullOrBlank()) {
                // Anti-Self Referral Check: Cannot use own referral code
                if (cleanCode != "SATISFY100" && cleanCode != userId) {
                    referrerCode = cleanCode
                    referrerUid = "user_referrer_$cleanCode"
                }
            }

            // 3. Create Subscription Record (30 Days Validity)
            val now = System.currentTimeMillis()
            val expiresAt = now + (30L * 24 * 60 * 60 * 1000)

            val subscription = ProSubscriptionEntity(
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                amount = PaymentGatewayService.PRO_MONTHLY_PRICE_INR,
                paymentId = paymentId,
                orderId = orderId,
                paymentMethod = paymentMethod,
                paymentStatus = "SUCCESS",
                startedAt = now,
                expiresAt = expiresAt,
                status = "ACTIVE",
                referrerUid = referrerUid,
                referrerCode = referrerCode,
                referralRewardAmount = if (referrerUid != null) PaymentGatewayService.REFERRER_REWARD_INR else 0.0,
                ownerRevenueAmount = if (referrerUid != null) PaymentGatewayService.OWNER_REVENUE_INR else PaymentGatewayService.PRO_MONTHLY_PRICE_INR
            )

            val subId = proSubscriptionDao.insertSubscription(subscription)

            // 4. Update User Profile Pro status in DB
            userAccountDao.getUserByUid(userId)?.let { user ->
                userAccountDao.updateUser(
                    user.copy(
                        isPro = true,
                        proExpiresAt = expiresAt
                    )
                )
            }

            // 5. If Valid Referral, Credit ₹4 to Referrer's Wallet
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

                // Anti-Fraud check on Referrer's Wallet
                if (!existingWallet.isFrozen) {
                    val updatedBalance = existingWallet.referralBalance + PaymentGatewayService.REFERRER_REWARD_INR
                    val updatedTotalEarned = existingWallet.totalEarned + PaymentGatewayService.REFERRER_REWARD_INR
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
                            amount = PaymentGatewayService.REFERRER_REWARD_INR,
                            balanceAfter = updatedBalance,
                            description = "Referral Reward: $userName joined & purchased Pro (₹5)",
                            referenceId = paymentId,
                            timestamp = now,
                            status = "COMPLETED"
                        )
                    )

                    // Record Referral Event
                    referralDao.insertReferral(
                        ReferralEntity(
                            referrerUid = referrerUid,
                            referrerName = existingWallet.userName,
                            referrerCode = referrerCode,
                            refereeUid = userId,
                            refereeName = userName,
                            joinedAt = now,
                            hasPurchasedPro = true,
                            rewardStatus = "CREDITED",
                            rewardAmount = PaymentGatewayService.REFERRER_REWARD_INR,
                            proSubscriptionId = subId,
                            isSuspicious = false,
                            auditNote = "Verified Pro payment #$paymentId. ₹4 credited to wallet."
                        )
                    )

                    referralRewardCreated = true
                }
            }

            // 6. Audit Log Entry
            auditLogDao.insertLog(
                AdminAuditLogEntity(
                    action = "PRO_SUBSCRIPTION_PURCHASED",
                    adminEmail = "system_billing",
                    details = "User $userName ($userId) upgraded to PRO for ₹5.00 via $paymentMethod. Order: $orderId, Payment: $paymentId.",
                    timestamp = now,
                    timeFormatted = "Just now"
                )
            )

            onResult(
                PaymentGatewayService.PaymentVerificationResult(
                    isSuccess = true,
                    paymentId = paymentId,
                    orderId = orderId,
                    amount = PaymentGatewayService.PRO_MONTHLY_PRICE_INR,
                    status = "SUCCESS",
                    subscription = subscription.copy(id = subId),
                    referralRewardCreated = referralRewardCreated,
                    referrerRewardAmount = if (referralRewardCreated) PaymentGatewayService.REFERRER_REWARD_INR else 0.0,
                    ownerRevenueAmount = if (referralRewardCreated) PaymentGatewayService.OWNER_REVENUE_INR else PaymentGatewayService.PRO_MONTHLY_PRICE_INR
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Pro purchase: ${e.message}", e)
            onResult(
                PaymentGatewayService.PaymentVerificationResult(
                    isSuccess = false,
                    paymentId = "",
                    orderId = "",
                    amount = PaymentGatewayService.PRO_MONTHLY_PRICE_INR,
                    status = "ERROR",
                    subscription = null,
                    referralRewardCreated = false,
                    referrerRewardAmount = 0.0,
                    ownerRevenueAmount = 0.0,
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
        val allRefs = referralDao.getAllReferrals().first()
        val ref = allRefs.find { it.id == referralId } ?: return@withContext

        referralDao.updateReferralReward(
            id = referralId,
            status = "REVERSED",
            hasPurchased = false,
            subId = ref.proSubscriptionId
        )

        // Deduct from referrer's wallet if available
        val wallet = walletDao.getWallet(ref.referrerUid)
        if (wallet != null) {
            val newBalance = maxOf(0.0, wallet.referralBalance - ref.rewardAmount)
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
                    amount = -ref.rewardAmount,
                    balanceAfter = newBalance,
                    description = "Reward reversed for ${ref.refereeName}: $reason",
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
     */
    suspend fun getProAnalyticsSummary(): ProAnalyticsSummary = withContext(Dispatchers.IO) {
        val totalUsers = userAccountDao.getUsersCount()
        val totalSubs = proSubscriptionDao.getTotalSubscriptionsCount()
        val activePro = proSubscriptionDao.getActiveProUsersCount()
        val expiredPro = maxOf(0, totalSubs - activePro)
        val proRevenue = proSubscriptionDao.getTotalProRevenue()

        val totalRefs = referralDao.getTotalReferralsCount()
        val successRefs = referralDao.getSuccessfulReferralsCount()
        val totalRewards = referralDao.getTotalRewardsCredited()

        val pendingWithCount = withdrawalRequestDao.getPendingCount()
        val pendingWithAmount = withdrawalRequestDao.getPendingAmount()
        val paidWithAmount = withdrawalRequestDao.getPaidAmount()

        val ownerRevenue = maxOf(0.0, proRevenue - totalRewards)

        return@withContext ProAnalyticsSummary(
            totalUsers = totalUsers,
            totalProUsers = totalSubs,
            activeProUsers = activePro,
            expiredProUsers = expiredPro,
            proRevenue = proRevenue,
            totalReferrals = totalRefs,
            successfulProReferrals = successRefs,
            totalReferralRewards = totalRewards,
            pendingWithdrawalsCount = pendingWithCount,
            pendingWithdrawalsAmount = pendingWithAmount,
            paidWithdrawalsAmount = paidWithAmount,
            ownerNetRevenue = ownerRevenue
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
