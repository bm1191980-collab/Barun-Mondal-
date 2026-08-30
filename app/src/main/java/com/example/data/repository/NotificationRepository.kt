package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.NotificationDao
import com.example.data.model.*
import com.example.data.service.SatisfyFirebaseMessagingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class NotificationRepository(
    private val context: Context,
    private val notificationDao: NotificationDao
) {
    companion object {
        private const val TAG = "NotificationRepo"
        private const val PREFS_NAME = "satisfy_notification_prefs"
        private const val KEY_PUSH_ENABLED = "pref_push_enabled"
        private const val KEY_IN_APP_BANNER = "pref_in_app_banner"
        private const val KEY_SOUND_VIBRATE = "pref_sound_vibrate"
        private const val KEY_VIDEO_ALERTS = "pref_video_alerts"
        private const val KEY_COMMENT_ALERTS = "pref_comment_alerts"
        private const val KEY_MONETIZATION_ALERTS = "pref_monetization_alerts"
        private const val KEY_ADMIN_ALERTS = "pref_admin_alerts"
        private const val KEY_PRO_ALERTS = "pref_pro_alerts"
        private const val FIRESTORE_COLLECTION = "notifications"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var firestore: FirebaseFirestore? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var firestoreListener: ListenerRegistration? = null

    // Preferences StateFlow
    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    // Real-Time In-App Notification Toast Stream (for floating heads-up banner)
    private val _inAppToast = MutableSharedFlow<InAppNotificationToast>(extraBufferCapacity = 10)
    val inAppToast: SharedFlow<InAppNotificationToast> = _inAppToast.asSharedFlow()

    // Active User ID
    private val _currentUserId = MutableStateFlow("user_creator")

    // Database Flows
    val allNotifications: Flow<List<NotificationEntity>> = _currentUserId.flatMapLatest { uid ->
        notificationDao.getAllNotifications(uid)
    }

    val unreadNotifications: Flow<List<NotificationEntity>> = _currentUserId.flatMapLatest { uid ->
        notificationDao.getUnreadNotifications(uid)
    }

    val unreadCount: Flow<Int> = _currentUserId.flatMapLatest { uid ->
        notificationDao.getUnreadCount(uid)
    }

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
            firebaseAuth = FirebaseAuth.getInstance()
            _isFirebaseConnected.value = true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization notice: ${e.message}")
        }

        // Initialize channels & subscribe to FCM topics
        SatisfyFirebaseMessagingService.initializeChannels(context)
        subscribeToDefaultTopics()

        // Seed initial notifications if empty
        scope.launch {
            seedInitialNotificationsIfEmpty()
            startRealtimeFirestoreSync("user_creator")
        }
    }

    fun setUserId(uid: String) {
        if (_currentUserId.value != uid) {
            _currentUserId.value = uid
            startRealtimeFirestoreSync(uid)
        }
    }

    private fun loadPreferences(): NotificationPreferences {
        return NotificationPreferences(
            pushEnabled = prefs.getBoolean(KEY_PUSH_ENABLED, true),
            inAppBannerEnabled = prefs.getBoolean(KEY_IN_APP_BANNER, true),
            soundVibrateEnabled = prefs.getBoolean(KEY_SOUND_VIBRATE, true),
            videoUploadAlerts = prefs.getBoolean(KEY_VIDEO_ALERTS, true),
            commentMentionAlerts = prefs.getBoolean(KEY_COMMENT_ALERTS, true),
            monetizationAlerts = prefs.getBoolean(KEY_MONETIZATION_ALERTS, true),
            adminBroadcastAlerts = prefs.getBoolean(KEY_ADMIN_ALERTS, true),
            proMembershipAlerts = prefs.getBoolean(KEY_PRO_ALERTS, true)
        )
    }

    fun updatePreferences(prefsUpdate: NotificationPreferences) {
        _preferences.value = prefsUpdate
        prefs.edit()
            .putBoolean(KEY_PUSH_ENABLED, prefsUpdate.pushEnabled)
            .putBoolean(KEY_IN_APP_BANNER, prefsUpdate.inAppBannerEnabled)
            .putBoolean(KEY_SOUND_VIBRATE, prefsUpdate.soundVibrateEnabled)
            .putBoolean(KEY_VIDEO_ALERTS, prefsUpdate.videoUploadAlerts)
            .putBoolean(KEY_COMMENT_ALERTS, prefsUpdate.commentMentionAlerts)
            .putBoolean(KEY_MONETIZATION_ALERTS, prefsUpdate.monetizationAlerts)
            .putBoolean(KEY_ADMIN_ALERTS, prefsUpdate.adminBroadcastAlerts)
            .putBoolean(KEY_PRO_ALERTS, prefsUpdate.proMembershipAlerts)
            .apply()
    }

    private fun subscribeToDefaultTopics() {
        try {
            SatisfyFirebaseMessagingService.subscribeToTopic("all_users")
            SatisfyFirebaseMessagingService.subscribeToTopic("creators")
            SatisfyFirebaseMessagingService.subscribeToTopic("pro_members")
        } catch (e: Exception) {
            Log.w(TAG, "Topic subscription notice: ${e.message}")
        }
    }

    /**
     * Listens to Firestore /notifications collection in REAL-TIME using addSnapshotListener.
     * When new documents are inserted in Firestore, syncs into Room database and shows in-app banner.
     */
    fun startRealtimeFirestoreSync(userId: String) {
        firestoreListener?.remove()

        val db = firestore ?: return
        try {
            firestoreListener = db.collection(FIRESTORE_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore real-time sync listener error: ${error.message}")
                        _isFirebaseConnected.value = false
                        return@addSnapshotListener
                    }

                    _isFirebaseConnected.value = true
                    if (snapshots != null && !snapshots.isEmpty) {
                        scope.launch {
                            for (doc in snapshots.documents) {
                                val recipient = doc.getString("recipientUid") ?: "all"
                                if (recipient == userId || recipient == "all" || recipient.isBlank()) {
                                    val entity = documentToNotificationEntity(doc)
                                    val existing = notificationDao.getByFirestoreId(entity.firestoreId)
                                    if (existing == null) {
                                        val insertedId = notificationDao.insertNotification(entity)
                                        val finalEntity = entity.copy(id = insertedId)

                                        // Trigger In-App Heads-up Notification Toast
                                        if (_preferences.value.inAppBannerEnabled) {
                                            _inAppToast.tryEmit(InAppNotificationToast(finalEntity))
                                        }

                                        // Show system notification if push enabled
                                        if (_preferences.value.pushEnabled) {
                                            SatisfyFirebaseMessagingService.showNotification(
                                                context = context,
                                                title = "${finalEntity.type.badgeEmoji} ${finalEntity.title}",
                                                body = finalEntity.body,
                                                topicOrCategory = finalEntity.type.displayName,
                                                actionUrl = finalEntity.actionUrl
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting up Firestore listener: ${e.message}")
        }
    }

    private fun documentToNotificationEntity(doc: DocumentSnapshot): NotificationEntity {
        val typeStr = doc.getString("type") ?: NotificationType.SYSTEM_ALERT.name
        val type = try {
            NotificationType.valueOf(typeStr)
        } catch (e: Exception) {
            NotificationType.SYSTEM_ALERT
        }

        return NotificationEntity(
            firestoreId = doc.id,
            recipientUid = doc.getString("recipientUid") ?: "all",
            senderUid = doc.getString("senderUid") ?: "",
            senderName = doc.getString("senderName") ?: "Satisfy Official",
            senderAvatar = doc.getString("senderAvatar") ?: "",
            type = type,
            title = doc.getString("title") ?: "Satisfy Notification",
            body = doc.getString("body") ?: "",
            targetId = doc.getLong("targetId"),
            targetType = doc.getString("targetType") ?: "NONE",
            targetThumbnailUrl = doc.getString("targetThumbnailUrl") ?: "",
            actionUrl = doc.getString("actionUrl") ?: "",
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
            isRead = doc.getBoolean("isRead") ?: false,
            isPinned = doc.getBoolean("isPinned") ?: false,
            priority = doc.getString("priority") ?: "NORMAL",
            deliveredVia = "FIREBASE_FIRESTORE"
        )
    }

    /**
     * Publishes a Real-time Notification to Firebase Firestore and local database.
     */
    suspend fun postNotification(
        recipientUid: String = "user_creator",
        senderUid: String = "system",
        senderName: String = "Satisfy Official",
        senderAvatar: String = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150",
        type: NotificationType,
        title: String,
        body: String,
        targetId: Long? = null,
        targetType: String = "NONE",
        targetThumbnailUrl: String = "",
        actionUrl: String = "",
        priority: String = "NORMAL"
    ): Long = withContext(Dispatchers.IO) {
        val firestoreId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val notification = NotificationEntity(
            firestoreId = firestoreId,
            recipientUid = recipientUid,
            senderUid = senderUid,
            senderName = senderName,
            senderAvatar = senderAvatar,
            type = type,
            title = title,
            body = body,
            targetId = targetId,
            targetType = targetType,
            targetThumbnailUrl = targetThumbnailUrl,
            actionUrl = actionUrl,
            timestamp = timestamp,
            isRead = false,
            isPinned = false,
            priority = priority,
            deliveredVia = "FIREBASE_FIRESTORE"
        )

        val localId = notificationDao.insertNotification(notification)
        val finalEntity = notification.copy(id = localId)

        // Show in-app heads up toast
        if (_preferences.value.inAppBannerEnabled) {
            _inAppToast.tryEmit(InAppNotificationToast(finalEntity))
        }

        // Show system notification
        if (_preferences.value.pushEnabled) {
            SatisfyFirebaseMessagingService.showNotification(
                context = context,
                title = "${type.badgeEmoji} $title",
                body = body,
                topicOrCategory = type.displayName,
                actionUrl = actionUrl
            )
        }

        // Push to Firebase Firestore
        try {
            firestore?.let { db ->
                val data = hashMapOf(
                    "firestoreId" to firestoreId,
                    "recipientUid" to recipientUid,
                    "senderUid" to senderUid,
                    "senderName" to senderName,
                    "senderAvatar" to senderAvatar,
                    "type" to type.name,
                    "title" to title,
                    "body" to body,
                    "targetId" to targetId,
                    "targetType" to targetType,
                    "targetThumbnailUrl" to targetThumbnailUrl,
                    "actionUrl" to actionUrl,
                    "timestamp" to timestamp,
                    "isRead" to false,
                    "isPinned" to false,
                    "priority" to priority
                )
                db.collection(FIRESTORE_COLLECTION).document(firestoreId).set(data, SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore write fallback: ${e.message}")
        }

        localId
    }

    suspend fun markAsRead(id: Long, firestoreId: String = "") = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
        if (firestoreId.isNotBlank()) {
            try {
                firestore?.collection(FIRESTORE_COLLECTION)?.document(firestoreId)?.update("isRead", true)
            } catch (e: Exception) {
                Log.w(TAG, "Firestore markRead error: ${e.message}")
            }
        }
    }

    suspend fun markAllAsRead(recipientUid: String = "user_creator") = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(recipientUid)
    }

    suspend fun togglePin(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.togglePin(id)
    }

    suspend fun deleteNotification(id: Long, firestoreId: String = "") = withContext(Dispatchers.IO) {
        notificationDao.deleteNotification(id)
        if (firestoreId.isNotBlank()) {
            try {
                firestore?.collection(FIRESTORE_COLLECTION)?.document(firestoreId)?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore delete error: ${e.message}")
            }
        }
    }

    suspend fun clearAllNotifications(recipientUid: String = "user_creator") = withContext(Dispatchers.IO) {
        notificationDao.clearAllNotifications(recipientUid)
    }

    /**
     * Seeds initial realistic notifications so users experience the rich notifications center immediately.
     */
    private suspend fun seedInitialNotificationsIfEmpty() {
        val count = notificationDao.getCount()
        if (count == 0) {
            val sampleNotifications = listOf(
                NotificationEntity(
                    firestoreId = "seed_notif_1",
                    recipientUid = "user_creator",
                    senderUid = "creator_asmr_flow",
                    senderName = "ASMR Sanctuary 4K",
                    senderAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    type = NotificationType.VIDEO_UPLOAD,
                    title = "New 4K Video Uploaded",
                    body = "ASMR Sanctuary just uploaded: 'Deep Soap Carving & Crunchy Block Crushing in Ultra 4K'. Watch now!",
                    targetType = "POST",
                    targetThumbnailUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600",
                    timestamp = System.currentTimeMillis() - (12 * 60 * 1000), // 12 mins ago
                    isRead = false,
                    isPinned = true,
                    priority = "HIGH"
                ),
                NotificationEntity(
                    firestoreId = "seed_notif_2",
                    recipientUid = "user_creator",
                    senderUid = "creator_kinetic",
                    senderName = "Maya Chen",
                    senderAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    type = NotificationType.COMMENT,
                    title = "New Comment on your video",
                    body = "Maya Chen commented: 'The 60FPS hydraulic press slow-motion in this video is utterly mesmerizing! Pure perfection 🤤✨'",
                    targetType = "POST",
                    targetThumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600",
                    timestamp = System.currentTimeMillis() - (45 * 60 * 1000), // 45 mins ago
                    isRead = false,
                    priority = "NORMAL"
                ),
                NotificationEntity(
                    firestoreId = "seed_notif_3",
                    recipientUid = "user_creator",
                    senderUid = "system_monetization",
                    senderName = "Satisfy Creator Program",
                    senderAvatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150",
                    type = NotificationType.MONETIZATION_UPDATE,
                    title = "Monetization Milestone Reached! 🎉",
                    body = "Congratulations! Your channel crossed 500 subscribers and 4,000 watch hours. You are now eligible to apply for the Satisfy Partner Program.",
                    targetType = "MONETIZATION",
                    timestamp = System.currentTimeMillis() - (3 * 3600 * 1000), // 3 hours ago
                    isRead = false,
                    priority = "HIGH"
                ),
                NotificationEntity(
                    firestoreId = "seed_notif_4",
                    recipientUid = "user_creator",
                    senderUid = "user_alex_rivers",
                    senderName = "Alex Rivers",
                    senderAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    type = NotificationType.LIKE,
                    title = "New Like on your Short",
                    body = "Alex Rivers and 48 others liked your Short 'Kinetic Sand Rainbow Slicing ASMR 🌈'",
                    targetType = "SHORT",
                    targetThumbnailUrl = "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?w=600",
                    timestamp = System.currentTimeMillis() - (6 * 3600 * 1000), // 6 hours ago
                    isRead = true,
                    priority = "NORMAL"
                ),
                NotificationEntity(
                    firestoreId = "seed_notif_5",
                    recipientUid = "user_creator",
                    senderUid = "admin_bm1191980",
                    senderName = "Satisfy SuperAdmin",
                    senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    type = NotificationType.ADMIN_BROADCAST,
                    title = "Satisfy v2.0 Platform Update ⚡",
                    body = "Ultra HD 4K HDR playback is now live for all creators! Enjoy smoother frame rates, instant audio scrubbing, and instant creator monetization.",
                    targetType = "BROADCAST",
                    timestamp = System.currentTimeMillis() - (24 * 3600 * 1000), // 1 day ago
                    isRead = true,
                    priority = "HIGH"
                ),
                NotificationEntity(
                    firestoreId = "seed_notif_6",
                    recipientUid = "user_creator",
                    senderUid = "system_wallet",
                    senderName = "Satisfy Financial System",
                    senderAvatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150",
                    type = NotificationType.WALLET_PAYOUT,
                    title = "Referral Reward Credited: +$2.50 💵",
                    body = "A new creator used your referral code (SATISFY100) to activate their Pro Membership. $2.50 has been credited to your Wallet balance!",
                    targetType = "WALLET",
                    timestamp = System.currentTimeMillis() - (2 * 24 * 3600 * 1000), // 2 days ago
                    isRead = true,
                    priority = "NORMAL"
                )
            )

            notificationDao.insertAll(sampleNotifications)
        }
    }
}
