package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PresencePrivacySetting {
    EVERYONE,            // Everyone can see my online status and last seen
    SUBSCRIBERS_ONLY,    // Only subscribers / followers can see
    NOBODY               // Nobody can see (Incognito / Stealth mode)
}

enum class PresenceStatus {
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY
}

data class UserPresence(
    val uid: String = "user_creator",
    val isOnline: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val status: PresenceStatus = PresenceStatus.ONLINE,
    val showOnlineStatus: Boolean = true,
    val showLastSeen: Boolean = true,
    val privacySetting: PresencePrivacySetting = PresencePrivacySetting.EVERYONE,
    val customStatusMessage: String = "Active on Satisfy ✨",
    val currentActivity: String = "Exploring Videos & Shorts 🎬"
) {
    /**
     * Formats the presence display string for another user viewing this profile,
     * taking privacy settings into account.
     */
    fun getDisplayStatus(isViewerSubscribed: Boolean = true, isSelf: Boolean = false): String {
        if (isSelf) {
            return when {
                !showOnlineStatus -> "Offline (Hidden in Privacy)"
                isOnline -> "Online now"
                showLastSeen -> formatLastSeen(lastSeenTimestamp)
                else -> "Last seen hidden"
            }
        }

        // Apply privacy rules
        if (!showOnlineStatus) return "Offline"

        when (privacySetting) {
            PresencePrivacySetting.NOBODY -> return "Offline"
            PresencePrivacySetting.SUBSCRIBERS_ONLY -> {
                if (!isViewerSubscribed) return "Offline"
            }
            PresencePrivacySetting.EVERYONE -> { /* Allowed */ }
        }

        return if (isOnline) {
            "Online"
        } else if (showLastSeen) {
            formatLastSeen(lastSeenTimestamp)
        } else {
            "Offline"
        }
    }

    fun isEffectivelyOnline(isViewerSubscribed: Boolean = true, isSelf: Boolean = false): Boolean {
        if (isSelf) return isOnline && showOnlineStatus
        if (!showOnlineStatus) return false
        return when (privacySetting) {
            PresencePrivacySetting.NOBODY -> false
            PresencePrivacySetting.SUBSCRIBERS_ONLY -> isViewerSubscribed && isOnline
            PresencePrivacySetting.EVERYONE -> isOnline
        }
    }

    companion object {
        fun formatLastSeen(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diffMs = (now - timestamp).coerceAtLeast(0L)
            val diffSec = diffMs / 1000
            val diffMin = diffSec / 60
            val diffHour = diffMin / 60
            val diffDay = diffHour / 24

            return when {
                diffMin < 1 -> "Last seen just now"
                diffMin < 60 -> "Last seen $diffMin min${if (diffMin == 1L) "" else "s"} ago"
                diffHour < 24 -> "Last seen $diffHour hr${if (diffHour == 1L) "" else "s"} ago"
                diffDay == 1L -> "Last seen yesterday"
                diffDay < 7 -> "Last seen $diffDay days ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    "Last seen ${sdf.format(Date(timestamp))}"
                }
            }
        }
    }
}
