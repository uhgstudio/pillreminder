package com.example.pillreminder.ui.pill

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.Pill
import kotlinx.coroutines.launch
import java.util.UUID

class AddPillViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()
    private var imageUri: String? = null

    fun setImageUri(uri: String) {
        imageUri = uri
    }

    fun savePill(name: String, memo: String) {
        val pill = Pill(
            id = UUID.randomUUID().toString(),
            name = name,
            imageUri = imageUri,
            memo = memo.takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            pillDao.insertPill(pill)
        }
    }
} 