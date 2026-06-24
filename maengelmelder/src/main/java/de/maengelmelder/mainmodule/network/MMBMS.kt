package de.maengelmelder.mainmodule.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.UserData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * This class is the first derivation of [MMAPI] from old API
 *
 * Almost every other API calls inherit from this class. It has 2 generic classes:
 * - T is a class for correct response (Generally, 200 response OK)
 * - K is a class for incorrect response (Response codes other than the above)
 *
 * INFO: This class can still be used as API listener from [MMNetworkRepository], but don't create a new API class from this anymore
 *          Use [MMNetworkRepository] instead
 */
abstract class MMBMS<T, K>(context: Context, method: String) : MMAPI() {

    /**
     * Reference to context
     */
    private var mWeakRef = WeakReference(context)

    /**
     * APP ID. Set 1 as default or any value the [MMConstants.OverrideAppId] gives so long as it's not empty
     */
    protected var appId: String = MMConstants.OverrideAppId

    /**
     * Generated user id
     * @see [ResourceProxy.getPhoneId]
     */
    protected var phoneId: String = ResourceProxy.getPhoneId(context)

    /**
     * The method (basically part of the url)
     */
    protected var mMethod: String = method

    /**
     * Indicates that the url uses /bmsapi/ path
     */
    private var mBmsApiS: String = context.getString(R.string.bmsapi)

    /**
     * User credentials
     */
    protected var userCred = UserData.getUserCred(context)

    /**
     * Listener
     */
    protected var listener: BMSListener<T, K>? = null

    /**
     * resources for retrieving files related to the API
     */
    private val mRes = context.resources

    /**
     * Package name, used mostly to be appended as path
     */
    private val mPkgName = context.packageName

    /**
     * External file directory
     */
    protected val fileDir = context.filesDir

    /**
     * Information to external system.
     */
    protected var externalSystemInfo: SystemInfo? = null
    var system: SystemInfo?
        set(value) {
            externalSystemInfo = value
            if (!MMConstants.ForceUseOverriddenAppId)
                appId = value?.appId?: MMConstants.OverrideAppId
        }
        get() = externalSystemInfo

    /**
     * Context for subclasses
     */
    protected val context: Context?
        get() = mWeakRef.get()

    /**
     * Returns either live server URL or test server depending on gradle config value and external system info
     */
    override fun getURL(): String {
        var url = if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        externalSystemInfo?.let { info -> url = info.domainName }

        url += "/$mBmsApiS"
        url += "/$mMethod"
        return url
    }

    /**
     * Returns a blank image. The image is [R.drawable.blank_image] and is used for [MMConstants.BypassImageReq] parameter.
     *
     * @return [File] containing the blank image, originally retrieved from [R.drawable.blank_image]
     */
    protected fun getBlankImage(): File {
        val extStorage = fileDir.absolutePath
        val output = File("$extStorage/.$mPkgName/blank.png")
        if (!output.exists()) {
            val folder = File("$extStorage/.$mPkgName")
            folder.mkdirs()

            val fromDrb = BitmapFactory.decodeResource(mRes, R.drawable.blank_image)
            try {
                output.createNewFile()
                val fos = FileOutputStream(output)
                fromDrb.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return output
    }

    override fun getBasicAuthentication(): String? {
        val ctx = mWeakRef.get()
        return if (ctx != null) UserData.getForBasicAuth(ctx, BuildConfig.debug) else null
    }

    override fun getBearerAuthentication(): String? {
        val ctx = mWeakRef.get()
        return if (ctx != null) UserData.getForBearerAuth(ctx) else null
    }

    override fun onPostExecute(result: BaseResponse?) {
        if (result == null) return

        if (result.code in 200..299) {
            val data = parseResponse(result)
            listener?.onData(data)
        } else {
            val err = parseError(result)
            listener?.onFail(err)
        }
    }

    /**
     * Set listener when the API is successfully executed or failed
     */
    fun setListener(listener: BMSListener<T, K>): MMBMS<T, K> {
        this.listener = listener
        return this
    }

    /**
     * This method should parse the successful response contained in [BaseResponse.body]
     */
    abstract fun parseResponse(resp: BaseResponse): T

    /**
     * This method should parse the failed response contained in [BaseResponse.body]
     */
    abstract fun parseError(resp: BaseResponse): K

    interface BMSListener<T, K> {
        fun onData(data: T)
        fun onFail(err: K)
    }
}