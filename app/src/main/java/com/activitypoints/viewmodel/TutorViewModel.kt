package com.activitypoints.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.repository.TutorRepository
import com.activitypoints.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State classes ──────────────────────────────────────────────────────────────

data class TutorStudentsUiState(
    val isLoading: Boolean          = true,
    val students: List<TutorStudent> = emptyList(),
    val searchQuery: String         = "",
    val error: String?              = null,
    val isRefreshing: Boolean       = false,
)

data class TutorPendingUiState(
    val isLoading: Boolean              = true,
    val certs: List<TutorPendingCert>  = emptyList(),
    val error: String?                  = null,
    val isRefreshing: Boolean           = false,
    val actionResult: String?           = null,
)

data class TutorApprovedUiState(
    val isLoading: Boolean              = true,
    val certs: List<TutorApprovedCert> = emptyList(),
    val searchQuery: String             = "",
    val error: String?                  = null,
    val isRefreshing: Boolean           = false,
)

data class StudentDetailUiState(
    val isLoading: Boolean           = true,
    val student: Student?            = null,
    val certificates: List<Certificate> = emptyList(),
    val activeFilter: String         = "all",
    val error: String?               = null,
)

data class CsvUploadUiState(
    val isUploading: Boolean = false,
    val result: String?      = null,
    val error: String?       = null,
)

// ── ViewModel ──────────────────────────────────────────────────────────────────

@HiltViewModel
class TutorViewModel @Inject constructor(
    private val tutorRepo: TutorRepository,
) : ViewModel() {

    private val _studentsState = MutableStateFlow(TutorStudentsUiState())
    val studentsState: StateFlow<TutorStudentsUiState> = _studentsState.asStateFlow()

    private val _pendingState = MutableStateFlow(TutorPendingUiState())
    val pendingState: StateFlow<TutorPendingUiState> = _pendingState.asStateFlow()

    private val _approvedState = MutableStateFlow(TutorApprovedUiState())
    val approvedState: StateFlow<TutorApprovedUiState> = _approvedState.asStateFlow()

    private val _detailState = MutableStateFlow(StudentDetailUiState())
    val detailState: StateFlow<StudentDetailUiState> = _detailState.asStateFlow()

    private val _csvState = MutableStateFlow(CsvUploadUiState())
    val csvState: StateFlow<CsvUploadUiState> = _csvState.asStateFlow()

    init {
        loadStudents()
        loadPending()
        loadApproved()
    }

    // ── Students ───────────────────────────────────────────────────────────────

    fun loadStudents(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _studentsState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh) }
            when (val r = tutorRepo.getStudents()) {
                is NetworkResult.Success -> _studentsState.update {
                    it.copy(students = r.data, isLoading = false, isRefreshing = false)
                }
                is NetworkResult.Error   -> _studentsState.update {
                    it.copy(error = r.message, isLoading = false, isRefreshing = false)
                }
                else -> Unit
            }
        }
    }

    fun setStudentSearch(q: String) = _studentsState.update { it.copy(searchQuery = q) }

    fun filteredStudents(): List<TutorStudent> {
        val q = _studentsState.value.searchQuery.trim().lowercase()
        return if (q.isEmpty()) _studentsState.value.students
        else _studentsState.value.students.filter {
            it.name.lowercase().contains(q) || it.registerNumber.lowercase().contains(q)
        }
    }

    // ── Pending ────────────────────────────────────────────────────────────────

    fun loadPending(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _pendingState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh) }
            when (val r = tutorRepo.getPendingCertificates()) {
                is NetworkResult.Success -> _pendingState.update {
                    it.copy(certs = r.data, isLoading = false, isRefreshing = false)
                }
                is NetworkResult.Error   -> _pendingState.update {
                    it.copy(error = r.message, isLoading = false, isRefreshing = false)
                }
                else -> Unit
            }
        }
    }

    fun approveCertificate(id: String, points: Int) {
        viewModelScope.launch {
            when (val r = tutorRepo.approveCertificate(id, points)) {
                is NetworkResult.Success -> {
                    _pendingState.update { it.copy(actionResult = "Approved successfully") }
                    loadPending()
                }
                is NetworkResult.Error -> _pendingState.update { it.copy(actionResult = "Error: ${r.message}") }
                else -> Unit
            }
        }
    }

    fun rejectCertificate(id: String, reason: String) {
        viewModelScope.launch {
            when (val r = tutorRepo.rejectCertificate(id, reason)) {
                is NetworkResult.Success -> {
                    _pendingState.update { it.copy(actionResult = "Rejected") }
                    loadPending()
                }
                is NetworkResult.Error -> _pendingState.update { it.copy(actionResult = "Error: ${r.message}") }
                else -> Unit
            }
        }
    }

    fun clearPendingActionResult() = _pendingState.update { it.copy(actionResult = null) }

    // ── Approved ───────────────────────────────────────────────────────────────

    fun loadApproved(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _approvedState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh) }
            when (val r = tutorRepo.getApprovedCertificates()) {
                is NetworkResult.Success -> _approvedState.update {
                    it.copy(certs = r.data, isLoading = false, isRefreshing = false)
                }
                is NetworkResult.Error   -> _approvedState.update {
                    it.copy(error = r.message, isLoading = false, isRefreshing = false)
                }
                else -> Unit
            }
        }
    }

    fun setApprovedSearch(q: String) = _approvedState.update { it.copy(searchQuery = q) }

    // ── Student Details ────────────────────────────────────────────────────────

    fun loadStudentDetail(studentId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            val studentResult = tutorRepo.getStudentDetails(studentId)
            val certsResult   = tutorRepo.getStudentCertificates(studentId)

            val student = (studentResult as? NetworkResult.Success)?.data
            val certs   = (certsResult as? NetworkResult.Success)?.data ?: emptyList()

            _detailState.update {
                it.copy(isLoading = false, student = student, certificates = certs)
            }
        }
    }

    fun setDetailFilter(filter: String) = _detailState.update { it.copy(activeFilter = filter) }

    fun filteredDetailCerts(): List<Certificate> {
        val state = _detailState.value
        return when (state.activeFilter) {
            "all"  -> state.certificates
            else   -> state.certificates.filter { it.status.lowercase() == state.activeFilter }
        }
    }

    // ── CSV Upload ─────────────────────────────────────────────────────────────

    fun uploadCsv(file: java.io.File) {
        viewModelScope.launch {
            _csvState.update { it.copy(isUploading = true, result = null, error = null) }
            when (val r = tutorRepo.uploadCsv(file)) {
                is NetworkResult.Success -> _csvState.update {
                    it.copy(isUploading = false, result = "CSV uploaded successfully!")
                }
                is NetworkResult.Error   -> _csvState.update {
                    it.copy(isUploading = false, error = r.message)
                }
                else -> Unit
            }
        }
    }

    fun clearCsvResult() = _csvState.update { it.copy(result = null, error = null) }
}
