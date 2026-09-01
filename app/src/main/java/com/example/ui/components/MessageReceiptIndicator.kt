package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Message Delivered and Read Receipts Indicator
 * - Single gray check: Sent / Transmitting
 * - Double gray check: Delivered to device / server
 * - Double sky blue check: Read by recipient
 */
@Composable
fun MessageReceiptIndicator(
    isFromMe: Boolean,
    isDelivered: Boolean,
    isRead: Boolean,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Row(
        modifier = modifier.testTag("message_receipt_indicator"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = dateFormat.format(Date(timestamp)),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )

        if (isFromMe) {
            when {
                isRead -> {
                    // Double check marks - Sky Blue / Cyan (Read receipt)
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = "Read",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier
                            .size(15.dp)
                            .testTag("receipt_icon_read")
                    )
                }
                isDelivered -> {
                    // Double check marks - Slate Gray (Delivered)
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = "Delivered",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(15.dp)
                            .testTag("receipt_icon_delivered")
                    )
                }
                else -> {
                    // Single check mark - Sent
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Sent",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(13.dp)
                            .testTag("receipt_icon_sent")
                    )
                }
            }
        }
    }
}
