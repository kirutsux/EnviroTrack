package com.ecocp.capstoneenvirotrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ecocp.capstoneenvirotrack.model.BookingUiModel
import com.ecocp.capstoneenvirotrack.repository.BookingRepository
import java.text.SimpleDateFormat
import java.util.Locale

class BookingViewModel : ViewModel() {

    private val repository = BookingRepository() // uses default FirebaseFirestore.getInstance()

    private val _booking = MutableLiveData<BookingUiModel?>()
    val booking: LiveData<BookingUiModel?> get() = _booking

    fun loadBooking(bookingId: String, role: BookingRepository.Role) {
        repository.fetchBooking(bookingId, role) { model ->
            _booking.postValue(model)
        }
    }

    fun formattedStatusDate(model: BookingUiModel?): String {
        val date = model?.statusUpdatedAt ?: return ""
        val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }
}
