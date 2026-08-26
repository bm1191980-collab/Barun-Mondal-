package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pro_subscriptions")
data class ProSubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subscriptionId: String = "SUB-" + UUID.randomUUID().toString().take(8).uppercase(),
    val userId: String,
    val userName: String,
    val userEmail: String = "",
    val planName: String = "Pro Membership (Monthly)",
    val amount: Double = 5.0,
    val paymentId: String = "PAY-" + UUID.randomUUID().toString().take(10).uppercase(),
    val orderId: String = "ORD-" + UUID.randomUUID().toString().take(10).uppercase(),
    val paymentMethod: String = "UPI / Razorpay Gateway",
    val paymentStatus: String = "SUCCESS", // "SUCCESS", "FAILED", "REFUNDED"
    val startedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 days
    val status: String = "ACTIVE", // "ACTIVE", "EXPIRED", "CANCELLED", "REFUNDED"
    val referrerUid: String? = null,
    val referrerCode: String? = null,
    val referralRewardAmount: Double = 4.0,
    val ownerRevenueAmount: Double = 1.0,
    val notes: String = "₹5 Paid → ₹4 Referrer Reward + ₹1 Owner Revenue"
)

@Entity(tableName = "referrals")
data class ReferralEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val referralId: String = "REF-" + UUID.randomUUID().toString().take(8).uppercase(),
    val referrerUid: String,
    val referrerName: String,
    val referrerCode: String,
    val refereeUid: String,
    val refereeName: String,
    val joinedAt: Long = System.currentTimeMillis(),
    val hasPurchasedPro: Boolean = false,
    val rewardStatus: String = "PENDING", // "PENDING", "CREDITED", "REVERSED", "FROZEN"
    val rewardAmount: Double = 4.0,
    val proSubscriptionId: Long? = null,
    val isSuspicious: Boolean = false,
    val auditNote: String = ""
)

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey
    val userId: String,
    val userName: String,
    val referralBalance: Double = 0.0,
    val totalEarned: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val pendingWithdrawalAmount: Double = 0.0,
    val successfulReferralsCount: Int = 0,
    val isFrozen: Boolean = false,
    val freezeReason: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: String = "TXN-" + UUID.randomUUID().toString().take(10).uppercase(),
    val userId: String,
    val userName: String,
    val type: String, // "REFERRAL_REWARD", "WITHDRAWAL_REQUEST", "WITHDRAWAL_COMPLETED", "WITHDRAWAL_REJECTED", "REFUND_REVERSAL", "ADMIN_ADJUSTMENT"
    val amount: Double,
    val balanceAfter: Double,
    val description: String,
    val referenceId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED" // "COMPLETED", "PENDING", "FAILED", "REVERSED"
)

@Entity(tableName = "withdrawal_requests")
data class WithdrawalRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requestId: String = "WDR-" + UUID.randomUUID().toString().take(8).uppercase(),
    val userId: String,
    val userName: String,
    val userEmail: String = "",
    val amount: Double,
    val paymentMethod: String = "UPI", // "UPI", "Bank Transfer", "Paytm"
    val paymentDetails: String, // e.g. "UPI: satisfy.user@oksbi"
    val accountHolderName: String,
    val requestedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // "PENDING", "APPROVED", "PAID", "REJECTED"
    val processedAt: Long? = null,
    val adminNotes: String = "",
    val rejectionReason: String = "",
    val paymentReference: String = ""
)

@Entity(tableName = "owner_chats")
data class OwnerChatEntity(
    @PrimaryKey
    val userId: String,
    val userName: String,
    val userAvatar: String = "",
    val userEmail: String = "",
    val isPro: Boolean = true,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCountForAdmin: Int = 0,
    val unreadCountForUser: Int = 0,
    val isBlocked: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatUserId: String, // foreign conversation key (user's uid)
    val senderId: String,   // "user_creator" or "admin_owner"
    val senderName: String,
    val senderRole: String, // "USER" or "OWNER"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class ProAnalyticsSummary(
    val totalUsers: Int = 0,
    val totalProUsers: Int = 0,
    val activeProUsers: Int = 0,
    val expiredProUsers: Int = 0,
    val proRevenue: Double = 0.0,
    val totalReferrals: Int = 0,
    val successfulProReferrals: Int = 0,
    val totalReferralRewards: Double = 0.0,
    val pendingWithdrawalsCount: Int = 0,
    val pendingWithdrawalsAmount: Double = 0.0,
    val paidWithdrawalsAmount: Double = 0.0,
    val ownerNetRevenue: Double = 0.0
)
