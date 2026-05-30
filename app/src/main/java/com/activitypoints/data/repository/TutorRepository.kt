package com.activitypoints.data.repository

import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.api.TutorApi
import com.activitypoints.data.api.safeApiCall
import com.activitypoints.models.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface TutorRepository {
    suspend fun getStudents(): NetworkResult<List<TutorStudent>>
    suspend fun getStudentDetails(id: String): NetworkResult<Student>
    suspend fun getStudentCertificates(studentId: String): NetworkResult<List<Certificate>>
    suspend fun getPendingCertificates(): NetworkResult<List<TutorPendingCert>>
    suspend fun getApprovedCertificates(): NetworkResult<List<Certificate>>
    suspend fun approveCertificate(id: String, points: Int): NetworkResult<Unit>
    suspend fun rejectCertificate(id: String, reason: String): NetworkResult<Unit>
    suspend fun uploadCsv(file: File): NetworkResult<Unit>
    suspend fun getTutorProfile(): NetworkResult<Tutor>
    suspend fun uploadTutorPhoto(file: File): NetworkResult<Tutor>
    suspend fun saveFcmToken(token: String): NetworkResult<Unit>
}

@Singleton
class TutorRepositoryImpl @Inject constructor(
    private val tutorApi: TutorApi,
) : TutorRepository {

    override suspend fun getStudents(): NetworkResult<List<TutorStudent>> =
        when (val result = safeApiCall { tutorApi.getStudents() }) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.students)
            is NetworkResult.Error   -> result
            is NetworkResult.Loading -> result
        }

    override suspend fun getStudentDetails(id: String) =
        safeApiCall { tutorApi.getStudentDetails(id) }

    override suspend fun getStudentCertificates(studentId: String): NetworkResult<List<Certificate>> =
        when (val result = safeApiCall { tutorApi.getApprovedCertificates() }) {
            is NetworkResult.Success -> NetworkResult.Success(
                result.data.certificates.filter { it.student?.id == studentId }
            )
            is NetworkResult.Error   -> result
            is NetworkResult.Loading -> result
        }

    override suspend fun getPendingCertificates() =
        safeApiCall { tutorApi.getPendingCertificates() }

    override suspend fun getApprovedCertificates(): NetworkResult<List<Certificate>> =
        when (val result = safeApiCall { tutorApi.getApprovedCertificates() }) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.certificates)
            is NetworkResult.Error   -> result
            is NetworkResult.Loading -> result
        }

    override suspend fun approveCertificate(id: String, points: Int) =
        safeApiCall { tutorApi.approveCertificate(id, mapOf("pointsAwarded" to points)) }

    override suspend fun rejectCertificate(id: String, reason: String) =
        safeApiCall { tutorApi.rejectCertificate(id, mapOf("rejectionReason" to reason)) }

    override suspend fun uploadCsv(file: File): NetworkResult<Unit> {
        val part = MultipartBody.Part.createFormData(
            name     = "file",
            filename = file.name,
            body     = file.asRequestBody("text/csv".toMediaTypeOrNull()),
        )
        return safeApiCall { tutorApi.uploadCsv(part) }
    }

    override suspend fun getTutorProfile() =
        safeApiCall { tutorApi.getMe() }

    override suspend fun uploadTutorPhoto(file: File): NetworkResult<Tutor> {
        val part = MultipartBody.Part.createFormData(
            name     = "photo",
            filename = file.name,
            body     = file.asRequestBody("image/jpeg".toMediaTypeOrNull()),
        )
        return safeApiCall { tutorApi.uploadPhoto(part) }
    }

    override suspend fun saveFcmToken(token: String) =
        safeApiCall { tutorApi.saveFcmToken(FcmTokenRequest(token)) }
}
