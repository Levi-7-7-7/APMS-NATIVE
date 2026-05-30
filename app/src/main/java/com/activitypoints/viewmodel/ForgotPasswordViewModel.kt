package com.activitypoints.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.repository.AuthRepository
import com.activitypoints.models.LoginResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPwUiState(
    val isLoading: Boolean       = false,
    val success: Boolean         = false,   // OTP sent
    val resetSuccess: Boolean    = false,   // password changed
    val otpLoginSuccess: LoginResponse? = null,
    val error: String?           = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPwUiState())
    val uiState: StateFlow<ForgotPwUiState> = _uiState.asStateFlow()

    fun sendStudentOtp(registerNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = authRepo.startOtpLogin(registerNumber)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, success = true) }
                is NetworkResult.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun verifyOtpLogin(registerNumber: String, otp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = authRepo.verifyOtp(registerNumber, otp)) {
                is NetworkResult.Success -> {
                    authRepo.saveStudentSession(r.data.token, r.data.student?.name ?: "Student")
                    _uiState.update { it.copy(isLoading = false, otpLoginSuccess = r.data) }
                }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun resetStudentPassword(registerNumber: String, otp: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = authRepo.resetPassword(registerNumber, otp, newPassword)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, resetSuccess = true) }
                is NetworkResult.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun sendTutorOtp(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = authRepo.tutorForgotPassword(email)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, success = true) }
                is NetworkResult.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun resetTutorPassword(email: String, otp: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = authRepo.tutorResetPassword(email, otp, newPassword)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, resetSuccess = true) }
                is NetworkResult.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(success = false, resetSuccess = false, otpLoginSuccess = null) }
    }
}
