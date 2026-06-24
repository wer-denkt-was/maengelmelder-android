package de.maengelmelder.mainmodule.adapters

import android.content.Context
import androidx.viewpager.widget.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.jsibbold.zoomage.ZoomageView
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.MessageImageUri
import de.maengelmelder.mainmodule.utils.images.ImageManipulator

class MessageImageViewPagerAdapter(c: Context, images: List<MessageImageUri>)
    : androidx.viewpager.widget.PagerAdapter() {

    private val mContext = c
    private var mImages = images

    override fun isViewFromObject(p0: View, p1: Any): Boolean = p0 == p1

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(mContext).inflate(R.layout.mm_viewpageritem_messageimage, container, false)
        val img = mImages[position]
        val imgContainer = view.findViewById<ZoomageView>(R.id.zoomableimage)
        if (img.originalUri.isNotEmpty()) {
            try {
                ImageManipulator.setImage(mContext, imgContainer, img.originalUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        container.addView(view)
        view.contentDescription = mContext.getString(R.string.acc_cd_msgdetail_foto_x_of_y,
                (position+1).toString(), mImages.size.toString())
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int = mImages.size

    fun setDataSource(list: List<MessageImageUri>) {
        mImages = list
        notifyDataSetChanged()
    }
}