package de.maengelmelder.mainmodule.network.collectives.coroutines

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.SystemInfo
import kotlinx.coroutines.*

/**
 * Basic abstraction for Kotlin Coroutine using IO thread
 */
abstract class IOCoroutine<T>(c: Context, siList: List<SystemInfo>) : ViewModel() {
    protected val context = c
    protected val systemInfos = siList

    private var mJob: Job? = null

    /**
     * Execute the process inside
     */
    fun execute() {
        // Executed before entering IO thread
        beforeDispatcherIO()
        mJob = viewModelScope.launch {
            val resp = withContext(Dispatchers.IO) { insideDispatcherIO(this) }

            // Back to main thread to return the value to user
            if (resp.first !== null) {
                onSuccess(resp.first!!)
            } else {
                resp.second?.let { err -> onError(err) }
            }
        }
    }

    /**
     * Cancels the [Job]
     */
    fun cancel(cause: String = "IOCoroutine.cancel() called!") {
        mJob?.cancel(cause)
    }

    /**
     * Check if the job is cancelled
     */
    fun isCancelled() : Boolean = mJob?.isCancelled?: true

    open fun beforeDispatcherIO() { }
    abstract fun insideDispatcherIO(scope: CoroutineScope): Pair<T?, BaseResponse?>
    abstract fun onSuccess(data: T)
    abstract fun onError(err: BaseResponse)
}