package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminProTabContent(
    subscriptions: List<ProSubscriptionEntity>,
    onCancelSubscription: (Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val totalRevenue = remember(subscriptions) { subscriptions.filter { it.paymentStatus == "SUCCESS" }.sumOf { it.amount } }
    val activeCount = remember(subscriptions) { subscriptions.count { it.status == "ACTIVE" && it.expiresAt > System.currentTimeMillis() } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("PRO Revenue", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${totalRevenue.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SatisfyGold)
                            Text("Across all 3 Plans", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Active Subscribers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$activeCount", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                            Text("Total Subs: ${subscriptions.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 3 Plans Breakdown Row
                val proCount = remember(subscriptions) { subscriptions.count { it.planId == "plan_pro_5" || it.planTier == "PRO" } }
                val premiumCount = remember(subscriptions) { subscriptions.count { it.planId == "plan_premium_pro_15" || it.planTier == "PREMIUM_PRO" } }
                val superPremiumCount = remember(subscriptions) { subscriptions.count { it.planId == "plan_super_premium_pro_25" || it.planTier == "SUPER_PREMIUM_PRO" } }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Pro (₹5)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                            Text("$proCount active", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Premium Pro (₹15)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SatisfyGold)
                            Text("$premiumCount active", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Super Premium (₹25)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF472B6))
                            Text("$superPremiumCount active", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "PRO SUBSCRIPTIONS LEDGER",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (subscriptions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No Pro subscriptions purchased yet.",
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(subscriptions) { sub ->
            val isActive = sub.status == "ACTIVE" && sub.expiresAt > System.currentTimeMillis()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = when (sub.planTier) {
                                    "SUPER_PREMIUM_PRO" -> Color(0xFFF472B6)
                                    "PREMIUM_PRO" -> SatisfyGold
                                    else -> Color(0xFF60A5FA)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = sub.userName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${sub.planName} (${sub.planId})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SatisfyGold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isActive) Color(0xFF065F46) else Color(0xFF7F1D1D)
                        ) {
                            Text(
                                text = if (isActive) "ACTIVE" else sub.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Amount: ₹${sub.amount.toInt()} / ${sub.billingPeriod} • Payment: ${sub.paymentId} • Order: ${sub.orderId}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Started: ${dateFormat.format(Date(sub.startedAt))} | Expires: ${dateFormat.format(Date(sub.expiresAt))}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!sub.referrerCode.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E1B4B),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Referred by: ${sub.referrerCode} (₹${String.format(Locale.US, "%.2f", sub.finalReferralPayout)} Referrer 50% Net | ₹${String.format(Locale.US, "%.2f", sub.gatewayFee)} PG Fee | ₹${String.format(Locale.US, "%.2f", sub.ownerRevenueAmount)} Owner 50% Net)",
                                fontSize = 11.sp,
                                color = Color(0xFFA5B4FC),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminReferralsTabContent(
    referrals: List<ReferralEntity>,
    onToggleSuspicious: (Long, Boolean, String) -> Unit,
    onReverseReward: (Long, String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    // 8 Required Admin Dashboard Metrics
    val successfulReferrals = remember(referrals) { referrals.filter { it.hasPurchasedPro && it.commissionStatus != "REVERSED" } }
    val totalReferralSales = remember(successfulReferrals) { successfulReferrals.sumOf { it.grossPayment } }
    val totalBaseCommission = remember(successfulReferrals) { successfulReferrals.sumOf { it.baseReferralCommission } }
    val totalFeesDeducted = remember(successfulReferrals) { successfulReferrals.sumOf { it.gatewayFee } }
    val totalFinalReferralPayout = remember(referrals) { referrals.filter { it.commissionStatus == "AVAILABLE" || it.commissionStatus == "WITHDRAWN" }.sumOf { it.finalReferralPayout } }
    val ownerCommissionTotal = remember(successfulReferrals) { successfulReferrals.sumOf { it.ownerCommission } }
    val pendingCommission = remember(referrals) { referrals.filter { it.commissionStatus == "PENDING" }.sumOf { it.finalReferralPayout } }
    val availableCommission = remember(referrals) { referrals.filter { it.commissionStatus == "AVAILABLE" }.sumOf { it.finalReferralPayout } }
    val withdrawnCommission = remember(referrals) { referrals.filter { it.commissionStatus == "WITHDRAWN" }.sumOf { it.finalReferralPayout } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top 8 Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PRO REFERRAL COMMISSION & REVENUE ANALYTICS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                // Row 1: Sales & Base Commission
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminMetricTile(
                        title = "Total Referral Sales",
                        value = "₹${String.format(Locale.US, "%.2f", totalReferralSales)}",
                        subtext = "${successfulReferrals.size} sales × ₹5.00",
                        accentColor = Color(0xFF6366F1),
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricTile(
                        title = "Total Base Commission",
                        value = "₹${String.format(Locale.US, "%.2f", totalBaseCommission)}",
                        subtext = "₹4.00 per eligible sale",
                        accentColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Fees Deducted & Final Referral Payout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminMetricTile(
                        title = "Total Fees Deducted",
                        value = "-₹${String.format(Locale.US, "%.2f", totalFeesDeducted)}",
                        subtext = "₹0.50 PG fee / referral",
                        accentColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricTile(
                        title = "Total Final Referral Payout",
                        value = "₹${String.format(Locale.US, "%.2f", totalFinalReferralPayout)}",
                        subtext = "₹4.00 - ₹0.50 = ₹3.50 net",
                        accentColor = SatisfyGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Owner Commission & Available Commission
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminMetricTile(
                        title = "Owner Commission",
                        value = "₹${String.format(Locale.US, "%.2f", ownerCommissionTotal)}",
                        subtext = "₹1.00 (Protected)",
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricTile(
                        title = "Available Commission",
                        value = "₹${String.format(Locale.US, "%.2f", availableCommission)}",
                        subtext = "In referrers' wallets",
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: Pending & Withdrawn
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminMetricTile(
                        title = "Pending Commission",
                        value = "₹${String.format(Locale.US, "%.2f", pendingCommission)}",
                        subtext = "Cooling / Unsettled",
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricTile(
                        title = "Withdrawn Commission",
                        value = "₹${String.format(Locale.US, "%.2f", withdrawnCommission)}",
                        subtext = "Paid out to users",
                        accentColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REFERRAL TRANSACTIONS AUDIT TRAIL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${referrals.size} Records",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (referrals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No referrals registered yet.",
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(referrals) { ref ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                border = if (ref.isSuspicious) BorderStroke(1.dp, Color(0xFFEF4444)) else null
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Header: Txn ID & Status Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = ref.transactionId,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = ref.proPlan,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Payment Status
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (ref.paymentStatus == "SUCCESS") Color(0xFF065F46) else Color(0xFF7F1D1D)
                            ) {
                                Text(
                                    text = ref.paymentStatus,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            // Commission Status
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (ref.commissionStatus) {
                                    "AVAILABLE" -> Color(0xFF047857)
                                    "WITHDRAWN" -> Color(0xFF6D28D9)
                                    "REVERSED" -> Color(0xFF991B1B)
                                    "PENDING" -> Color(0xFFB45309)
                                    else -> Color(0xFF374151)
                                }
                            ) {
                                Text(
                                    text = ref.commissionStatus,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // User Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Referrer", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${ref.referrerName} (${ref.referrerCode})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("ID: ${ref.referrerUid}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Referred User (Referee)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = ref.refereeName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("ID: ${ref.refereeUid}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Financial Ledger Breakdown Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gross Payment:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format(Locale.US, "%.2f", ref.grossPayment)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Referrer Base Commission:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format(Locale.US, "%.2f", ref.baseReferralCommission)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Applicable PG / Platform Fee:", fontSize = 11.sp, color = Color(0xFFEF4444))
                                Text("-₹${String.format(Locale.US, "%.2f", ref.gatewayFee)}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Referrer Final Payout:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SatisfyGold)
                                Text("₹${String.format(Locale.US, "%.2f", ref.finalReferralPayout)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SatisfyGold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("App Owner Commission:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                Text("₹${String.format(Locale.US, "%.2f", ref.ownerCommission)} (Protected)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }

                    // Audit Info & Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFormat.format(Date(ref.joinedAt)),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (ref.paymentId.isNotBlank()) {
                            Text(
                                text = "Ref: ${ref.paymentId}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (ref.auditNote.isNotBlank()) {
                        Text(
                            text = "Note: ${ref.auditNote}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (ref.commissionStatus == "AVAILABLE") {
                            TextButton(
                                onClick = { onReverseReward(ref.id, "Admin manual referral refund/reversal") },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                            ) {
                                Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reverse Commission", fontSize = 11.sp)
                            }
                        }

                        TextButton(
                            onClick = { onToggleSuspicious(ref.id, !ref.isSuspicious, if (!ref.isSuspicious) "Suspicious referral pattern flagged by admin" else "") },
                            colors = ButtonDefaults.textButtonColors(contentColor = if (ref.isSuspicious) Color(0xFF10B981) else Color(0xFFF59E0B))
                        ) {
                            Icon(if (ref.isSuspicious) Icons.Filled.Check else Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (ref.isSuspicious) "Clear Flag" else "Flag Suspicious", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricTile(
    title: String,
    value: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = subtext,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminWithdrawalsTabContent(
    withdrawals: List<WithdrawalRequestEntity>,
    onApproveWithdrawal: (Long, String, String) -> Unit,
    onRejectWithdrawal: (Long, String, String) -> Unit,
    onToggleFreezeWallet: (String, Boolean, String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredList = remember(withdrawals, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> withdrawals.filter { it.status == "PENDING" }
            "PAID" -> withdrawals.filter { it.status == "PAID" }
            "REJECTED" -> withdrawals.filter { it.status == "REJECTED" }
            else -> withdrawals
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "PENDING", "PAID", "REJECTED").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No withdrawals found in this category.",
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(filteredList) { req ->
            var showApproveDialog by remember { mutableStateOf(false) }
            var showRejectDialog by remember { mutableStateOf(false) }
            var paymentRefInput by remember { mutableStateOf("UPI-TXN-${UUID.randomUUID().toString().take(8).uppercase()}") }
            var rejectionReasonInput by remember { mutableStateOf("Invalid payment details / suspicious activity") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = req.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Req ID: ${req.requestId} • ${dateFormat.format(Date(req.requestedAt))}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "₹${req.amount.toInt()}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = when (req.status) {
                                "PAID" -> Color(0xFF10B981)
                                "REJECTED" -> Color(0xFFEF4444)
                                else -> SatisfyGold
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "Method: ${req.paymentMethod} • Details: ${req.paymentDetails}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Holder: ${req.accountHolderName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (req.status == "PENDING") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showApproveDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Approve & Mark Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reject & Refund", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (req.status == "PAID") {
                        Text(
                            text = "✅ Paid • Ref: ${req.paymentReference.ifBlank { "N/A" }}",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    } else if (req.status == "REJECTED") {
                        Text(
                            text = "❌ Rejected: ${req.rejectionReason.ifBlank { "Policy violation" }}",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            // Approve Dialog
            if (showApproveDialog) {
                AlertDialog(
                    onDismissRequest = { showApproveDialog = false },
                    title = { Text("Approve Withdrawal of ₹${req.amount.toInt()}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Enter the banking/UPI payment transaction reference ID after transferring funds:")
                            OutlinedTextField(
                                value = paymentRefInput,
                                onValueChange = { paymentRefInput = it },
                                label = { Text("Payment Ref / UTR No") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onApproveWithdrawal(req.id, paymentRefInput, "Approved by Admin")
                                showApproveDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Confirm Payout")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showApproveDialog = false }) { Text("Cancel") }
                    }
                )
            }

            // Reject Dialog
            if (showRejectDialog) {
                AlertDialog(
                    onDismissRequest = { showRejectDialog = false },
                    title = { Text("Reject Withdrawal") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("The funds (₹${req.amount.toInt()}) will be safely refunded back to user's wallet. Please state the reason:")
                            OutlinedTextField(
                                value = rejectionReasonInput,
                                onValueChange = { rejectionReasonInput = it },
                                label = { Text("Rejection Reason") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onRejectWithdrawal(req.id, rejectionReasonInput, "Rejected by Admin")
                                showRejectDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Reject & Refund Balance")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminOwnerChatsTabContent(
    chats: List<OwnerChatEntity>,
    activeChatUserId: String?,
    messages: List<ChatMessageEntity>,
    onSelectChat: (String) -> Unit,
    onSendReply: (String, String) -> Unit,
    onToggleBlockUser: (String, Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    var replyText by remember { mutableStateOf("") }

    if (activeChatUserId == null) {
        // Inbox list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "PRO VIP DIRECT MESSAGES (${chats.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (chats.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "No direct chats initiated yet by Pro members.",
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(chats) { chat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectChat(chat.userId) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SatisfyGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = SatisfyGold)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = chat.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Filled.Stars, contentDescription = null, tint = SatisfyGold, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = chat.lastMessage.ifBlank { "No messages" },
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormat.format(Date(chat.lastMessageTimestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (chat.unreadCountForAdmin > 0) {
                            Badge {
                                Text("${chat.unreadCountForAdmin}")
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Active Conversation View
        val currentChat = chats.find { it.userId == activeChatUserId }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top chat banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onSelectChat("") }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back to list")
                        }
                        Column {
                            Text(text = currentChat?.userName ?: "User", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Pro Member • ${currentChat?.userId}", fontSize = 11.sp, color = SatisfyGold)
                        }
                    }

                    TextButton(
                        onClick = { onToggleBlockUser(activeChatUserId, !(currentChat?.isBlocked ?: false)) }
                    ) {
                        Text(
                            text = if (currentChat?.isBlocked == true) "Unblock" else "Block Chat",
                            color = if (currentChat?.isBlocked == true) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val isFromAdmin = msg.senderRole == "OWNER"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isFromAdmin) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = if (isFromAdmin) Alignment.End else Alignment.Start,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isFromAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = msg.message,
                                    fontSize = 13.sp,
                                    color = if (isFromAdmin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                            Text(
                                text = "${msg.senderName} • ${dateFormat.format(Date(msg.timestamp))}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Reply Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 70.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply as Owner/Support...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                val text = replyText.trim()
                                replyText = ""
                                onSendReply(activeChatUserId, text)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send Reply", tint = SatisfyGold)
                    }
                }
            }
        }
    }
}
