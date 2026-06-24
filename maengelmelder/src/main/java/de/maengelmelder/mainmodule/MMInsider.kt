package de.maengelmelder.mainmodule

import android.content.Context
import de.maengelmelder.mainmodule.database.MMDB

/**
 * Class that enables app host to get information specific to MM.
 * These information are read-only. No methods in this class can change
 * any settings within MM
 */
class MMInsider(c: Context) {

    private val mDB = MMDB.instance(c)

    /**
     * Returns the number of reports that are not uploaded yet
     */
    fun getRemainingReports(): Int {
        return mDB.getMessagesCount(
                mDB.constants.COL_ORIGIN to mDB.constants.ORIGIN_SERVER,
                mDB.constants.COL_UPLOAD_STATUS to "")
    }

}