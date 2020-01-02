package com.transcend.otg.utilities

import java.text.DecimalFormat

/**
 * Created by wangbojie on 2017/3/17.
 */
object MathUtils {
    const val KB: Long = 1000
    const val MB = KB * KB
    const val GB = MB * KB
    const val TB = GB * KB
    const val KB_S: Long = 1024
    const val MB_S = KB_S * KB_S
    const val GB_S = MB_S * KB_S
    const val TB_S = GB_S * KB_S

    fun getBytes(number: Long): String {
        val dividers = longArrayOf(TB, GB, MB, KB, 1)
        val units = arrayOf("TB", "GB", "MB", "KB", "B")
        for (i in dividers.indices) {
            if (number >= dividers[i]) {
                val value =
                    number.toDouble() / dividers[i].toDouble()
                val df = DecimalFormat("#,##0.##")
                return String.format("%s%s", df.format(value), units[i])
            }
        }
        return "0B"
    }

    fun getStorageSize(number: Long): String {
        val dividers = longArrayOf(TB_S, GB_S, MB_S, KB_S, 1)
        val units = arrayOf("TB", "GB", "MB", "KB", "B")
        for (i in dividers.indices) {
            if (number >= dividers[i]) {
                val value =
                    number.toDouble() / dividers[i].toDouble()
                val df = DecimalFormat("#,##0.##")
                return String.format("%s%s", df.format(value), units[i])
            }
        }
        return "0B"
    }

    fun getStoragePercentage(son: Float, mother: Float): String {
        var result = " ("
        val df = DecimalFormat("##.##")
        result += df.format(son / mother * 100.toDouble()) + "%) "
        return result
    }

    fun getStoragePercentageInt(son: Float, mother: Float): Int {
        return (son / mother * 100).toInt()
    }
}