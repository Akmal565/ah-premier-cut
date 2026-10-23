package com.example.domain

import android.content.Context
import com.example.auth.AuthManager
import com.example.auth.AuthResult
import com.example.auth.StorageBreakdown
import com.example.data.local.OAuthConnectionEntity
import com.example.data.local.UserAccountEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object StudioAccountManager {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var authManager: AuthManager? = null

  // Real Account & Auth State
  private val _currentAccount = MutableStateFlow<UserAccountEntity?>(null)
  val currentAccount: StateFlow<UserAccountEntity?> = _currentAccount.asStateFlow()

  private val _allAccounts = MutableStateFlow<List<UserAccountEntity>>(emptyList())
  val allAccounts: StateFlow<List<UserAccountEntity>> = _allAccounts.asStateFlow()

  private val _oauthConnections = MutableStateFlow<List<OAuthConnectionEntity>>(emptyList())
  val oauthConnections: StateFlow<List<OAuthConnectionEntity>> = _oauthConnections.asStateFlow()

  private val _storageBreakdown = MutableStateFlow(StorageBreakdown(0L, 0L, 0L, 0L))
  val storageBreakdown: StateFlow<StorageBreakdown> = _storageBreakdown.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  fun init(context: Context) {
    if (authManager == null) {
      val manager = AuthManager.getInstance(context.applicationContext)
      authManager = manager

      scope.launch {
        manager.currentAccount.collect { account ->
          _currentAccount.value = account
        }
      }

      scope.launch {
        manager.allAccounts.collect { list ->
          _allAccounts.value = list
        }
      }

      scope.launch {
        manager.oauthConnections.collect { list ->
          _oauthConnections.value = list
        }
      }

      scope.launch {
        manager.storageBreakdown.collect { breakdown ->
          _storageBreakdown.value = breakdown
        }
      }

      scope.launch {
        manager.isLoading.collect { loading ->
          _isLoading.value = loading
        }
      }

      scope.launch {
        manager.authError.collect { err ->
          _authError.value = err
        }
      }
    }
  }

  suspend fun signInWithGoogle(activityContext: Context): AuthResult {
    val manager = authManager ?: AuthManager.getInstance(activityContext.applicationContext).also { authManager = it }
    return manager.signInWithGoogle(activityContext)
  }

  suspend fun signInWithEmail(email: String, pass: String): AuthResult {
    val manager = authManager ?: throw IllegalStateException("AuthManager not initialized")
    return manager.signInWithEmail(email, pass)
  }

  suspend fun registerWithEmail(email: String, pass: String, name: String): AuthResult {
    val manager = authManager ?: throw IllegalStateException("AuthManager not initialized")
    return manager.registerWithEmail(email, pass, name)
  }

  suspend fun sendPasswordReset(email: String): Result<Unit> {
    val manager = authManager ?: throw IllegalStateException("AuthManager not initialized")
    return manager.sendPasswordReset(email)
  }

  suspend fun updateProfile(name: String, bio: String, handle: String): Result<Unit> {
    val manager = authManager ?: return Result.failure(IllegalStateException("AuthManager not initialized"))
    return manager.updateProfile(name, bio, handle)
  }

  suspend fun switchActiveAccount(uid: String) {
    authManager?.switchAccount(uid)
  }

  suspend fun removeAccount(uid: String) {
    authManager?.removeAccount(uid)
  }

  suspend fun signOut() {
    authManager?.signOut()
  }

  suspend fun connectOAuthPlatform(
    platformId: String,
    accountHandle: String,
    accountId: String,
    accessToken: String,
    refreshToken: String? = null
  ) {
    authManager?.connectOAuthPlatform(platformId, accountHandle, accountId, accessToken, refreshToken)
  }

  suspend fun disconnectOAuthPlatform(platformId: String) {
    authManager?.disconnectOAuthPlatform(platformId)
  }

  suspend fun refreshStorageUsage(): StorageBreakdown {
    return authManager?.calculateRealStorageUsage() ?: StorageBreakdown(0L, 0L, 0L, 0L)
  }

  suspend fun clearRealAppCache(): Long {
    return authManager?.clearRealAppCache() ?: 0L
  }
}
