package com.example.pillreminder.ui.pill

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pillreminder.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.util.UUID

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddPillScreen(
    viewModel: AddPillViewModel,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var showImageSelectionDialog by remember { mutableStateOf(false) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                viewModel.setImageUri(uri.toString())
            }
        }
    }

    val selectImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.setImageUri(uri.toString())
        }
    }

    fun takePhoto() {
        val photoFile = File(
            context.getExternalFilesDir(null),
            "pill_${UUID.randomUUID()}.jpg"
        )
        currentPhotoUri = Uri.fromFile(photoFile)
        takePicture.launch(currentPhotoUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_add_pill)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 이미지 미리보기 및 선택 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 이미지 선택 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        launcher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_select_image))
                }

                Button(
                    onClick = {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            File(
                                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "pill_${System.currentTimeMillis()}.jpg"
                            )
                        )
                        imageUri = uri
                        takePicture.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_take_photo))
                }
            }

            // 입력 필드
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text(stringResource(R.string.hint_pill_name)) },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text(stringResource(R.string.hint_pill_memo)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // 저장 버튼
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = context.getString(R.string.error_pill_name_empty)
                        return@Button
                    }
                    viewModel.savePill(name, memo)
                    onNavigateUp()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_save))
            }
        }
    }

    if (showImageSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showImageSelectionDialog = false },
            title = { Text(stringResource(R.string.dialog_select_image_title)) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showImageSelectionDialog = false
                            if (cameraPermissionState.status.isGranted) {
                                takePhoto()
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.dialog_camera))
                    }
                    TextButton(
                        onClick = {
                            showImageSelectionDialog = false
                            selectImage.launch("image/*")
                        }
                    ) {
                        Text(stringResource(R.string.dialog_gallery))
                    }
                }
            },
            confirmButton = {}
        )
    }
} 