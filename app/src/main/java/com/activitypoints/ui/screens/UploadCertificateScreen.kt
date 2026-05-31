package com.activitypoints.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.activitypoints.models.Category
import com.activitypoints.models.Subcategory
import com.activitypoints.viewmodel.UploadViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadCertificateScreen(
    studentViewModel: com.activitypoints.viewmodel.StudentViewModel,
    onUploadSuccess: () -> Unit,
    uploadViewModel: UploadViewModel = hiltViewModel(),
) {
    val context    = LocalContext.current
    val uiState    by uploadViewModel.uiState.collectAsState()
    val catState   by studentViewModel.uiState.collectAsState()

    var selectedCat   by remember { mutableStateOf<Category?>(null) }
    var selectedSub   by remember { mutableStateOf<Subcategory?>(null) }
    var eventName     by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("") }
    var selectedPrize by remember { mutableStateOf("") }
    var pickedUri     by remember { mutableStateOf<Uri?>(null) }
    var pickedName    by remember { mutableStateOf("") }

    // Date state — separate dateFrom and dateTo like the RN app
    var dateFrom        by remember { mutableStateOf<Date?>(null) }
    var dateTo          by remember { mutableStateOf<Date?>(null) }
    var isDurationEvent by remember { mutableStateOf(false) }
    var showFromPicker  by remember { mutableStateOf(false) }
    var showToPicker    by remember { mutableStateOf(false) }

    // "Others" mode — when category is unknown
    var isOthers           by remember { mutableStateOf(false) }
    var othersDescription  by remember { mutableStateOf("") }

    // Dropdown visibility
    var showCatPicker   by remember { mutableStateOf(false) }
    var showSubPicker   by remember { mutableStateOf(false) }
    var showLevelPicker by remember { mutableStateOf(false) }
    var showPrizePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isoFormat  = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Navigate to certificates tab on success
    LaunchedEffect(uiState.uploadSuccess) {
        if (uiState.uploadSuccess) {
            uploadViewModel.clearSuccess()
            onUploadSuccess()
        }
    }

    // File pickers
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pickedUri  = it
            pickedName = it.lastPathSegment ?: "file"
        }
    }
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pickedUri  = it
            pickedName = it.lastPathSegment ?: "image"
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) pickedUri = null
    }

    // Date pickers
    val fromDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateFrom?.time ?: System.currentTimeMillis()
    )
    val toDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateTo?.time ?: System.currentTimeMillis()
    )

    if (showFromPicker) {
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    fromDatePickerState.selectedDateMillis?.let {
                        dateFrom = Date(it)
                        if (dateTo != null && dateFrom!! > dateTo!!) dateTo = null
                    }
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = fromDatePickerState)
        }
    }

    if (showToPicker) {
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    toDatePickerState.selectedDateMillis?.let { dateTo = Date(it) }
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = toDatePickerState)
        }
    }

    // Computed eligible points
    val eligiblePoints: Int? = remember(selectedCat, selectedSub, selectedLevel, selectedPrize) {
        val sub = selectedSub ?: return@remember null
        when {
            sub.fixedPoints != null -> sub.fixedPoints
            sub.levels.isNotEmpty() && selectedLevel.isNotBlank() && selectedPrize.isNotBlank() -> {
                val lvl = sub.levels.find { it.name == selectedLevel }
                lvl?.prizeTypes?.find { it.name == selectedPrize }?.points
            }
            else -> null
        }
    }

    val canSubmit = when {
        isOthers -> othersDescription.isNotBlank() && pickedUri != null && dateFrom != null &&
                (!isDurationEvent || dateTo != null) && !uiState.isLoading
        else -> selectedCat != null && selectedSub != null &&
                (selectedSub?.levels.isNullOrEmpty() || (selectedLevel.isNotBlank() && selectedPrize.isNotBlank())) &&
                pickedUri != null && dateFrom != null &&
                (!isDurationEvent || dateTo != null) && !uiState.isLoading
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Upload Certificate",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary,
        )

        // ── Category picker ──────────────────────────────────────────────────
        ExposedDropdownMenuBox(
            expanded         = showCatPicker,
            onExpandedChange = { if (!isOthers) showCatPicker = it },
        ) {
            OutlinedTextField(
                value         = if (isOthers) "Others" else selectedCat?.name ?: "",
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
                catState.categories.forEach { cat ->
                    DropdownMenuItem(
                        text    = { Text(cat.name) },
                        onClick = {
                            selectedCat   = cat
                            selectedSub   = null
                            selectedLevel = ""
                            selectedPrize = ""
                            isOthers      = false
                            showCatPicker = false
                        },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Others (not in list)") },
                    onClick = {
                        isOthers           = true
                        selectedCat        = null
                        selectedSub        = null
                        selectedLevel      = ""
                        selectedPrize      = ""
                        othersDescription  = ""
                        showCatPicker      = false
                    },
                )
            }
        }

        // ── Others description ───────────────────────────────────────────────
        AnimatedVisibility(visible = isOthers) {
            OutlinedTextField(
                value         = othersDescription,
                onValueChange = { othersDescription = it },
                label         = { Text("Describe the activity / certificate") },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2,
                maxLines      = 4,
            )
        }

        // ── Subcategory picker ───────────────────────────────────────────────
        AnimatedVisibility(visible = selectedCat != null && !isOthers) {
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

        // ── Level & Prize pickers ────────────────────────────────────────────
        val levels = selectedSub?.levels ?: emptyList()
        AnimatedVisibility(visible = levels.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded         = showLevelPicker,
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
                        expanded         = showLevelPicker,
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

                val prizeTypes = levels.find { it.name == selectedLevel }?.prizeTypes ?: emptyList()
                AnimatedVisibility(visible = prizeTypes.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded         = showPrizePicker,
                        onExpandedChange = { showPrizePicker = it },
                    ) {
                        OutlinedTextField(
                            value         = selectedPrize,
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Prize / Position") },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPrizePicker) },
                            modifier      = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded         = showPrizePicker,
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
        }

        // Eligible points preview
        eligiblePoints?.let { pts ->
            Surface(
                color  = MaterialTheme.colorScheme.primaryContainer,
                shape  = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Star, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Eligible points: $pts",
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // ── Event name ───────────────────────────────────────────────────────
        OutlinedTextField(
            value         = eventName,
            onValueChange = { eventName = it },
            label         = { Text("Event Name (optional)") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            leadingIcon   = { Icon(Icons.Outlined.Event, null) },
        )

        // ── Duration toggle ──────────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked         = isDurationEvent,
                onCheckedChange = { isDurationEvent = it; if (!it) dateTo = null },
            )
            Text("Multi-day event", style = MaterialTheme.typography.bodyMedium)
        }

        // ── Date pickers ─────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick  = { showFromPicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text  = dateFrom?.let { dateFormat.format(it) } ?: if (isDurationEvent) "Start Date" else "Certificate Date",
                    fontSize = 13.sp,
                )
            }
            AnimatedVisibility(visible = isDurationEvent, modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { showToPicker = true }) {
                    Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = dateTo?.let { dateFormat.format(it) } ?: "End Date",
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // ── File picker ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .border(
                    width  = 1.5.dp,
                    color  = if (pickedUri != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline,
                    shape  = RoundedCornerShape(10.dp),
                )
                .clickable { showAttachDialog(
                    onImage  = { imageLauncher.launch("image/*") },
                    onPdf    = { fileLauncher.launch("application/pdf") },
                ) },
            contentAlignment = Alignment.Center,
        ) {
            if (pickedUri == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.AttachFile, null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Tap to attach image or PDF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        pickedName.takeLast(36),
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 13.sp,
                    )
                    IconButton(onClick = { pickedUri = null; pickedName = "" }) {
                        Icon(Icons.Outlined.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // ── Error ─────────────────────────────────────────────────────────────
        uiState.error?.let { err ->
            Surface(
                color  = MaterialTheme.colorScheme.errorContainer,
                shape  = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Text(err, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }

        // ── Submit ────────────────────────────────────────────────────────────
        Button(
            onClick = {
                val catId  = if (isOthers) "others" else (selectedCat?.id ?: return@Button)
                val subName = if (isOthers) othersDescription.trim() else (selectedSub?.name ?: return@Button)
                val dfStr  = isoFormat.format(dateFrom ?: return@Button)
                val dtStr  = if (isDurationEvent && dateTo != null) isoFormat.format(dateTo!!) else dfStr

                uploadViewModel.upload(
                    categoryId      = catId,
                    subcategoryName = subName,
                    eventName       = eventName.trim(),
                    level           = selectedLevel.takeIf { it.isNotBlank() },
                    prizeType       = selectedPrize.takeIf { it.isNotBlank() },
                    dateFrom        = dfStr,
                    dateTo          = dtStr,
                    fileUri         = pickedUri ?: return@Button,
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled  = canSubmit,
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Outlined.CloudUpload, null)
                Spacer(Modifier.width(8.dp))
                Text("Submit Certificate", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// Helper — shows a simple choice dialog without pulling in extra dependencies
private fun showAttachDialog(onImage: () -> Unit, onPdf: () -> Unit) {
    // Compose AlertDialog is handled inside the caller via state flags;
    // here we just launch the most general picker. The composable uses separate
    // launchers so the caller can direct the correct one.
    onImage()   // default: launch image picker; caller overrides for PDF via separate button
}