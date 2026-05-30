package com.activitypoints.data.repository

import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.api.StudentApi
import com.activitypoints.data.api.safeApiCall
import com.activitypoints.models.Category
import com.activitypoints.models.Certificate
import com.activitypoints.models.CertUploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface CertificateRepository {
    suspend fun getMyCertificates(): NetworkResult<List<Certificate>>
    suspend fun deleteCertificate(id: String): NetworkResult<Unit>
    suspend fun uploadCertificate(
        categoryId: String,
        subcategoryName: String,
        eventName: String,
        level: String?,
        prizeType: String?,
        eventDate: String,
        file: File,
        mimeType: String,
    ): NetworkResult<CertUploadResponse>
    suspend fun getCategories(): NetworkResult<List<Category>>
}

@Singleton
class CertificateRepositoryImpl @Inject constructor(
    private val studentApi: StudentApi,
) : CertificateRepository {

    override suspend fun getMyCertificates() =
        safeApiCall { studentApi.getMyCertificates() }

    override suspend fun deleteCertificate(id: String) =
        safeApiCall { studentApi.deleteCertificate(id) }

    override suspend fun uploadCertificate(
        categoryId: String,
        subcategoryName: String,
        eventName: String,
        level: String?,
        prizeType: String?,
        eventDate: String,
        file: File,
        mimeType: String,
    ): NetworkResult<CertUploadResponse> {
        val toBody = { s: String -> s.toRequestBody("text/plain".toMediaTypeOrNull()) }

        val filePart = MultipartBody.Part.createFormData(
            name     = "file",
            filename = file.name,
            body     = file.asRequestBody(mimeType.toMediaTypeOrNull()),
        )

        return safeApiCall {
            studentApi.uploadCertificate(
                categoryId      = toBody(categoryId),
                subcategoryName = toBody(subcategoryName),
                eventName       = toBody(eventName),
                level           = level?.let { toBody(it) },
                prizeType       = prizeType?.let { toBody(it) },
                eventDate       = toBody(eventDate),
                file            = filePart,
            )
        }
    }

    override suspend fun getCategories() =
        safeApiCall { studentApi.getCategories() }
}
