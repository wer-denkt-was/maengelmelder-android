package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import de.maengelmelder.mainmodule.R

class ImageSelectionDialog(c: Context,
                           onCameraSelected: () -> Unit,
                           onGallerySelected: () -> Unit) : Dialog(c) {

    /**
     * Generated dialog view
     */
    private var mView: View

    init {
        setCancelable(true)
        setTitle(R.string.step_choose_photo)
        mView = LayoutInflater.from(context).inflate(R.layout.mm_dialog_imageselection, null)
        setContentView(mView)

        val camera = mView.findViewById<View>(R.id.option_camera)
        val gallery = mView.findViewById<View>(R.id.option_gallery)
        camera.setOnClickListener {
            cancel()
            onCameraSelected()
        }
        gallery.setOnClickListener {
            cancel()
            onGallerySelected()
        }
    }
}