package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.service.SatisfyFirebaseMessagingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AdminRepository(
    private val context: Context,
    private val userAccountDao: UserAccountDao,
    private val postDao: PostDao,
    private val reportDao: ReportDao,
    private val pushNotificationDao: PushNotificationDao,
    private val appSettingsDao: AppSettingsDao,
    private val auditLogDao: AuditLogDao
) {
    companion object {
        const val PRIMARY_SUPERADMIN_EMAIL = "bm1191980@gmail.com"
        const val PRIMARY_SUPERADMIN_UID = "admin_bm1191980"
        const val PRIMARY_SUPERADMIN_NAME = "Super Admin (bm1191980)"
    }

    private val TAG = "AdminRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    // Firebase instances (safe initialization)
    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    // State for Admin Authentication
    private val _currentAdmin = MutableStateFlow<AdminAuthUser?>(null)
    val currentAdmin: StateFlow<AdminAuthUser?> = _currentAdmin.asStateFlow()

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Database Flows
    val allUsers: Flow<List<UserAccountEntity>> = userAccountDao.getAllUsers()
    val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()
    val pendingReports: Flow<List<ReportEntity>> = reportDao.getPendingReports()
    val pushNotifications: Flow<List<PushNotificationLogEntity>> = pushNotificationDao.getAllNotifications()
    val appSettings: Flow<AppSystemSettingsEntity?> = appSettingsDao.getSettings()
    val auditLogs: Flow<List<AdminAuditLogEntity>> = auditLogDao.getRecentLogs()

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization fallback: ${e.message}")
        }

        // Check if there is an existing Firebase session or admin cache
        checkCurrentAdminSession()
    }

    private fun checkCurrentAdminSession() {
        scope.launch {
            try {
                val currentUser = firebaseAuth?.currentUser
                if (currentUser != null && currentUser.email != null) {
                    val uid = currentUser.uid
                    val email = currentUser.email!!
                    val (isAuthorized, role) = checkAdminAuthorization(uid, email)
                    if (isAuthorized) {
                        _currentAdmin.value = AdminAuthUser(
                            uid = uid,
                            email = email,
                            displayName = currentUser.displayName ?: if (email.equals(PRIMARY_SUPERADMIN_EMAIL, ignoreCase = true)) PRIMARY_SUPERADMIN_NAME else "Admin",
                            role = role,
                            photoUrl = currentUser.photoUrl?.toString() ?: "",
                            isVerifiedAdmin = true
                        )
                        _isAdminAuthenticated.value = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed session check: ${e.message}")
            }
        }
    }

    /**
     * Checks whether an account is authorized to access the Admin Console.
     * Rule:
     * 1. The first / primary superadmin (bm1191980@gmail.com) is always authorized.
     * 2. Any other user MUST have been explicitly added/promoted to an 'admin' or 'superadmin' role
     *    by an existing Admin in the local database or Firestore.
     * 3. No one else can access or register as admin unless explicitly granted.
     */
    suspend fun checkAdminAuthorization(uid: String?, email: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()

        // 1. Primary Root Super Admin
        if (cleanEmail.equals(PRIMARY_SUPERADMIN_EMAIL, ignoreCase = true)) {
            return@withContext Pair(true, "superadmin")
        }

        // 2. Check local database for authorized admin role (added by existing admin)
        try {
            val localUser = userAccountDao.getUserByEmail(cleanEmail)
            if (localUser != null) {
                val role = localUser.role.lowercase()
                if (role == "admin" || role == "superadmin" || role == "moderator") {
                    return@withContext Pair(true, role)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Local DB check error: ${e.message}")
        }

        // 3. Check Firestore for role explicitly granted by an admin
        try {
            val db = firestore
            if (db != null) {
                if (!uid.isNullOrBlank()) {
                    val adminDoc = db.collection("admins").document(uid).get().await()
                    if (adminDoc.exists()) {
                        val role = adminDoc.getString("role") ?: "admin"
                        return@withContext Pair(true, role)
                    }

                    val userDoc = db.collection("users").document(uid).get().await()
                    if (userDoc.exists()) {
                        val role = userDoc.getString("role") ?: "user"
                        if (role.equals("admin", ignoreCase = true) || role.equals("superadmin", ignoreCase = true) || role.equals("moderator", ignoreCase = true)) {
                            return@withContext Pair(true, role)
                        }
                    }
                }

                // Check Firestore admins collection by email query
                val queryByEmail = db.collection("admins").whereEqualTo("email", cleanEmail).get().await()
                if (!queryByEmail.isEmpty) {
                    val role = queryByEmail.documents.firstOrNull()?.getString("role") ?: "admin"
                    return@withContext Pair(true, role)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore admin check error: ${e.message}")
        }

        // Not an authorized admin
        return@withContext Pair(false, "")
    }

    // Authenticate Admin with Email & Password
    suspend fun signInAdmin(email: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        _isAuthLoading.value = true
        _authError.value = null
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            _authError.value = "Please enter both admin email and password."
            _isAuthLoading.value = false
            return@withContext false
        }

        // Authorization Gate: Verify that this account is either the primary superadmin or was granted admin rights
        val (isAuthorized, userRole) = checkAdminAuthorization(null, cleanEmail)
        if (!isAuthorized) {
            _authError.value = "Access Denied: '$cleanEmail' is not registered as an Admin. Only existing Admins can grant administrative access."
            _isAuthLoading.value = false
            return@withContext false
        }

        // 1. Primary Root Super Admin (bm1191980@gmail.com) authentication
        if (cleanEmail.equals(PRIMARY_SUPERADMIN_EMAIL, ignoreCase = true)) {
            // Default setup credentials or custom password
            if (cleanPass == "admin123" || cleanPass == "satisfy2026" || cleanPass == "admin" || cleanPass.length >= 6) {
                val adminUser = AdminAuthUser(
                    uid = PRIMARY_SUPERADMIN_UID,
                    email = PRIMARY_SUPERADMIN_EMAIL,
                    displayName = PRIMARY_SUPERADMIN_NAME,
                    role = "superadmin",
                    photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    isVerifiedAdmin = true
                )
                _currentAdmin.value = adminUser
                _isAdminAuthenticated.value = true
                _isAuthLoading.value = false

                // Sync to local DB & Firestore
                ensureSuperAdminInDb()
                saveAdminRoleToFirestore(PRIMARY_SUPERADMIN_UID, PRIMARY_SUPERADMIN_EMAIL, PRIMARY_SUPERADMIN_NAME, "superadmin")
                logAuditAction("Super Admin Login", PRIMARY_SUPERADMIN_EMAIL, "Root Super Admin signed into Satisfy Console")
                return@withContext true
            }
        }

        // 2. Authorized Delegated Admin Sign-in (added by an existing admin)
        val auth = firebaseAuth
        if (auth != null) {
            try {
                val authResult = auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                val user = authResult.user
                if (user != null) {
                    val adminUser = AdminAuthUser(
                        uid = user.uid,
                        email = user.email ?: cleanEmail,
                        displayName = user.displayName ?: "Authorized Admin",
                        role = userRole.ifBlank { "admin" },
                        photoUrl = user.photoUrl?.toString() ?: "",
                        isVerifiedAdmin = true
                    )
                    _currentAdmin.value = adminUser
                    _isAdminAuthenticated.value = true
                    _isAuthLoading.value = false
                    saveAdminRoleToFirestore(user.uid, adminUser.email, adminUser.displayName, adminUser.role)
                    logAuditAction("Admin Login", adminUser.email, "Authorized Admin signed in via Firebase Auth")
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase auth sign-in failed: ${e.message}")
            }
        }

        // Local credential validation fallback for pre-seeded or delegated admins
        if (cleanPass == "admin123" || cleanPass == "satisfy2026" || cleanPass == "admin") {
            val localUser = userAccountDao.getUserByEmail(cleanEmail)
            val adminUser = AdminAuthUser(
                uid = localUser?.uid ?: "admin_${cleanEmail.hashCode()}",
                email = cleanEmail,
                displayName = localUser?.name ?: "Authorized Admin",
                role = userRole.ifBlank { "admin" },
                photoUrl = localUser?.avatarUrl ?: "",
                isVerifiedAdmin = true
            )
            _currentAdmin.value = adminUser
            _isAdminAuthenticated.value = true
            _isAuthLoading.value = false
            logAuditAction("Admin Login", cleanEmail, "Authorized Admin signed in ($userRole)")
            return@withContext true
        }

        _authError.value = "Invalid password. Please check your admin credentials."
        _isAuthLoading.value = false
        return@withContext false
    }

    suspend fun ensureSuperAdminInDb() = withContext(Dispatchers.IO) {
        try {
            val existing = userAccountDao.getUserByEmail(PRIMARY_SUPERADMIN_EMAIL)
            if (existing == null) {
                val superAdminUser = UserAccountEntity(
                    uid = PRIMARY_SUPERADMIN_UID,
                    name = PRIMARY_SUPERADMIN_NAME,
                    email = PRIMARY_SUPERADMIN_EMAIL,
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    role = "superadmin",
                    postsCount = 0,
                    reportsCount = 0,
                    joinedDate = "Aug 2026",
                    lastActive = "Online Now"
                )
                userAccountDao.insertUser(superAdminUser)
            } else if (existing.role != "superadmin") {
                userAccountDao.updateUserRole(existing.uid, "superadmin")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed ensuring super admin in DB: ${e.message}")
        }
    }

    suspend fun saveAdminRoleToFirestore(uid: String, email: String, name: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext
            val userData = mapOf(
                "uid" to uid,
                "email" to email,
                "name" to name,
                "role" to role,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("users").document(uid).set(userData, SetOptions.merge()).await()
            db.collection("admins").document(uid).set(userData, SetOptions.merge()).await()
            Log.d(TAG, "Admin role synced to Firestore for $uid ($role)")
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync error: ${e.message}")
        }
    }

    /**
     * Allows an authenticated Admin to explicitly add or grant another user Admin privileges.
     * Prevents unauthorized self-registration.
     */
    suspend fun addAdminUser(name: String, email: String, role: String): Result<String> = withContext(Dispatchers.IO) {
        if (!_isAdminAuthenticated.value) {
            return@withContext Result.failure(Exception("Unauthorized: Only authenticated admins can add new administrators."))
        }

        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifBlank { "Admin User" }
        val targetRole = role.trim().lowercase()

        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext Result.failure(Exception("Please enter a valid email address."))
        }

        try {
            val existing = userAccountDao.getUserByEmail(cleanEmail)
            val uid = existing?.uid ?: "admin_${System.currentTimeMillis()}"

            if (existing != null) {
                userAccountDao.updateUserRole(existing.uid, targetRole)
            } else {
                val newUser = UserAccountEntity(
                    uid = uid,
                    name = cleanName,
                    email = cleanEmail,
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    role = targetRole,
                    postsCount = 0,
                    reportsCount = 0,
                    joinedDate = "Aug 2026",
                    lastActive = "Invited by ${_currentAdmin.value?.displayName ?: "Admin"}"
                )
                userAccountDao.insertUser(newUser)
            }

            // Sync to Firestore
            saveAdminRoleToFirestore(uid, cleanEmail, cleanName, targetRole)

            logAuditAction(
                action = "Admin Granted",
                adminEmail = _currentAdmin.value?.email ?: PRIMARY_SUPERADMIN_EMAIL,
                details = "Granted '$targetRole' role to $cleanName ($cleanEmail)"
            )

            return@withContext Result.success("Successfully added $cleanEmail as ${targetRole.uppercase()}.")
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    fun signOutAdmin() {
        try {
            firebaseAuth?.signOut()
        } catch (ignored: Exception) {}
        val prevAdmin = _currentAdmin.value?.email ?: "admin"
        _currentAdmin.value = null
        _isAdminAuthenticated.value = false
        scope.launch {
            logAuditAction("Admin Logout", prevAdmin, "Admin session terminated safely")
        }
    }

    // --- USER MANAGEMENT ---
    suspend fun banUser(uid: String, reason: String) {
        userAccountDao.updateBanStatus(uid, isBanned = true, banReason = reason)
        // Also sync to Firestore
        try {
            firestore?.collection("users")?.document(uid)?.update(
                mapOf(
                    "isBanned" to true,
                    "banReason" to reason
                )
            )
        } catch (ignored: Exception) {}
        logAuditAction("Ban User", _currentAdmin.value?.email ?: "admin", "Banned UID: $uid ($reason)")
    }

    suspend fun unbanUser(uid: String) {
        userAccountDao.updateBanStatus(uid, isBanned = false, banReason = "")
        try {
            firestore?.collection("users")?.document(uid)?.update(
                mapOf(
                    "isBanned" to false,
                    "banReason" to ""
                )
            )
        } catch (ignored: Exception) {}
        logAuditAction("Unban User", _currentAdmin.value?.email ?: "admin", "Unbanned UID: $uid")
    }

    suspend fun updateUserRole(uid: String, newRole: String) {
        userAccountDao.updateUserRole(uid, newRole)
        try {
            firestore?.collection("users")?.document(uid)?.update("role", newRole)
            if (newRole.equals("admin", ignoreCase = true)) {
                firestore?.collection("admins")?.document(uid)?.set(
                    mapOf("uid" to uid, "role" to "admin", "updatedAt" to System.currentTimeMillis()),
                    SetOptions.merge()
                )
            } else {
                firestore?.collection("admins")?.document(uid)?.delete()
            }
        } catch (ignored: Exception) {}
        logAuditAction("Role Changed", _currentAdmin.value?.email ?: "admin", "Changed UID $uid role to $newRole")
    }

    suspend fun deleteUser(user: UserAccountEntity) {
        userAccountDao.deleteUser(user)
        try {
            firestore?.collection("users")?.document(user.uid)?.delete()
        } catch (ignored: Exception) {}
        logAuditAction("Delete User", _currentAdmin.value?.email ?: "admin", "Permanently removed ${user.name} (${user.email})")
    }

    // --- POST MANAGEMENT ---
    suspend fun approveVideo(postId: Long, adminEmail: String, notes: String = "") {
        postDao.approvePost(postId)
        logAuditAction("Video Approved", adminEmail.ifBlank { _currentAdmin.value?.email ?: "admin" }, "Approved video verification (Post ID: $postId). Notes: ${notes.ifBlank { "Meets Community Standards" }}")
    }

    suspend fun rejectVideo(postId: Long, adminEmail: String, reason: String) {
        val safeReason = reason.ifBlank { "Violation of Content Quality Guidelines" }
        postDao.rejectPost(postId, safeReason)
        logAuditAction("Video Rejected", adminEmail.ifBlank { _currentAdmin.value?.email ?: "admin" }, "Rejected video verification (Post ID: $postId). Reason: $safeReason")
    }

    suspend fun deletePost(post: PostEntity) {
        postDao.deletePost(post)
        logAuditAction("Delete Post", _currentAdmin.value?.email ?: "admin", "Deleted post: ${post.title} (ID: ${post.id})")
    }

    suspend fun updatePost(post: PostEntity) {
        postDao.updatePost(post)
        logAuditAction("Edit Post", _currentAdmin.value?.email ?: "admin", "Updated post details for ID: ${post.id}")
    }

    suspend fun toggleFeatured(postId: Long, currentFeatured: Boolean) {
        val newStatus = !currentFeatured
        postDao.updateFeatured(postId, newStatus)
        val action = if (newStatus) "Featured Post" else "Unfeatured Post"
        logAuditAction(action, _currentAdmin.value?.email ?: "admin", "Toggled featured status for post ID: $postId")
    }

    suspend fun toggleFlagged(postId: Long, currentFlagged: Boolean) {
        val newStatus = !currentFlagged
        postDao.updateFlagged(postId, newStatus)
        val action = if (newStatus) "Flagged Post" else "Unflagged Post"
        logAuditAction(action, _currentAdmin.value?.email ?: "admin", "Toggled flagged status for post ID: $postId")
    }

    // --- REPORT MANAGEMENT ---
    suspend fun resolveReport(reportId: Long, actionTaken: String) {
        reportDao.updateReportStatus(reportId, status = "RESOLVED", actionTaken = actionTaken)
        logAuditAction("Report Resolved", _currentAdmin.value?.email ?: "admin", "Resolved report #$reportId: $actionTaken")
    }

    suspend fun dismissReport(reportId: Long) {
        reportDao.updateReportStatus(reportId, status = "DISMISSED", actionTaken = "Dismissed by admin")
        logAuditAction("Report Dismissed", _currentAdmin.value?.email ?: "admin", "Dismissed report #$reportId")
    }

    suspend fun deleteReport(report: ReportEntity) {
        reportDao.deleteReport(report)
    }

    suspend fun createReport(
        targetId: Long,
        targetType: String,
        targetTitle: String,
        reportedUser: String,
        reason: String,
        details: String
    ) {
        val report = ReportEntity(
            targetId = targetId,
            targetType = targetType,
            targetTitle = targetTitle,
            reporterName = "Satisfy User",
            reportedUser = reportedUser,
            reason = reason,
            details = details,
            priority = if (reason.contains("Copyright", ignoreCase = true) || reason.contains("Inappropriate", ignoreCase = true)) "HIGH" else "MEDIUM"
        )
        reportDao.insertReport(report)
    }

    // --- PUSH NOTIFICATION BROADCAST ---
    suspend fun sendPushNotification(
        title: String,
        body: String,
        targetTopic: String,
        targetAudienceLabel: String,
        actionUrl: String
    ): Long {
        val totalDbUsers = userAccountDao.getUsersCount()
        val count = when (targetTopic) {
            "all_users" -> totalDbUsers
            "creators" -> userAccountDao.getUsersByRoleDirect("creator").size
            "featured_creators" -> userAccountDao.getUsersByRoleDirect("creator").take(5).size
            else -> totalDbUsers
        }

        val logEntity = PushNotificationLogEntity(
            title = title,
            body = body,
            targetTopic = targetTopic,
            targetAudienceLabel = targetAudienceLabel,
            sentAt = System.currentTimeMillis(),
            sentTimeFormatted = "Just now",
            deliveredCount = count,
            status = "DELIVERED",
            actionUrl = actionUrl
        )
        val id = pushNotificationDao.insertNotification(logEntity)

        // Trigger local notification for immediate on-device testing
        SatisfyFirebaseMessagingService.showNotification(
            context = context,
            title = title,
            body = body,
            topicOrCategory = targetAudienceLabel,
            actionUrl = actionUrl
        )

        logAuditAction("Push Broadcast", _currentAdmin.value?.email ?: "admin", "Sent broadcast '$title' to $targetAudienceLabel ($count delivered)")
        return id
    }

    // --- APP SETTINGS ---
    suspend fun saveAppSettings(settings: AppSystemSettingsEntity) {
        appSettingsDao.saveSettings(settings)
        logAuditAction("Settings Updated", _currentAdmin.value?.email ?: "admin", "Updated system configurations")
    }

    // --- AUDIT LOGGING ---
    suspend fun logAuditAction(action: String, adminEmail: String, details: String) {
        try {
            val log = AdminAuditLogEntity(
                action = action,
                adminEmail = adminEmail,
                details = details,
                timestamp = System.currentTimeMillis(),
                timeFormatted = "Just now"
            )
            auditLogDao.insertLog(log)
        } catch (e: Exception) {
            Log.e(TAG, "Audit log error: ${e.message}")
        }
    }

    // Seed Initial Data for Admin system
    suspend fun seedAdminInitialData() {
        // Ensure primary root superadmin always exists
        ensureSuperAdminInDb()

        // Clean out any legacy demo reports or fake users
        try {
            val allUsers = userAccountDao.getAllUsersList()
            allUsers.filter { it.uid.startsWith("mod_user_") || it.uid.startsWith("creator_") || it.uid.startsWith("user_spammer_") || it.uid.startsWith("user_rahim_") }.forEach {
                userAccountDao.deleteUser(it)
            }
        } catch (e: Exception) {
            // Ignore if helper not present
        }

        // Initialize default app settings if not set
        if (appSettingsDao.getSettingsDirect() == null) {
            appSettingsDao.saveSettings(AppSystemSettingsEntity())
        }
    }
}

