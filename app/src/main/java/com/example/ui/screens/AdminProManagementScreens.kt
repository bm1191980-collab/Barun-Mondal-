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
                        Text("₹5 / Subscription", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Active Pro Members", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$activeCount", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                        Text("Total: ${subscriptions.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                tint = SatisfyGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sub.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isActive) Color(0xFF065F46) else Color(0xFF7F1D1D)
                        ) {
                            Text(
                                text = if (isActive) "ACTIVE" else "EXPIRED / INACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Payment: ${sub.paymentId} • Order: ${sub.orderId} • Method: ${sub.paymentMethod}",
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
                                text = "Referred by: ${sub.referrerCode} (₹4 Reward Credited + ₹1 Owner)",
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
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val totalRewards = remember(referrals) { referrals.filter { it.rewardStatus == "CREDITED" }.sumOf { it.rewardAmount } }
    val successfulCount = remember(referrals) { referrals.count { it.hasPurchasedPro } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
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
                        Text("Total Referrals", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${referrals.size}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Pro Upgrades: $successfulCount", fontSize = 10.sp, color = Color(0xFF10B981))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Rewards Distributed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${totalRewards.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SatisfyGold)
                        Text("₹4 per upgrade", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Text(
                text = "REFERRALS AUDIT LIST",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
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
                                text = "Referrer: ${ref.referrerName} (${ref.referrerCode})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Referee: ${ref.refereeName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (ref.rewardStatus) {
                                "CREDITED" -> Color(0xFF065F46)
                                "REVERSED" -> Color(0xFF7F1D1D)
                                else -> Color(0xFF92400E)
                            }
                        ) {
                            Text(
                                text = ref.rewardStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Joined: ${dateFormat.format(Date(ref.joinedAt))} • Reward: ₹${ref.rewardAmount.toInt()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (ref.rewardStatus == "CREDITED") {
                            TextButton(
                                onClick = { onReverseReward(ref.id, "Admin manual reward reversal") },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                            ) {
                                Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reverse Reward", fontSize = 11.sp)
                            }
                        }

                        TextButton(
                            onClick = { onToggleSuspicious(ref.id, !ref.isSuspicious, if (!ref.isSuspicious) "Suspicious referral pattern" else "") },
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
