package com.ecocp.capstoneenvirotrack.repository

import com.ecocp.capstoneenvirotrack.api.ApiService
import com.ecocp.capstoneenvirotrack.model.TsdBooking
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TsdRepository(private val api: ApiService) {

    private suspend fun getAuthHeader(): String {
        val user = FirebaseAuth.getInstance().currentUser
        val token = user?.getIdToken(true)?.await()?.token
        return "Bearer $token"
    }

    suspend fun fetchBookings(onResult: (List<TsdBooking>?) -> Unit) {
        val header = getAuthHeader()
        api.getTsdBookings(header).enqueue(object: Callback<List<TsdBooking>> {
            override fun onResponse(
                call: Call<List<TsdBooking>>,
                response: Response<List<TsdBooking>>
            ) {
                onResult(response.body())
            }

            override fun onFailure(call: Call<List<TsdBooking>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    suspend fun accept(bookingId: String, remarks: String, callback: (Boolean) -> Unit) {
        val header = getAuthHeader()
        api.acceptTsdBooking(header, bookingId, mapOf("remarks" to remarks))
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, res: Response<Void>) {
                    callback(res.isSuccessful)
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    callback(false)
                }
            })
    }

    suspend fun reject(bookingId: String, reason: String, callback: (Boolean) -> Unit) {
        val header = getAuthHeader()
        api.rejectTsdBooking(header, bookingId, mapOf("reason" to reason))
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, res: Response<Void>) {
                    callback(res.isSuccessful)
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    callback(false)
                }
            })
    }

    suspend fun receive(bookingId: String, qty: Double, callback: (Boolean) -> Unit) {
        val header = getAuthHeader()
        api.receiveTsdBooking(header, bookingId, mapOf("quantity" to qty))
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, res: Response<Void>) {
                    callback(res.isSuccessful)
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    callback(false)
                }
            })
    }

    suspend fun treat(bookingId: String, notes: String, callback: (Boolean) -> Unit) {
        val header = getAuthHeader()
        api.treatTsdBooking(header, bookingId, mapOf("notes" to notes))
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, res: Response<Void>) {
                    callback(res.isSuccessful)
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    callback(false)
                }
            })
    }
}