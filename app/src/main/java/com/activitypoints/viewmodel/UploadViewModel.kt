package com.activitypoints.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.repository.CertificateRepository
import com.activitypoints.utils.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class UploadUiState(
    val isLoading: Boolean = false,
    val success: Boolean   = false,
    val error: String?     = null,
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val certRepo: CertificateRepository,
    private val compressor: ImageCompressor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun upload(
        context: Context,
        categoryId: String,
        subcategoryName: String,
        eventName: String,
        level: String?,
        prizeType: String?,
        eventDate: String,
        fileUri: Uri,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }

            // Determine MIME type
            val mime = context.contentResolver.getType(fileUri) ?: "application/octet-stream"
            val isPdf = mime.contains("pdf", ignoreCase = true)

            // Get a real File — compress images, copy PDFs as-is
            val file: File? = withContext(Dispatchers.IO) {
                if (isPdf) {
                    copyUriToCache(context, fileUri, "upload.pdf")
                } else {
                    compressor.compress(fileUri)
                }
            }

            if (file == null) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to process the selected file.") }
                return@launch
            }

            val mimeForUpload = if (isPdf) "application/pdf" else "image/jpeg"

            when (val result = certRepo.uploadCertificate(
                categoryId      = categoryId,
                subcategoryName = subcategoryName,
                eventName       = eventName,
                level           = level,
                prizeType       = prizeType,
                eventDate       = eventDate,
                file            = file,
                mimeType        = mimeForUpload,
            )) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, success = true) }
                is NetworkResult.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }

            // Clean temp file
            withContext(Dispatchers.IO) { file.delete() }
        }
    }

    fun resetState() {
        _uiState.update { UploadUiState() }
    }

    private fun copyUriToCache(context: Context, uri: Uri, name: String): File? = try {
        val outFile = File(context.cacheDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        }
        outFile
    } catch (e: Exception) { null }
}
