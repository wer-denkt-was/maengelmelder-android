package de.maengelmelder.mainmodule.objects

import java.io.Serializable

class MessageImageUri : Serializable {

    var isPublic: Boolean = true
    var thumbnailUri: String = "" // Use res400
    var originalUri: String = "" // Use actual url / res800
    var contentType: String = ""

}