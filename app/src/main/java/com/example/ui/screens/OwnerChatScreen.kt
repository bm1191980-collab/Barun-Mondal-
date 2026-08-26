package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessageEntity
import com.example.data.model.OwnerChatEntity
import com.example.data.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerChatScreen(
    userProfile: UserProfile = UserProfile(),
    isPro: Boolean = false,
    chat: OwnerChatEntity? = null,
    chatInfo: OwnerChatEntity? = chat,
    messages: List<ChatMessageEntity> = emptyList(),
    onBack: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    onNavigateToPro: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(SatisfyGold, Color(0xFFD97706)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "App Owner (VIP Support)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.Stars, contentDescription = null, tint = SatisfyGold, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "Direct 1-on-1 VIP Hotline • Online",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (isPro) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SatisfyGold.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "PRO VIP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SatisfyGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!isPro) {
            // Non-Pro Upgrade Locked Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, SatisfyGold.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(SatisfyGold, Color(0xFFB45309)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "Upgrade to PRO to Chat with Owner",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Direct 1-on-1 chat with the App Owner and Super Admin team is an exclusive privilege reserved for PRO Members (₹5/month).",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        // Benefits Mini List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = SatisfyGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Instant VIP support and fast responses", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = SatisfyGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Channel feature and monetization advice", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = SatisfyGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Direct feedback to core developers", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = onNavigateToPro,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SatisfyGold,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upgrade to PRO (₹5 / month)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        } else {
            // Active Pro Chat Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Welcome announcement bubble
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.7f)),
                            border = BorderStroke(1.dp, SatisfyGold.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = SatisfyGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Welcome to Owner Direct VIP Chat! Send any inquiries, feature requests, or creator assistance directly to the app owner.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE0E7FF),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (messages.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Start a conversation with the Owner below 👋",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(messages) { msg ->
                        val isFromMe = msg.senderRole == "USER"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isFromMe) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(SatisfyGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Column(
                                horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (isFromMe) 14.dp else 2.dp,
                                        bottomEnd = if (isFromMe) 2.dp else 14.dp
                                    ),
                                    color = if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shadowElevation = 1.dp
                                ) {
                                    Text(
                                        text = msg.message,
                                        fontSize = 14.sp,
                                        color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(msg.timestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isFromMe) {
                                        Icon(
                                            imageVector = if (msg.isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                                            contentDescription = null,
                                            tint = if (msg.isRead) Color(0xFF60A5FA) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Prompt Suggestions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val promptSuggestions = listOf(
                        "How to withdraw referral earnings?",
                        "Can I get Creator Page verified?",
                        "Feature suggestion for next update",
                        "Promote my video on Satisfy"
                    )
                    promptSuggestions.forEach { prompt ->
                        SuggestionChip(
                            onClick = { messageInput = prompt },
                            label = { Text(prompt, fontSize = 11.sp) }
                        )
                    }
                }

                // Input Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            placeholder = { Text("Message App Owner...", fontSize = 13.sp) },
                            maxLines = 3,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SatisfyGold,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (messageInput.isNotBlank()) {
                                    val textToSend = messageInput.trim()
                                    messageInput = ""
                                    onSendMessage(textToSend)
                                    coroutineScope.launch {
                                        if (messages.isNotEmpty()) {
                                            listState.animateScrollToItem(messages.size - 1)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SatisfyGold)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
