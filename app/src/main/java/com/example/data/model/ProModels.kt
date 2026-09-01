package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Satisfy Pro System: 3 Monthly Subscription Plans
 * 1. Pro — ₹5/month (Basic Pro features)
 * 2. Premium Pro — ₹15/month (All Pro features + Additional Premium features)
 * 3. Super Premium Pro — ₹25/month (All Premium Pro features + Highest-level Super Premium features)
 *
 * Revenue Split Architecture:
 * - Net Amount = Gross Plan Price - Applicable Payment Gateway Fee
 * - With Verified Referrer: 50% to App Owner & 50% to Verified Referrer
 * - Without Referrer (Direct): 100% Net Amount to App Owner
 */
enum class SatisfyProPlan(
    val planId: String,
    val planName: String,
    val priceInr: Double,
    val billingPeriod: String = "1 Month (30 Days)",
    val tierLevel: Int,
    val tagLine: String,
    val badgeLabel: String,
    val gatewayFeeInr: Double,
    val features: List<String>
) {
    PRO(
        planId = "plan_pro_5",
        planName = "Pro",
        priceInr = 5.0,
        billingPeriod = "1 Month (30 Days)",
        tierLevel = 1,
        tagLine = "Basic Pro Experience",
        badgeLabel = "PRO",
        gatewayFeeInr = 0.50,
        features = listOf(
            "PRO Verified Gold Badge on profile & video comments",
            "Full 1080p Full HD crystal streaming & zero banner ads",
            "Basic Referral Program: Earn 50% net share (₹2.25/invite)",
            "Standard creator upload allowance & basic video analytics",
            "Exclusive community supporter badge"
        )
    ),
    PREMIUM_PRO(
        planId = "plan_premium_pro_15",
        planName = "Premium Pro",
        priceInr = 15.0,
        billingPeriod = "1 Month (30 Days)",
        tierLevel = 2,
        tagLine = "Enhanced Creator & Priority Perks",
        badgeLabel = "PREMIUM PRO",
        gatewayFeeInr = 1.00,
        features = listOf(
            "All Basic Pro features included",
            "1-on-1 Direct VIP Priority Chat with App Owner & support",
            "Ultra 4K 60fps streaming & high-bitrate spatial sound",
            "Fast-track video monetization & priority verification queue",
            "Custom animated channel banner & VIP Creator badge",
            "Enhanced Referral Share: Earn ₹7.00 per Premium Pro referral"
        )
    ),
    SUPER_PREMIUM_PRO(
        planId = "plan_super_premium_pro_25",
        planName = "Super Premium Pro",
        priceInr = 25.0,
        billingPeriod = "1 Month (30 Days)",
        tierLevel = 3,
        tagLine = "The Ultimate Elite VIP & Creator Experience",
        badgeLabel = "SUPER PRO VIP",
        gatewayFeeInr = 1.50,
        features = listOf(
            "All Premium Pro features included",
            "VIP Diamond Super PRO Badge & Verified Star on channel",
            "Dedicated 24/7 VIP Instant Owner Hotline & priority support",
            "Priority Homepage algorithm boost for all uploaded videos & shorts",
            "Unlimited 4K HDR master uploads with zero bitrate compression",
            "Maximum Referral Earnings: Earn ₹11.75 per Super Pro invite",
            "Exclusive early access to Satisfy AI creator studio & beta tools"
        )
    );

    val netAmount: Double get() = priceInr - gatewayFeeInr
    val referrerPayout: Double get() = (priceInr - gatewayFeeInr) * 0.50
    val ownerRevenue: Double get() = (priceInr - gatewayFeeInr) * 0.50

    companion object {
        fun fromPlanId(id: String?): SatisfyProPlan {
            return entries.firstOrNull { it.planId.equals(id, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
                ?: entries.firstOrNull { it.planName.equals(id, ignoreCase = true) }
                ?: PRO
        }

        fun fromPrice(price: Double): SatisfyProPlan {
            return entries.firstOrNull { it.priceInr == price } ?: PRO
        }
    }
}

@Entity(tableName = "pro_subscriptions")
data class ProSubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subscriptionId: String = "SUB-" + UUID.randomUUID().toString().take(8).uppercase(),
    val userId: String,
    val userName: String,
    val userEmail: String = "",
    val planId: String = "plan_pro_5",
    val planName: String = "Pro",
    val planTier: String = "PRO", // "PRO", "PREMIUM_PRO", "SUPER_PREMIUM_PRO"
    val billingPeriod: String = "1 Month (30 Days)",
    val amount: Double = 5.0,
    val paymentId: String = "PAY-" + UUID.randomUUID().toString().take(10).uppercase(),
    val orderId: String = "ORD-" + UUID.randomUUID().toString().take(10).uppercase(),
    val paymentMethod: String = "UPI / Razorpay Gateway",
    val paymentStatus: String = "SUCCESS", // "SUCCESS", "FAILED", "REFUNDED"
    val startedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 days
    val status: String = "ACTIVE", // "ACTIVE", "UPGRADED", "SUPERSEDED", "EXPIRED", "CANCELLED", "REFUNDED"
    val referrerUid: String? = null,
    val referrerCode: String? = null,
    val baseReferralCommission: Double = 4.50,
    val gatewayFee: Double = 0.50,
    val finalReferralPayout: Double = 2.25,
    val referralRewardAmount: Double = 2.25, // compatibility alias
    val ownerRevenueAmount: Double = 2.25,
    val notes: String = ""
)

@Entity(tableName = "referrals")
data class ReferralEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: String = "TXN-" + UUID.randomUUID().toString().take(10).uppercase(),
    val referralId: String = "REF-" + UUID.randomUUID().toString().take(8).uppercase(),
    val referrerUid: String,
    val referrerName: String,
    val referrerCode: String,
    val refereeUid: String,
    val refereeName: String,
    val planId: String = "plan_pro_5",
    val proPlan: String = "Pro (₹5/month)",
    val planTier: String = "PRO",
    val grossPayment: Double = 5.0,
    val baseReferralCommission: Double = 4.50,
    val gatewayFee: Double = 0.50,
    val finalReferralPayout: Double = 2.25,
    val ownerCommission: Double = 2.25,
    val paymentStatus: String = "SUCCESS", // "SUCCESS", "FAILED", "REFUNDED"
    val commissionStatus: String = "AVAILABLE", // "PENDING", "AVAILABLE", "WITHDRAWN", "REVERSED", "CANCELLED"
    val rewardStatus: String = "AVAILABLE", // compatibility alias
    val rewardAmount: Double = 2.25, // compatibility alias (= finalReferralPayout)
    val joinedAt: Long = System.currentTimeMillis(),
    val hasPurchasedPro: Boolean = true,
    val proSubscriptionId: Long? = null,
    val paymentId: String = "",
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
    val proPlanTier: String = "PRO", // "PRO", "PREMIUM_PRO", "SUPER_PREMIUM_PRO"
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
    val isDelivered: Boolean = true,
    val isRead: Boolean = false,
    val deliveredTimestamp: Long = System.currentTimeMillis(),
    val readTimestamp: Long = 0L
)

data class ProAnalyticsSummary(
    val totalUsers: Int = 0,
    val totalProUsers: Int = 0,
    val activeProUsers: Int = 0,
    val expiredProUsers: Int = 0,
    val proRevenue: Double = 0.0,
    // Per-Plan breakdown
    val proPlanUsersCount: Int = 0,
    val proPlanRevenue: Double = 0.0,
    val premiumProPlanUsersCount: Int = 0,
    val premiumProPlanRevenue: Double = 0.0,
    val superPremiumProPlanUsersCount: Int = 0,
    val superPremiumProPlanRevenue: Double = 0.0,
    val totalReferrals: Int = 0,
    val successfulProReferrals: Int = 0,
    // 8 Referral Commission Metrics for Admin Dashboard
    val totalReferralSales: Double = 0.0,
    val totalBaseCommission: Double = 0.0,
    val totalFeesDeducted: Double = 0.0,
    val totalFinalReferralPayout: Double = 0.0,
    val ownerCommissionTotal: Double = 0.0,
    val pendingCommission: Double = 0.0,
    val availableCommission: Double = 0.0,
    val withdrawnCommission: Double = 0.0,
    // Summary helpers
    val totalReferralRewards: Double = 0.0,
    val pendingWithdrawalsCount: Int = 0,
    val pendingWithdrawalsAmount: Double = 0.0,
    val paidWithdrawalsAmount: Double = 0.0,
    val ownerNetRevenue: Double = 0.0
)


