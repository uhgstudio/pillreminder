package com.example.pillreminder.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.Pill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()

    val pills: Flow<List<Pill>> = pillDao.getAllPills()

    fun deletePill(pill: Pill) {
        viewModelScope.launch {
            pillDao.deletePill(pill)
        }
    }
} 