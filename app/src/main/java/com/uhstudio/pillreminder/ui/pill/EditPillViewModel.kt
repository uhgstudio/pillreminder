package com.uhstudio.pillreminder.ui.pill

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.Pill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditPillViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()

    private val _pill = MutableStateFlow<Pill?>(null)
    val pill: StateFlow<Pill?> = _pill

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _memo = MutableStateFlow("")
    val memo: StateFlow<String> = _memo

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadPill(pillId: String) {
        viewModelScope.launch {
            val pill = pillDao.getPillById(pillId)
            pill?.let {
                _pill.value = it
                _name.value = it.name
                _memo.value = it.memo ?: ""
            }
        }
    }

    fun updateName(name: String) {
        _name.value = name
    }

    fun updateMemo(memo: String) {
        _memo.value = memo
    }

    fun updatePill(onSuccess: () -> Unit) {
        val currentPill = _pill.value ?: return
        
        if (_name.value.isBlank()) {
            return
        }

        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val updatedPill = currentPill.copy(
                    name = _name.value,
                    memo = _memo.value.takeIf { it.isNotBlank() }
                )
                pillDao.updatePill(updatedPill)
                onSuccess()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 