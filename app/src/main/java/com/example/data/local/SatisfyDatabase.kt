package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

@Database(
    entities = [
        PostEntity::class,
        CommentEntity::class,
        WatchHistoryEntity::class,
        UserAccountEntity::class,
        ReportEntity::class,
        PushNotificationLogEntity::class,
        AppSystemSettingsEntity::class,
        AdminAuditLogEntity::class,
        CreatorPageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SatisfyDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun reportDao(): ReportDao
    abstract fun pushNotificationDao(): PushNotificationDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun creatorPageDao(): CreatorPageDao

    companion object {
        @Volatile
        private var INSTANCE: SatisfyDatabase? = null

        fun getDatabase(context: Context): SatisfyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SatisfyDatabase::class.java,
                    "satisfy_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
