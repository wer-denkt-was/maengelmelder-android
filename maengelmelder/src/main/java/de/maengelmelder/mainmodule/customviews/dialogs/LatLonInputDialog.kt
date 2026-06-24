package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import de.maengelmelder.mainmodule.R

class LatLonInputDialog (ctx: Context, onLocation: (Double, Double) -> Unit) : Dialog(ctx) {

    private val mOnLocationSet = onLocation

    private var mLatInp: EditText
    private var mLonInp: EditText
    private var mBtnSet: Button
    private var mBtnCancel: Button

    init {

        // inflate layout
        val view = LayoutInflater.from(context).inflate(R.layout.mm_dialog_latlon_form, null)
        setContentView(view)

        mLatInp = view.findViewById(R.id.inp_latitude)
        mLonInp = view.findViewById(R.id.inp_longitude)
        mBtnSet = view.findViewById(R.id.btn_location_mark)
        mBtnCancel = view.findViewById(R.id.btn_cancel)

        mBtnSet.setOnClickListener { v ->
            val lat = mLatInp.text.toString().toDoubleOrNull()
            val lon = mLonInp.text.toString().toDoubleOrNull()
            if (lat != null && lon != null) {
                mOnLocationSet.invoke(lat, lon)
                cancel()
            }
        }

        mBtnCancel.setOnClickListener { v -> cancel() }
    }

}