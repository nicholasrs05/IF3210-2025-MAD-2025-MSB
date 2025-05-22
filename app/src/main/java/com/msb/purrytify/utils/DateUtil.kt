package com.msb.purrytify.utils

object DateUtil {
    fun getMonthString(month: Int): String {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        return monthNames[month - 1]
    }
}