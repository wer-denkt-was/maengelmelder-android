package de.maengelmelder.mainmodule.service.util

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.R
import java.util.*
import kotlin.math.roundToInt

/**
 * Put any time-related functions to this object.
 */
object Time {

    const val SECONDS_IN_A_DAY = 24 * 3600
    const val SECONDS_IN_A_WEEK = 7 * 24 * 3600
    const val SECONDS_IN_A_MONTH = 30 * 24 * 3600
    const val SECONDS_IN_A_YEAR = 365 * 24 * 3600

    /**
     * Returns a print-ready time consisting of how long the time has passed in elapsed format (e.g. 3 seconds ago, a day ago, 2 months ago).
     * The level of the elapsed time goes from seconds, minutes, hours, days, weeks, months, to years
     */
    fun getRelativeSpanFromToday(mContext: Context, ts: Long): String {
        if (ts.toInt() == -1) {
            return ""
        }

        val diffS = ((Date().time - ts) / 1000).toInt() // In seconds
        when (diffS) {
            // Under 1 minute ago
            in 0..60 -> return mContext.getString(R.string.seconds_ago, diffS.toLong())

            // Under 1 hr ago
            in 61..3600 -> {
                val min = (diffS / 60).toLong()
                return if (min == 1L) mContext.getString(R.string.one_minute_ago)
                else mContext.getString(R.string.minute_ago, min)
            }

            // Under 1 day ago
            in 3601..SECONDS_IN_A_DAY -> {
                val hours = (diffS / 3600).toLong()
                return if (hours == 1L) mContext.getString(R.string.one_hour_ago)
                else mContext.getString(R.string.hour_ago, hours)
            }

            // Under 1 week ago
            in (SECONDS_IN_A_DAY + 1)..SECONDS_IN_A_WEEK -> {
                val day = (diffS / SECONDS_IN_A_DAY).toLong()
                return if (day == 1L) mContext.getString(R.string.one_day_ago)
                else mContext.getString(R.string.day_ago, day)
            }

            // Under 1 month ago
            in (SECONDS_IN_A_WEEK + 1)..SECONDS_IN_A_MONTH -> {
                val wk = (diffS / SECONDS_IN_A_WEEK).toLong()
                return if (wk == 1L) mContext.getString(R.string.one_week_ago)
                else mContext.getString(R.string.week_ago, wk)
            }

            // Under 1 year ago
            in (SECONDS_IN_A_MONTH + 1)..SECONDS_IN_A_YEAR -> {
                val mo = (diffS / SECONDS_IN_A_MONTH).toLong()
                return if (mo == 1L) mContext.getString(R.string.one_month_ago)
                else mContext.getString(R.string.month_ago, mo)
            }

            // More than 1 year
            else -> {
                val year = (diffS / SECONDS_IN_A_YEAR).toLong()
                return if (year == 1L) mContext.getString(R.string.one_year_ago)
                else mContext.getString(R.string.year_ago, year)
            }
        }
    }
}