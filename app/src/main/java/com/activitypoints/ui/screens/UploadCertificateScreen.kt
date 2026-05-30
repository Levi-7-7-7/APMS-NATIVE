package com.activitypoints.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.activitypoints.models.Category
import com.activitypoints.models.Subcategory
import com.activitypoints.utils.ImageCompressor
import com.activitypoints.viewmodel.UploadViewModel
import dagger.hilt.android.EntryPointAccessors
import java.text.SimpleDateFormat
import java.util.*

// A lightweight upload-specific ViewModel that wraps CertificateRepository
@Composable
fun UploadCertificateScreen(
    studentViewModel: com.activitypoints.viewmodel.StudentViewModel,
    onUploadSuccess: () -> Unit,
    uploadViewModel: UploadViewModel = hiltViewModel(),
) {
    val context     = LocalContext.current
    val uiState     by uploadViewModel.uiState.collectAsState()
    val categories  by studentViewModel.uiState.collectAsState()

    var selectedCat    by remember { mutableStateOf<Category?>(null) }
    var selectedSub    by remember { mutableStateOf<Subcategory?>(null) }
    var eventName      by remember { mutableStateOf("") }
    var selectedLevel  by remember { mutableStateOf("") }
    var selectedPrize  by remember { mutableStateOf("") }
    var eventDate      by remember { mutableStateOf("") }
    var pickedUri      by remember { mutableStateOf<Uri?>(null) }

    var showCatPicker  by remember { mutableStateOf(false) }
    var showSubPicker  by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate on success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            snackbarHostState.showSnackbar("Certificate uploaded!")
            studentViewModel.loadAll()
            uploadViewModel.resetState()
            onUploadSuccess()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    // Image / PDF picker
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> pickedUri = uri }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Upload Certificate", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // ── Category picker ────────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded         = showCatPicker,
                onExpandedChange = { showCatPicker = it },
            ) {
                OutlinedTextField(
                    value         = selectedCat?.name ?: "",
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Category") },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCatPicker) },
                    modifier      = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded         = showCatPicker,
                    onDismissRequest = { showCatPicker = false },
                ) {
                    categories.categories.forEach { cat ->
                        DropdownMenuItem(
                            text    = { Text(cat.name) },
                            onClick = {
                                selectedCat   = cat
                                selectedSub   = null
                                selectedLevel = ""
                                selectedPrize = ""
                                showCatPicker = false
                            },
                        )
                    }
                }
            }

            // ── Subcategory picker ─────────────────────────────────────────────
            if (selectedCat != null) {
                ExposedDropdownMenuBox(
                    expanded         = showSubPicker,
                    onExpandedChange = { showSubPicker = it },
                ) {
                    OutlinedTextField(
                        value         = selectedSub?.name ?: "",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Sub-category / Activity") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubPicker) },
                        modifier      = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded         = showSubPicker,
                        onDismissRequest = { showSubPicker = false },
                    ) {
                        (selectedCat?.subcategories ?: emptyList()).forEach { sub ->
                            DropdownMenuItem(
                                text    = { Text(sub.name) },
                                onClick = {
                                    selectedSub   = sub
                                    selectedLevel = ""
                                    selectedPrize = ""
                                    showSubPicker = false
                                },
                            )
                        }
                    }
                }
            }

            // ── Event name ─────────────────────────────────────────────────────
            OutlinedTextField(
                value         = eventName,
                onValueChange = { eventName = it },
                label         = { Text("Event Name") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // ── Level & Prize (if subcategory has them) ────────────────────────
            val levels = selectedSub?.levels ?: emptyList()
            if (levels.isNotEmpty()) {
                var showLevelPicker by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = showLevelPicker,
                    onExpandedChange = { showLevelPicker = it },
                ) {
                    OutlinedTextField(
                        value         = selectedLevel,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Level") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLevelPicker) },
                        modifier      = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = showLevelPicker,
                        onDismissRequest = { showLevelPicker = false },
                    ) {
                        levels.forEach { level ->
                            DropdownMenuItem(
                                text    = { Text(level.name) },
                                onClick = {
                                    selectedLevel = level.name
                                    selectedPrize = ""
                                    showLevelPicker = false
                                },
                            )
                        }
                    }
                }

                // Prize types for selected level
                val prizeTypes = levels.find { it.name == selectedLevel }?.prizeTypes ?: emptyList()
                if (prizeTypes.isNotEmpty()) {
                    var showPrizePicker by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = showPrizePicker,
                        onExpandedChange = { showPrizePicker = it },
                    ) {
                        OutlinedTextField(
                            value         = selectedPrize,
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Prize Type") },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPrizePicker) },
                            modifier      = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = showPrizePicker,
                            onDismissRequest = { showPrizePicker = false },
                        ) {
                            prizeTypes.forEach { prize ->
                                DropdownMenuItem(
                                    text    = { Text("${prize.name} — ${prize.points} pts") },
                                    onClick = {
                                        selectedPrize  = prize.name
                                        showPrizePicker = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ── Event date ─────────────────────────────────────────────────────
            OutlinedTextField(
                value         = eventDate,
                onValueChange = { eventDate = it },
                label         = { Text("Event Date (YYYY-MM-DD)") },
                leadingIcon   = { Icon(Icons.Outlined.CalendarMonth, null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("2024-01-15") },
            )

            // ── File picker ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .border(
                        width  = 1.5.dp,
                        color  = MaterialTheme.colorScheme.outline,
                        shape  = RoundedCornerShape(8.dp),
                    )
                    .clickable { fileLauncher.launch("*/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (pickedUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap to pick image or PDF", style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("File selected", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // ── Submit ─────────────────────────────────────────────────────────
            Button(
                onClick = {
                    uploadViewModel.upload(
                        context       = context,
                        categoryId    = selectedCat?.id ?: return@Button,
                        subcategoryName = selectedSub?.name ?: return@Button,
                        eventName     = eventName,
                        level         = selectedLevel.takeIf { it.isNotBlank() },
                        prizeType     = selectedPrize.takeIf { it.isNotBlank() },
                        eventDate     = eventDate,
                        fileUri       = pickedUri ?: return@Button,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !uiState.isLoading &&
                           selectedCat != null &&
                           selectedSub != null &&
                           eventName.isNotBlank() &&
                           eventDate.isNotBlank() &&
                           pickedUri != null,
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else { Icon(Icons.Outlined.CloudUpload, null); Spacer(Modifier.width(8.dp)); Text("Upload Certificate", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
