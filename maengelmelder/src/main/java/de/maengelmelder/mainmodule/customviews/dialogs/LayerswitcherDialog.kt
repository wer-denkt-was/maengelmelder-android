package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.utils.GoogleMapHelper

class LayerswitcherDialog(
    val ctx: Context,
    val mapHelper: GoogleMapHelper,
    val singleSwitch: Boolean = false,
    val switchName: String? = null
): Dialog(ctx) {

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.mm_dialog_layerswitcher, null)
        setContentView(view)
        setCancelable(true)

        val parent = view.findViewById<LinearLayout>(R.id.layerswitchers)
        val layers = mapHelper.getCachedLayers()

        if (singleSwitch) {
            // If singleSwitch is enabled, the visible-Property of each MapLayerInfo is ignored.
            // (true by default)
            val name = switchName?: ctx.getString(R.string.layerswitch_name)
            val chkbox = CheckBox(context).apply {
                text = name
                isChecked = mapHelper.canShowLayer(name, null)
            }
            chkbox.setOnCheckedChangeListener(OnLayerSwitched(layers.keys.toTypedArray()))
            chkbox.setPadding(0, 10, 0, 10)
            chkbox.layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            parent.addView(chkbox)
        } else {
            layers.forEach { entry ->
                val chkbox = CheckBox(context).apply {
                    text = entry.key
                    isChecked = mapHelper.canShowLayer(entry.key, entry.value)
                }
                chkbox.setOnCheckedChangeListener(OnLayerSwitched(arrayOf(entry.key)))
                chkbox.setPadding(0, 10, 0, 10)
                chkbox.layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                parent.addView(chkbox)
            }
        }

        view.findViewById<Button>(R.id.btn_exit).setOnClickListener { v ->
            dismiss()
        }
    }

    inner class OnLayerSwitched(private val layerNames: Array<String>): OnCheckedChangeListener {
        override fun onCheckedChanged(p0: CompoundButton, p1: Boolean) {
            mapHelper.toggleLayer(layerNames, p1)
        }
    }

}