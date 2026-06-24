package de.maengelmelder.mainmodule.objects

import de.maengelmelder.mainmodule.objects.interfaces.IdGenerable
import org.json.JSONArray

class Geometry: IdGenerable {

    companion object {
        fun fromJSONArray(array: JSONArray, sysId: Int, domId: Int): List<Geometry> {
            val list = arrayListOf<Geometry>()
            (0 until array.length()).forEach { idx ->
                array.getJSONObject(idx)?.let { item ->
                    val geomid = item.optInt("id")
                    item.optJSONArray("parts")?.let { partsJsonArr ->
                        (0 until partsJsonArr.length()).forEach { idx2 ->
                            partsJsonArr.optJSONObject(idx2)?.let { part ->
                                val g = Geometry().apply {
                                    partId = part.optInt("id")
                                    wktString = part.optString("wkt")
                                    id = geomid
                                    systemId = sysId
                                    domainId = domId
                                }
                                list.add(g)
                            }
                        }
                    }
                }
            }
            return list
        }
    }

    var systemId: Int = 0
    var domainId: Int = 0
    var id: Int = 0
    var partId: Int = 0
    var wktString: String = ""
    var categoryIds: List<String> = listOf()
    var minLat: Double = 0.0
    var maxLat: Double = 0.0
    var minLon: Double = 0.0
    var maxLon: Double = 0.0

    val categories: ArrayList<Category> = arrayListOf()

    override fun generateId(): String {
        return "$systemId-$domainId-$id"
    }

}