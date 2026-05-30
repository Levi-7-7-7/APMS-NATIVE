package com.activitypoints.data.repository

import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.api.StudentApi
import com.activitypoints.data.api.TutorApi
import com.activitypoints.data.api.safeApiCall
import com.activitypoints.data.local.TokenStore
import com.activitypoints.models.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val roleFlow: Flow<String?>
    suspend fun getRole(): String?

    // Student auth
    suspend fun loginStudent(registerNumber: String, password: String): NetworkResult<LoginResponse>
    suspend fun startOtpLogin(registerNumber: String): NetworkResult<Unit>
    suspend fun verifyOtp(registerNumber: String, otp: String): NetworkResult<LoginResponse>
    suspend fun forgotPassword(registerNumber: String): NetworkResult<Unit>
    suspend fun resetPassword(registerNumber: String, otp: String, newPassword: String): NetworkResult<Unit>
    suspend fun getStudentProfile(): NetworkResult<Student>

    // Tutor auth
    suspend fun loginTutor(email: String, password: String): NetworkResult<TutorLoginResponse>
    suspend fun tutorForgotPassword(email: String): NetworkResult<Unit>
    suspend fun tutorResetPassword(email: String, otp: String, newPassword: String): NetworkResult<Unit>
    suspend fun getTutorProfile(): NetworkResult<Tutor>

    // Session
    suspend fun saveStudentSession(token: String, name: String)
    suspend fun saveTutorSession(token: String, name: String)
    suspend fun logout()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val studentApi: StudentApi,
    private val tutorApi: TutorApi,
    private val tokenStore: TokenStore,
) : AuthRepository {

    override val roleFlow: Flow<String?> = tokenStore.roleFlow
    override suspend fun getRole(): String? = tokenStore.getRole()

    // ── Student ────────────────────────────────────────────────────────────────

    override suspend fun loginStudent(registerNumber: String, password: String) =
        safeApiCall { studentApi.login(LoginRequest(registerNumber, password)) }

    override suspend fun startOtpLogin(registerNumber: String) =
        safeApiCall { studentApi.startLogin(OtpRequest(registerNumber)) }

    override suspend fun verifyOtp(registerNumber: String, otp: String) =
        safeApiCall { studentApi.verifyOtp(VerifyOtpRequest(registerNumber, otp)) }

    override suspend fun forgotPassword(registerNumber: String) =
        safeApiCall { studentApi.forgotPassword(ForgotPasswordRequest(registerNumber)) }

    override suspend fun resetPassword(registerNumber: String, otp: String, newPassword: String) =
        safeApiCall { studentApi.resetPassword(ResetPasswordRequest(registerNumber, otp, newPassword)) }

    override suspend fun getStudentProfile() =
        safeApiCall { studentApi.getMe() }

    // ── Tutor ──────────────────────────────────────────────────────────────────

    override suspend fun loginTutor(email: String, password: String) =
        safeApiCall { tutorApi.login(TutorLoginRequest(email, password)) }

    override suspend fun tutorForgotPassword(email: String) =
        safeApiCall { tutorApi.forgotPassword(TutorForgotPasswordRequest(email)) }

    override suspend fun tutorResetPassword(email: String, otp: String, newPassword: String) =
        safeApiCall { tutorApi.resetPassword(TutorResetPasswordRequest(email, otp, newPassword)) }

    override suspend fun getTutorProfile() =
        safeApiCall { tutorApi.getMe() }

    // ── Session helpers ────────────────────────────────────────────────────────

    override suspend fun saveStudentSession(token: String, name: String) =
        tokenStore.saveStudentSession(token, name)

    override suspend fun saveTutorSession(token: String, name: String) =
        tokenStore.saveTutorSession(token, name)

    override suspend fun logout() = tokenStore.clearAll()
}
