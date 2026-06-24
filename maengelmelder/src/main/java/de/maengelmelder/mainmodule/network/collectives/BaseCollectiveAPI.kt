package de.maengelmelder.mainmodule.network.collectives

import android.content.Context
import android.os.AsyncTask
import de.maengelmelder.mainmodule.objects.SystemInfo
import java.lang.ref.WeakReference

/**
 * This serves as a base class for api calls that should be done in FIFO order and requires iteration for each given [SystemInfo].
 * Use [de.maengelmelder.mainmodule.network.MMAPI.executeInThread] inside [AsyncTask.doInBackground] so each API calls will be sequential
 */
@Deprecated(
        message = "AsyncTask is deprecated since SDK 30.",
        replaceWith = ReplaceWith("network.collectives.coroutine.IOCoroutine")
)
abstract class BaseCollectiveAPI<T>(context: Context, systemInfos: List<SystemInfo>): AsyncTask<Void, Void, T>() {

    /**
     * @property mContext weak ref to Context
     * @property mSystems system infos
     * @property mOnFinished closure when all API calls are finished and the result is given
     */
    protected val mContext = WeakReference(context)
    protected val mSystems = systemInfos
    protected var mOnFinished: ((T) -> Unit)? = null

    var onFinished: ((T) -> Unit)?
        get() = mOnFinished
        set(value) { mOnFinished = value }

    override fun onPostExecute(result: T) {
        mOnFinished?.invoke(result)
    }

    open fun cancelRequest() {
        cancel(true)
    }
}