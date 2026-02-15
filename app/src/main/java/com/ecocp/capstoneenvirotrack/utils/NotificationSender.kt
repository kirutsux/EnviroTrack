@file:Suppress("LocalVariableName")

package com.ecocp.capstoneenvirotrack.utils

import android.annotation.SuppressLint
import com.ecocp.capstoneenvirotrack.model.NotificationModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date


object NotificationSender{
    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()

    suspend fun sendQuarterlySmrReminder(){
        val QuarterUtils = QuarterUtils()
        val quarterInfo = QuarterUtils().getCurrentQuarterInfo()
        if(!QuarterUtils.isReminderDue()) return

        val userSnapshot = db.collection("users")
            .whereEqualTo("userType","pco")
            .get()
            .await()

        val title = "SMR Submission Reminder"
        val message = "Reminder: Submit your Self-Monitoring Report (SMR) for Quarter ${quarterInfo.quarter}. Deadline was ${quarterInfo.reminderDate.time.toLocaleString()}."
        val type = "smr_reminder"
        val actionLink = "smrDashboard"

        for(userDoc in userSnapshot.documents){
            val receiverId = userDoc.id
            val receiverType = "pco"
            val hasSubmitted = checkIfSubmittedForQuarterly(receiverId, quarterInfo.quarter)
            if(hasSubmitted) continue

            val notification = NotificationModel (
                title = title,
                message = message,
                receiverId = receiverId,
                receiverType = receiverType,
                timestamp = Timestamp.now(),
                type = type,
                isRead = false,
                actionLink = actionLink
            )
            db.collection("notifications").add(notification)
        }
    }

    private suspend fun checkIfSubmittedForQuarterly(userId: String, quarter:Int):Boolean{
        val startOfQuarter = getQuarterStartDate(quarter)
        val endOfQuarter = getQuarterEndDate(quarter)

        val submissions = db.collection("smr_submissions")
            .whereEqualTo("uid", userId)
            .whereGreaterThanOrEqualTo("dateSubmitted", Timestamp(startOfQuarter))
            .whereLessThanOrEqualTo("dateSubmitted", Timestamp(endOfQuarter))
            .get()
            .await()
        return submissions.documents.isNotEmpty()
    }

    private fun getQuarterStartDate(quarter: Int): Date {
        val cal = Calendar.getInstance()
        when (quarter) {
            1 -> { cal.set(Calendar.MONTH, Calendar.JANUARY); cal.set(Calendar.DAY_OF_MONTH, 1) }
            2 -> { cal.set(Calendar.MONTH, Calendar.APRIL); cal.set(Calendar.DAY_OF_MONTH, 1) }
            3 -> { cal.set(Calendar.MONTH, Calendar.JULY); cal.set(Calendar.DAY_OF_MONTH, 1) }
            4 -> { cal.set(Calendar.MONTH, Calendar.OCTOBER); cal.set(Calendar.DAY_OF_MONTH, 1) }
        }
        return cal.time
    }
    private fun getQuarterEndDate(quarter: Int): Date {
        val cal = Calendar.getInstance()
        when (quarter) {
            1 -> { cal.set(Calendar.MONTH, Calendar.MARCH); cal.set(Calendar.DAY_OF_MONTH, 31) }
            2 -> { cal.set(Calendar.MONTH, Calendar.JUNE); cal.set(Calendar.DAY_OF_MONTH, 30) }
            3 -> { cal.set(Calendar.MONTH, Calendar.SEPTEMBER); cal.set(Calendar.DAY_OF_MONTH, 30) }
            4 -> { cal.set(Calendar.MONTH, Calendar.DECEMBER); cal.set(Calendar.DAY_OF_MONTH, 31) }
        }
        return cal.time
    }
}