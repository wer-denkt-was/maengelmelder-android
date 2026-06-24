# About the Library

This contains the complete Mängelmelder App that you can incorporate to your Main application

## Building .AAR artifact

To build the AAR artifact, execute these commands in the library root folder
```
./gradlew clean
./gradlew assemble
```
Or if you have added the library as module in your project
```
# in your project's root folder
./gradlew maengelmelder:clean
./gradlew maengelmelder:assemble
```
The .AAR artifact will be located in `build/outputs` folder. It can then be included as a library in another Android project.

## Using the library in an android gradle project

### Adding the library or .AAR artifact to an android project

There are 2 ways to include the library into your application project: through AAR artifact of by adding the library as module.

For .AAR Artifact:
* Copy the .AAR artifact to the android project (e.g. in `src/main/libs`)
* Add the .AAR artifact to the project
  * Android Studio > File > Project Structure > Dependencies
  * Click `+` under `All Dependencies`, then pick `JAR/AAR dependency`
  * Put the path to the .AAR artifact
  * Click `OK`
  * Sync gradle project

For library project as module:
* Android Studio > File > New > Import Module
* In `Source directory`, add the path to the library project
* Click `Finish`
* Gradle will automatically sync the project
* The project might create a new `settings.gradle` file in the root folder if you are using Kotlin gradle. Simply delete this file and add the reference to the library in `settings.gradle.kts`
  * `include(":app", ":maengelmelder")`
* In your project's `build.gradle`, 
  * set the version of `org.jetbrains.kotlin.android` to v1.9.21
  * Set gradle build tools version (`com.android.tools.build:gradle`) to v8.9.3
* In your app's `build.gradle`, 
  * add dependency `implementation project(":maengelmelder")`

### Setting up the application project

The .AAR artifact does not include any transitive dependencies. Therefore, we need to add the dependencies to the main project.
This does not apply to library project, as the module itself is imported directly to the project files along with the transitive dependencies

Requirements:
- SDK 35 or higher on your application's settings
- Java 17
- Gradle 8.6.0+
- Gradle Wrapper `gradle-8.11.1-all`

In your root's `settings.gradle`, add this for your buildscript (Applies to both AAR artifact and module import)
```kotlin
dependencyResolutionManagement { 
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://jzaccone.github.io/SlidingMenu-aar")
  }
}
```

In your root's `gradle.properties`, add `android.enableJetifier = true` to allow legacy non-X libraries to be used. (Applies to both AAR artifact and module import)

In your app's `build.gradle`, add these dependencies (Only applies to AAR artifact)
```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    // Androidx's compose UI library might interfere with MM's UI library so we comment it out
    // implementation("androidx.compose.ui:ui")
    // implementation("androidx.compose.ui:ui-graphics")
    // implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    // androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    // debugImplementation("androidx.compose.ui:ui-tooling")
    // debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Required external libraries (Only if you import the AAR artifact and not the whole library as module)
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.google.android.gms:play-services-maps:18.2.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ('com.google.maps.android:android-maps-utils:3.8.0')
    implementation ("com.jeremyfeinstein.slidingmenu:library:1.3@aar")
    implementation ("io.github.inflationx:calligraphy3:3.1.1")
    implementation ("io.github.inflationx:viewpump:2.0.3")
    implementation ("com.github.JakeWharton:ViewPagerIndicator:2.4.1")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("com.jsibbold:zoomage:1.3.1")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.github.amlcurran.showcaseview:library:5.4.3")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.preference:preference-ktx:1.2.1")
    implementation ("androidx.appcompat:appcompat:1.7.1")
    implementation ("androidx.exifinterface:exifinterface:1.3.7")
    implementation ("com.squareup.picasso:picasso:2.71828")
    // This should be added automatically when you add the AAR artifact as library
    implementation (files("src/main/libs/maengelmelder-release.aar"))
}
```
Also, if you are adding the module as library (Non-AAR), add this to your App's `build.gradle`
```groovy
android {
  ...
  buildFeatures {
    ...
    buildConfig true
    viewBinding true
  }
  
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_17
    targetCompatibility JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = '17'
  }
  ...
}
```

In your root's `build.gradle.kts`, add this for your buildscript (Only applies to AAR artifact)
```kotlin
buildscript {
    repositories {
        flatDir {
            dirs("src/main/libs")
        }
        mavenCentral()
    }
}
```

Then, sync gradle, clean project, and build it

INFO: After replacing AAR-file with the newest one, simply Sync project with gradle files and rebuild the project.

## Customizing your Mängelmelder library

The library comes with plenty of customization options, some are mandatory for it to functions correctly

### Starting the library's Mängelmelder activity

To start the Mängelmelder activity, you need to set up some variables first before launching it
```kotlin
MMConstants.apply {
  // version name of your application
  APP_VERSION = packageManager.getPackageInfo(packageName, 0).versionName
  // Appid and domain id. They both have to be the same
  OverrideAppId = "<appid>"
  DefaultDomainId = <domainid>
  MessageUploadOnDefaultDomainOnly = true
  UseDefaultDomainWhenPossible = true
  // Default latitude-longitude for map
  DefaultLatLon = Pair(10.894446, 48.366512)
  // Optional. Can be skipped
  DefaultValuesMap.apply {
    put(MMConstants.DefaultValues.FormEmail, "sample@email.com")
    put(MMConstants.DefaultValues.FormFirstName, "Vorname")
    put(MMConstants.DefaultValues.FormLastName, "Nachname")
  }
  // Default feature settings
  FeatureSettingsMap[MMConstants.FeatureSetting.ActivityHistory] = false
  FeatureSettingsMap[MMConstants.FeatureSetting.OfflineMode] = false
  FeatureSettingsMap[MMConstants.FeatureSetting.MultipleImages] = true
  FeatureSettingsMap[MMConstants.FeatureSetting.UserLogin] = false
  
  // If enabled, it will use HTML-files located in your asset folder to load text for privacy policy, etc.
  UseStaticToS = true
  UseStaticPrivacyPolicy = true
  UseStaticImpressum = true
  UseStaticAbout = true
}
// Save it all
MMConstants.save(applicationContext)
// If the module is started by an activity from app-project and you wish to go back to that activity after quitting Mängelmelder (instead of quitting the App), add this before starting
// this::class.java -> it will go back to the original caller of the Mängelmelder activity
MMInitiator.setOriginActivity(this::class.java)
// Start the activity
MMInitiator(this).setConfig(MMInitiator.Config.APP_TITLE, "Sample App with MM Library").start()
```

In your Application class, add this. (Don't forget to assign the application class in AndroidManifest.xml)
```kotlin
class ApplicationClass : Application(), DefaultLifecycleObserver {

  override fun onCreate() {
    super<Application>.onCreate()
    ....
    MMInitiator.init(this)
  }

  override fun onDestroy(owner: LifecycleOwner) {
    MMInitiator.triggerAppStopped(this)
    super.onDestroy(owner)
  }

  override fun onPause(owner: LifecycleOwner) {
    super.onPause(owner)
    MMInitiator.triggerAppStopped(this)
  }
}
```
Complete list of settings can be found [here](readme_settings.md)

### Styling the Mängelmelder module

The module also comes with various styling parameters such as colors, banners, etc. that can be customized from the main App. See [Styling guide](readme_styling.md) for more information

### Using Request classes

The module also exposes useful API Request classes when you don't want to use the activities in the module.
Click [here](readme_api.md) for more information.

## Troubleshooting and FAQ

### Map is greyed out

Mängelmelder module uses Google Map and it requires Google Map api key in order to work. 
For current setup, you need to add the google map api key to `~/.gradle/gradle.properties` file
```
default_google_map_apikey=<apikey>
```
Another solution is to override the google map api key from your app's `AndroidManifest.xml`. Add this in your `<application>`-Tag
```xml
<meta-data
    tools:replace="android:value"
    android:name="com.google.android.geo.API_KEY"
    android:value="<apikey>"/>
```

### Debugging API Requests

Debugging for API Requests can be done by setting [MMConstants.DebugAPICalls] to true during setup.

### Images from camera cannot be loaded

This may happen due to the App container missing `FileProvider` definition in the AndroidManifest.xml. 
The FileProvider is required so that the camera is able to store the taken image in the designated location defined in FileProvider and reference it later
Simply add this in your App-level's AndroidManifest.xml
```xml
<application>
  ...
  <provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
      android:name="android.support.FILE_PROVIDER_PATHS"
      android:resource="@xml/provider_paths"/>
  </provider>
  ...
</application>
```
`${applicationId}` is your app's package name.

Content of `@xml/provider_paths` (`res/xml/provider_paths.xml`) is as follow:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
  <files-path name="images" path="images/" />
</paths>
```