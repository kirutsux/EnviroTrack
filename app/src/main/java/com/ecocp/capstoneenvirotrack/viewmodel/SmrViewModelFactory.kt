package com.ecocp.capstoneenvirotrack.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SmrViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmrViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SmrViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}