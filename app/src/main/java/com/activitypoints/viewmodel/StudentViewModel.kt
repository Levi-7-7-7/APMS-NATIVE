package com.activitypoints.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.repository.CertificateRepository
import com.activitypoints.models.Category
import com.activitypoints.models.Certificate
import com.activitypoints.utils.CalcPoints
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentUiState(
    val isLoading: Boolean        = true,
    val certificates: List<Certificate> = emptyList(),
    val categories: List<Category>      = emptyList(),
    val totalPoints: Int          = 0,
    val passThreshold: Int        = 60,
    val isLateralEntry: Boolean   = false,
    val activeFilter: String      = "all",
    val error: String?            = null,
    val isRefreshing: Boolean     = false,
)

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val certRepo: CertificateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            // Run both requests; categories are cached so the second call is fast
            val certsResult = certRepo.getMyCertificates()
            val catsResult  = certRepo.getCategories()

            val certs = when (certsResult) {
                is NetworkResult.Success -> certsResult.data
                is NetworkResult.Error   -> {
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = certsResult.message)
                    }
                    return@launch
                }
                else -> emptyList()
            }

            val cats = when (catsResult) {
                is NetworkResult.Success -> catsResult.data
                else -> emptyList()
            }

            val approved     = certs.filter { it.status.lowercase() == "approved" }
            val isLateral    = _uiState.value.isLateralEntry
            val totalPoints  = CalcPoints.calcCappedPoints(approved, cats, isLateral)
            val threshold    = CalcPoints.passThreshold(isLateral)

            _uiState.update {
                it.copy(
                    isLoading     = false,
                    isRefreshing  = false,
                    certificates  = certs,
                    categories    = cats,
                    totalPoints   = totalPoints,
                    passThreshold = threshold,
                    error         = null,
                )
            }
        }
    }

    fun setLateralEntry(isLateral: Boolean) {
        _uiState.update { state ->
            val approved    = state.certificates.filter { it.status.lowercase() == "approved" }
            val totalPoints = CalcPoints.calcCappedPoints(approved, state.categories, isLateral)
            state.copy(
                isLateralEntry = isLateral,
                totalPoints    = totalPoints,
                passThreshold  = CalcPoints.passThreshold(isLateral),
            )
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun deleteCertificate(id: String) {
        viewModelScope.launch {
            when (certRepo.deleteCertificate(id)) {
                is NetworkResult.Success -> loadAll()
                is NetworkResult.Error   -> { /* surface snackbar via SharedFlow if desired */ }
                else -> Unit
            }
        }
    }

    fun getFilteredCertificates(): List<Certificate> {
        val state = _uiState.value
        return when (state.activeFilter) {
            "all"      -> state.certificates
            else       -> state.certificates.filter {
                it.status.lowercase() == state.activeFilter
            }
        }
    }
}
