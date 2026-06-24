package de.maengelmelder.mainmodule.objects

import com.google.gson.Gson

/**
 * Data structure for user credentials and profile. This class does not hold password
 */
class UserCred {

    companion object {
        /**
         * User's account status
         */
        val STATUS_ENABLED = "enabled"
        val STATUS_DISABLED = "disabled"
    }

    /**
     * User Id
     */
    private var mId = ""
    /**
     * name
     */
    private var mPublicName = ""

    /**
     * First and lastname
     */
    private var mFirstname = ""
    private var mLastname = ""
    /**
     * Email, also used as login
     */
    private var mEmail = ""
    /**
     * Token. Used for various API usages to identify user
     */
    private var mToken = ""
    /**
     * Url to user's avatar image
     */
    private var mAvatarUri = ""
    /**
     * Profile status
     */
    private var mStatus = STATUS_ENABLED

    /**
     * Type
     */
    private var mType = ""

    /**
     * Domain
     */
    private var mDomain: Domain? = null

    /**
     * User's system info
     */
    private var mSystem: SystemInfo? = null

    var id: String
        get() = mId
        set(value) { mId = value }

    var publicName: String
        get() = mPublicName
        set(value) { mPublicName = value }

    var firstname: String
        get() = mFirstname
        set(value) { mFirstname = value }

    var lastname: String
        get() = mLastname
        set(value) { mLastname = value }

    var email: String
        get() = mEmail
        set(value) { mEmail = value }

    var token: String
        get() = mToken
        set(value) { mToken = value }

    var avatarUri: String
        get() = mAvatarUri
        set(value) {mAvatarUri = value}

    var status: String
        get() = mStatus
        set(value) { mStatus = value }

    var domain: Domain?
        get() = mDomain
        set(value) { mDomain = value }

    var type: String
        get() = mType
        set(value) { mType = value }

    var systemInfo: SystemInfo?
        get() = mSystem
        set(value) { mSystem = value }

    fun isAdmin(): Boolean = type == "admin"

    /**
     * Convert this object to a JSON-string using [Gson]
     *
     * @see Gson.toJson
     */
    fun toJson(gson: Gson? = null): String {
        val converter = gson?: Gson()
        return converter.toJson(this)
    }

    /**
     * Check whether this object holds a valid user info
     *
     * @return true if [mId], [mToken], and [mEmail] are not empty
     */
    fun isUserValid(): Boolean = mId.isNotEmpty() && mToken.isNotEmpty() && mEmail.isNotEmpty()

}