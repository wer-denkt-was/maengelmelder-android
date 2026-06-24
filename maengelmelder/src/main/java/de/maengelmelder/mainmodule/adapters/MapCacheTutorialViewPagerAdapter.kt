package de.maengelmelder.mainmodule.adapters

import android.content.Context
import androidx.viewpager.widget.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import de.maengelmelder.mainmodule.R

class MapCacheTutorialViewPagerAdapter(c: Context, tutorials: Array<Triple<Int, String, String>>)
    : androidx.viewpager.widget.PagerAdapter() {

    private val mContext = c
    private val mTuts = tutorials

    override fun isViewFromObject(p0: View, p1: Any): Boolean = p0 == p1

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(mContext).inflate(R.layout.viewpager_tutorial_mapcache, container, false)
        val tutItem = mTuts[position]
        view.findViewById<ImageView>(R.id.img).setImageResource(tutItem.first)
        view.findViewById<TextView>(R.id.title).text = tutItem.second
        view.findViewById<TextView>(R.id.desc).text = tutItem.third
        container.addView(view)
        return view
    }

    // override fun isViewFromObject(view: View?, `object`: Any?): Boolean = view == `object`

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int = mTuts.size
}