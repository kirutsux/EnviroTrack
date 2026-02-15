package com.ecocp.capstoneenvirotrack.utils

import java.util.*

class QuarterUtils {
    data class QuarterInfo(val quarter: Int, val reminderDate: Calendar)

    fun getCurrentQuarterInfo(): QuarterInfo{
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)+1

        val quarter = when(month) {
            in 1..3 ->1
            in 4..6 ->2
            in 7..9 ->3
            else -> 4
        }

        val reminderCalendar = Calendar.getInstance().apply{
            set(Calendar.YEAR, year)
            when (quarter) {
                1 -> { set(Calendar.MONTH, Calendar.APRIL); set(Calendar.DAY_OF_MONTH, 15) }
                2 -> { set(Calendar.MONTH, Calendar.JULY); set(Calendar.DAY_OF_MONTH, 15) }
                3 -> { set(Calendar.MONTH, Calendar.OCTOBER); set(Calendar.DAY_OF_MONTH, 15) }
                4 -> { set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 15); add(Calendar.YEAR, 1) }
            }
        }
        return QuarterInfo(quarter, reminderCalendar)

    }

    fun isReminderDue(): Boolean {
        val now = Calendar.getInstance()
        val quarterInfo = getCurrentQuarterInfo()
        return now.after(quarterInfo.reminderDate)
    }
}