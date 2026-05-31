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

enum class StudentSortKey { REGISTER_NUMBER, NAME, TOTAL_POINTS, BATCH, BRANCH, RECENTLY_ADDED }
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
    val actionResult: String?    = null,
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
                is NetworkResult.Error   -> _studentsState.update {
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
                if (key == StudentSortKey.TOTAL_POINTS || key == StudentSortKey.RECENTLY_ADDED)
                    SortDir.DESC else SortDir.ASC
            }
            state.copy(sortKey = key, sortDir = newDir)
        }
    }
    fun setStudentFilterBatch(b: String)  = _studentsState.update { it.copy(filterBatch  = b) }
    fun setStudentFilterBranch(b: String) = _studentsState.update { it.copy(filterBranch = b) }
    fun clearStudentFilters()             = _studentsState.update { it.copy(filterBatch = "", filterBranch = "") }

    fun filteredSortedStudents(): List<TutorStudent> {
        val s = _studentsState.value
        var list = s.students
        if (s.searchQuery.isNotBlank()) {
            val q = s.searchQuery.lowercase()
            list = list.filter { it.name.lowercase().contains(q) || it.registerNumber.lowercase().contains(q) }
        }
        if (s.filterBatch.isNotBlank())  list = list.filter { it.batch?.name == s.filterBatch }
        if (s.filterBranch.isNotBlank()) list = list.filter { it.branch?.name == s.filterBranch }
        val m = if (s.sortDir == SortDir.ASC) 1 else -1
        return list.sortedWith(Comparator { a, b ->
            when (s.sortKey) {
                StudentSortKey.NAME            -> a.name.compareTo(b.name) * m
                StudentSortKey.TOTAL_POINTS    -> (a.totalPoints - b.totalPoints) * m
                StudentSortKey.BATCH           -> (a.batch?.name ?: "").compareTo(b.branch?.name ?: "") * m
                StudentSortKey.BRANCH          -> (a.branch?.name ?: "").compareTo(b.branch?.name ?: "") * m
                StudentSortKey.RECENTLY_ADDED  -> {
                    val at = a.createdAt ?: ""
                    val bt = b.createdAt ?: ""
                    bt.compareTo(at) * m  // desc by default for recent
                }
                else -> a.registerNumber.compareTo(b.registerNumber) * m
            }
        })
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
                    loadApproved()
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

    fun filteredApproved(): List<Certificate> {
        val s = _approvedState.value
        if (s.searchQuery.isBlank()) return s.certs
        val q = s.searchQuery.lowercase()
        return s.certs.filter {
            it.student?.name?.lowercase()?.contains(q) == true ||
                    it.student?.registerNumber?.lowercase()?.contains(q) == true
        }
    }

    /** Revert an approved certificate back to pending — mirrors RN TutorApprovedScreen. */
    fun revertCertificate(id: String) {
        viewModelScope.launch {
            when (val r = tutorRepo.revertCertificateToPending(id)) {
                is NetworkResult.Success -> {
                    _approvedState.update { it.copy(actionResult = "Certificate reverted to pending") }
                    loadApproved()
                    loadPending()
                }
                is NetworkResult.Error -> _approvedState.update {
                    it.copy(actionResult = "Error: ${r.message}")
                }
                else -> Unit
            }
        }
    }

    fun clearApprovedActionResult() = _approvedState.update { it.copy(actionResult = null) }

    // ── Student Details ────────────────────────────────────────────────────────

    fun loadStudentDetail(studentId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }

            val studentResult = tutorRepo.getStudentDetails(studentId)
            val certsResult   = tutorRepo.getStudentCertificates(studentId)
            val catsResult    = certRepo.getCategories()

            val student    = (studentResult as? NetworkResult.Success)?.data
            val certs      = (certsResult   as? NetworkResult.Success)?.data ?: emptyList()
            val categories = (catsResult    as? NetworkResult.Success)?.data ?: emptyList()

            val error = (studentResult as? NetworkResult.Error)?.message

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

    fun detailCappedPoints(): Int {
        val state    = _detailState.value
        val approved = state.certificates.filter { it.status.lowercase() == "approved" }
        return CalcPoints.calcCappedPoints(
            approvedCerts  = approved,
            categories     = state.categories,
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