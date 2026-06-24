package de.maengelmelder.mainmodule.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Login
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Logout
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.LoginWithTokenResponse
import de.maengelmelder.mainmodule.objects.UserCred
import java.lang.Exception
import java.nio.charset.Charset
import kotlin.math.roundToInt

/**
 * This object is used to manage user's credentials used for login, etc.
 */
object UserData {

    /**
     * Preference key to save user's username and pass
     */
    private val KEY_USER = "wdw.maengelmelder.user.user"
    private val KEY_PASS = "wdw.maengelmelder.user.pass"
    private val KEY_USERID = "wdw.maengelmelder.user.userid"
    private val KEY_TOKEN = "wdw.maengelmelder.user.token"
    private val KEY_USER_CRED = "wdw.maengelmelder.usercred"

    /**
     * Encode user object into JSON and vice versa
     */
    private val mGson = Gson()
    private lateinit var mPref: SharedPreferences

    /**
     * Get singleton [SharedPreferences] instance
     *
     * @param ctx context
     * @return [SharedPreferences]
     */
    private fun getPref(ctx: Context): SharedPreferences {
        if (!::mPref.isInitialized) mPref = PreferenceManager.getDefaultSharedPreferences(ctx)
        return mPref
    }

    /**
     * Retrieve the username and password in a form of basic authentication string (Basic <username:password in base64 string>)
     * Use [getForBearerAuth] to authenticate requests instead of this
     *
     * @param ctx context
     * @param useTestAuth If true, the method will return the basic authentication for test server, regardless of any saved credentials
     */
    fun getForBasicAuth(ctx: Context, useTestAuth: Boolean = false): String? {
        var username = ""
        var password = ""
        if (!useTestAuth) {
            val pref = getPref(ctx)
            username = pref.getString(KEY_USER, "")?: ""
            password = pref.getString(KEY_PASS, "")?: ""
            if (username.isEmpty() || password.isEmpty()) {
                // If AuthCred is available, use this instead
                if (MMConstants.AuthCred != null) {
                    username = MMConstants.AuthCred!!.first
                    password = MMConstants.AuthCred!!.second
                } else {
                    return  null
                }
            }
        } else {
            username = MMConstants.AuthCred_Test.first
            password = MMConstants.AuthCred_Test.second
        }
        if (BuildConfig.debug) {
            Log.d("API", "Authenticator: Basic: $username:$password")
        }
        val base64 = Base64.encodeToString("$username:$password".toByteArray(Charset.forName("UTF-8")), Base64.NO_WRAP)
        return "Basic $base64"
    }

    /**
     * Return saved token from user login as bearer token. Used to authenticate requests
     *
     * @param ctx Context
     * @return Bearer token or null if no token is found
     */
    fun getForBearerAuth(ctx: Context): String? {
        val uc = getUserCred(ctx)
        if (uc != null && uc.token.isNotEmpty()) {
            return "Bearer ${uc.token}"
        }
        return null
    }

    /**
     * Execute the [MMv1Login] task to obtain authentication params. These params (token/userId) can be used on other requests
     * so that any endpoints executed will be done as if the user is logged in (e.g. admin messages, etc). It automatically saves
     * all data upon successful login
     *
     * @param ctx context
     * @param username username
     * @param password pass
     * @param response [MMv1Login]'s response function.
     */
    fun login(ctx: Context, username: String, password: String, response: (UserCred?) -> Unit) {
        MMv1Login(ctx, username, password).apply {
            listener = (object: MMBMS.BMSListener<UserCred, BaseResponse> {
                override fun onData(data: UserCred) {
                    // save the username, password, userID, and token
                    if (data.isUserValid()) {
                        saveUserCred(ctx, data)
                    } else {
                        removeUserCred(ctx)
                    }
                    saveUsername(ctx, username)
                    response.invoke(data)
                }

                override fun onFail(err: BaseResponse) {
                    // save only the username
                    saveUsername(ctx, username)
                    response.invoke(null)
                }
            })
        }.execute()
    }

    /**
     * Remove the server token by calling [MMv1Logout]. Also removes them from device and logs user out
     *
     * @param ctx Context
     * @param response resulting response
     */
    fun logout(ctx: Context, response: (BaseResponse?) -> Unit) {
        val existing = getUserCred(ctx)
        if (existing == null) {
            // If there is no saved credentials, just invoke the listener
            response.invoke(null)
            return
        }
        removeUserCred(ctx)
        MMv1Logout(ctx).apply {
            listener = (object : MMBMS.BMSListener<BaseResponse, BaseResponse> {
                override fun onData(data: BaseResponse) {
                    response.invoke(data)
                }
                override fun onFail(err: BaseResponse) {
                    response.invoke(null)
                }
            })
        }.execute()
    }

    /**
     * Get saved username
     *
     * @param ctx context
     */
    fun getUsername(ctx: Context): String {
        mPref = getPref(ctx)
        return mPref.getString(KEY_USER, "")?: ""
    }

    /**
     * Get saved token
     *
     * @param ctx Context
     */
    @Deprecated("Use getUserCred(Context) instead")
    fun getToken(ctx: Context): String {
        mPref = getPref(ctx)
        return mPref.getString(KEY_TOKEN, "")?: ""
    }

    /**
     * Get saved password
     *
     * @param ctx context
     */
    @Deprecated("Password should not be saved per deprecation of save() method. This will always return empty string")
    fun getPassword(ctx: Context): String {
        mPref = getPref(ctx)
        return mPref.getString(KEY_PASS, "")?: ""
    }

    /**
     * Save the username and password provided through the parameters
     *
     * @param ctx context
     * @param user username
     * @param pass password
     */
    @Deprecated("Password should not be saved in Preference in clear text for security reason. Only username should be saved")
    fun save(ctx: Context, user: String, pass: String) {
        mPref = getPref(ctx)
        mPref.edit().putString(KEY_USER, user).putString(KEY_PASS, pass).apply()
    }

    /**
     * Save user's credential object ([UserCred]). The object will be transformed to JSON string using [Gson] and saved
     * to the preference
     *
     * @param ctx Context
     * @param userCred [UserCred] object
     */
    fun saveUserCred(ctx: Context, userCred: UserCred) {
        mPref = getPref(ctx)
        mPref.edit().putString(KEY_USER_CRED, userCred.toJson(mGson)).apply()
    }

    /**
     * Remove the saved, json-encoded [UserCred] from preference
     *
     * @param ctx Context
     */
    fun removeUserCred(ctx: Context) {
        mPref = getPref(ctx)
        mPref.edit().remove(KEY_USER_CRED).apply()
    }

    /**
     * Returns the json-encoded [UserCred] from preference as the object itself
     *
     * @param ctx Context
     * @return [UserCred] or null if it does not exist
     */
    fun getUserCred(ctx: Context): UserCred? {
        mPref = getPref(ctx)
        val ucJson = mPref.getString(KEY_USER_CRED, "")
        return if (ucJson?.isEmpty() == true) null
            else try { mGson.fromJson<UserCred>(ucJson, UserCred::class.java) } catch (e: Exception) { null }
    }

    fun saveUsername(ctx: Context, username: String) {
        mPref = getPref(ctx)
        mPref.edit().putString(KEY_USER, username).apply()
    }

    /**
     * Save authentication parameters
     *
     * @param ctx Context
     * @param token Authentication token
     * @param userId ID of the user
     *
     */
    @Deprecated("use saveUserCred() instead")
    fun saveAuth(ctx: Context, token: String, userId: String) {
        mPref = getPref(ctx)
        mPref.edit().putString(KEY_TOKEN, token).putString(KEY_USERID, userId).apply()
    }

    /**
     * Remove the saved username and password
     *
     * @param ctx Context
     */
    @Deprecated("use removeUserCred(Context) instead")
    fun unsave(ctx: Context) {
        mPref = getPref(ctx)
        mPref.edit().remove(KEY_PASS).remove(KEY_TOKEN).remove(KEY_USERID).apply()
    }

    /**
     * Encrypt a text
     */
    private fun encrypt(clearText: String): String {
        val buffer = StringBuilder()
        val mod = 2
        clearText.forEachIndexed {idx, c ->
            when (idx % mod) {
                0 -> buffer.append(c.plus(clearText.length))
                1 -> buffer.append(c.minus(clearText.length))
            }
            buffer.append((Math.random() * 100).roundToInt().toChar())
        }
        return buffer.toString()
    }

    /**
     * Decrypt the ciphertext encrypted with [encrypt]
     */
    private fun decrypt(encrypted: String): String {
        val buffer = StringBuilder()
        val mod = 2
        val originalSize = encrypted.length / 2
        var minus = true
        encrypted.forEachIndexed { idx, c ->
            when (idx % mod) {
                0 -> {
                    if (minus) buffer.append(c.minus(originalSize))
                    else buffer.append(c.plus(originalSize))
                    minus = !minus
                }
            }
        }
        return buffer.toString()
    }

}