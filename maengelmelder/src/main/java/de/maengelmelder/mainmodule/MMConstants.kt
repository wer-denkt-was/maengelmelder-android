package de.maengelmelder.mainmodule

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import de.maengelmelder.mainmodule.objects.Log
import de.maengelmelder.mainmodule.utils.ResourceProxy
import org.json.JSONObject
import java.lang.Exception

object MMConstants {

    var APP_VERSION: String = BuildConfig.VERSION_CODE.toString()
    private val SAVEKEY = "wdw.mm.constants"
    private var bConfigLoaded = false

    /**
     * @property AuthCred_Test basic auth for productionl if required
     * @property AuthCred_Test basic auth for test server
     * @property AutoLoginCred_Test username-password pair for automated login (Test-Server). Set both as empty to ignore it
     * @property AutoLoginCred Same with [AutoLoginCred_Test], but for Live system
     */
    var AuthCred: Pair<String, String>? = null
    var AuthCred_Test = Pair("", "")
    var AutoLoginCred_Test = Pair("", "")
    var AutoLoginCred = Pair("", "")

    /**
     * @property ServerUrl Mängelmelder's Live server URL
     * @property ServerUrl_Test Mängelmelder's Test server URL
     * @property V1ApiPath path to v1 API
     * @property BmsAppApiPath path to bmsapp API
     * @property OverridingApiKey api key sent along with message upload.
     * @property OverridingApiKey_Test Same with [OverridingApiKey], but for test server
     */
    var ServerUrl = ""
    var ServerUrl_Test = ""
    const val V1ApiPath = "/api/v1"
    const val BmsAppApiPath = "/bmsapp"
    var OverridingApiKey = ""
    var OverridingApiKey_Test = ""

    /**
     * @property RegistrationPageUrl URL for registration page
     * @property RegistrationPageUrl_Test Same as [RegistrationPageUrl], but for Test server
     */
    var RegistrationPageUrl = ""
    var RegistrationPageUrl_Test = ""

    /**
     * @property BypassImageReq If set to true, it will bypass image requirement for any category
     * by adding a blank image when an image is required
     * @property ShowDomainOnlyMessage if set to true, it will only show messages that come from the
     * specified domain id in [DefaultDomainId]
     * @property OverrideAppId App Id
     * @property DefaultDomainId default domain id. 32 is mängelmelder.de
     * @property DefaultDomainName name of the default domain
     * @property ForceUseOverriddenAppId if set to true, [OverrideAppId] will be used instead of any AppId from queried system
     * @property ForceHost_Test if set to true, the test app will always bypass v1/system api and use the given host every time
     * @property HideDescripton if set to true, description box for every message will not be shown
     * @property UseDefaultDomainWhenPossible if set to true, whenever no domain is found (e.g. logging in), it will create a domain from [DefaultDomainId] und [DefaultDomainName] and use them
     * @property AutoExpandCategoryList if set to true, it will automatically expand the header categories to show all child categories
     * @property AutoExpandWhenOnlyNSubcategory auto expand the category header when there is only N subcategory under it. Only works if [AutoExpandCategoryList] is set to false
     * @property ShowWelcomeMenuItem if true, it will show "Wilkommen" menu in sliding menu that links to Intro page
     * @property CategoryBeforePosition if true, the message creation page will show category first before position.
     * @property ForcePositionConfirmation if true, during message creation, user must confirm the correct position before proceeding to other steps.
     * @property ShowWarningNotEmergencyService if true, at first install, user will be warned not to use MM for emergency services
     * @property SpecializedMarkerFrameSuffix If given, the app will use specialized marker frame images with added suffix for message markers. Can only be used for app with limited domain usage
     * @property DebugAPICalls Toggles debugging messages for API calls
     * @property UseMarkerUri If true, use marker Uri obtained from the message object instead of building your own
     * @property SkipSplashscreen If true, skip the splashscreen (terms and privacy are still checked)
     * @property HideAboutAppMenu If true, hide the "About App"-Menuitem
     * @property EnableAddressSearch if true, shows address search in Location-Step.
     * @property EnableCategorySearch If true, enables category search in Category-Step. This only works if the domainid is known beforehand (whether it is from location search or default domainid)
     * @property ShowAppRatingWindow If true, app rating window will be shown based on settings for [ShowAppRatingWindowAfterNMessages]
     * @property ShowAppRatingWindowAfterNMessages App rating window will be shown in [OverviewMap] after the given number of messages uploaded
     * @property EnableMessageCreationFromQRCode If enabled, there will be an option to scan QR code to create message when clicking "Neue Meldung"
     * */
    var BypassImageReq = false
    var AutoExpandCategoryList = true
    var AutoExpandWhenOnlyNSubcategory = 1
    var ShowDomainOnlyMessage = false
    var MessageUploadOnDefaultDomainOnly = false
    var OverrideAppId = "1"
    var ForceUseOverriddenAppId = false
    var DefaultDomainId = 32
    var DefaultDomainName = "Mängelmelder.de"
    var UseDefaultDomainWhenPossible = false
    var NoLocationFallbackMessage: String = ""
    var ForceHost_Test = false
    var HideDescripton = false
    var ShowWelcomeMenuItem = false
    var CategoryBeforePosition = false
    var ForcePositionConfirmation = false
    var ShowWarningNotEmergencyService = false
    var SpecializedMarkerFrameSuffix = ""
    var SkipSplashscreen = false
    var HideAboutAppMenu = false
    var UseMarkerUri = false
    var EnableCategorySearch = false
    var ShowAppRatingWindow = false
    var ShowAppRatingWindowAfterNMessages = 5
    var EnableAddressSearch = false
    var EnableMessageCreationFromQRCode = false

    var AllowedDomainHostsForBrowsableUrl: Array<String> = arrayOf()
    var DebugAPICalls = false

    /**
     * @property UseStaticToS if set to true, the App will use ToS located in mm-html/terms.html instead of the web version
     * @property UseStaticPrivacyPolicy if set to true, the App will use privacy policy located in mm-html/policy.html instead of the web version
     * @property UseStaticImpressum if set to true, the App will use impressum located in mm-html/impressum.html instead of the web version
     * @property UseStaticAbout if set to true, the App will use about page located in mm-html/aboutapp.html instead of the web version
     */
    var UseStaticToS = true
    var UseStaticPrivacyPolicy = true
    var UseStaticImpressum = true
    var UseStaticAbout = true

    /**
     * @property DefaultLatLon default longitude-latitude pair. The map should move to this coordinate
     * when user's location is not available
     */
    var DefaultLatLon = Pair(0.0, 0.0)

    /**
     * In Photo-Step, allow this amount of images to be chosen
     */
    var MaxImageUploadOnReportCreation = 3

    // Various default values
    enum class DefaultValues { FormEmail, FormFirstName, FormLastName }
    var DefaultValuesMap = hashMapOf<DefaultValues, Any>()

    /**
     * Map of external URLs
     */
    enum class ExternalURL { AboutApp, Usage, Impressum, DataProtection }
    var ExternalURLsMap = hashMapOf(
            ExternalURL.AboutApp to "",
            ExternalURL.Usage to "",
            ExternalURL.Impressum to "",
            ExternalURL.DataProtection to ""
    )

    /**
     * Map of in-app features that can be turned on and off
     */
    enum class FeatureSetting { OfflineMode, UserLogin, ActivityHistory, MessageList, AppSettings, MultipleImages }
    var FeatureSettingsMap = hashMapOf(
            FeatureSetting.UserLogin to false,
            FeatureSetting.MessageList to true,
            FeatureSetting.ActivityHistory to false,
            FeatureSetting.AppSettings to false,
            FeatureSetting.MultipleImages to false,
            FeatureSetting.OfflineMode to false,
    )

    /**
     *  Save current settings to
     */
    fun save(c: Context) {
        val json = JSONObject()

        json.put("APPVERSION", APP_VERSION)

        json.put("DefaultValuesMap_FormEmail", DefaultValuesMap[DefaultValues.FormEmail])
        json.put("DefaultValuesMap_FormFirstName", DefaultValuesMap[DefaultValues.FormFirstName])
        json.put("DefaultValuesMap_FormLastName", DefaultValuesMap[DefaultValues.FormLastName])

        json.put("ExternalURLMap_AboutApp", ExternalURLsMap[ExternalURL.AboutApp])
        json.put("ExternalURLMap_Usage", ExternalURLsMap[ExternalURL.Usage])
        json.put("ExternalURLMap_Impressum", ExternalURLsMap[ExternalURL.Impressum])
        json.put("ExternalURLMap_DataProtection", ExternalURLsMap[ExternalURL.DataProtection])

        json.put("FeatureSettingMap_UserLogin", FeatureSettingsMap[FeatureSetting.UserLogin])
        json.put("FeatureSettingMap_MessageList", FeatureSettingsMap[FeatureSetting.MessageList])
        json.put("FeatureSettingMap_ActivityHistory", FeatureSettingsMap[FeatureSetting.ActivityHistory])
        json.put("FeatureSettingMap_AppSettings", FeatureSettingsMap[FeatureSetting.AppSettings])
        json.put("FeatureSettingMap_MultipleImages", FeatureSettingsMap[FeatureSetting.MultipleImages])
        json.put("FeatureSettingMap_OfflineMode", FeatureSettingsMap[FeatureSetting.OfflineMode])

        AuthCred?.let { auth ->
            json.put("AuthCred_id", auth.first)
            json.put("AuthCred_password", auth.second)
        }
        json.put("AutoLoginCred_Test_id", AutoLoginCred_Test.first)
        json.put("AutoLoginCred_Test_pass", AutoLoginCred_Test.second)
        json.put("AutoLoginCred_id", AutoLoginCred.first)
        json.put("AutoLoginCred_pass", AutoLoginCred.second)
        json.put("ServerUrl", ServerUrl)
        json.put("OverridingApiKey", OverridingApiKey)
        json.put("OverridingApiKey_Test", OverridingApiKey_Test)
        json.put("RegistrationPageUrl", RegistrationPageUrl)
        json.put("RegistrationPageUrl_Test", RegistrationPageUrl_Test)

        json.put("BypassImageReq", BypassImageReq)
        json.put("AutoExpandCategoryList", AutoExpandCategoryList)
        json.put("AutoExpandWhenOnlyNSubcategory", AutoExpandWhenOnlyNSubcategory)
        json.put("ShowDomainOnlyMessage", ShowDomainOnlyMessage)
        json.put("MessageUploadOnDefaultDomainOnly", MessageUploadOnDefaultDomainOnly)
        json.put("OverrideAppId", OverrideAppId)
        json.put("ForceUseOverriddenAppId", ForceUseOverriddenAppId)
        json.put("DefaultDomainId", DefaultDomainId)
        json.put("DefaultDomainName", DefaultDomainName)
        json.put("UseDefaultDomainWhenPossible", UseDefaultDomainWhenPossible)
        json.put("NoLocationFallbackMessage", NoLocationFallbackMessage)
        json.put("ForceHost_Test", ForceHost_Test)
        json.put("HideDescripton", HideDescripton)
        json.put("ShowWelcomeMenuItem", ShowWelcomeMenuItem)
        json.put("CategoryBeforePosition", CategoryBeforePosition)
        json.put("MaxImageUploadOnReportCreation", MaxImageUploadOnReportCreation)
        json.put("ForcePositionConfirmation", ForcePositionConfirmation)
        json.put("ShowWarningNotEmergencyService", ShowWarningNotEmergencyService)
        json.put("SkipSplashscreen", SkipSplashscreen)
        json.put("HideAboutAppMenu", HideAboutAppMenu)
        json.put("EnableCategorySearch", EnableCategorySearch)
        json.put("EnableAddressSearch", EnableAddressSearch)
        json.put("EnableMessageCreationFromQRCode", EnableMessageCreationFromQRCode)
        json.put("AllowedDomainHostsForBrowsableUrl", AllowedDomainHostsForBrowsableUrl.joinToString("||"))

        json.put("UseStaticToS", UseStaticToS)
        json.put("UseStaticPrivacyPolicy", UseStaticPrivacyPolicy)
        json.put("UseStaticImpressum", UseStaticImpressum)
        json.put("UseStaticAbout", UseStaticAbout)

        json.put("DefaultLat", DefaultLatLon.first)
        json.put("DefaultLon", DefaultLatLon.second)

        json.put("SpecializedMarkerFrameSuffix", SpecializedMarkerFrameSuffix)
        json.put("UseMarkerUri", UseMarkerUri)
        json.put("DebugAPICalls", DebugAPICalls)

        json.put("ShowAppRatingWindow", ShowAppRatingWindow)
        json.put("ShowAppRatingWindowAfterNMessages", ShowAppRatingWindowAfterNMessages)

        val pref = PreferenceManager.getDefaultSharedPreferences(c)
        pref.edit().putString(SAVEKEY, json.toString()).commit()
    }

    /**
     * Load MM settings from preference
     */
    fun load(c: Context) {
        val pref = PreferenceManager.getDefaultSharedPreferences(c)
        pref.getString(SAVEKEY, "")?.let { saved ->
            if (saved.isEmpty()) return
            val json: JSONObject? = try { JSONObject(saved) } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

            json?.let { j ->
                if (j.has("APPVERSION"))
                    APP_VERSION = j.optString("APPVERSION")

                if (j.has("DefaultValuesMap_FormEmail"))
                    DefaultValuesMap[DefaultValues.FormEmail] = j.optString("DefaultValuesMap_FormEmail")
                if (j.has("DefaultValuesMap_FormFirstName"))
                    DefaultValuesMap[DefaultValues.FormFirstName] = j.optString("DefaultValuesMap_FormFirstName")
                if (j.has("DefaultValuesMap_FormLastName"))
                    DefaultValuesMap[DefaultValues.FormLastName] = j.optString("DefaultValuesMap_FormLastName")

                if (j.has("ExternalURLMap_AboutApp"))
                    ExternalURLsMap[ExternalURL.AboutApp] = j.optString("ExternalURLMap_AboutApp")
                if (j.has("ExternalURLMap_Usage"))
                    ExternalURLsMap[ExternalURL.Usage] = j.optString("ExternalURLMap_Usage")
                if (j.has("ExternalURLMap_Impressum"))
                    ExternalURLsMap[ExternalURL.Impressum] = j.optString("ExternalURLMap_Impressum")
                if (j.has("ExternalURLMap_DataProtection"))
                    ExternalURLsMap[ExternalURL.DataProtection] = j.optString("ExternalURLMap_DataProtection")

                if (j.has("FeatureSettingMap_UserLogin"))
                    FeatureSettingsMap[FeatureSetting.UserLogin] = j.getBoolean("FeatureSettingMap_UserLogin")
                if (j.has("FeatureSettingMap_MessageList"))
                    FeatureSettingsMap[FeatureSetting.MessageList] = j.getBoolean("FeatureSettingMap_MessageList")
                if (j.has("FeatureSettingMap_ActivityHistory"))
                    FeatureSettingsMap[FeatureSetting.ActivityHistory] = j.getBoolean("FeatureSettingMap_ActivityHistory")
                if (j.has("FeatureSettingMap_AppSettings"))
                    FeatureSettingsMap[FeatureSetting.AppSettings] = j.getBoolean("FeatureSettingMap_AppSettings")
                if (j.has("FeatureSettingMap_MultipleImages"))
                    FeatureSettingsMap[FeatureSetting.MultipleImages] = j.getBoolean("FeatureSettingMap_MultipleImages")
                if (j.has("FeatureSettingMap_OfflineMode"))
                    FeatureSettingsMap[FeatureSetting.OfflineMode] = j.getBoolean("FeatureSettingMap_OfflineMode")

                if (j.has("AuthCred_id") && j.has("AuthCred_password")) {
                    AuthCred = Pair(j.getString("AuthCred_id"), j.getString("AuthCred_password"))
                }

                if (j.has("AutoLoginCred_Test_id") && j.has("AutoLoginCred_Test_pass"))
                    AutoLoginCred_Test = Pair(j.getString("AutoLoginCred_Test_id"), j.getString("AutoLoginCred_Test_pass"))
                if (j.has("AutoLoginCred_id") && j.has("AutoLoginCred_pass"))
                    AutoLoginCred = Pair(j.getString("AutoLoginCred_id"), j.getString("AutoLoginCred_pass"))
                if (j.has("ServerUrl")) ServerUrl = j.getString("ServerUrl")
                if (j.has("OverridingApiKey")) OverridingApiKey = j.getString("OverridingApiKey")
                if (j.has("OverridingApiKey_Test")) OverridingApiKey_Test = j.getString("OverridingApiKey_Test")
                if (j.has("RegistrationPageUrl")) RegistrationPageUrl = j.getString("RegistrationPageUrl")
                if (j.has("RegistrationPageUrl_Test")) RegistrationPageUrl_Test = j.getString("RegistrationPageUrl_Test")

                if (j.has("BypassImageReq")) BypassImageReq = j.getBoolean("BypassImageReq")
                if (j.has("AutoExpandCategoryList")) AutoExpandCategoryList = j.getBoolean("AutoExpandCategoryList")
                if (j.has("AutoExpandWhenOnlyNSubcategory")) AutoExpandWhenOnlyNSubcategory = j.getInt("AutoExpandWhenOnlyNSubcategory")
                if (j.has("ShowDomainOnlyMessage")) ShowDomainOnlyMessage = j.getBoolean("ShowDomainOnlyMessage")
                if (j.has("MessageUploadOnDefaultDomainOnly")) MessageUploadOnDefaultDomainOnly = j.getBoolean("MessageUploadOnDefaultDomainOnly")
                if (j.has("OverrideAppId")) OverrideAppId = j.getString("OverrideAppId")
                if (j.has("ForceUseOverriddenAppId")) ForceUseOverriddenAppId = j.getBoolean("ForceUseOverriddenAppId")
                if (j.has("DefaultDomainId")) DefaultDomainId = j.getInt("DefaultDomainId")
                if (j.has("DefaultDomainName")) DefaultDomainName = j.getString("DefaultDomainName")
                if (j.has("UseDefaultDomainWhenPossible")) UseDefaultDomainWhenPossible = j.getBoolean("UseDefaultDomainWhenPossible")
                if (j.has("NoLocationFallbackMessage")) NoLocationFallbackMessage = j.getString("NoLocationFallbackMessage")
                if (j.has("ForceHost_Test")) ForceHost_Test = j.getBoolean("ForceHost_Test")
                if (j.has("HideDescripton")) HideDescripton = j.getBoolean("HideDescripton")
                if (j.has("ShowWelcomeMenuItem")) ShowWelcomeMenuItem = j.getBoolean("ShowWelcomeMenuItem")
                if (j.has("CategoryBeforePosition")) CategoryBeforePosition = j.getBoolean("CategoryBeforePosition")
                if (j.has("MaxImageUploadOnReportCreation")) MaxImageUploadOnReportCreation = j.getInt("MaxImageUploadOnReportCreation")
                if (j.has("ForcePositionConfirmation")) ForcePositionConfirmation = j.getBoolean("ForcePositionConfirmation")
                if (j.has("ShowWarningNotEmergencyService")) ShowWarningNotEmergencyService = j.getBoolean("ShowWarningNotEmergencyService")
                if (j.has("SkipSplashscreen")) SkipSplashscreen = j.getBoolean("SkipSplashscreen")
                if (j.has("HideAboutAppMenu")) HideAboutAppMenu = j.getBoolean("HideAboutAppMenu")
                if (j.has("EnableMessageCreationFromQRCode")) EnableMessageCreationFromQRCode = j.getBoolean("EnableMessageCreationFromQRCode")
                if (j.has("AllowedDomainHostsForBrowsableUrl")) AllowedDomainHostsForBrowsableUrl = j.getString("AllowedDomainHostsForBrowsableUrl").split("||").toTypedArray()

                if (j.has("UseStaticToS")) UseStaticToS = j.getBoolean("UseStaticToS")
                if (j.has("UseStaticPrivacyPolicy")) UseStaticPrivacyPolicy = j.getBoolean("UseStaticPrivacyPolicy")
                if (j.has("UseStaticImpressum")) UseStaticImpressum = j.getBoolean("UseStaticImpressum")
                if (j.has("UseStaticAbout")) UseStaticAbout = j.getBoolean("UseStaticAbout")

                if (j.has("SpecializedMarkerFrameSuffix"))
                    SpecializedMarkerFrameSuffix = j.getString("SpecializedMarkerFrameSuffix")
                if (j.has("DebugAPICalls"))
                    DebugAPICalls = j.getBoolean("DebugAPICalls")
                if (j.has("UseMarkerUri"))
                    SpecializedMarkerFrameSuffix = j.getString("UseMarkerUri")
                if (j.has("EnableCategorySearch"))
                    EnableCategorySearch = j.getBoolean("EnableCategorySearch")
                if (j.has("ShowAppRatingWindow"))
                    ShowAppRatingWindow = j.getBoolean("ShowAppRatingWindow")
                if (j.has("ShowAppRatingWindowAfterNMessages"))
                    ShowAppRatingWindowAfterNMessages = j.getInt("ShowAppRatingWindowAfterNMessages")
                if (j.has("EnableAddressSearch"))
                    EnableAddressSearch = j.getBoolean("EnableAddressSearch")

                if (j.has("DefaultLat") && j.has("DefaultLon"))
                    DefaultLatLon = Pair(j.getDouble("DefaultLat"), j.getDouble("DefaultLon"))
            }
            bConfigLoaded = true
        }
    }

    /**
     * Check if config is loaded
     */
    fun configIsLoaded(): Boolean {
        return bConfigLoaded
    }

    private var mDomainConfigText: JSONObject? = null
    private fun doLoadDomainConfig(c: Context) {
        if (mDomainConfigText == null) {
            val text = ResourceProxy.readFromAssets(c, "data/domain-package.json")
            mDomainConfigText = try { JSONObject(text) } catch (e: Exception) { null }
        }
    }

    fun getStaticDomainId(c: Context): Int {
        doLoadDomainConfig(c)
        if (mDomainConfigText == null) return 0
        val domain = mDomainConfigText?.optJSONObject(c.packageName)
        if (domain == null) return 0
        return domain.optInt("domainid", 0)
    }
}