package com.ecocp.capstoneenvirotrack.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ecocp.capstoneenvirotrack.utils.NotificationSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuarterlyReminderWorker (
    appContext: Context,
    workerParams: WorkerParameters
    ): CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result{
        return try{
            withContext(Dispatchers.IO){
                NotificationSender.sendQuarterlySmrReminder()
            }
            Result.success()
        } catch(e:Exception){
            Result.retry()
        }
    }
}