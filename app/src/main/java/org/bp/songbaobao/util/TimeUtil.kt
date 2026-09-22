package org.bp.songbaobao.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
private val DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun todayStr(): String = LocalDate.now().format(DATE_FMT)
fun nowTimeStr(): String = LocalTime.now().format(TIME_FMT)
fun nowStamp(): String = LocalDateTime.now().format(DATETIME_FMT)

fun startOf(days: Int): String =
    LocalDate.now().minusDays(days.toLong() - 1).format(DATE_FMT)

fun minutesBetween(from: String, to: String): Int {
    fun mins(t: String): Int = try {
        val (h, m) = t.split(":")
        h.toInt() * 60 + m.toInt()
    } catch (e: Exception) {
        0
    }
    return mins(to) - mins(from)
}

/** "08:00,20:00" -> ["08:00","20:00"]，排序去重 */
fun normalizeTimes(raw: String): String =
    raw.split(",", "，", ";", "；")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .sorted()
        .joinToString(",")

fun parseTimes(raw: String): List<String> =
    if (raw.isBlank()) emptyList() else normalizeTimes(raw).split(",").filter { it.isNotEmpty() }

/** 规范化标签：中文逗号转英文、去重去空 */
fun normalizeTags(raw: String): String =
    raw.replace("，", ",")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(",")

fun parseTags(raw: String): List<String> =
    if (raw.isBlank()) emptyList() else raw.replace("，", ",").split(",")
        .map { it.trim() }.filter { it.isNotEmpty() }
