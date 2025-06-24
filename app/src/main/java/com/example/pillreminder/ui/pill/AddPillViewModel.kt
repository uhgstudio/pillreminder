package com.example.pillreminder.ui.pill

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.Pill
import com.example.pillreminder.util.ImageUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AddPillViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()
    private val context = application
    
    private val _imageUri = MutableStateFlow<String?>(null)
    val imageUri: StateFlow<String?> = _imageUri.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun setImageUri(uri: String) {
        _imageUri.value = uri
    }

    fun savePill(name: String, memo: String, onComplete: () -> Unit = {}) {
        if (name.isBlank()) return
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                // 이미지가 있으면 내부 저장소에 저장
                val savedImagePath = _imageUri.value?.let { uriString ->
                    val uri = Uri.parse(uriString)
                    ImageUtil.saveImageToInternalStorage(context, uri)
                }

                val pill = Pill(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    imageUri = savedImagePath,
                    memo = memo.takeIf { it.isNotBlank() }
                )

                pillDao.insertPill(pill)
                onComplete()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePill(originalPill: Pill, name: String, memo: String, onComplete: () -> Unit = {}) {
        if (name.isBlank()) return
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                // 새 이미지가 있으면 저장하고 기존 이미지는 삭제
                val savedImagePath = if (_imageUri.value != null && _imageUri.value != originalPill.imageUri) {
                    val uri = Uri.parse(_imageUri.value!!)
                    val newImagePath = ImageUtil.saveImageToInternalStorage(context, uri)
                    
                    // 기존 이미지 삭제
                    originalPill.imageUri?.let { oldPath ->
                        ImageUtil.deleteImage(oldPath)
                    }
                    
                    newImagePath
                } else {
                    originalPill.imageUri
                }

                val updatedPill = originalPill.copy(
                    name = name,
                    imageUri = savedImagePath,
                    memo = memo.takeIf { it.isNotBlank() }
                )

                pillDao.updatePill(updatedPill)
                onComplete()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPillForEdit(pill: Pill) {
        _imageUri.value = pill.imageUri
    }
} 