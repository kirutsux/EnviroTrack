package com.ecocp.capstoneenvirotrack.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecocp.capstoneenvirotrack.model.TsdBooking
import com.ecocp.capstoneenvirotrack.repository.TsdRepository
import kotlinx.coroutines.launch

class TsdViewModel(private val repo: TsdRepository) : ViewModel() {

    val bookingsLiveData = MutableLiveData<List<TsdBooking>>()
    val loading = MutableLiveData<Boolean>()
    val actionSuccess = MutableLiveData<Boolean>()

    fun loadBookings() {
        loading.value = true
        viewModelScope.launch {
            repo.fetchBookings {
                bookingsLiveData.value = it ?: emptyList()
                loading.value = false
            }
        }
    }

    fun accept(id: String, remarks: String) {
        loading.value = true
        viewModelScope.launch {
            repo.accept(id, remarks) { ok ->
                actionSuccess.value = ok
                if (ok) loadBookings()
                loading.value = false
            }
        }
    }

    fun reject(id: String, reason: String) {
        loading.value = true
        viewModelScope.launch {
            repo.reject(id, reason) { ok ->
                actionSuccess.value = ok
                if (ok) loadBookings()
                loading.value = false
            }
        }
    }

    fun receive(id: String, qty: Double) {
        loading.value = true
        viewModelScope.launch {
            repo.receive(id, qty) { ok ->
                actionSuccess.value = ok
                if (ok) loadBookings()
                loading.value = false
            }
        }
    }

    fun treat(id: String, notes: String) {
        loading.value = true
        viewModelScope.launch {
            repo.treat(id, notes) { ok ->
                actionSuccess.value = ok
                if (ok) loadBookings()
                loading.value = false
            }
        }
    }
}