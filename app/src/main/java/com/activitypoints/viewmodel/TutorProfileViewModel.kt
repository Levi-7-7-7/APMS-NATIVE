package com.activitypoints.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.repository.TutorRepository
import com.activitypoints.models.Tutor
import com.activitypoints.utils.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TutorProfileUiState(
    val isUploading: Boolean = false,
    val updatedTutor: Tutor? = null,
    val error: String?       = null,
)

@HiltViewModel
class TutorProfileViewModel @Inject constructor(
    private val tutorRepo: TutorRepository,
    private val compressor: ImageCompressor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TutorProfileUiState())
    val uiState: StateFlow<TutorProfileUiState> = _uiState.asStateFlow()

    fun uploadPhoto(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }

            val file = withContext(Dispatchers.IO) { compressor.compress(uri) }
            if (file == null) {
                _uiState.update { it.copy(isUploading = false, error = "Failed to process image") }
                return@launch
            }

            when (val result = tutorRepo.uploadTutorPhoto(file)) {
                is NetworkResult.Success -> {
                    withContext(Dispatchers.IO) { file.delete() }
                    _uiState.update { it.copy(isUploading = false, updatedTutor = result.data) }
                }
                is NetworkResult.Error -> {
                    withContext(Dispatchers.IO) { file.delete() }
                    _uiState.update { it.copy(isUploading = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(updatedTutor = null, error = null) }
    }
}
