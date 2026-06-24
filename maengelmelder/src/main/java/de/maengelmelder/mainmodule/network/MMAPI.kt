package de.maengelmelder.mainmodule.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.utils.ContentUriRequestBody
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URLConnection
import java.net.URLEncoder
import java.security.cert.CertificateException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 *
 * MMAPI is the base API class for any network-related operation / data retrieval, particularly regarding REST API.
 * Since May 2018, it replaces the old, deprecated HTTP library with [OkHttpClient] from http://square.github.io/okhttp/.
 * Google's Volley library was also considered, but deemed not effective for large file uploads
 */
@Deprecated(
        message = "AsyncTask is deprecated since Android SDK 30",
        replaceWith = ReplaceWith("MMNetworkRepository")
)
abstract class MMAPI : AsyncTask<Any, Int, BaseResponse>() {

    companion object {

        /**
         * Basic client
         */
        private var DefaultClient: OkHttpClient = OkHttpClient.Builder().build()

        /**
         * Must be called in Your Application class. It initiates a default builder
         */
        fun init() {
            DefaultClient = getDefaultBuilder().build()
            System.setProperty("http.keepAlive", "true")
        }

        private fun getDefaultBuilder(): OkHttpClient.Builder {
            return OkHttpClient.Builder()
                    // 60 seconds connection timeout
                    .connectTimeout(60, TimeUnit.SECONDS)
                    // No timeout for read/write
                    .readTimeout(0, TimeUnit.SECONDS)
                    .writeTimeout(0, TimeUnit.SECONDS)
                    // No redirections
                    .followRedirects(false)
                    .followSslRedirects(false)
                    // basic connection pool that keeps max 5 connections
                    .connectionPool(ConnectionPool())
                    // Use modern TLS wherever possible
                    .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                    // Use 2.0, then fallback to 1.1 when not available
                    .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
        }

        /**
         * Toggle the debugging mode of the API calls. Adding [StethoInterceptor] so it can be analyzed through
         * chrome:inspect
         */
        fun toggleDebug(toggle: Boolean) {
            val builder = getDefaultBuilder()
            if (toggle) {
                builder.addNetworkInterceptor(StethoInterceptor())
                        .eventListener(DebugEventListener())
            }
            DefaultClient = builder.build()
        }

        /**
         * Request is aborted, particularly due to [cancel] call.
         */
        val RESPSTATUS_ABORTED              = -2

        /**
         * Failed to connect (No internet connection)
         */
        val RESPSTATUS_CONNECTION_FAILED    = -100

        /**
         * Endpoint is unknown or has expired. Ask server admin about this error
         */
        val RESPSTATUS_IOEXC                = -101

        /**
         * The task is already executed by [execute] call
         */
        val RESPSTATUS_ALREADY_EXEC         = -102

        /**
         * Timeout, probably due to slow internet connection
         */
        val RESPSTATUS_TIMEOUT              = -103

        /**
         * Timeout, probably due to slow internet connection
         */
        val RESPSTATUS_INVALID_CERT         = -104
    }

    private val mClient: OkHttpClient = DefaultClient
    private val mBuilder: Request.Builder = Request.Builder()

    private var mMultipartBody: MultipartBody.Builder? = null
    private var mRawJsonBody: RequestBody? = null

    private var mCall: Call? = null
    private var mCreationNS = System.nanoTime().toString()

    val creationTimestamp: String get() = mCreationNS

    /**
     * Returns a URL parameter string from a map. (?key=value&key2=value2&..). Both key and value will be encoded in UTF-8
     *
     * @param map map of key and values in string
     *
     * @return URL parameter
     */
    protected fun paramFromMap(map: Map<String, String?>): String {
        val param = StringBuilder()
        param.append("?")
        val it = map.iterator()

        while (it.hasNext()) {
            val entry = it.next()
            param.append(entry.key)
            param.append("=")
            param.append(URLEncoder.encode(entry.value, "utf-8"))
            if (it.hasNext()) param.append("&")
        }

        return param.toString()
    }

    override fun doInBackground(vararg p0: Any?): BaseResponse {
        return executeInThread()
    }

    /**
     * Make the API call. This method runs on the same thread as the calling thread.
     * We do not put the code within [doInBackground] due to the fact that some chained calls may need
     * to call this within the same thread for progress, tracking, and FIFO purpose.
     *
     * @see [de.maengelmelder.mainmodule.service.tasks.MessageCreateTask]
     */
    fun executeInThread(): BaseResponse {
        val req = setupRequest()
        if (req == null) {
            return BaseResponse(RESPSTATUS_ABORTED, "")
        }

        var resp: Response? = null
        var code = RESPSTATUS_IOEXC
        var body = ""
        try {
            // Execute the call
            if (isCancelled) { return BaseResponse(RESPSTATUS_ABORTED, "") }
            mCall = mClient.newCall(req)
            resp = mCall?.execute()
            code = resp?.code?: 0
            body = resp?.body?.string()?: ""

            if ((BuildConfig.debug || MMConstants.DebugAPICalls) && resp != null) {
                doDebugResponse(resp)
                Log.d("MMAPI", "- Response Body:")
                Log.d("MMAPI", body)
            }

        } catch (e: ConnectException) {
            e.printStackTrace()
            code = RESPSTATUS_CONNECTION_FAILED
        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
            code = RESPSTATUS_TIMEOUT
        } catch (e: IOException) {
            e.printStackTrace()
            code = RESPSTATUS_IOEXC
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            code = RESPSTATUS_ALREADY_EXEC
        } catch (e: CertificateException) {
            e.printStackTrace()
            code = RESPSTATUS_INVALID_CERT
        } finally {
            resp?.close()
        }

        return BaseResponse(code, body)
    }

    /**
     * Use [Call.enqueue] to put the request to a queue to be executed later. If you are using this method,
     * do not call [AsyncTask.execute] since it will unnecessarily call the REST APi again. One advantage using
     * this method is that you can re-use this class multiple times without creating a new instance of it.
     *
     * [AsyncTask.onPostExecute] will not be manually called after the call is resolved through [Callback].
     * The [AsyncTask.onPostExecute] is not called within the UI thread , so you need to use [android.app.Activity.runOnUiThread]
     * to make any updates for the UI
     */
    fun enqueue() {
        val req = setupRequest()
        if (req != null) {
            mCall = mClient.newCall(req)
            mCall?.enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onPostExecute(BaseResponse(RESPSTATUS_CONNECTION_FAILED, ""))
                }
                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    val body = response.body?.string()?: ""
                    onPostExecute(BaseResponse(code, body))
                }
            })
        }
    }

    /**
     * Build [Request] object
     */
    private fun setupRequest(): Request? {
        val param = getUrlParam()

        val url = if (param == null) getURL() else { getURL() + paramFromMap(param) }
        if (BuildConfig.debug) Log.i("MMAPI", "url : $url")

        if (isCancelled) { return null }

        // Add URL and required headers
        mBuilder.url(url)

        // Basic/Bearer auth
        val bearerToken = getBearerAuthentication()
        if (BuildConfig.debug) {
            Log.d("MMAPI", "Authorization = $bearerToken")
        }
        if (bearerToken != null && bearerToken.isNotEmpty()) {
            // Bearer token found? Use it to authenticate request
            mBuilder.addHeader("Authorization", bearerToken)
        } else if (BuildConfig.debug) {
            // Debug mode without user login? use wdw:up2date for basic auth
            val auth = getBasicAuthentication()
            if (auth != null) {
                mBuilder.addHeader("Authorization", auth)
            }
        }

        if (isCancelled) { return null }

        // Turn to multipart-form / JSON body immediately, when there's a POST request payload to be sent
        val postBody = mMultipartBody
        val rawPostBody = mRawJsonBody
        if (postBody != null) {
            mBuilder.post(postBody.build())
        } else if (rawPostBody != null) {
            mBuilder.post(rawPostBody)
        }

        if (isCancelled) { return null }

        val req = mBuilder.build()

        // Debugging request body and header
        if (BuildConfig.debug || MMConstants.DebugAPICalls) {
            doDebugRequest(req)
        }

        return req
    }

    /**
     * Method to show the request's parameters, response, body, and headers
     */
    private fun doDebugRequest(req: Request) {
        try {
            Log.d("MMAPI", "\nHeaders:")
            req.headers.toMultimap().forEach { entry ->
                Log.d("MMAPI", "- ${entry.key}: ${entry.value.size}")
                entry.value.forEach { value -> Log.d("MMAPI", "---- $value") }
            }
            val buff = Buffer()
            req.body?.writeTo(buff)
            Log.d("MMAPI", "\nBody:\n${buff.readUtf8()}")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Method to show response's params
     */
    private fun doDebugResponse(resp: Response) {
        Log.d("MMAPI", "- req sent at: ${resp.sentRequestAtMillis} ms")
        Log.d("MMAPI", "- resp received at: ${resp.receivedResponseAtMillis} ms")
        Log.d("MMAPI", "- RTT: ${resp.receivedResponseAtMillis - resp.sentRequestAtMillis} ms")
        Log.d("MMAPI", "- resp header size: ${resp.headers.byteCount()} bytes")
        (0 until resp.headers.size).forEach { i ->
            Log.d("MMAPI", "------ header(${resp.headers.name(i)}): ${resp.headers.value(i)}")
        }
    }

    /**
     * Cancel request call and the thread also
     * @see [Call.cancel]
     */
    fun cancelRequest() {
        mCall?.cancel()
        cancel(true)
    }

    fun isRequestCancelled(): Boolean {
        return mCall?.isCanceled() == true
    }

    fun isRequestExecuted(): Boolean {
        return mCall?.isExecuted() == true
    }

    /**
     * Add body content to the request. The request will automatically be turned to POST request with multipart/form-data
     *
     * @param name name of the request body
     * @param content content of the request body
     */
    fun addContent(name: String, content: String) {
        if (mMultipartBody == null) {
            mMultipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        }
        mMultipartBody?.addFormDataPart(name, content)
    }

    /**
     * Add file content to the request. The request will automatically be turned to POST request with multipart/form-data.
     * Request type is auto-detected depending on the extension of the file
     *
     * @param name name of the request body
     * @param content [File] that is going to be uploaded. It should exists
     */
    fun addContent(name: String, content: File) {
        if (mMultipartBody == null) {
            mMultipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        }

        val ctype = URLConnection.guessContentTypeFromName(content.name)
        mMultipartBody?.addFormDataPart(name, content.name, RequestBody.create(ctype.toMediaTypeOrNull(), content))
    }

    /**
     * Add image file to the request. The request will automatically be turned to POST request with multipart/form-data.
     * @param name name of the request body
     * @param quality quality of the image. From 0 - 100
     */
    fun addContent(name: String, imgFile: File, quality: Int, format: Bitmap.CompressFormat? = null) {
        if (mMultipartBody == null) {
            mMultipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        }
        val type = URLConnection.guessContentTypeFromName(imgFile.name)

        val mediaType = type.toMediaTypeOrNull()
        if (mediaType?.type != "image") throw IllegalArgumentException("Content is not an image!!")

        // Compress the image
        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                ?: throw IllegalArgumentException("Bitmap from "+ imgFile.absolutePath +" is null!!")

        val baos = ByteArrayOutputStream()
        val imgFormat = format?: when (mediaType.subtype) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        bitmap.compress(imgFormat, quality, baos)
        val byteArr = baos.toByteArray()

        // Attach to request body
        mMultipartBody?.addFormDataPart(name, imgFile.name, RequestBody.create(mediaType, byteArr))
    }

    /**
     * Add [Bitmap] to the request. The request will automatically be turned to POST request with multipart/form-data.
     * @param name name of the request body
     * @param imgFileName image filename with extension. Used only to guess compression format
     * @param bitmap Bitmap that will be uploaded
     * @param quality image quality, 0-100
     * @param format if not given, it will be derived from imgFileName
     */
    fun addContent(name: String, imgFileName: String, bitmap: Bitmap, quality: Int, format: Bitmap.CompressFormat? = null) {
        if (mMultipartBody == null) {
            mMultipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        }
        val type = URLConnection.guessContentTypeFromName(imgFileName)

        val mediaType = type.toMediaTypeOrNull()
        if (mediaType?.type != "image") throw IllegalArgumentException("Content is not an image!!")

        // Compress the image
        val baos = ByteArrayOutputStream()
        val imgFormat = format?: when (mediaType.subtype) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        bitmap.compress(imgFormat, quality, baos)
        val byteArr = baos.toByteArray()

        // Attach to request body
        mMultipartBody?.addFormDataPart(name, imgFileName, RequestBody.create(mediaType, byteArr))
    }

    /**
     * Add [android.net.Uri] as content of multipart body
     */
    fun addContent(c: Context, name: String, filename: String?, uri: Uri) {
        if (mMultipartBody == null) {
            mMultipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        }
        val uriBody = ContentUriRequestBody(c.contentResolver, uri)
        mMultipartBody?.addFormDataPart(name, filename, uriBody)
    }

    /**
     * Sets a raw POST body in JSON format. Since it conflicts with [addContent],
     * the JSON body will be set if [mMultipartBody] is null. So, it will prioritize multipart body first.
     *
     * @param json content of the JSON body in JSON format
     */
    fun setRawJsonBody(json: String) {
        mRawJsonBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)
    }

    /**
     * This method should return the basic authentication string that will be included within the request header
     * Use [getBearerAuthentication] for authentication instead of this method
     *
     * @see [de.maengelmelder.mainmodule.utils.UserData.getForBasicAuth]
     */
    abstract fun getBasicAuthentication(): String?

    /**
     * This method should return the bearer auth string that will be included for authentication
     *
     * @see [de.maengelmelder.mainmodule.utils.UserData.getForBearerAuth]
     */
    abstract fun getBearerAuthentication(): String?

    /**
     * This method should return the URL for the API call. You may also include URL parameter, but note that [getUrlParam] will also
     * generate the parameter for it.
     */
    abstract fun getURL(): String

    /**
     * This method should return the map of keys and values in string for the URL parameter. Null if not used
     */
    abstract fun getUrlParam(): Map<String, String?>?

}