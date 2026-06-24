package de.maengelmelder.mainmodule.adapters

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.utils.MapMarkerFromURLCoroutine
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.comparators.SortBy
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class MessagesListAdapter(c: Context, messages: List<Message>) : BaseAdapter() {

    private val mContext = c
    private val mDB = MMDB.instance(c)
    private val mInflater = LayoutInflater.from(c)
    private val mBuilders = ArrayList<MessageBuilder>()
    private var mFiltered: List<MessageBuilder>? = null
    private val mComparator = SortBy()

    init {
        doInit(messages)
    }

    fun setData(msgs: List<Message>) {
        mBuilders.clear()
        doInit(msgs)
    }

    fun add(msg: Message) {
        val mb = MessageBuilder(msg)
        mBuilders.add(mb)
    }

    fun filter(desc: String, cat: String, favOnly: Boolean, status: Array<String>, domainId: Int) {
        if (mFiltered == null) mFiltered = ArrayList()

        mFiltered = mBuilders.filter { b ->
            var shouldAdd = desc.isEmpty() || b.description.lowercase().contains(desc.lowercase()) // Desc
            shouldAdd = shouldAdd && (!favOnly || b.message.isFavorite) // fav only
            shouldAdd = shouldAdd && (cat.isEmpty() || b.category.name.lowercase().contains(cat.lowercase())) // category
            shouldAdd = shouldAdd && (status.isEmpty() || status.contains(b.message.state)) // status
            shouldAdd = shouldAdd && (domainId < 0) || b.category.domainId == domainId.toString() // Domain Id
            shouldAdd

        }

        notifyDataSetChanged()
    }

    fun getFilteredSize(): Int = mFiltered?.size?: 0

    fun toggleFavorite(id: String) {
        mFiltered?.forEach { mb ->
            val msgId = mb.messageId
            if (msgId == id) {
                val isFav = mb.message.isFavorite
                mb.message.isFavorite = !isFav
                mDB.updateMessage(msgId, Pair(mDB.constants.COL_MARK_FAV, !isFav))
                return@forEach
            }
        }

        notifyDataSetChanged()
    }

    fun sort(by: SortBy.Param = SortBy.Param.DESC) {
        mComparator.param = by
        Collections.sort(mFiltered, mComparator)
        notifyDataSetChanged()
    }

    fun getStatuses(): Set<Pair<String, Int>> {
        val set = HashSet<Pair<String, Int>>()

        mBuilders.forEach { b ->
            val color = if (b.message.colorString == "green2") "lime" else b.message.colorString
            set.add(Pair(b.message.state, Color.parseColor(color)))
        }

        return set
    }

    private fun doInit(messages: List<Message>) {
        messages.forEach { msg -> add(msg) }
        mFiltered = mBuilders.toList()
    }

    override fun getView(idx: Int, v: View?, vg: ViewGroup?): View {
        val view = v?: mInflater.inflate(R.layout.mm_listitem_message, vg, false)
        if (view.tag == null) {
            view.tag = ViewHolder(view)
        }
        val vh = view.tag as ViewHolder

        val msg = getItem(idx) as MessageBuilder
        val markerid = msg.category.markerId
        if (MMConstants.UseMarkerUri) {
            MapMarkerFromURLCoroutine.run(msg.message.markerUrl) {
                if (it != null) vh.icon.setImageBitmap(it)
            }
        } else {
            vh.icon.setImageDrawable(ResourceProxy.getMarker(mContext, msg.message.colorString, markerid))
        }

        vh.title.text = msg.message.getDescriptionOnly()
        vh.status.text = msg.message.state
        vh.category.text = msg.category.getActualName()
        vh.favicon.visibility = if (msg.message.isFavorite) View.VISIBLE else View.INVISIBLE

        view.contentDescription = "${msg.message.state}, ${msg.message.category.displayedName}, ${msg.message.getDescriptionOnly()}"

        return view
    }

    fun removeAll() {
        mBuilders.clear()
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = mFiltered?.size?: 0

    override fun getItem(position: Int): Any? {
        return mFiltered?.get(position)
    }

    inner class ViewHolder(v: View) {
        val icon = v.findViewById<ImageView>(R.id.message_icon)
        val title = v.findViewById<TextView>(R.id.title)
        val category = v.findViewById<TextView>(R.id.category)
        val status = v.findViewById<TextView>(R.id.status)
        val favicon = v.findViewById<ImageView>(R.id.fav)
    }
}