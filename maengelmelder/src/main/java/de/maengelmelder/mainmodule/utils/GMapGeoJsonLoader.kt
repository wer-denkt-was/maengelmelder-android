package de.maengelmelder.mainmodule.utils

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle
import de.maengelmelder.mainmodule.objects.LayerswitcherInfo
import de.maengelmelder.mainmodule.objects.MapLayerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GMapGeoJsonLoader : ViewModel() {

    fun load(c: Context,
             map: GoogleMap,
             markerManager: MarkerManager,
             onFinished: (LayerswitcherInfo?, Map<String, MapLayerInfo>) -> Unit) {
        viewModelScope.launch {
            val geojson: Pair<LayerswitcherInfo?, Map<String, MapLayerInfo>> = with(Dispatchers.IO) {
                val layerInfos = ResourceProxy.getAdditionalMapLayerInfo(c)
                val geojsonLayers = hashMapOf<String, MapLayerInfo>()
                layerInfos?.second?.forEach { x ->
                    val text = x.getGeoJson(c)
                    if (text != null) {
                        val layer = GeoJsonLayer(
                            map,
                            text,
                            markerManager,
                            null,
                            null,
                            null)

                        val polygonStyle = GeoJsonPolygonStyle()
                        polygonStyle.strokeWidth = x.lineWidth.toFloat()
                        polygonStyle.strokeColor = x.lineColor
                        polygonStyle.fillColor = x.fillColor

                        val lineStringStyle = GeoJsonLineStringStyle()
                        lineStringStyle.width = x.lineWidth.toFloat()
                        lineStringStyle.color = x.lineColor

                        // apply styling if any
                        for (feature in layer.features) {
                            if (feature.hasGeometry()) {
                                val geomType = feature.geometry.geometryType
                                if (geomType == "Polygon" || geomType == "MultiPolygon") {
                                    feature.polygonStyle = polygonStyle
                                } else if (geomType == "LineString") {
                                    feature.lineStringStyle = lineStringStyle
                                }
                            }
                        }
                        x.geojsonLayer = layer
                        geojsonLayers[x.name] = x
                    }
                }
                Pair(layerInfos?.first, geojsonLayers)
            }
            onFinished.invoke(geojson.first, geojson.second)
        }
    }

}