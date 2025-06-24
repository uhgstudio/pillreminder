package com.example.pillreminder.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pillreminder.R
import com.example.pillreminder.data.model.Pill
import java.io.File

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddPillClick: () -> Unit,
    onPillClick: (Pill) -> Unit
) {
    val pills by viewModel.pills.collectAsState(initial = emptyList())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPillClick,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.btn_add_pill)
                )
            }
        }
    ) { paddingValues ->
        if (pills.isEmpty()) {
            EmptyPillList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pills) { pill ->
                    PillItem(
                        pill = pill,
                        onPillClick = onPillClick,
                        onDeleteClick = { viewModel.deletePill(pill) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyPillList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.empty_pill_list),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PillItem(
    pill: Pill,
    onPillClick: (Pill) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { onPillClick(pill) }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 이미지 또는 기본 아이콘 표시
            if (pill.imageUri != null) {
                // 파일 경로인지 URI인지 확인
                val imageModel = if (pill.imageUri.startsWith("content://") || pill.imageUri.startsWith("file://")) {
                    pill.imageUri
                } else {
                    // 내부 저장소의 파일 경로인 경우
                    File(pill.imageUri)
                }
                
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = pill.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (!pill.memo.isNullOrBlank()) {
                    Text(
                        text = pill.memo,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
} 