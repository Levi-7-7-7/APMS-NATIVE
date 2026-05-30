package com.activitypoints.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.api.StudentApi
import com.activitypoints.data.api.safeApiCall
import com.activitypoints.models.ProfileUpdateRequest
import com.activitypoints.models.Student
import com.activitypoints.utils.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import javax.inject.Inject

data class ProfileUiState(
    val isUpdating: Boolean      = false,
    val isUploadingPhoto: Boolean = false,
    val updatedStudent: Student? = null,
    val error: String?           = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val studentApi: StudentApi,
    private val compressor: ImageCompressor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun updateStudentPhone(phone: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }
            when (val result = safeApiCall { studentApi.updateProfile(ProfileUpdateRequest(phone = phone)) }) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isUpdating = false, updatedStudent = result.data)
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isUpdating = false, error = result.message)
                }
                else -> Unit
            }
        }
    }

    fun uploadStudentPhoto(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, error = null) }

            val file = withContext(Dispatchers.IO) { compressor.compress(uri) }
            if (file == null) {
                _uiState.update { it.copy(isUploadingPhoto = false, error = "Failed to process image") }
                return@launch
            }

            val part = MultipartBody.Part.createFormData(
                name     = "photo",
                filename = file.name,
                body     = file.asRequestBody("image/jpeg".toMediaTypeOrNull()),
            )
            when (val result = safeApiCall { studentApi.uploadPhoto(part) }) {
                is NetworkResult.Success -> {
                    withContext(Dispatchers.IO) { file.delete() }
                    _uiState.update { it.copy(isUploadingPhoto = false, updatedStudent = result.data) }
                }
                is NetworkResult.Error -> {
                    withContext(Dispatchers.IO) { file.delete() }
                    _uiState.update { it.copy(isUploadingPhoto = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(updatedStudent = null, error = null) }
    }
}
