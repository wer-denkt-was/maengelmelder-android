package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Message
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MMv1Duplicates(c: Context, category: Category, lat: Double, lon: Double) :
    MMNetworkRepository<List<Message>, BaseResponse> (
            c,
            if (MMConstants.UseDefaultDomainWhenPossible && category.domainId.isEmpty())
                MMConstants.DefaultDomainId
            else category.domainId.toInt(),
            "bms/duplicates") {


    private val mDtFromatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    private val mLat = lat
    private val mLon = lon
    private val mCat = category

    init {
        val body = JSONObject().apply {
            put("typeid", mCat.typeId)
            put("lat", mLat)
            put("lon", mLon)
        }
        setJsonBody(body.toString())
    }

    override fun getQueryParameters(): Map<String, String>? = mapOf(
            Pair("fieldset", "mmv2")
    )

    override fun parseResponse(resp: BaseResponse): List<Message> {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        val msgs = arrayListOf<Message>()

        json?.run {
            optJSONObject("data")?.let { data ->
                data.optJSONArray("duplicates")?.let { duplicates ->
                    (0 until duplicates.length()).forEach { i ->
                        duplicates.optJSONObject(i)?.let { m ->
                            val msg = Message()
                            msg.id = m.optString("id")?: ""
                            msg.serverId = msg.id
                            msg.title = m.optString("title")
                            msg.desc = m.optString("text")
                            msg.state = m.optString("state_german")
                            msg.state_en = m.optString("state")
                            msg.markerUrl = m.optString("marker_uri", "")

                            val dtString = m.optString("created")
                            msg.createdAt = try { mDtFromatter.parse(dtString).time } catch (e: Exception) { -1 }

                            m.optJSONObject("message_type")?.let { mtype ->
                                val cat = Category().apply {
                                    typeId = mtype.getLong("id")
                                    name = mtype.getString("name")
                                    domainText = mtype.getString("description")
                                    markerId = m.getInt("marker_id").toString()
                                    systemId = mCat.systemId
                                    domainId = mCat.domainId
                                }
                                msg.category = cat
                            }

                            msg.colorString = m.optString("marker_color", "white")
                            msg.lat = mLat
                            msg.lon = mLon
                            msg.systemId = msg.category.systemId
                            msg.imagePaths.add(m.optString("thumbnail_sq64", ""))

                            msgs.add(msg)
                        }
                    }
                }

            }
        }

        return msgs
    }

    override fun parseError(resp: BaseResponse): BaseResponse = resp
}