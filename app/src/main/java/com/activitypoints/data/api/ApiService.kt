package com.activitypoints.data.api

import com.activitypoints.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// ── Student / Auth API ─────────────────────────────────────────────────────────

interface StudentApi {

    // Auth
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/start-login")
    suspend fun startLogin(@Body request: OtpRequest): Response<Unit>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<LoginResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Unit>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>

    // Profile
    @GET("students/me")
    suspend fun getMe(): Response<Student>

    @PATCH("students/me")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<Student>

    @Multipart
    @PATCH("students/me/photo")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): Response<Student>

    // Certificates
    @GET("certificates/my")
    suspend fun getMyCertificates(): Response<List<Certificate>>

    @DELETE("certificates/{id}")
    suspend fun deleteCertificate(@Path("id") id: String): Response<Unit>

    @Multipart
    @POST("certificates/upload")
    suspend fun uploadCertificate(
        @Part("categoryId")     categoryId: RequestBody,
        @Part("subcategoryName") subcategoryName: RequestBody,
        @Part("eventName")      eventName: RequestBody,
        @Part("level")          level: RequestBody? = null,
        @Part("prizeType")      prizeType: RequestBody? = null,
        @Part("eventDate")      eventDate: RequestBody,
        @Part file: MultipartBody.Part,
    ): Response<CertUploadResponse>

    // Categories
    @GET("categories")
    suspend fun getCategories(): Response<List<Category>>

    // FCM
    @POST("students/fcm-token")
    suspend fun saveFcmToken(@Body request: FcmTokenRequest): Response<Unit>
}

// ── Tutor API ──────────────────────────────────────────────────────────────────

interface TutorApi {

    @POST("tutors/login")
    suspend fun login(@Body request: TutorLoginRequest): Response<TutorLoginResponse>

    @POST("tutors/forgot-password")
    suspend fun forgotPassword(@Body request: TutorForgotPasswordRequest): Response<Unit>

    @POST("tutors/reset-password")
    suspend fun resetPassword(@Body request: TutorResetPasswordRequest): Response<Unit>

    @GET("tutors/me")
    suspend fun getMe(): Response<Tutor>

    @Multipart
    @PATCH("tutors/me/photo")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): Response<Tutor>

    // Students
    @GET("tutors/students")
    suspend fun getStudents(): Response<List<TutorStudent>>

    @GET("tutors/students/{id}")
    suspend fun getStudentDetails(@Path("id") id: String): Response<Student>

    @GET("tutors/students/{id}/certificates")
    suspend fun getStudentCertificates(@Path("id") id: String): Response<List<Certificate>>

    // Pending / Approved
    @GET("tutors/pending")
    suspend fun getPendingCertificates(): Response<List<TutorPendingCert>>

    @GET("tutors/approved")
    suspend fun getApprovedCertificates(): Response<List<TutorApprovedCert>>

    @PATCH("tutors/certificates/{id}/approve")
    suspend fun approveCertificate(
        @Path("id") id: String,
        @Body body: Map<String, Int>,
    ): Response<Unit>

    @PATCH("tutors/certificates/{id}/reject")
    suspend fun rejectCertificate(
        @Path("id") id: String,
        @Body body: Map<String, String>,
    ): Response<Unit>

    // CSV Upload
    @Multipart
    @POST("tutors/upload-csv")
    suspend fun uploadCsv(@Part file: MultipartBody.Part): Response<Unit>

    // FCM
    @POST("tutors/fcm-token")
    suspend fun saveFcmToken(@Body request: FcmTokenRequest): Response<Unit>
}
