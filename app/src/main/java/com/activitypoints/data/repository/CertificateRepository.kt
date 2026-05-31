package com.activitypoints.data.repository

import android.content.Context
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.api.StudentApi
import com.activitypoints.data.api.safeApiCall
import com.activitypoints.models.*
import com.activitypoints.utils.ImageCompressor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface CertificateRepository {
    suspend fun getMyCertificates(): NetworkResult<List<Certificate>>
    suspend fun getCategories(): NetworkResult<List<Category>>
    suspend fun deleteCertificate(id: String): NetworkResult<Unit>
    suspend fun uploadCertificate(
        context: Context,
        categoryId: String,
        subcategoryName: String,
        eventName: String,
        level: String?,
        prizeType: String?,
        dateFrom: String,
        dateTo: String,
        fileUri: android.net.Uri,
    ): NetworkResult<CertUploadResponse>
    suspend fun getMyTutor(): NetworkResult<MyTutorResponse>
}

@Singleton
class CertificateRepositoryImpl @Inject constructor(
    private val studentApi: StudentApi,
) : CertificateRepository {

    override suspend fun getMyCertificates(): NetworkResult<List<Certificate>> =
        when (val r = safeApiCall { studentApi.getMyCertificates() }) {
            is NetworkResult.Success -> NetworkResult.Success(r.data.certificates)
            is NetworkResult.Error   -> r
            is NetworkResult.Loading -> r
        }

    override suspend fun getCategories(): NetworkResult<List<Category>> =
        when (val r = safeApiCall { studentApi.getCategories() }) {
            is NetworkResult.Success -> NetworkResult.Success(r.data.categories)
            is NetworkResult.Error   -> r
            is NetworkResult.Loading -> r
        }

    override suspend fun deleteCertificate(id: String) =
        safeApiCall { studentApi.deleteCertificate(id) }

    override suspend fun uploadCertificate(
        context: Context,
        categoryId: String,
        subcategoryName: String,
        eventName: String,
        level: String?,
        prizeType: String?,
        dateFrom: String,
        dateTo: String,
        fileUri: android.net.Uri,
    ): NetworkResult<CertUploadResponse> {
        // Compress image if needed (≤3 MB)
        val compressedFile = ImageCompressor.compressFromUri(context, fileUri)
            ?: return NetworkResult.Error("Could not read the selected file.")

        val mimeType = when {
            compressedFile.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            else -> "image/jpeg"
        }

        val filePart = MultipartBody.Part.createFormData(
            name     = "file",
            filename = compressedFile.name,
            body     = compressedFile.asRequestBody(mimeType.toMediaTypeOrNull()),
        )

        val levelPart  = level?.takeIf { it.isNotBlank() }
            ?.toRequestBody("text/plain".toMediaTypeOrNull())
        val prizePart  = prizeType?.takeIf { it.isNotBlank() }
            ?.toRequestBody("text/plain".toMediaTypeOrNull())

        return safeApiCall {
            studentApi.uploadCertificate(
                categoryId      = categoryId.toRequestBody("text/plain".toMediaTypeOrNull()),
                subcategoryName = subcategoryName.toRequestBody("text/plain".toMediaTypeOrNull()),
                eventName       = eventName.toRequestBody("text/plain".toMediaTypeOrNull()),
                level           = levelPart,
                prizeType       = prizePart,
                dateFrom        = dateFrom.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateTo          = dateTo.toRequestBody("text/plain".toMediaTypeOrNull()),
                file            = filePart,
            )
        }
    }

    override suspend fun getMyTutor(): NetworkResult<MyTutorResponse> =
        safeApiCall { studentApi.getMyTutor() }
}