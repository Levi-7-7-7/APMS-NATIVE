package com.activitypoints.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.repository.CertificateRepository
import com.activitypoints.data.repository.TutorRepository
import com.activitypoints.models.*
import com.activitypoints.utils.CalcPoints
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Sort / Filter ──────────────────────────────────────────────────────────────

enum class StudentSortKey { REGISTER_NUMBER, NAME, TOTAL_POINTS, BATCH, BRANCH }
enum class SortDir { ASC, DESC }

// ── State classes ──────────────────────────────────────────────────────────────

data class TutorStudentsUiState(
    val isLoading: Boolean           = true,
    val students: List<TutorStudent> = emptyList(),
    val searchQuery: String          = "",
    val sortKey: StudentSortKey      = StudentSortKey.REGISTER_NUMBER,
    val sortDir: SortDir             = SortDir.ASC,
    val filterBatch: String          = "",
    val filterBranch: String         = "",
    val error: String?               = null,
    val isRefreshing: Boolean        = false,
)

data class TutorPendingUiState(
    val isLoading: Boolean             = true,
    val certs: List<TutorPendingCert>  = emptyList(),
    val error: String?                 = null,
    val isRefreshing: Boolean          = false,
    val actionResult: String?          = null,
)

data class TutorApprovedUiState(
    val isLoading: Boolean       = true,
    val certs: List<Certificate> = emptyList(),
    val searchQuery: String      = "",
    val error: String?           = null,
    val isRefreshing: Boolean    = false,
)

data class StudentDetailUiState(
    val isLoading: Boolean              = true,
    val student: Student?               = null,
    val certificates: List<Certificate> = emptyList(),
    val categories: List<Category>      = emptyList(),
    val activeFilter: String            = "all",
    val error: String?                  = null,
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
    private val certRepo: CertificateRepository,
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
                is NetworkResult.Error -> _studentsState.update {
                    it.copy(error = r.message, isLoading = false, isRefreshing = false)
                }
                else -> Unit
            }
        }
    }

    fun setStudentSearch(q: String) = _studentsState.update { it.copy(searchQuery = q) }

    fun setStudentSort(key: StudentSortKey) {
        _studentsState.update { state ->
            val newDir = if (state.sortKey == key) {
                if (state.sortDir == SortDir.ASC) SortDir.DESC else SortDir.ASC
            } else {
                when (key) {
                    StudentSortKey.TOTAL_POINTS -> SortDir.DESC
                    else                        -> SortDir.ASC
                }
            }
            state.copy(sortKey = key, sortDir = newDir)
        }
    }

    fun setFilterBatch(batch: String)   = _studentsState.update { it.copy(filterBatch = batch) }
    fun setFilterBranch(branch: String) = _studentsState.update { it.copy(filterBranch = branch) }
    fun clearStudentFilters() = _studentsState.update { it.copy(filterBatch = "", filterBranch = "") }

    fun filteredStudents(): List<TutorStudent> {
        val s   = _studentsState.value
        val q   = s.searchQuery.trim().lowercase()
        var list = s.students

        if (q.isNotEmpty()) {
            list = list.filter {
                it.name.lowercase().contains(q) || it.registerNumber.lowercase().contains(q)
            }
        }
        if (s.filterBatch.isNotEmpty())  list = list.filter { it.batch?.name == s.filterBatch }
        if (s.filterBranch.isNotEmpty()) list = list.filter { it.branch?.name == s.filterBranch }

        val m = if (s.sortDir == SortDir.ASC) 1 else -1
        list = when (s.sortKey) {
            StudentSortKey.NAME            -> list.sortedWith(compareBy { it.name.lowercase() })
            StudentSortKey.REGISTER_NUMBER -> list.sortedWith(compareBy { it.registerNumber.lowercase() })
            StudentSortKey.TOTAL_POINTS    -> list.sortedWith(compareByDescending { it.totalPoints * m })
            StudentSortKey.BATCH           -> list.sortedWith(compareBy { it.batch?.name?.lowercase() ?: "" })
            StudentSortKey.BRANCH          -> list.sortedWith(compareBy { it.branch?.name?.lowercase() ?: "" })
        }
        // For non-TOTAL_POINTS keys apply direction after sorting ascending
        if (s.sortKey != StudentSortKey.TOTAL_POINTS && s.sortDir == SortDir.DESC) {
            list = list.reversed()
        }
        return list
    }

    fun allBatches(): List<String> =
        _studentsState.value.students.mapNotNull { it.batch?.name }.distinct().sorted()

    fun allBranches(): List<String> =
        _studentsState.value.students.mapNotNull { it.branch?.name }.distinct().sorted()

    // ── Pending ────────────────────────────────────────────────────────────────

    fun loadPending(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _pendingState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh) }
            when (val r = tutorRepo.getPendingCertificates()) {
                is NetworkResult.Success -> _pendingState.update {
                    it.copy(certs = r.data, isLoading = false, isRefreshing = false)
                }
                is NetworkResult.Error -> _pendingState.update {
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
                is NetworkResult.Error -> _approvedState.update {
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
            _detailState.update { it.copy(isLoading = true, error = null) }

            // Fetch student info, all approved certs (filter client-side), and categories in parallel
            val studentResult = tutorRepo.getStudentDetails(studentId)
            val certsResult   = tutorRepo.getStudentCertificates(studentId)
            val catsResult    = certRepo.getCategories()

            val student    = (studentResult as? NetworkResult.Success)?.data
            val certs      = (certsResult   as? NetworkResult.Success)?.data ?: emptyList()
            val categories = (catsResult    as? NetworkResult.Success)?.data ?: emptyList()

            val error = when {
                studentResult is NetworkResult.Error -> studentResult.message
                else -> null
            }

            _detailState.update {
                it.copy(
                    isLoading    = false,
                    student      = student,
                    certificates = certs,
                    categories   = categories,
                    error        = error,
                )
            }
        }
    }

    fun setDetailFilter(filter: String) = _detailState.update { it.copy(activeFilter = filter) }

    fun filteredDetailCerts(): List<Certificate> {
        val state = _detailState.value
        return when (state.activeFilter) {
            "all" -> state.certificates
            else  -> state.certificates.filter { it.status.lowercase() == state.activeFilter }
        }
    }

    /**
     * Compute capped points for the student detail screen.
     * Uses the full category list (with maxPoints) — matches RN calcCappedPoints exactly.
     */
    fun detailCappedPoints(): Int {
        val state    = _detailState.value
        val approved = state.certificates.filter { it.status.lowercase() == "approved" }
        return CalcPoints.calcCappedPoints(
            approvedCerts = approved,
            categories    = state.categories,
            isLateralEntry = state.student?.isLateralEntry ?: false,
        )
    }

    fun detailRawPoints(): Int {
        val approved = _detailState.value.certificates.filter { it.status.lowercase() == "approved" }
        return approved.sumOf { it.pointsAwarded ?: 0 }
    }

    // ── CSV Upload ─────────────────────────────────────────────────────────────

    fun uploadCsv(file: java.io.File) {
        viewModelScope.launch {
            _csvState.update { it.copy(isUploading = true, result = null, error = null) }
            when (val r = tutorRepo.uploadCsv(file)) {
                is NetworkResult.Success -> _csvState.update {
                    it.copy(isUploading = false, result = "CSV uploaded successfully!")
                }
                is NetworkResult.Error -> _csvState.update {
                    it.copy(isUploading = false, error = r.message)
                }
                else -> Unit
            }
        }
    }

    fun clearCsvResult() = _csvState.update { it.copy(result = null, error = null) }
}
