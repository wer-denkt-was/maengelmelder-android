package de.maengelmelder.mainmodule.network.collectives

import android.util.Log
import de.maengelmelder.mainmodule.network.MMAPI
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.ConcurrentModificationException
import kotlin.properties.Delegates

@Deprecated(
        message = "Since AsyncTask is deprecated starting from SDK 30, we leave it to coroutines"
)
class MMAPIMetaQueue: AbstractQueue<MMAPI>() {

    private val mQueue = arrayListOf<MMAPI>()
    private var mGroupApiListener: GroupAPIListener? = null

    var listener: GroupAPIListener?
        get() = mGroupApiListener
        set(value) { mGroupApiListener = value }

    private var mAPICount: Int by Delegates.observable(0) { _, old, new ->
        val remaining = if (new < 0) 0 else new
        if (old > 0 && remaining == 0) {
            mGroupApiListener?.onQueueEmpty()
        } else if (remaining > old) {
            mGroupApiListener?.onAPIAdded(mQueue.size)
        }
    }

    fun resolve(api: MMAPI) {
        try {
            val idx = mQueue.indexOfFirst { a -> a.creationTimestamp == api.creationTimestamp }
            if (idx > -1) {
                mQueue.removeAt(idx)
                mAPICount -= 1
            }
        }
        catch (e: IndexOutOfBoundsException) { /* Queue is empty but tries to resolve */ }
        catch (e: Exception) { /* Other exception */ }
    }


    fun clearQueue() {
        try {
            mQueue.forEach { api -> if (!api.isRequestCancelled()) api.cancelRequest() }
            mQueue.clear()
            mAPICount = 0
        } catch (e: ConcurrentModificationException) {
            // Trying to modify queue when it is being modified
        }
    }

    override fun offer(e: MMAPI?): Boolean {
        if (e != null) {
            mQueue.add(e)
            mAPICount += 1
            e.enqueue()
            return true
        }
        return false
    }

    override fun iterator(): MutableIterator<MMAPI> = mQueue.iterator()

    override fun peek(): MMAPI? = try { mQueue[0] } catch (e: Exception) { null }

    override fun poll(): MMAPI? {
        val api = peek()
        mQueue.removeAt(0)
        return api
    }

    override val size: Int get() = mQueue.size

    interface GroupAPIListener {
        fun onQueueEmpty()
        fun onAPIAdded(queueLength: Int)
    }
}