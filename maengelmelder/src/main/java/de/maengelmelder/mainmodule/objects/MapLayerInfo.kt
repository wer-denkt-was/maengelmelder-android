package de.maengelmelder.mainmodule.objects

import android.content.Context
import android.graphics.Color
import com.google.maps.android.data.geojson.GeoJsonLayer
import de.maengelmelder.mainmodule.utils.ResourceProxy
import org.json.JSONObject

class MapLayerInfo(
    val name: String,
    val assetFilepath: String,
    val initialVisibility: Boolean,
    val lineWidth: Int = 3,
    val lineColor: Int = Color.parseColor("black"),
    val fillColor: Int = Color.TRANSPARENT
) {
    companion object {
        fun fromJSON(jsonObj: JSONObject): MapLayerInfo {
            return MapLayerInfo(
                name = jsonObj.optString("name", ""),
                initialVisibility = jsonObj.optBoolean("visible", true),
                assetFilepath = jsonObj.optString("file", ""),
                lineWidth = jsonObj.optInt("line_width", 3),
                lineColor = Color.parseColor(jsonObj.optString("line_color", "black")),
                fillColor = if (jsonObj.has("fill_color")) Color.parseColor(jsonObj.optString("fill_color")) else Color.TRANSPARENT
            )
        }
    }

    private var mGeoJsonLayer: GeoJsonLayer? = null
    var geojsonLayer get() = mGeoJsonLayer
        set(value) { mGeoJsonLayer = value }

    fun getGeoJson(c: Context): JSONObject? {
        if (assetFilepath.isEmpty()) {
            return null
        }

        val content = ResourceProxy.readFromAssets(c, assetFilepath)
        val json = try { JSONObject(content) } catch (e: Exception) { null }
        return json
    }
}