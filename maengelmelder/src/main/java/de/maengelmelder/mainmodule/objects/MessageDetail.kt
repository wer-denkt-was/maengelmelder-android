package de.maengelmelder.mainmodule.objects

import de.maengelmelder.mainmodule.MMConstants
import org.json.JSONObject
import java.io.Serializable
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap

/**
 * Message detail object
 */
class MessageDetail : Serializable {
    companion object {
        val DT_PARSER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        fun fromJSON(j: JSONObject, domId: String? = null): MessageDetail {
            return MessageDetail().apply {
                j.optJSONObject("data")?.let { data ->
                    id = data.optString("id")
                    data.optJSONObject("domain")?.let { dom ->
                        domainName = dom.optString("title")
                        domainId = dom.optString("id", domId?: MMConstants.DefaultDomainId.toString())
                    }
                    data.optJSONArray("history")?.let { history ->
                        val historyList = arrayListOf<MessageHistory>()
                        (0 until history.length()).forEach { i ->
                            val historyItem = history.getJSONObject(i)
                            val historyObj = MessageHistory().apply {
                                created = try {
                                    DT_PARSER.parse(historyItem.optString("created"))
                                } catch (e: Exception) {
                                    null
                                }
                                id = historyItem.optLong("id")
                                text = historyItem.optString("text")
                                manualText = historyItem.optString("manual_text")
                            }

                            historyItem.optJSONObject("owner")?.let { owner ->
                                historyObj.ownerName = owner.optString("public_name")
                                historyObj.ownerAvatarUrl = owner.optString("avatar_uri")
                            }

                            historyList.add(historyObj)
                        }
                        details = historyList
                    }
                    data.optJSONArray("attachments")?.let { atch ->
                        if (atch.length() > 0) {
                            val imagesList = arrayListOf<MessageImageUri>()
                            (0 until atch.length()).forEach { i ->
                                val item = atch.optJSONObject(i)
                                imagesList.add(MessageImageUri().apply {
                                    isPublic = item.optInt("public", 0) == 1
                                    thumbnailUri = item.optJSONObject("thumbnails")?.optString("w256", "")?: ""
                                    originalUri = item.optJSONObject("thumbnails")?.optString("w800", "")?: ""
                                    contentType = item.optString("content_type", "")
                                })
                            }
                            images = imagesList
                        }
                    }
                    createdAt = try {
                        DT_PARSER.parse(data.optString("created"))
                    } catch (e: Exception) {
                        null
                    }

                    description = data.optString("text")
                    lat = data.optDouble("lat")
                    lon = data.optDouble("lon")
                    address = data.optString("address")
                    allowComment = data.optBoolean("allow_comment", false)
                    state = data.optString("state_german", "")
                    state_en = data.optString("state_en", "")
                    colorString = data.optString("marker_color", "white")
                    domainId = domId ?: ""
                }
            }
        }
    }

    var id: String? = null
    var lat = Double.MAX_VALUE
    var lon = Double.MIN_VALUE
    var address = ""
    var domainId = ""
    var domainName = ""
    var description: String = ""
    var images: List<MessageImageUri> = listOf()
    var createdAt: Date? = null
    var allowComment: Boolean = false
    var details: List<MessageHistory> = listOf()

    var state = ""
    var state_en = ""
    var colorString = "white"

    override fun toString(): String {
        return "Message-[ID:$id][loc:$lat,$lon]"
    }
}