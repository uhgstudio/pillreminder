package com.example.pillreminder.ui.pill

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.ads.AdManager
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.Pill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AddPillViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()
    private val adManager = AdManager.getInstance(application)

    private val _editingPill = MutableStateFlow<Pill?>(null)
    val editingPill: StateFlow<Pill?> = _editingPill

    private val _imageUri = MutableStateFlow<String?>(null)
    val imageUri: StateFlow<String?> = _imageUri

    fun loadPill(pillId: String) {
        viewModelScope.launch {
            val pill = pillDao.getPillById(pillId)
            _editingPill.value = pill
            _imageUri.value = pill?.imageUri
        }
    }

    fun setImageUri(uri: String) {
        _imageUri.value = uri
    }

    fun savePill(name: String, memo: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val existingPill = _editingPill.value
            val pill = if (existingPill != null) {
                // 편집 모드
                existingPill.copy(
                    name = name,
                    imageUri = _imageUri.value,
                    memo = memo.takeIf { it.isNotBlank() }
                )
            } else {
                // 새로 추가
                Pill(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    imageUri = _imageUri.value,
                    memo = memo.takeIf { it.isNotBlank() }
                )
            }

            if (existingPill != null) {
                pillDao.updatePill(pill)
            } else {
                pillDao.insertPill(pill)
            }

            onComplete()
        }
    }
} 