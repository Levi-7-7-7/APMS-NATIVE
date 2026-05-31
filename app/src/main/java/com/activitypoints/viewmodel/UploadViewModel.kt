package com.activitypoints.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.repository.CertificateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val isLoading: Boolean     = false,
    val uploadSuccess: Boolean = false,
    val error: String?         = null,
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val certRepo: CertificateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun upload(
        categoryId: String,
        subcategoryName: String,
        eventName: String,
        level: String?,
        prizeType: String?,
        dateFrom: String,
        dateTo: String,
        fileUri: Uri,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, uploadSuccess = false) }

            when (val r = certRepo.uploadCertificate(
                categoryId      = categoryId,
                subcategoryName = subcategoryName,
                eventName       = eventName,
                level           = level,
                prizeType       = prizeType,
                dateFrom        = dateFrom,
                dateTo          = dateTo,
                fileUri         = fileUri,
            )) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, uploadSuccess = true)
                }
                is NetworkResult.Error   -> _uiState.update {
                    it.copy(isLoading = false, error = r.message)
                }
                else -> Unit
            }
        }
    }

    fun clearSuccess() = _uiState.update { it.copy(uploadSuccess = false) }
    fun clearError()   = _uiState.update { it.copy(error = null) }
}