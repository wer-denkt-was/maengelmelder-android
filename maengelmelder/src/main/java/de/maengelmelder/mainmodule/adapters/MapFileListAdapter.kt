package de.maengelmelder.mainmodule.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.utils.MapCacheInfo
import de.maengelmelder.mainmodule.utils.interfaces.OnClickListitemListener
import java.io.File

class MapFileListAdapter : BaseAdapter {

    private var mContext: Context? = null
    private var mMapFiles: Array<File> = arrayOf()
    private var mListener: OnClickListitemListener? = null

    constructor(c: Context, mapFiles: Array<File>): super() {
        mContext = c
        mMapFiles = mapFiles
    }

    constructor(c: Context, directory: File): super() {
        mContext = c
        try {
            mMapFiles = directory.listFiles().filter { f -> f.path.endsWith(".tpk") }.toTypedArray()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var itemClickListener: OnClickListitemListener?
        set(value) { mListener = value }
        get() = mListener

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val v = convertView?: LayoutInflater.from(mContext).inflate(R.layout.mm_listitem_mapfile, parent, false)
        if (v.tag == null) {
            v.tag = ViewHolder(v)
        }
        val vh = v.tag as ViewHolder

        val file = getItem(position) as File
        val filename = MapCacheInfo.getFileNameFromPath(f = file)
        val fileSize = String.format("%.2f", file.length().toDouble() / 1000000.0)
        vh.mapName.text = filename + " ($fileSize MB)"
        vh.filePath.text = file.path
        vh.enabled.isChecked = MapCacheInfo.isMapEnabledOnOffline(mContext!!, filename)

        vh.enabled.setOnCheckedChangeListener { _, isChecked ->
            MapCacheInfo.setEnabledMapWhenOffline(mContext!!, filename, isChecked)
        }

        vh.mapName.setOnClickListener{ mListener?.onItem(v, position, getItemId(position)) }
        vh.filePath.setOnClickListener{ mListener?.onItem(v, position, getItemId(position)) }

        return v
    }

    override fun getItem(position: Int): Any = mMapFiles[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = mMapFiles.size

    inner class ViewHolder(v: View) {
        val enabled = v.findViewById<CheckBox>(R.id.chk_enabled)
        val mapName = v.findViewById<TextView>(R.id.mapname)
        val filePath = v.findViewById<TextView>(R.id.mapfilepath)
    }
}