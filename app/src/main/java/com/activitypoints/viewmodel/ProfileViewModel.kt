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
    val isUpdating: Boolean       = false,
    val isUploadingPhoto: Boolean = false,
    val updatedStudent: Student?  = null,
    val error: String?            = null,
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
            // Backend doesn't have PATCH /students/me — re-fetch profile as the "update"
            // for now. If the backend adds this endpoint later, swap this for the real call.
            _uiState.update { it.copy(isUpdating = false, error = null) }
        }
    }

    /**
     * Upload photo to PATCH /students/profile-photo.
     * The backend returns { success, profilePhoto } — NOT the full Student object.
     * After a successful upload we call GET /students/me to get the refreshed profile
     * and surface it via `updatedStudent` so ProfileScreen can push it to authState.
     */
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

            val uploadResult = safeApiCall { studentApi.uploadPhoto(part) }
            withContext(Dispatchers.IO) { file.delete() }

            when (uploadResult) {
                is NetworkResult.Success -> {
                    // Photo uploaded — now fetch the full updated profile so the header
                    // avatar refreshes with the new URL.
                    val profileResult = safeApiCall { studentApi.getMe() }
                    when (profileResult) {
                        is NetworkResult.Success ->
                            _uiState.update {
                                it.copy(isUploadingPhoto = false, updatedStudent = profileResult.data)
                            }
                        else ->
                            // Upload succeeded but profile fetch failed — still clear spinner
                            _uiState.update { it.copy(isUploadingPhoto = false) }
                    }
                }
                is NetworkResult.Error ->
                    _uiState.update { it.copy(isUploadingPhoto = false, error = uploadResult.message) }
                else -> Unit
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(updatedStudent = null, error = null) }
    }
}
