## MMConstants

| Property                           | Required | Value           | Info                                                                                                                                                                      |
|------------------------------------|----------|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| APP_VERSION                        | yes      | String          | Your app's package name (e.g. com.example.myapp)                                                                                                                          |
| ServerUrl                          | yes      | String          | Base URL for API calls. Has no default value and must be set by the application                                                                                          |
| AuthCred                           | no       | Pair of String  | Basic authentication (username, password) for every API call (Default is null). Leave this to null for production unless stated otherwise                                 |
| OverrideAppId                      | yes      | String          | AppId                                                                                                                                                                     |
| DefaultDomainId                    | yes      | Int             | Domain Id. Same as AppId, but in integer                                                                                                                                  |
| UseDefaultDomainWhenPossible       | yes      | Boolean         | Has to be set to true so it doesn't show other domains' data                                                                                                              |
| MessageUploadOnDefaultDomainOnly   | yes      | Boolean         | Has to be set to true so it doesn't accidentally upload a message to another domain                                                                                       |
| ForceUseOverriddenAppId            | yes      | Boolean         | Has to be set to true so it forces the application to only show its domain's categories and attributes                                                                    |
| DefaultLatLon                      | yes      | Pair of Doubles | (Longitude,Latitude) of the default location for map                                                                                                                      |
| DefaultValuesMap                   | no       | HashMap         | If filled, it will set default email/firstname/lastname for creating messages. See `DefaultValuesMap` for more information                                                |
| UseStaticPrivacyPolicy             | no       | Boolean         | Default is false. if set to true, privacy policy from assets folder will be used when displaying it. See `Asset Files` for more information                               |
| UseStaticTos                       | no       | Boolean         | Default is false. if set to true, ToS from assets folder will be used when displaying it. See `Asset Files` for more information                                          |
| UseStaticImpressum                 | no       | Boolean         | Default is false. if set to true, Impressum from assets folder will be used when displaying it. See `Asset Files` for more information                                    |
| UseStaticAbout                     | no       | Boolean         | Default is false. if set to true, About-App section from assets folder will be used when displaying it. See `Asset Files` for more information                            |
| CategoryBeforePosition             | no       | Boolean         | Default is false. If set to true, message creation will ask for category first before location                                                                            |
| ExternalURLsMap                    | no       | HashMap         | List of external URLs for pages such as Privacy policy, terms, etc. Not needed if static files are used. See `ExternalURLsMap` for more information                       |
| FeatureSettingsMap                 | no       | HashMap         | List of additional features in Mängelmelder module. See `FeatureSettingsMap` for more information                                                                         |
| SpecializedMarkerFrameSuffix       | no       | String          | Default is empty. Suffix for using specialized frame image for markers.                                                                                                   |
| DebugAPICalls                      | no       | Boolean         | Default is false. If true, it will print debug messages for API calls                                                                                                     |
| ShowWarningNotEmergencyService     | no       | Boolean         | Default is false. If true, it will show a warning message to not use the App for emergency service. Only once per installation.                                           |
| MaxImageUploadOnReportCreation     | no       | Int             | Default is 3. Set the maximum amount of images that can be uploaded in 1 message. Min 1, Max 5                                                                            |
| AutoExpandCategoryList             | no       | Boolean         | Default is true. If set to true, category list will be automatically expanded if they are grouped                                                                         |
| UseMarkerUri                       | no       | Boolean         | Default is false. If set to true, overview map will use marker URLs from the message object. More about it in `UseMarkerUri` section                                      |
| SkipSplashscreen                   | no       | Boolean         | Default is false. If set to true, Splashscreen will be skipped (terms and privacy will still be checked and shown if not agreed in the first)                             |
| HideAboutAppMenu                   | no       | Boolean         | Default is false. If set to true, "About App"-Menu item will not be shown                                                                                                 |
| ShowAppRating                      | no       | Boolean         | Default is true. If set to true, a window will be shown to user after N messages are uploaded where user can navigate to the app store page to rate the app.              |
| ShowAppRatingWindowAfterNMessages  | no       | Int             | Default is 2. Number of minimum messages to be uploaded before the app rating window is shown                                                                             |
| EnableAddressSearch                | no       | Boolean         | Default is false. If set to true, address search field will be shown in location step. User can use it to search for address                                              |
| EnableCategorySearch               | no       | Boolean         | Default is false. If set to true, category search field will be shown in category step. User can use it to search for specific category                                   |
| EnableMessageCreationFromQRCode    | no       | Boolean         | Default is false. If set to true, enables using QR code to create a message (either from camera or from App)                                                              |
| AllowedDomainHostsForBrowsableUrl  | no       | String Array    | Only applicable if `EnableMessageCreationFromQRCode` is enabled. App will only create message out of URL with the listed hostname. See `Deeplinking` for more information |

### Regarding `ServerUrl` and `AuthCred`

`ServerUrl` must be set to your backend's base URL; the library does not ship with a default. `AuthCred` is optional and should be left as `null` unless your backend requires basic authentication on every API call.

### Property `DefaultValuesMap`
```kotlin
DefaultValuesMap.apply {
    put(MMConstants.DefaultValues.FormEmail, "sample@email.com")
    put(MMConstants.DefaultValues.FormFirstName, "Vorname")
    put(MMConstants.DefaultValues.FormLastName, "Nachname")
}
```

### Property `ExternalURLsMap`
```kotlin
ExternalURLsMap.apply {
    put(MMConstants.ExternalURL.AboutApp, "<about app url>")
    put(MMConstants.ExternalURL.Usage, "<terms of service url>")
    put(MMConstants.ExternalURL.Impressum, "<impressum url>")
    put(MMConstants.ExternalURL.DataProtection, "<privacy policy url>")
}
```
By default, external URLs (http/https) are opened in browser. If you wish to open it inside the app's webview, add the prefix `wdw-wv://` before the URL
Example: 
```kotlin
ExternalURLsMap.apply {
    put(MMConstants.ExternalURL.Usage, "wdw-wv://https://mängelmelder.de/page/nutzungsbedingungen")
    ...
}
```

### Property `FeatureSettingsMap`
```kotlin
FeatureSettingsMap.apply {
    // If enabled, user will be able to log in with their Mängelmelder credentials to the app
    put(MMConstants.FeatureSetting.UserLogin, false)
    // If enabled, user can view their activity history
    put(MMConstants.FeatureSetting.ActivityHistory, false)
    // If enabled, user can view messages in list mode in addition with map mode
    put(MMConstants.FeatureSetting.MessageList, true)
    // If enabled, user can upload multiple images in 1 message
    put(MMConstants.FeatureSetting.MultipleImages, true)
    // If enabled, user can view and edit user settings for the module
    put(MMConstants.FeatureSetting.AppSettings, false)
    // If enabled, user can use the app without internet connection. Restriction applies. See "Offline Mode" for more information
    put(MMConstants.FeatureSetting.OfflineMode, true)
}
```
### UseMarkerUri
When it is enabled:
* Showing marker on map will be a bit slower than usual due to underlying network requests (especially when you zoom out far)
* Marker highlighting will not work
* The marker images may flash for a bit after moving the map due to the time required for downloading and rendering the image

### Asset files
Static files such as privacy policy, impressum, etc. are placed in `src/main/assets/mm-html`, overriding the default ones in the library.
* `aboutapp.html`: The "About App" page
* `intro.html`: The page that is shown only first time after installation when Mängelmelder module is started
* `policy.html`: Privacy policy page (*Datenschutzbedingungen*)
* `terms.html`: Terms of Usage page (*Nutzungsbedingungen*)

### Geojson layers
Geojson layers can be displayed in Overview map, message detail map, and the map in Position-Step of message creation by putting the geojson files (.geojson) in `assets/mm-geojson`.
Additionally, you need to create a settings.json inside the same folder to define which geojson files that can be loaded. The format is as follow:
```json
{
  "layers": [
    {
      "name": "Name of the layer", 
      "visible": true,
      "file": "mm-geojson/layer1.geojson",
      "line_width": 2,
      "line_color": "#FF0000",
      "fill_color": "green"
    },
    {
      "name": "Name of the second layer",
      "visible": false,
      "file": "mm-geojson/layer2.geojson",
      "line_width": 5,
      "line_color": "black",
      "fill_color": "#aad3d3d3"
    },
    ...
  ]
}
```
Note that the bigger the geojson files are, the longer it takes for the map to load the layers.
`visible`-Property is only applied the first time. It's overridden by user preferences

### Custom fonts

* Add the font files in `assets/fonts`. (E.g. `assets/fonts/mycustomfont.ttf`)
* Add reference to the font in your `strings.xml` or `settings.xml`
```xml
<resources>
    ...
    <!-- normal font -->
    <string name="mm_custom_font_normal_path">fonts/mycustomfont.ttf</string>
    <!-- bold font -->
    <string name="mm_custom_font_bold_path">fonts/mycustomfont-bold.ttf</string>
    ...
</resources>
```

### Offline Mode
When offline mode feature is active, users are able to use the app and create messages without internet connection. Several restrictions apply:
* Google Map will only show parts of the map that the users have visited before
* Users can only select default categories for normal Mängelmelder App. These categories may not be applicable to the given position where the message is created
* Message upload may fail if the selected category is not valid for the area where the message is positioned during the time with no internet connection
* Users can then re-select the correct category and fill the missing attributes for the message when they have internet connection

### Deeplinking
The App is able to make use of deep linking to create a message from the given QR code or URL
* Enable QR code scanning `EnableMessageCreationFromQRCode` (this include clicking URL as well)
* Put allowed hostname inside `AllowedDomainHostsForBrowsableUrl`
* Set up your deep-linking Activity following Google Android's guide
  * https://developer.android.com/training/app-links/create-deeplinks
* In your deep-linking activity, call the following method inside the `onCreate()`
```
/*
    This basically calls the library's DeepLinkDetectActivity to validate the URL and the given parameters
    Then it starts the activity when the URL is valid
    Validation includes:
    1. Hostname
    2. valid lat-lon and category id parameter
*/

// Load Mängelmelder application config first
// AppConfig is a class from Mängelmelder library
AppConfig.load(applicationContext)
MMConstants.save(applicationContext)

// Read query parameter
val data: Uri? = intent.data

// Start validating URL and creating message processing activity
startActivity(Intent(this, DeepLinkDetectActivity::class.java).apply {
    DeepLinkDetectActivity.deepLinkExtras(this, data?.query?: "")
})
```
* Additionally, you can add query parameters to your url so it can autofill several message parameters such as:
  * `lat` and `lon` (DOUBLE). If given, the message's location will be set to the given location. Default is 0.0 for both
  * `force_loc` (INTEGER. 1|0). If 1, the message's location cannot be changed anymore. Default is 0
    * `force_loc` does not apply when `lat` and `lon` are not given
  * `selected_typeid` (INTEGER). Category Id. If given, the message's category is set to the given ID
  * `force_typeid` (INTEGER). Same as `selected_typeid`, but user cannot change it anymore
  * `selected_attributeXXX` (ANY). `XXX` is an attribute ID. If given, it will prefill the attribute with the given value
  * `force_attributeXXX` (ANY). `XXX` is an attribute ID. Same as `selected_attributeXXX`, but user will not be able to change the value
    * if the attribute is a checkbox, put `1` (checked) or `0` (unchecked)
    * if the attribute is a dropdown, use the value of the dropdown instead of the displayed text
    * when you have both `force_attributeXXX` and `selected_attributeXXX` with same ID, the forced attribute is prioritized
* Example: `https://<host>/<path>/?lat=1&lon=1&force_loc=1&selected_typeid=25&selected_attribute12=Test&force_attribute14=200`
  * This will create a message:
    * with lat-lon coordinate 1,1 and unchangeable
    * with category ID 25, but still changeable
    * with attribute 12 filled with value Test
    * with attribute 14 filled with value 200 and unchangeable
* When giving forced value for location/category, the relevant step is shown as locked and cannot be opened. 
  * The user can still view the chosen location/category in the last step.
* checkbox attribute that is mandatory but whose default value is unticked (false) will ignore the `force_attribute` parameter. It will always be editable