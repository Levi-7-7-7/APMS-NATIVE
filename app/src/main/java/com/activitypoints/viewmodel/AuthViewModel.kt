package com.activitypoints.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.repository.AuthRepository
import com.activitypoints.models.Student
import com.activitypoints.models.Tutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.activitypoints.viewmodel.AuthViewModel
// ── UI state ───────────────────────────────────────────────────────────────────

sealed interface AuthState {
    data object Loading  : AuthState
    data object LoggedOut: AuthState
    data class  Student(val profile: com.activitypoints.models.Student?) : AuthState
    data class  Tutor(val profile: com.activitypoints.models.Tutor?)     : AuthState
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String?     = null,
    val success: Boolean   = false,
)

// ── ViewModel ──────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    init {
        initSession()
    }

    /**
     * On startup: check stored role and validate the session.
     * Mirrors AuthContext's init() in the RN app exactly.
     */
    private fun initSession() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                when (authRepo.getRole()) {
                    "student" -> {
                        val result = authRepo.getStudentProfile()
                        _authState.value = when (result) {
                            is NetworkResult.Success -> AuthState.Student(result.data)
                            is NetworkResult.Error   ->
                                if (result.code == 401) AuthState.LoggedOut
                                else AuthState.Student(null) // keep session on network error
                            else -> AuthState.LoggedOut
                        }
                    }
                    "tutor" -> {
                        val result = authRepo.getTutorProfile()
                        _authState.value = when (result) {
                            is NetworkResult.Success -> AuthState.Tutor(result.data)
                            is NetworkResult.Error   ->
                                if (result.code == 401) AuthState.LoggedOut
                                else AuthState.Tutor(null) // keep session on network error
                            else -> AuthState.LoggedOut
                        }
                    }
                    else -> _authState.value = AuthState.LoggedOut
                }
            } catch (e: Exception) {
                _authState.value = AuthState.LoggedOut
            }
        }
    }

    // ── Student login ──────────────────────────────────────────────────────────

    fun loginStudent(registerNumber: String, password: String) {
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepo.loginStudent(registerNumber, password)) {
                is NetworkResult.Success -> {
                    val token = result.data.token
                    val name  = result.data.student?.name ?: "Student"
                    authRepo.saveStudentSession(token, name)
                    _authState.value = AuthState.Student(result.data.student)
                    _loginState.update { it.copy(isLoading = false, success = true) }
                }
                is NetworkResult.Error -> {
                    _loginState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }

    // ── Tutor login ────────────────────────────────────────────────────────────

    fun loginTutor(email: String, password: String) {
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepo.loginTutor(email, password)) {
                is NetworkResult.Success -> {
                    val token = result.data.token
                    val name  = result.data.tutor?.name ?: "Tutor"
                    authRepo.saveTutorSession(token, name)
                    _authState.value = AuthState.Tutor(result.data.tutor)
                    _loginState.update { it.copy(isLoading = false, success = true) }
                }
                is NetworkResult.Error -> {
                    _loginState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }

    // ── Logout ─────────────────────────────────────────────────────────────────

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _authState.value = AuthState.LoggedOut
        }
    }

    fun clearLoginError() {
        _loginState.update { it.copy(error = null) }
    }

    // ── Profile refresh (called after profile edits) ───────────────────────────

    fun refreshStudentProfile(profile: Student) {
        _authState.value = AuthState.Student(profile)
    }

    fun refreshTutorProfile(profile: Tutor) {
        _authState.value = AuthState.Tutor(profile)
    }
}
