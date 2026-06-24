package de.maengelmelder.mainmodule.network.coroutines

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.utils.ContentUriRequestBody
import de.maengelmelder.mainmodule.network.utils.NetworkUtil
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.UserData
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URLConnection
import java.security.cert.CertificateException

/**
 * Base API class for network request using Kotlin coroutines.
 * AsyncTask is deprecated beginning from Android 11 (SDK 30)
 *
 * NOTE: Use this instead of the deprecated MMAPI if you are creating new endpoint class
 */
abstract class MMNetworkRepository<T, K>(
        c: Context,
        domainId: Int,
        methodName: String,
        externalSys: SystemInfo? = null): ViewModel() {

    companion object {
        private val DEBUG_TEXT = "MMNetworkRepo"
        private val DefaultClient = MMOkHttpClient
                .generateDefaultBuilder(BuildConfig.DEBUG, DEBUG_TEXT)
                .build()
    }

    /**
     * APP ID.
     * Set 1 as default or any value the [MMConstants.OverrideAppId] gives so long as it's not empty
     */
    protected var appId: String =
            if (MMConstants.ForceUseOverriddenAppId) MMConstants.OverrideAppId
            else "1"

    /**
     * Basic parameters for other derived classes
     */
    protected var context = c
    protected var mListener: MMBMS.BMSListener<T, K>? = null
    protected var extSystemInfo = externalSys
    protected var domainId = domainId
    protected var methodName = methodName
    protected var phoneId: String = ResourceProxy.getPhoneId(context)
    protected var addBearerTokenWhenAvailable: Boolean = true
    protected val userCred = UserData.getUserCred(context)

    /**
     * Multipart form-data
     */
    private var mMultipartBody: MultipartBody.Builder? = null
    /**
     * Raw JSON POST data
     */
    private var mJsonBody: RequestBody? = null

    private var mCall: Call? = null
    private var mJob: Job? = null

    /**
     * Setter-getter for listener
     */
    var listener: MMBMS.BMSListener<T, K>?
        get() = mListener
        set(value) { mListener = value }

    /**
     * Setter-getter for external [SystemInfo]
     */
    var externalSystemInfo : SystemInfo?
        get() = extSystemInfo
        set(value) { extSystemInfo = value }

    var attachUserCred: Boolean
        get() = addBearerTokenWhenAvailable
        set(value) {
            addBearerTokenWhenAvailable = value
        }

    /**
     * Execute the API call using IO dispatcher and returns the result back to main thread
     */
    fun execute() {
        mJob = viewModelScope.launch {
            // This is now inside a default coroutine

            val resp = withContext(Dispatchers.IO) {
                // Switch to IO Thread for network request
                if (isActive) doExecuteAPI(this)
                else null
            }

            // Back to default coroutine to display feedback to user
            resp?.let { r ->
                if (r.isSuccess()) {
                    val successResp = parseResponse(r)
                    mListener?.onData(successResp)
                } else {
                    val errorResp = parseError(r)
                    mListener?.onFail(errorResp)
                }
            }
        }
    }

    /**
     * Cancels currently running job and call if possible
     */
    fun cancel(reason: String = "cancel() called manually from code") {
        mCall?.cancel()
        mJob?.cancel(CancellationException(reason))
    }

    private fun buildRequest(scope: CoroutineScope? = null): Request? {
        // Get query parameters
        val mapParam = getQueryParameters()
        if (scope?.isActive == false) return null

        // Generate url
        var platformParam = "pf=android&appv=${MMConstants.APP_VERSION}&package=${context.packageName}"
        val url = if (!mapParam.isNullOrEmpty()) {
            val qParam = NetworkUtil.paramFromMap(mapParam)
            (getUrl() + qParam + "&" + platformParam)
        } else {
            (getUrl() + "?" + platformParam)
        }


        debug(url)
        if (scope?.isActive == false) return null

        // Create a request builder
        val reqB = Request.Builder().url(url)

        // Setup authentication header
        val bearerAuth = NetworkUtil.getBearerAuthentication(context)
        if (!bearerAuth.isNullOrEmpty() && addBearerTokenWhenAvailable) {
            debug("bearer auth = $bearerAuth")
            reqB.addHeader("Authorization", bearerAuth)
        } else if (BuildConfig.debug) {
            // Debug mode without user login? use wdw:up2date for basic auth
            val auth = NetworkUtil.getBasicAuthentication(context)
            debug("using basic auth: $auth")
            if (auth != null) {
                reqB.addHeader("Authorization", auth)
            }
        }
        if (scope?.isActive == false) return null

        val multipart = mMultipartBody
        val jsonBody = mJsonBody
        if (multipart != null) {
            reqB.post(multipart.build())
        } else if (jsonBody != null) {
            reqB.post(jsonBody)
        }

        // Create the request
        val req = reqB.build()
        debugRequest(req)
        if (scope?.isActive == false) return null

        return req
    }

    open fun getUrl(): String {
        val url = StringBuilder()
        var domainName = if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        extSystemInfo?.let { info -> domainName = info.domainName }

        var staticDomainId = MMConstants.getStaticDomainId(context)
        if (staticDomainId == 0) {
            staticDomainId = domainId
        }

        return url
                .append(domainName)
                .append(MMConstants.V1ApiPath)
                .append("/domain")
                .append("/$staticDomainId")
                .append("/$methodName")
                .toString()
    }

    fun doExecuteAPI(scope: CoroutineScope? = null): BaseResponse? {
        val req = buildRequest()
        var resp: Response? = null
        var code: Int
        var body = ""

        if (req == null) return null

        // Synchronously send the request
        try {
            if (scope?.isActive == false) return null
            mCall = DefaultClient.newCall(req)
            resp = mCall?.execute()
            code = resp?.code?: MMOkHttpClient.RESPSTATUS_CONNECTION_FAILED
            body = resp?.body?.string()?: ""

            if (BuildConfig.debug && resp != null) {
                doDebugResponse(resp)
                debug("- Response body:\n$body")
            }

        } catch (e: ConnectException) {
            e.printStackTrace()
            code = MMOkHttpClient.RESPSTATUS_CONNECTION_FAILED
            debug("- Connection Exception !!")
        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
            code = MMOkHttpClient.RESPSTATUS_TIMEOUT
            debug("- Socket Timeout Exception !!")
        } catch (e: IOException) {
            e.printStackTrace()
            code = MMOkHttpClient.RESPSTATUS_IOEXC
            debug("- IO Exception !!")
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            code = MMOkHttpClient.RESPSTATUS_ALREADY_EXEC
            debug("- Illegal State Exception !!")
        } catch (e: CertificateException) {
            e.printStackTrace()
            code = MMOkHttpClient.RESPSTATUS_INVALID_CERT
            debug("- Invalid Cert Exception !!")
        } finally {
            resp?.close()
        }

        return BaseResponse(code, body)
    }

    /**
     * debug message
     */
    private fun debug(msg: String) {
        if (BuildConfig.DEBUG || MMConstants.DebugAPICalls) Log.d(DEBUG_TEXT, msg)
    }
    /**
     * Method to debug the request's parameters, response, body, and headers
     */
    private fun debugRequest(req: Request) {
        if (!BuildConfig.DEBUG && !MMConstants.DebugAPICalls) return
        try {
            debug("\nHeaders:")
            req.headers.toMultimap().forEach { entry ->
                debug("- ${entry.key}: ${entry.value.size}")
                entry.value.forEach { value -> debug("---- $value") }
            }
            val buff = Buffer()
            req.body?.writeTo(buff)
            debug("\nBody:\n${buff.readUtf8()}")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Method to debug response
     */
    private fun doDebugResponse(resp: Response) {
        if (!BuildConfig.DEBUG) return
        debug("- req sent at: ${resp.sentRequestAtMillis} ms")
        debug("- resp received at: ${resp.receivedResponseAtMillis} ms")
        debug("- RTT: ${resp.receivedResponseAtMillis - resp.sentRequestAtMillis} ms")
        debug("- resp header size: ${resp.headers.byteCount()} bytes")
        (0 until resp.headers.size).forEach { i ->
            debug("------ header(${resp.headers.name(i)}): ${resp.headers.value(i)}")
        }
    }

    /**
     * Add multipart/form-data parameter with String content
     * @param name the key
     * @param content the value as string
     */
    fun multipartFormAddString(name: String, content: String) {
        if (mMultipartBody == null) {
            mMultipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        }
        mMultipartBody?.addFormDataPart(name, content)
    }

    /**
     * Add multipart/form-data parameter with image content. Bitmap will be loaded here
     * @param name the key
     * @param imgFile the image ion [File]
     * @param quality quality level of the image. Value between 0-100
     * @param maxWidth maximal width of the image
     * @param format image format. If none is given, it will try to guess the format from the file
     * @see [multipartFormAddImage]
     */
    fun multipartFormAddImage(name: String, imgFile: File, quality: Int, maxWidth: Int = 1280, format: Bitmap.CompressFormat? = null) {
        if (mMultipartBody == null) {
            mMultipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        }
        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                ?: throw IllegalArgumentException("Bitmap from "+ imgFile.absolutePath +" is null!!")
        multipartFormAddImage(name, imgFile.name, bitmap, quality, maxWidth, format)
    }

    /**
     * Add multipart/form-data parameter with image content
     * @param name the key
     * @param imgFileName the name of image file
     * @param bitmap [Bitmap] of the image
     * @param quality quality level of the image. Value between 0-100
     * @param maxWidth max width of the image.
     * @param format image format. If none is given, it will try to guess the format from the file
     */
    fun multipartFormAddImage(name: String, imgFileName: String, bitmap: Bitmap,
                              quality: Int = 100,
                              maxWidth: Int = 1280,
                              format: Bitmap.CompressFormat? = null) {
        if (mMultipartBody == null) {
            mMultipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        }
        // Fetch the media type from the filename
        val type = URLConnection.guessContentTypeFromName(imgFileName)
        val mediaType = type.toMediaTypeOrNull()
        if (mediaType?.type != "image") throw IllegalArgumentException("Content is not an image!!")

        var scaledBitmap = bitmap
        if (maxWidth > 0) {
            // Reduce the size of the image to defined width if given
            scaledBitmap = ImageManipulator.getScaledBitmap(bitmap, maxWidth)
        }

        // Compress the image
        val baos = ByteArrayOutputStream()
        val imgFormat = format?: when (mediaType.subtype) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP_LOSSLESS
            else -> Bitmap.CompressFormat.JPEG
        }
        scaledBitmap.compress(imgFormat, quality, baos)
        val byteArr = baos.toByteArray()

        // Attach to request body
        mMultipartBody?.addFormDataPart(name, imgFileName, byteArr.toRequestBody(mediaType))
    }

    /**
     * Add [android.net.Uri] as content of multipart body
     */
    fun multipartFormAddUri(c: Context, name: String, filename: String?, uri: Uri) {
        if (mMultipartBody == null) {
            mMultipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        }
        val uriBody = ContentUriRequestBody(c.contentResolver, uri)
        mMultipartBody?.addFormDataPart(name, filename, uriBody)
    }

    /**
     * Adds JSON-String as POST body parameter. The multipart/form-data is checked first before this
     * @param json valid JSON string
     */
    fun setJsonBody(json: String) {
        mJsonBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
    }

    abstract fun getQueryParameters() : Map<String, String>?
    abstract fun parseResponse(resp: BaseResponse): T
    abstract fun parseError(resp: BaseResponse): K
}