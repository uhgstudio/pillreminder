package com.uhstudio.pillreminder.ui.pill

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import com.uhstudio.pillreminder.ui.components.AlarmScheduleSection
import com.uhstudio.pillreminder.ui.components.DosagePickerDialog
import com.uhstudio.pillreminder.ui.components.SoundPickerDialog
import com.uhstudio.pillreminder.ui.theme.BlobBlue
import com.uhstudio.pillreminder.ui.theme.BlobLavender
import com.uhstudio.pillreminder.ui.theme.BlobMint
import com.uhstudio.pillreminder.ui.theme.BlobPink
import com.uhstudio.pillreminder.ui.theme.BlobYellow
import com.uhstudio.pillreminder.ui.theme.StitchDeepBrown
import com.uhstudio.pillreminder.ui.theme.WarmBackground
import com.uhstudio.pillreminder.ui.theme.WarmPrimary
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddPillScreen(
    viewModel: AddPillViewModel,
    pillId: String? = null,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = remember { AdManager.getInstance(context) }

    var showImageSelectionDialog by remember { mutableStateOf(false) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    val editingPill by viewModel.editingPill.collectAsState()
    val savedImageUri by viewModel.imageUri.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(30) }
    var showQuantityDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var initialized by remember { mutableStateOf(false) }

    // Alarm State
    var isAlarmEnabled by remember { mutableStateOf(true) }
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(30) } // Match HTML 08:30 AM
    var scheduleType by remember { mutableStateOf(ScheduleType.DAILY) }
    var selectedDays by remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)) } // Match HTML selected days
    
    // INTERVAL_DAYS
    var intervalDays by remember { mutableStateOf(2) }
    var intervalStartDate by remember { mutableStateOf(LocalDate.now()) }
    
    // SPECIFIC_DATES
    var specificDates by remember { mutableStateOf(listOf<LocalDate>()) }
    
    // DATE RANGE
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    
    var alarmSoundUri by remember { mutableStateOf<String?>(null) }
    var alarmSoundName by remember { mutableStateOf("Birdsong") } // Match HTML

    var showTimePicker by remember { mutableStateOf(false) }
    var showDaySelector by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDosageDialog by remember { mutableStateOf(false) }
    var datePickerMode by remember { mutableStateOf("start") }

    // 편집 모드일 경우 기존 데이터 로드
    LaunchedEffect(pillId) {
        if (pillId != null) {
            viewModel.loadPill(pillId)
        }
    }

    // 기존 데이터로 폼 초기화
    LaunchedEffect(editingPill) {
        if (!initialized && editingPill != null) {
            name = editingPill!!.name
            dosage = editingPill!!.dosage
            memo = editingPill!!.memo ?: ""
            initialized = true
        }
    }

    val imageUri = savedImageUri?.let { Uri.parse(it) }

    val medicationIcons = listOf(
        R.drawable.ic_type_capsule,
        R.drawable.ic_type_tablet,
        R.drawable.ic_type_liquid,
        R.drawable.ic_type_cream,
        R.drawable.ic_type_liquid // Placeholder for Spray
    )
    
    data class MedType(val name: String, val icon: Int, val color: Color)
    val medTypeItems = listOf(
        MedType("Capsule", R.drawable.ic_type_capsule, BlobPink),
        MedType("Tablet", R.drawable.ic_type_tablet, BlobMint),
        MedType("Syrup", R.drawable.ic_type_liquid, BlobLavender),
        MedType("Cream", R.drawable.ic_type_cream, BlobYellow),
        MedType("Spray", R.drawable.ic_type_liquid, BlobBlue)
    )
    
    var selectedType by remember { mutableStateOf(medTypeItems[0].name) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) currentPhotoUri?.let { viewModel.setImageUri(it.toString()) }
    }
    val selectImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.processSelectedImage(it) }
    }

    fun takePhoto() {
        val photoFile = File(context.getExternalFilesDir(null), "pill_${UUID.randomUUID()}.jpg")
        currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
        takePicture.launch(currentPhotoUri)
    }

    val isEditMode = pillId != null

    Box(modifier = Modifier.fillMaxSize().background(WarmBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Sticky Top Bar Implementation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmBackground.copy(alpha = 0.9f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.5f), CircleShape)
                        .clickable { onNavigateUp() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.btn_back),
                        modifier = Modifier.size(18.dp),
                        tint = StitchDeepBrown
                    )
                }
                
                Text(
                    text = if (isEditMode) stringResource(R.string.title_edit_medication) else stringResource(R.string.title_add_medication),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = StitchDeepBrown,
                        fontSize = 20.sp
                    )
                )
                
                Text(
                    text = stringResource(R.string.label_done),
                    modifier = Modifier.clickable { /* Save triggers */ },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = WarmPrimary,
                        fontSize = 18.sp
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Hero Image Area (4:3)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .shadow(elevation = 30.dp, shape = RoundedCornerShape(32.dp), ambientColor = WarmPrimary.copy(alpha = 0.1f))
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFFFFEBD6).copy(alpha = 0.8f))
                        .border(4.dp, Color.White, RoundedCornerShape(32.dp))
                        .clickable { showImageSelectionDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                    .shadow(8.dp, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                    tint = WarmPrimary
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.label_add_photo_hint),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StitchDeepBrown
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Input Section: Medication Name
                Text(
                    text = stringResource(R.string.label_medication_name),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = StitchDeepBrown.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    placeholder = { Text(stringResource(R.string.hint_medication_name), color = Color.LightGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(20.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(32.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = WarmPrimary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Medication Type Selector
                Text(
                    text = stringResource(R.string.label_medication_type),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = StitchDeepBrown.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    medTypeItems.forEach { medItem ->
                        val isSelected = selectedType == medItem.name
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { selectedType = medItem.name }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer {
                                        scaleX = if (isSelected) 1.05f else 1f
                                        scaleY = if (isSelected) 1.05f else 1f
                                    }
                                    .background(medItem.color, RoundedCornerShape(24.dp))
                                    .border(4.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(24.dp))
                                    .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val vectorIcon = when(medItem.name) {
                                    "Capsule" -> Icons.Filled.HealthAndSafety
                                    "Tablet" -> Icons.Filled.Circle
                                    "Syrup" -> Icons.Filled.Opacity
                                    "Cream" -> Icons.Filled.Healing
                                    "Spray" -> Icons.Filled.BlurOn
                                    else -> Icons.Filled.QuestionMark
                                }
                                Icon(
                                    imageVector = vectorIcon,
                                    contentDescription = medItem.name,
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = medItem.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) StitchDeepBrown else StitchDeepBrown.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Instructions (Memo)
                Text(
                    text = stringResource(R.string.label_instructions),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = StitchDeepBrown.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    placeholder = { Text(stringResource(R.string.hint_instructions), color = Color.LightGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .shadow(20.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(32.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Unified Alarm Schedule Section
                AlarmScheduleSection(
                    hour = hour,
                    minute = minute,
                    onTimeClick = { showTimePicker = true },
                    selectedDays = selectedDays,
                    onDaysChange = { selectedDays = it },
                    dosage = dosage,
                    onDosageClick = { showDosageDialog = true },
                    alarmSoundName = alarmSoundName,
                    onSoundClick = { showSoundPicker = true }
                )

                Spacer(modifier = Modifier.height(120.dp)) // Padding for bottom button
            }
        }

        // Bottom Fixed Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, WarmBackground, WarmBackground)))
                .padding(24.dp)
        ) {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = context.getString(R.string.error_name_required)
                        return@Button
                    }
                    
                    val scheduleConfig = when (scheduleType) {
                        ScheduleType.DAILY -> ScheduleConfig.Daily
                        ScheduleType.WEEKLY -> ScheduleConfig.Weekly.from(selectedDays)
                        else -> ScheduleConfig.Daily
                    }

                    viewModel.savePill(
                        name = name,
                        memo = memo,
                        type = selectedType,
                        dosage = dosage,
                        isAlarmEnabled = isAlarmEnabled,
                        hour = hour,
                        minute = minute,
                        scheduleType = scheduleType,
                        scheduleConfig = scheduleConfig,
                        startDate = startDate,
                        endDate = endDate,
                        alarmSoundUri = alarmSoundUri,
                        onComplete = {
                             Toast.makeText(context, context.getString(R.string.msg_saved_success), Toast.LENGTH_SHORT).show()
                            onNavigateUp()
                        },
                        onShowAd = {
                            activity?.let { adManager.showInterstitialAd(it, onAdClosed = onNavigateUp) } ?: onNavigateUp() 
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(40.dp), ambientColor = WarmPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.btn_save_medication), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal Components
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = WarmPrimary)
        }
    }

    if (showImageSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showImageSelectionDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(32.dp),
            title = { 
                Text(
                    stringResource(R.string.dialog_select_image_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = StitchDeepBrown)
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showImageSelectionDialog = false
                            if (cameraPermissionState.status.isGranted) takePhoto()
                            else cameraPermissionState.launchPermissionRequest()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF3E0), contentColor = WarmPrimary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.dialog_camera), fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = {
                            showImageSelectionDialog = false
                            selectImage.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF3E0), contentColor = WarmPrimary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.dialog_gallery), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSelectionDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(32.dp),
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok), color = WarmPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
            },
            text = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            selectorColor = WarmPrimary,
                            periodSelectorSelectedContainerColor = Color(0xFFFFF3E0),
                            periodSelectorSelectedContentColor = WarmPrimary,
                            timeSelectorSelectedContainerColor = Color(0xFFFFF3E0),
                            timeSelectorSelectedContentColor = WarmPrimary
                        )
                    ) 
                }
            }
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(32.dp),
            title = { Text(stringResource(R.string.dialog_alarm_permission_title), fontWeight = FontWeight.Bold, color = StitchDeepBrown) },
            text = { Text(stringResource(R.string.dialog_alarm_permission_text), color = StitchDeepBrown.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.btn_go_to_settings), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text(stringResource(R.string.btn_later), color = Color.Gray) }
            }
        )
    }

    if (showSoundPicker) {
        SoundPickerDialog(
            context = context,
            currentUri = alarmSoundUri,
            onDismissRequest = { showSoundPicker = false },
            onSoundSelected = { uri, name ->
                alarmSoundUri = uri
                alarmSoundName = name
                showSoundPicker = false
            }
        )
    }
}
