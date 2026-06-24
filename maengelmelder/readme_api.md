## API Request classes

### Preparation

Before using any API Request classes, you need to set up the module parameters.
```kotlin
MMConstants.apply {
  // version name of your application
  APP_VERSION = packageManager.getPackageInfo(packageName, 0).versionName
  // Appid and domain id. They both have to be the same
  OverrideAppId = "<appid>"
  DefaultDomainId = <domainid>
  UseDefaultDomainWhenPossible = true
}
MMConstants.save(applicationContext)
```

### Authentication

Each API request (except for login) can be authenticated using Bearer token. The bearer token is obtained from [network.coroutines.v1.MMv1Login]

### Location of classes

The API request classes are located in package `network.coroutines` and `network.collectives.coroutine`.
The classes can be safely called in main thread since it creates its own coroutine.

The objects are located in `objects` package.

### Log in

```kotlin
MMv1Login(context, username, password).apply {
    checkOriginalSystemOnly = true
    listener = (object: MMBMS.BMSListener<UserCred, BaseResponse> {
        override fun onData(data: UserCred) {
            // data.token -> The bearer token that can be used to authenticate other requests
            // To save it for all requests, call [UserData.saveUserCred(context, data)]
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    })
}.execute()
```

### Log out
```kotlin
MMv1Logout(context).apply {
    listener = (object : MMBMS.BMSListener<BaseResponse, BaseResponse> {
        override fun onData(data: BaseResponse) {
            // To remove it from all future requests, call [UserData.removeUserCred(context)]
        }
        override fun onFail(err: BaseResponse) {
            // Failed? You can always remove the saved credentials either way
        }
    })
}.execute()
```

### Querying or checking Domain 

```kotlin
// lat = latitude (double), lon = longitude (double)
MMv1Domain(context, lat, lon).apply {
    checkOriginalSystemOnly = true
    listener = (object: MMBMS.BMSListener<List<Domain>, BaseResponse> {
        override fun onData(data: List<Domain>) {
            // List<Domain> can be used to check if the domain bearing your domain id exists in the given location
            // If it exists, it means that you can create a message there
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    })
}.execute()
```

### Retrieving the list of messages in a given location

```kotlin
// bottomLat, bottomLon, topLat, topLon are the boundary points of the viewable map
MMv1Message(context, bottomLat, bottomLon, topLat, topLon, zoomLevel, domainid).apply {
    listener = (object : MMBMS.BMSListener<List<Message>, BaseResponse> {
        override fun onData(data: List<Message>) {
            // List of messages
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    }) 
}.execute()
```

### Retrieving message detail

```kotlin
/** 
 * Endpoint is usually used to query detailed properties of an existing message object
 * message is of Message object
 * Third parameter is used whether the API call should also append device's unique ID in the query parameter
 * **/
MMv1MessageDetail(context, message, true).apply {
    listener = (object : MMBMS.BMSListener<MessageDetail, BaseResponse> {
        override fun onData(data: MessageDetail?) {
            // message detail
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    })
}.execute()
```

### Retrieving the list of category
```kotlin
MMv1Categories(context, domainid).apply {
    listener = (object : MMBMS.BMSListener<Domain, BaseResponse> {
        override fun onData(data: Domain?) {
            // domain.categoriesAsArray() --> list of categories
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    })
}.execute()
```

### Retrieving a category
```kotlin
MMv1Category(context, domainid, categoryid).apply {
    listener = (object : MMBMS.BMSListener<Category, BaseResponse> {
        override fun onData(data: Category?) {
            // data is of Category object, complete with its attributes
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    })
}.execute()
```

### Checking possible duplicates of a message based on category and lat-lon
```kotlin
/**
 * category is Category object
 * lat and lon are latitude and longitude (double)
**/
MMv1Duplicates(context, category, lat, lon).apply {
    listener = (object : MMBMS.BMSListener<List<Message>, BaseResponse> {
        override fun onData(data: List<Message>?) {
            // the list of possible duplicate messages
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    })
}.execute()
```

### Creating a bundle for hosting images for soon-to-be message

When uploading multiple images for a message, we use a bundle to host the images first before uploading the message.
The bundle ID will then be added when uploading the message so the server can find the images for this message.

```kotlin
MMv1CreateBundle(context, domainid).apply {
    listener = (object : MMBMS.BMSListener<String, BaseResponse> {
        override fun onData(data: String?) {
            // data is the bundle id used for uploading images to the bundle. Will also be needed when uploading message
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    })
}
```

### Uploading an image to a file bundle for soon-to-be message
```kotlin
/**
 * context = Context object
 * bundle  = Bundle Id, retrieved from `MMv1CreateBundle` call
 * filename = the filename
 * img = Bitmap object containing the image
 */
MMv1UploadFileToBundle(context, domainid, bundle, filename, img).apply {
    listener = (object : MMBMS.BMSListener<List<UploadedFileInfo>, BaseResponse> {
        override fun onData(data: List<UploadedFileInfo>?) {
            // Returns the list of uploaded files in this bundle, including the image uploaded with this call
            // The list has to be added when uploading message as well to ensure file integrity
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    })
}
```
### Uploading message

API Class for uploading message is [de.maengelmelder.mainmodule.network.coroutines.v1.MMv1SendMessage]
```kotlin
/**
 * context = Context object
 * messageBuilder = [objects.MessageBuilder] object. See `Using Message and MessageBuilder` object for more information
 * startTS = Timestamp in ms when the message uploading process is started (Simply from [System.currentTimeMillis])
 * bundleid = bundle id of previously uploaded messages (from [MMv1CreateBundle]). Remove if no images are uploaded
 * filenames = list of filenames (array of string) of the uploaded images to the bundle. Remove if no images are uploaded
 */
MMv1SendMessage(context, messageBuilder, startTS, bundleID, filenames).apply {
    listener = (object : MMBMS.BMSListener<CreateMessageResponse, BaseResponse> {
        override fun onData(data: CreateMessageResponse?) {
            // Contains the newly created message (message id, etc.)
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    })
}
```

### Adding comment to an existing message

```kotlin
/**
 * context (Required) = Context object
 * domainid (Required) = domain id
 * messageid (Required) = message id
 * comment (Required) = comment text
 * pathToImage (Optional) = absolute path to image file if you want to attach image to the comment
 * solved (Optional) = Whether the comment marks the message as solved or not (default = false)
 * attributeValues (Optional) = Additional attribute values . The attributes can be queried from [MMv1Category].
 */
MMv1UpdateMessage(context, domainid, messageid, comment, pathToImage, solved, attributeValues).apply {
    listener = (object : MMBMS.BMSListener<MessageUpdateResponse, BaseResponse> {
        override fun onData(data: MessageUpdateResponse?) {
            // Comment is successfully posted
        }
        override fun onFail(err: BaseResponse) {
            // Failed
        }
    })
}
```

### Using [Message] and [MessageBuilder]

[Message] and [MessageBuilder] are an easy way to construct message objects that are ready for upload. 
[MessageBuilder] is first constructed and [Message] object can be built from it.
```kotlin
/**
 * Creates a new Message builder (You can optionally give existing [Message] object as parameter)
 * Call this when you first create a new message. An auto-generated message-id ([MessageBuilder.messageId]) will be provided as local message identifier
 */
val builder = MessageBuilder()

// Set location (latitude, longitude)
builder.setLocation(lat, lon)

// Add category object [type of objects.Category]
builder.category = myChosenCategory

// Add image path (Should be absolute path to the image)
builder.addImagePath("absolute/path/to/firstimage.png")
builder.addImagePath("absolute/path/to/secondimage.png")

// Add title and description
builder.title = "Message title"
builder.description = "Message description"

// Add attribute values
builder.addAttributeValue("<attribute-id>", "the value")
builder.addAttributeValue("<boolean-attribute-id>", true)
builder.addAttributeValue("<multiple-attribute-id>", ["val1", "val2", "val3"])

/**
 * Add attribute values from a json array ([JSONArray] object)
 * Example JSON construct
 *  [ 
 *      { "id": 1, "value": "string value" }, 
 *      { "id": 2, "value": false }, 
 *      { "id": 3, "value": ["value1", "value2", ...] }, 
 *      ... 
 *  ]
 */
builder.attributeValuesFromJson(jsonArray)

// Add additional data (Optional. This will be appended to the API call in the request body)
builder.addAdditionalData(key, value)

// Check if entries (description, images, attributes) are valid before uploading 
builder.isDescriptionValid() // Returns true if valid
builder.isCategoryValid()
builder.isLocationValid()
builder.areAttributeValuesFilled()

// Get the message object (Can be used for upload purpose)
val message = builder.message
```

