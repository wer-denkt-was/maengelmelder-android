package de.maengelmelder.mainmodule.network.collectives

import android.content.Context
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.v1.MMv1MessageDetail
import de.maengelmelder.mainmodule.objects.Message

/**
 * Fetch the latest message detail from the given [Message]s list and update the database entry for it
 * [Message]s in the list should have a valid [Message.serverId]. Only messages with valid server id and returned response
 * that will be updated in the local database
 */
@Deprecated(
        message = "AsyncTask is deprecated since SDK 30",
        replaceWith = ReplaceWith("network.collectives.coroutines.APIMassMessageDetailUpdater")
)
class MassMessageDetailUpdater(c: Context, messagesToUpdate: List<Message>) : BaseCollectiveAPI<Void?>(c, listOf()) {

    private val mMessagesToUpdate = messagesToUpdate;

    override fun doInBackground(vararg params: Void?): Void? {
        val context = mContext.get()
        if (mMessagesToUpdate.isEmpty() || context == null) return null

        val db = MMDB.instance(context)

        // For each message, fetch the message detail from server and update it in the local db entry
        mMessagesToUpdate.forEach { message ->

            if (isCancelled) return null

            if (message.serverId.isNotEmpty()) {
                val apiCall = MMv1MessageDetail(context, message, true)
                val response = apiCall.executeInThread()
                if (response.code == 200) {
                    // Successful response. Parse and update entry in DB
                    val msgDetail = apiCall.parseResponse(response)
                    msgDetail?.let { d ->
                        // updates the states and color in db entry
                        db.updateMessage(message.id,
                                db.constants.COL_STATE to d.state,
                                db.constants.COL_STATE_EN to d.state_en,
                                db.constants.COL_COLOR to d.colorString)
                    }
                }
            }
        }

        return null
    }
}