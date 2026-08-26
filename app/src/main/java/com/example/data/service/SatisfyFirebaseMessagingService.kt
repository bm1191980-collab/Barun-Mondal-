package com.example.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SatisfyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM registration token: $token")
        // In production, sync token with Firestore /users/{uid}
        saveFcmToken(this, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "Satisfy Update 🌟"
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "New trending content is available on Satisfy!"
        val actionUrl = remoteMessage.data["action_url"] ?: ""
        val topic = remoteMessage.from ?: "all_users"

        showNotification(this, title, body, topic, actionUrl)
    }

    companion object {
        private const val TAG = "SatisfyFCM"
        const val CHANNEL_ID_BROADCAST = "satisfy_broadcast_channel"
        const val CHANNEL_ID_ADMIN = "satisfy_admin_channel"
        private const val PREFS_NAME = "satisfy_fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"

        fun saveFcmToken(context: Context, token: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        }

        fun getSavedFcmToken(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_FCM_TOKEN, "fcm_token_${System.currentTimeMillis()}") 
                ?: "fcm_token_default"
        }

        fun initializeChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                val broadcastChannel = NotificationChannel(
                    CHANNEL_ID_BROADCAST,
                    "Satisfy Community Broadcasts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for admin announcements, video releases, and featured highlights"
                    enableVibration(true)
                }

                val adminChannel = NotificationChannel(
                    CHANNEL_ID_ADMIN,
                    "Satisfy Admin Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for reports, security, and moderation alerts"
                    enableVibration(true)
                }

                notificationManager.createNotificationChannel(broadcastChannel)
                notificationManager.createNotificationChannel(adminChannel)
            }
        }

        fun subscribeToTopic(topic: String, onComplete: (Boolean) -> Unit = {}) {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener { task ->
                        Log.d(TAG, "Subscribed to $topic: ${task.isSuccessful}")
                        onComplete(task.isSuccessful)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to topic $topic: ${e.message}")
                onComplete(false)
            }
        }

        fun showNotification(
            context: Context,
            title: String,
            body: String,
            topicOrCategory: String = "Broadcast",
            actionUrl: String = ""
        ) {
            initializeChannels(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                    return
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action_url", actionUrl)
                putExtra("notification_title", title)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = if (topicOrCategory.contains("admin", ignoreCase = true)) {
                CHANNEL_ID_ADMIN
            } else {
                CHANNEL_ID_BROADCAST
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSubText(topicOrCategory)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
        }
    }
}
