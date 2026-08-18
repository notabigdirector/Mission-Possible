package com.mission.app.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Formatters {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    fun date(ms: Long): String = dateFormat.format(Date(ms))

    fun dateTime(ms: Long): String = dateTimeFormat.format(Date(ms))

    fun countdown(dueAt: Long, now: Long): String {
        val days = (dueAt - now) / 86_400_000.0
        return when {
            dueAt <= now -> "已逾期"
            days < 1 -> "今天到期"
            days < 2 -> "明天到期"
            else -> "剩 ${days.roundToInt()} 天"
        }
    }
}