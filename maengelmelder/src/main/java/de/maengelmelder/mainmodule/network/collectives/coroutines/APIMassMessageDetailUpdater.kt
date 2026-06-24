package de.maengelmelder.mainmodule.network.collectives.coroutines

import android.content.Context
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1MessageDetail
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Message
import kotlinx.coroutines.CoroutineScope

class APIMassMessageDetailUpdater(c: Context,
                                  messagesToUpdate: List<Message>,
                                  onFinished: ((List<Message>) -> Unit)? = null)
    : IOCoroutine<List<Message>>(c, listOf()) {

    private val mMsgsToUpdate = messagesToUpdate
    private val mOnFinished = onFinished

    override fun insideDispatcherIO(scope: CoroutineScope): Pair<List<Message>?, BaseResponse?> {
        if (mMsgsToUpdate.isEmpty()) return Pair(null, BaseResponse(-1, ""))

        val db = MMDB.instance(context)

        // For each message, fetch the message detail from server and update it in the local db entry
        mMsgsToUpdate.forEach { message ->
            if (message.serverId.isNotEmpty()) {
                if (isCancelled()) {
                    return Pair(null, BaseResponse(-1, "Cancelled"))
                }

                val apiCall = MMv1MessageDetail(context, message, true)
                val response = apiCall.doExecuteAPI()
                if (response?.isSuccess() == true) {
                    // Successful response. Parse and update entry in DB
                    val msgDetail = apiCall.parseResponse(response)
                    msgDetail?.let { d ->
                        // updates the states and color in db entry
                        db.updateMessage(message.id,
                                db.constants.COL_STATE to d.state,
                                db.constants.COL_STATE_EN to d.state_en,
                                db.constants.COL_COLOR to d.colorString)
                        message.state = d.state
                        message.state_en = d.state_en
                        message.colorString = d.colorString
                    }
                }
            }
        }

        return Pair(mMsgsToUpdate, null)
    }

    override fun onSuccess(data: List<Message>) {
        mOnFinished?.invoke(data)
    }

    override fun onError(err: BaseResponse) {
        // Will not be reached anyway
        mOnFinished?.invoke(listOf())
    }
}