package de.maengelmelder.mainmodule.service.tasks

import android.content.Context
import android.os.AsyncTask
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.adapters.MessagesListAdapter
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.collectives.coroutines.IOCoroutine
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import kotlinx.coroutines.CoroutineScope
import java.lang.ref.WeakReference

/**
 * This Coroutine is used by [de.maengelmelder.mainmodule.activities.MessagesListActivity] to update all messages with the given IDs.
 */
internal class MessageLoadingCoroutine(ctx: Context,
                                       prevAdapter: MessagesListAdapter? = null,
                                       msgIds: List<String>? = null)
    : IOCoroutine<MessagesListAdapter>(ctx, listOf()) {

    /**
     * @property mDB database connection
     * @property mAdapter holds the adapter which will be sent as result
     * @property mListener output listener
     * @property mMsgIds the list of message Ids that are included
     */
    private val mDB = MMDB.instance(ctx)
    private val mMsgIds = msgIds
    private var mAdapter = prevAdapter
    private var mListener: Listener? = null

    override fun insideDispatcherIO(scope: CoroutineScope): Pair<MessagesListAdapter?, BaseResponse?> {
        var messages = mDB.getMessages(mDB.constants.COL_ORIGIN to mDB.constants.ORIGIN_SERVER).toList()

        if (mMsgIds != null && mMsgIds.isNotEmpty()) {
            messages = messages.filter { m -> mMsgIds.contains(m.generateId()) }
        }

        if (mAdapter == null) {
            mAdapter = MessagesListAdapter(context, messages)
        } else {
            mAdapter?.removeAll()
            mAdapter?.setData(messages)
        }
        return Pair(mAdapter, null)
    }

    override fun onSuccess(data: MessagesListAdapter) {
        mListener?.onAdapterReady(data)
    }

    override fun onError(err: BaseResponse) {
        // Should not happen
    }

    fun setListener(l: Listener) {
        mListener = l
    }
    interface Listener {
        fun onAdapterReady(adapter: MessagesListAdapter?)
    }
}