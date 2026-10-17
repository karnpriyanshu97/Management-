package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("edupulse_multi_tenant_session_prefs", Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(prefs.getString(KEY_USER_ID, null))
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _currentPhone = MutableStateFlow(prefs.getString(KEY_PHONE, null))
    val currentPhone: StateFlow<String?> = _currentPhone.asStateFlow()

    private val _activeOrganizationId = MutableStateFlow(prefs.getString(KEY_ORG_ID, null))
    val activeOrganizationId: StateFlow<String?> = _activeOrganizationId.asStateFlow()

    private val _activeMembershipId = MutableStateFlow(prefs.getString(KEY_MEMBERSHIP_ID, null))
    val activeMembershipId: StateFlow<String?> = _activeMembershipId.asStateFlow()

    fun createSession(userId: String, phoneNumber: String, organizationId: String, membershipId: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_PHONE, phoneNumber)
            .putString(KEY_ORG_ID, organizationId)
            .putString(KEY_MEMBERSHIP_ID, membershipId)
            .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            .apply()
        _currentUserId.value = userId
        _currentPhone.value = phoneNumber
        _activeOrganizationId.value = organizationId
        _activeMembershipId.value = membershipId
    }

    fun switchOrganization(organizationId: String, membershipId: String) {
        prefs.edit()
            .putString(KEY_ORG_ID, organizationId)
            .putString(KEY_MEMBERSHIP_ID, membershipId)
            .apply()
        _activeOrganizationId.value = organizationId
        _activeMembershipId.value = membershipId
    }

    fun switchInstitution(organizationId: String, userId: String, membershipId: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_ORG_ID, organizationId)
            .putString(KEY_MEMBERSHIP_ID, membershipId)
            .apply()
        _currentUserId.value = userId
        _activeOrganizationId.value = organizationId
        _activeMembershipId.value = membershipId
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _currentUserId.value = null
        _currentPhone.value = null
        _activeOrganizationId.value = null
        _activeMembershipId.value = null
    }

    fun isLoggedIn(): Boolean {
        return prefs.getString(KEY_USER_ID, null) != null && prefs.getString(KEY_ORG_ID, null) != null
    }

    companion object {
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_PHONE = "session_phone"
        private const val KEY_ORG_ID = "session_organization_id"
        private const val KEY_MEMBERSHIP_ID = "session_membership_id"
        private const val KEY_LOGIN_TIME = "session_login_time"
    }
}
