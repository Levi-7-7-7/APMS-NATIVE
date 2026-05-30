package com.activitypoints.models

import com.google.gson.annotations.SerializedName

// ── Auth ───────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val registerNumber: String,
    val password: String,
)

data class LoginResponse(
    val token: String,
    // Login endpoint only returns { name } inside student — full profile
    // is fetched separately via GET /students/me
    val student: Student?,
)

data class TutorLoginRequest(
    val email: String,
    val password: String,
)

data class TutorLoginResponse(
    val token: String,
    val tutor: Tutor?,
)

data class OtpRequest(val registerNumber: String)
data class VerifyOtpRequest(val registerNumber: String, val otp: String)
data class ForgotPasswordRequest(val registerNumber: String)
data class ResetPasswordRequest(val registerNumber: String, val otp: String, val newPassword: String)
data class TutorForgotPasswordRequest(val email: String)
data class TutorResetPasswordRequest(val email: String, val otp: String, val newPassword: String)

// ── Student ────────────────────────────────────────────────────────────────────
//
// IMPORTANT: The backend's GET /students/me populates `batch` and `branch` as
// nested objects { _id, name }, NOT plain strings.  Gson cannot map a JsonObject
// into a String field — it throws silently and returns null for the whole object,
// which is why the name never showed up.
//
// The StudentDeserializer (FlexibleDeserializers.kt) extracts the nested `name`
// from those objects and stores it in `batchName` / `branchName` here.
//
// `photoUrl` maps to the backend field `profilePhoto` via StudentDeserializer.

data class Student(
    @SerializedName("_id") val id: String = "",
    val name: String = "",
    val registerNumber: String = "",
    val email: String = "",
    val isLateralEntry: Boolean = false,
    // Populated from batch.name / branch.name by StudentDeserializer
    val batchName: String? = null,
    val branchName: String? = null,
    // Maps from "profilePhoto" field — handled by StudentDeserializer
    val photoUrl: String? = null,
    val semester: String? = null,
    val phone: String? = null,
)

// ── Tutor ──────────────────────────────────────────────────────────────────────

data class Tutor(
    @SerializedName("_id") val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val department: String? = null,
)

// ── Certificate ────────────────────────────────────────────────────────────────

data class Certificate(
    @SerializedName("_id")  val id: String = "",
    val eventName: String? = null,
    val subcategory: String? = null,
    val category: Category? = null,
    val level: String? = null,
    val prizeType: String? = null,
    val status: String = "pending",
    val pointsAwarded: Int? = null,
    val potentialPoints: Int? = null,
    val fileUrl: String? = null,
    val eventDate: String? = null,
    val uploadedAt: String? = null,
    val rejectionReason: String? = null,
    val remarks: String? = null,
    val student: Student? = null,
)

// ── Category ───────────────────────────────────────────────────────────────────

data class Category(
    @SerializedName("_id") val id: String = "",
    val name: String = "",
    val maxPoints: Int? = null,
    val subcategories: List<Subcategory> = emptyList(),
)

data class Subcategory(
    @SerializedName("_id") val id: String = "",
    val name: String = "",
    val levels: List<Level> = emptyList(),
)

data class Level(
    @SerializedName("_id") val id: String = "",
    val name: String = "",
    val prizeTypes: List<PrizeType> = emptyList(),
    val points: Int? = null,
)

data class PrizeType(
    @SerializedName("_id") val id: String = "",
    val name: String = "",
    val points: Int = 0,
)

// ── Tutor Student (for tutor screens) ─────────────────────────────────────────

data class TutorStudent(
    @SerializedName("_id")          val id: String = "",
    val name: String = "",
    val registerNumber: String = "",
    val email: String = "",
    val batch: BatchBranch? = null,
    val branch: BatchBranch? = null,
    val isLateralEntry: Boolean = false,
    @SerializedName("profilePhoto") val photoUrl: String? = null,
    val totalPoints: Int = 0,
    val passThreshold: Int = 60,
    val createdAt: String? = null,
) {
    val totalApprovedPoints: Int get() = totalPoints
}

data class BatchBranch(
    @SerializedName("_id") val id: String = "",
    val name: String = "",
)

data class TutorPendingCert(
    @SerializedName("_id")    val id: String = "",
    val eventName: String? = null,
    val subcategory: String? = null,
    val category: Category? = null,
    val level: String? = null,
    val prizeType: String? = null,
    val status: String = "pending",
    val potentialPoints: Int? = null,
    val fileUrl: String? = null,
    val eventDate: String? = null,
    val student: Student? = null,
)

data class TutorApprovedCert(
    @SerializedName("_id")   val id: String = "",
    val eventName: String? = null,
    val subcategory: String? = null,
    val category: Category? = null,
    val level: String? = null,
    val prizeType: String? = null,
    val pointsAwarded: Int? = null,
    val fileUrl: String? = null,
    val eventDate: String? = null,
    val student: Student? = null,
)

// ── API Response Wrappers ──────────────────────────────────────────────────────

data class CertificatesResponse(
    val certificates: List<Certificate> = emptyList(),
)

data class CategoriesResponse(
    val categories: List<Category> = emptyList(),
)

data class StudentsResponse(
    val students: List<TutorStudent> = emptyList(),
)

// ── Upload ─────────────────────────────────────────────────────────────────────

data class CertUploadResponse(
    val message: String? = null,
    val certificate: Certificate? = null,
)

// Photo upload returns { success: true, profilePhoto: "https://..." }
data class PhotoUploadResponse(
    val success: Boolean = false,
    val profilePhoto: String? = null,
)

// ── Profile ────────────────────────────────────────────────────────────────────

data class ProfileUpdateRequest(
    val phone: String? = null,
)

// ── FCM ────────────────────────────────────────────────────────────────────────

data class FcmTokenRequest(val fcmToken: String)
