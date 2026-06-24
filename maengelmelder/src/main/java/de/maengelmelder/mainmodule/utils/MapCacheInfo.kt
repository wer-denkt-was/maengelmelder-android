package de.maengelmelder.mainmodule.utils

import android.content.Context
import android.os.Environment
import androidx.preference.PreferenceManager
import java.io.File

/**
 *
 * ## Overview
 * This object manages offline Map files in .tpk extension
 *
 */
internal object MapCacheInfo {

    /**
     * @property PREF_PREFIX_MAPENABLED Prefix for preference key that denotes whether the map file should be displayed when needed. It is appended with the file name of the map file
     * @property MAP_FOLDER_NAME Name of the folder where the map files are stored
     */
    private val PREF_PREFIX_MAPENABLED = "mm.map.enabled."
    private val MAP_FOLDER_NAME = "mapcache"
    private val PREF_OFFLINE_MAP_ACTIVE = "wdw.map.offline_enabled"

    /**
     * Get the folder for storing map files. It will be created when it does not exist yet.
     * Debug and Live version of the module will use the same folder
     *
     * @param c Context object
     * @return [File] object which is the folder of the map files
     */
    fun getMapFolder(c: Context): File {
        var packageName = c.packageName
        if (packageName.endsWith(".debug")) packageName = packageName.removeSuffix(".debug")
        val folder = File(c.getExternalFilesDir("maengelmelder"), packageName + File.separator + MAP_FOLDER_NAME)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    /**
     * Get the map file inside the folder from [getMapFolder]
     *
     * @param c Context
     * @param nameWithoutExt name of the map file without the extension
     */
    fun getMapFile(c: Context, nameWithoutExt: String) = File(getMapFolder(c).absolutePath + File.separator + nameWithoutExt + ".tpk")

    /**
     * Check whether the map file is shown when being toggled on
     *
     * @param c Context
     * @param fileNameWithoutExt name of the map file without the extension
     *
     * @return true if it is shown, false otherwise
     */
    fun isMapEnabledOnOffline(c: Context, fileNameWithoutExt: String): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(c)
        return pref.getBoolean(PREF_PREFIX_MAPENABLED+fileNameWithoutExt, true)
    }

    /**
     * Set the map to be shown or not when toggled on
     *
     * @param c Context
     * @param fileNameWithoutExt name of the map file without the extension
     * @param enabled true, if the map should be shown when toggled on. False otherwise
     */
    fun setEnabledMapWhenOffline(c: Context, fileNameWithoutExt: String, enabled: Boolean = true) {
        val pref = PreferenceManager.getDefaultSharedPreferences(c)
        pref.edit().putBoolean(PREF_PREFIX_MAPENABLED+fileNameWithoutExt, enabled).apply()
    }

    /**
     * Get all map files' information and its "toggling" attribute. This method does not check for permission. So, you need check it first
     * before calling this method, otherwise it will return empty array.
     *
     * @param c Context
     * @return Array of String-Boolean pair. The string represents the map file name without extension. The boolean refers to the result obtained from [isMapEnabledOnOffline]
     */
    fun getAllMapInfo(c: Context): Array<Pair<String, Boolean>> {
        val pref = PreferenceManager.getDefaultSharedPreferences(c)
        val folder = getMapFolder(c).listFiles()?.filter { f -> f.path.endsWith(".tpk") }?.toTypedArray()
        val list = arrayListOf<Pair<String, Boolean>>()
        folder?.forEach { file ->
            val name = getFileNameFromPath(file)
            list.add(Pair(name, pref.getBoolean(PREF_PREFIX_MAPENABLED+name, true)))
        }
        return list.toTypedArray()
    }

    /**
     * Get the file name from the path without extension or folder path
     *
     * @param f file
     * @return file name
     */
    fun getFileNameFromPath(f: File): String {
        val path = f.absolutePath
        val lastIdxSlash = path.lastIndexOf("/")
        val extStart = path.lastIndexOf(".")
        if (lastIdxSlash < extStart && lastIdxSlash != -1 && extStart != -1) {
            return path.substring(lastIdxSlash+1, extStart)
        }
        return ""
    }

    /**
     * Get the extension of the file without dot (.)
     *
     * @param f File
     * @return the extension or empty string if the file has no extension
     */
    fun getExtension(f: File): String {
        val path = f.absolutePath
        val extStart = path.lastIndexOf(".")
        if (extStart != -1) {
            return path.substring(extStart+1, path.length)
        }
        return ""
    }

    fun isOfflineMapActive(c: Context): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(c)
        return pref.getBoolean(PREF_OFFLINE_MAP_ACTIVE, false)
    }

}