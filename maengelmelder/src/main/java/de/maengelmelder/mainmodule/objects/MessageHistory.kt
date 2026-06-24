package de.maengelmelder.mainmodule.objects

import java.io.Serializable
import java.util.*
import kotlin.collections.LinkedHashMap

/**
 * Message history object
 */
class MessageHistory : Serializable {
    var id: Long = 0
    var created: Date? = null
    var ownerName: String = ""
    var ownerAvatarUrl: String = ""
    var manualText: String = ""
    var text: String = ""
}