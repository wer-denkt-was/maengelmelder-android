package de.maengelmelder.mainmodule.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.LoginActivity
import de.maengelmelder.mainmodule.activities.MessageProcessActivity
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.MMOkHttpClient
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Bms
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1BmsAppCategory
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Domain
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1System
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.BmsDomain
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.service.tasks.CategoriesAndAttributesThread

object DomainUtil {

    /**
     * Check if the message can be uploaded in the correct domain
     * based on its location and according to the app configuration
     */
    fun isMessageInCorrectDomain(context: Context, message: Message, categoryForced: Boolean = false, onResult: (Boolean) -> Unit) {
        MMv1System(context, message.lat, message.lon, false).apply {
            listener = (object: MMBMS.BMSListener<List<SystemInfo>, BaseResponse> {
                override fun onData(data: List<SystemInfo>) {
                    val extOnly = if (data.size == 1) data else data.filter { d -> d.isExternal }

                    if (categoryForced) {
                        // Forced category -> just make sure the category exists
                        MMv1BmsAppCategory(context, message.category.typeId).apply {
                            listener = (object : MMBMS.BMSListener<Category?, BaseResponse> {
                                override fun onData(data: Category?) {
                                    onResult(data != null)
                                }
                                override fun onFail(err: BaseResponse) {
                                    onResult(false)
                                }
                            })
                        }.execute()
                    } else {
                        // Category not forced -> check normally
                        MMv1Domain(context, message.lat, message.lon).apply {
                            externalSystemInfo = extOnly[0]
                            listener = (object: MMBMS.BMSListener<List<Domain>, BaseResponse> {
                                override fun onData(data: List<Domain>) {
                                    val correctDomains = data.find { d -> d.id == message.category.domainId }
                                    if (correctDomains != null && correctDomains.hasCategories()) {
                                        // Found the domain
                                        // The resulting domain should also have categories in it
                                        onResult(true)
                                    } /*else if (!MMConstants.MessageUploadOnDefaultDomainOnly || MMConstants.OverrideAppId !== "1") {
                                    // If the message can be uploaded anywhere (or appid is overridden), just return true
                                    Log.d("isMessageInCorrectDomain", "message can be uploaded anywhere")
                                    onResult(true)
                                } else if (message.category.isValid() && message.category.posReq != Category.POS_REQ) {
                                    // If message's category does not require position, ignore it as well
                                    Log.d("isMessageInCorrectDomain", "message doesn't need position")
                                    onResult(true)
                                }*/ else {
                                        onResult(false)
                                    }
                                }
                                override fun onFail(err: BaseResponse) {
                                    // Failed to obtain domain
                                    onResult(false)
                                }
                            })
                        }.execute()
                    }
                }
                override fun onFail(err: BaseResponse) {
                    // Failed to obtain System info
                    onResult(false)
                }
            })
        }.execute()
    }

    /**
     * Shows login dialog, but with forced domainid and system info
     * Note that this will log the previous account out
     */
    fun goToLogin(c: Context, a: Activity, domainId: Int, domainName: String, sysInfo: SystemInfo? = null) {
        val i = Intent(c, LoginActivity::class.java)
        i.putExtra(LoginActivity.BUNDLE_FORCE_DOMAIN_NAME, domainName)
        i.putExtra(LoginActivity.BUNDLE_FORCE_DOMAIN_ID, domainId)
        i.putExtra(LoginActivity.BUNDLE_FORCE_SYSTEM, sysInfo)
        a.startActivityForResult(i, LoginActivity.REQ_CODE)
    }

    /**
     *      Check if category can be selected for new message against logged in user's domainid
     *
     *      onResult returns 2 variables:
     *      1. Boolean. True = can continue, False = cannot continue
     *      2. Int. Status code
     *          1: Message can be uploaded with this category (user is not logged in and domain allows anonymous posting)
     *          2: Message can be uploaded with this category (user is logged in and domain allows anonymous posting, but domain id mismatches)
     *          3: Message is allowed to be created with this category due to failed API call from no connection
     *          -1: Not allowed because domain does not allow anonymous posting and user is not logged in
     *          -2: Not allowed because domain does not allow anonymous posting and domain id mismatches
     */
    fun isCategoryPickable(c: Context, a: Activity, cat: Category, sysInfo: SystemInfo? = null, onResult: (Boolean, Int) -> Unit) {
        val userData = UserData.getUserCred(c)
        MMv1Bms(c, cat.domainId.toInt(), false).apply {
            externalSystemInfo = sysInfo
            listener = (object : MMBMS.BMSListener<BmsDomain?, BaseResponse> {
                override fun onData(data: BmsDomain?) {
                    val canPostAnonym = data?.settings?.get("anon_questions") as Boolean
                    // uncomment this one for testing (assume all domains required signing in for uploading message)
                    // val canPostAnonym = false
                    if (userData == null) {
                        if (canPostAnonym) {
                            // Domain allows posting anonymously
                            onResult(true, 1)
                        } else {
                            // Domain does not allow posting as anonym
                            androidx.appcompat.app.AlertDialog.Builder(c)
                                .setMessage(c.getString(R.string.category_selected_no_posting_without_login, cat.domainText))
                                .setNegativeButton(R.string.dialog_cancel) { dialog, which ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(R.string.login2) { dialog, which ->
                                    goToLogin(c, a, cat.domainId.toInt(), cat.domainText, sysInfo)
                                    dialog.dismiss()
                                }.show()
                            onResult(false, -1)
                        }
                    } else if (userData.domain?.id != cat.domainId) {
                        if (canPostAnonym) {
                            onResult(true, 2)
                            // domain does not match, but message with given category can be uploaded anonymously
                            // No need to show dialog
                            /*
                            androidx.appcompat.app.AlertDialog.Builder(c)
                                .setMessage(c.getString(R.string.category_selected_only_anonym_post, cat.domainText))
                                .setPositiveButton(R.string.dialog_ok) { dialog, which ->
                                    // User confirms that
                                    dialog.dismiss()
                                    onResult(true, 2)
                                }
                                .setNegativeButton(R.string.dialog_cancel) { dialog, which ->
                                    dialog.dismiss()
                                }.show()
                            */
                        } else {
                            // Domain does not allow posting as anonym and user's domain mismatches
                            androidx.appcompat.app.AlertDialog.Builder(c)
                                .setMessage(c.getString(R.string.category_selected_no_posting_without_login, cat.domainText))
                                .setNegativeButton(R.string.dialog_cancel) { dialog, which ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(R.string.login2) { dialog, which ->
                                    goToLogin(c, a, cat.domainId.toInt(), cat.domainText, sysInfo)
                                    dialog.dismiss()
                                }.show()

                            onResult(false, -2)
                        }
                    } else {
                        // User is logged in and domain matches
                        // doSelectCategory(cat)
                        onResult(true, 1)
                    }
                }

                override fun onFail(err: BaseResponse) {
                    // Failed to retrieve BMS settings? Check connection error
                    if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.OfflineMode] == true
                        && (err.code == MMOkHttpClient.RESPSTATUS_CONNECTION_FAILED || err.code == MMOkHttpClient.RESPSTATUS_IOEXC)
                    ) {
                        // We assume that user has no internet connection so we can just continue
                        // The user won't be able to upload the message anyway
                        onResult(true, 3)
                    }
                }
            })
        }.execute()
    }

    /**
     * Check whether the given category id is available in the location defined by the given lat lon
     */
    fun isMessageCategoryAvailable(context: Context, lat: Double, lon: Double,
                                   typeid: Long,
                                   categoryForced: Boolean = false,
                                   onResult: (Category?, Boolean) -> Unit) {
        MMv1System(context, lat, lon, false).apply {
            listener = (object: MMBMS.BMSListener<List<SystemInfo>, BaseResponse> {
                override fun onData(data: List<SystemInfo>) {
                    val extOnly = if (data.size == 1) data else data.filter { d -> d.isExternal }

                    // 2 different categories:
                    // 1. open category -> checks with api/v1/bmsapp/<appid>/domain
                    // 2. link-exclusive category -> checks with api/v1/bmsapp/<appid>/category
                    if (categoryForced) {
                        MMv1BmsAppCategory(context, typeid).apply {
                            externalSystemInfo = extOnly[0]
                            listener = (object: MMBMS.BMSListener<Category?, BaseResponse> {
                                override fun onData(data: Category?) {
                                    if (data != null) {
                                        // If category exists, we trust it's the correct one
                                        onResult(data, true)
                                        // Save the category in DB so we can reference it later
                                        val newDomain = Domain().apply {
                                            systemId = extOnly[0].appId
                                            id = data.domainId
                                            uri = extOnly[0].domainName
                                            name = data.domainText
                                        }
                                        newDomain.addCategory(data)
                                        CategoriesAndAttributesThread(context, listOf(newDomain)).start()
                                    } else {
                                        onResult(null, false)
                                    }
                                }
                                override fun onFail(err: BaseResponse) {
                                    onResult(null, false)
                                }
                            })
                        }.execute()
                    } else {
                        MMv1Domain(context, lat, lon).apply {
                            externalSystemInfo = extOnly[0]
                            listener = (object: MMBMS.BMSListener<List<Domain>, BaseResponse> {
                                override fun onData(data: List<Domain>) {
                                    // For every domain, check if the given typeid exists in it
                                    var category: Category? = null
                                    data.forEach { d ->
                                        val foundCat = d.categoriesAsArray().find { c -> c.typeId == typeid }
                                        if (foundCat != null) {
                                            category = foundCat

                                            // We need to save the domain categories and attributes
                                            // So the app can refer to them later
                                            CategoriesAndAttributesThread(context, data).start()

                                            return@forEach
                                        }
                                    }
                                    if (category != null) {
                                        // Found the category
                                        onResult(category, true)
                                    } else {
                                        onResult(null, true)
                                    }
                                }
                                override fun onFail(err: BaseResponse) {
                                    // Failed to obtain domain
                                    onResult(null, false)
                                }
                            })
                        }.execute()
                    }
                }
                override fun onFail(err: BaseResponse) {
                    // Failed to obtain System info
                    onResult(null, false)
                }
            })
        }.execute()
    }

}