package de.maengelmelder.mainmodule.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.data.geojson.GeoJsonLayer
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.customviews.dialogs.LayerswitcherDialog
import de.maengelmelder.mainmodule.objects.LayerswitcherInfo
import de.maengelmelder.mainmodule.objects.MapLayerInfo
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.utils.interfaces.IMapHelper

/**
 * Helper class for Google map
 */
class GoogleMapHelper(c: Context, map: GoogleMap) : IMapHelper {

    companion object {
        val PREF_LAYER_VIS_PREFIX = "mm.maplayers.visibility"
    }

    /**
     * @property mMap Instance of [GoogleMap]
     * @property mFusedLocService location service for user's position
     * @property mCenterLat map's center latitude
     * @property mCenterLon map's center longitude
     * @property mMarkers map of identifier-marker
     */
    private val mCtx = c
    private var mMap = map
    private var mFusedLocService = LocationServices.getFusedLocationProviderClient(c)
    private var mLastHighlightedMarker: Pair<String, Any?>? = null

    private val mMarkers = hashMapOf<String, Marker>()
    private val mPolygons = hashMapOf<String, Polygon>()
    private var bUserPanMap = false
    private var mMyLocMarker: Marker? = null

    private val mMarkerManager = MarkerManager(map)
    private val mMarkerCollection = mMarkerManager.newCollection()

    private var mLayerMaps: Map<String, MapLayerInfo> = mapOf()
    private var mLayerSwitcherInfo: LayerswitcherInfo? = null
    private val mPrefManager = PreferenceManager.getDefaultSharedPreferences(mCtx)
    private var mLayerSwitcherDialog: LayerswitcherDialog? = null

    init {
        // Disable most of the default controls
        map.uiSettings.apply {
            isRotateGesturesEnabled = false
            isMapToolbarEnabled = false
            isCompassEnabled = false
            isMyLocationButtonEnabled = false
            isTiltGesturesEnabled = false
        }

    }

    /**
     * Returns the instance of layerswitcher dialog
     */
    fun getLayerswitcherDialog(): LayerswitcherDialog {
        if (mLayerSwitcherDialog == null) {
            mLayerSwitcherDialog = LayerswitcherDialog(
                mCtx,
                this,
                mLayerSwitcherInfo?.singleSwitch?: false,
                mLayerSwitcherInfo?.switchName)
        }
        return mLayerSwitcherDialog!!
    }

    /**
     * Loads geojson layer from asset .json file and add them to the map
     */
    fun loadGeoJSONLayers(layerswitcherToggler: View? = null) {
        GMapGeoJsonLoader.load(mCtx, mMap, mMarkerManager) { info, layers ->
            mLayerMaps = layers
            mLayerSwitcherInfo = info
            layerswitcherToggler?.let { t -> toggleLayerswitcher(t) }
            if (layers.isNotEmpty()) {
                layers.forEach { entry ->
                    val geojson = entry.value.geojsonLayer
                    val canShow = canShowLayer(entry.key, entry.value)
                    if (canShow && geojson != null) {
                        geojson.addLayerToMap()
                    }
                }
            }
        }
    }

    /**
     * show/hide layerswitcher button depending on whether there is at least one loaded layer
     */
    fun toggleLayerswitcher(layerswitcherToggler: View) {
        if (mLayerMaps.isEmpty()) {
            layerswitcherToggler.visibility = View.GONE
            layerswitcherToggler.setOnClickListener(null)
        } else {
            layerswitcherToggler.visibility = View.VISIBLE
            layerswitcherToggler.setOnClickListener { v ->
                getLayerswitcherDialog().show()
            }
        }
    }

    /**
     * returns true if the given Geojson layer can be shown or not
     * the preference is updated through Layerswitcher dialog or [toggleLayer]
     */
    fun canShowLayer(layerName: String, layerInfo: MapLayerInfo?): Boolean {
        val key = "$PREF_LAYER_VIS_PREFIX.$layerName"
        return if (mPrefManager.contains(key)) {
            mPrefManager.getBoolean(key, true)
        } else {
            layerInfo?.initialVisibility?: true
        }
    }

    /**
     * show/hide geojson layers with the given names
     */
    fun toggleLayer(layerNames: Array<String>, value: Boolean) {
        layerNames.forEach { l ->
            mPrefManager.edit().putBoolean("$PREF_LAYER_VIS_PREFIX.$l", value).apply()
            val geojson = mLayerMaps[l]?.geojsonLayer

            if (geojson != null) {
                try {
                    if (value) {
                        geojson.addLayerToMap()
                    } else {
                        geojson.removeLayerFromMap()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Layer is either already added or removed from map
                }
            }
        }
    }

    /**
     * Returns the loaded geojson layer
     */
    fun getCachedLayers(): Map<String, MapLayerInfo> {
        return mLayerMaps
    }

    /**
     * Returns Google Map's [Projection]
     */
    fun getProjection(): Projection {
        return mMap.projection
    }

    /**
     * Toggle panning functionality
     * @param toggle true to turn on, false otherwise
     */
    fun togglePanning(toggle: Boolean) {
        mMap.uiSettings.isScrollGesturesEnabled = toggle
    }

    /**
     * Implementation of [IMapHelper.moveTo]. No moving animation
     */
    override fun moveTo(lat: Double, lon: Double) {
        val camUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 15f)
        mMap.moveCamera(camUpdate)
    }

    /**
     * Implementation of [IMapHelper.moveTo]. No moving animation
     */
    override fun moveTo(lat: Double, lon: Double, zoom: Int) {
        val camUpdate = CameraPosition.Builder()
                .target(LatLng(lat, lon))
                .zoom(zoom.toFloat())
                .build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camUpdate))
    }

    /**
     * Wrapped call to native [GoogleMap.animateCamera]
     */
    fun animateCamera(update: CameraUpdate) {
        mMap.animateCamera(update)
    }

    /**
     * Implementation of [IMapHelper.addMarker]. Only 1 marker per identifier. Existing marker will have its image and position
     * replaced
     */
    override fun addMarker(identifier: String, drawable: Drawable, lat: Double, lon: Double, data: Any?, markerDescriptor: String?) {
        val existing = mMarkers[identifier]
        val icon = BitmapDescriptorFactory.fromBitmap(drawableToBitmap(drawable))

        if (existing == null) {
            val mo = MarkerOptions().position(LatLng(lat, lon)).icon(icon)
            mMarkerCollection.addMarker(mo)?.also { m ->
                m.tag = Pair(identifier, data)
                mMarkers[identifier] = m
            }
        } else {
            existing.position = LatLng(lat, lon)
            existing.setIcon(icon)
            existing.tag = Pair(identifier, data)
        }
    }

    /**
     * Same as [IMapHelper.addMarker]. but it uses image URL instead
     * Executes [MapMarkerFromURLCoroutine] to load the bitmap from URL and add it as marker
     */
    fun addMarkerFromImageUrl(
        identifier: String,
        url: String,
        lat: Double,
        lon: Double,
        data: Any?,
        markerDescriptor: String?
    ) {
        val existing = mMarkers[identifier]
        MapMarkerFromURLCoroutine.run(url, 120) { b ->
            if (b == null) return@run
            if (existing == null) {
                val mo = MarkerOptions()
                    .position(LatLng(lat, lon))
                    .icon(BitmapDescriptorFactory.fromBitmap(b))

                mMarkerCollection.addMarker(mo)?.also { m ->
                    m.tag = Pair(identifier, data)
                    mMarkers[identifier] = m
                }
            } else {
                existing.position = LatLng(lat, lon)
                existing.setIcon(BitmapDescriptorFactory.fromBitmap(b))
                existing.tag = Pair(identifier, data)
            }
        }
    }

    /**
     * Add my location marker on the map. It is excluded from other generic markers
     */
    fun addMyLocationMarker(markerIcon: Int, lat: Double, lon: Double) {
        if (mMyLocMarker !== null) {
            mMyLocMarker?.remove()
            mMyLocMarker = null
        }

        val mo = MarkerOptions()
                .position(LatLng(lat, lon))
                .icon(BitmapDescriptorFactory.fromResource(markerIcon))

        mMarkerCollection.addMarker(mo)?.also { m ->
            m.tag = "MyLocation"
            mMyLocMarker = m
        }
    }

    /**
     * Simply add marker to the map without caching. Gives back the [Marker] object
     * Returned [Marker] object is nullable depending on GoogleMap's runtime
     */
    fun addMarkerNoCache(identifier: String, drawable: Drawable, lat: Double, lon: Double, data: Any?, descriptor: String? = null): Marker? {
        val icon = BitmapDescriptorFactory.fromBitmap(drawableToBitmap(drawable))
        val mo = MarkerOptions().position(LatLng(lat, lon)).icon(icon)
        return mMarkerCollection.addMarker(mo)?.also { m ->
            m.tag = Pair(identifier, data)
            mMarkers[identifier] = m
            descriptor?.let { d -> m.title = d }
        }
    }

    /**
     * Implementation of [IMapHelper.addPolygon]. It removes existing polygon with the same identifier
     */
    override fun addPolygon(identifier: String, coords: List<Pair<Double, Double>>, fillColor: Int, strokeColor: Int) {
        if (mPolygons.contains(identifier)) {
            val poly = mPolygons[identifier]
            poly?.remove()
        }

        val opt = PolygonOptions().strokeColor(strokeColor).fillColor(fillColor)
        coords.forEach { c -> opt.add(LatLng(c.second, c.first)) }
        mMap.addPolygon(opt).also { polygon -> mPolygons[identifier] = polygon }
    }

    /**
     * Convert the given [Drawable] to [Bitmap]. Useful to convert shapes/layer drawables since they
     * cannot produce the [Bitmap].
     *
     * @param drawable the drawable
     * @return the bitmap of the drawable
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        var bitmap: Bitmap
        try {
            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }
            bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        } catch (e: OutOfMemoryError) {
            val conf = Bitmap.Config.ARGB_4444 // see other conf types
            bitmap = Bitmap.createBitmap(50, 50, conf) // this creates a MUTABLE bitmap
        }

        return bitmap
    }

    /**
     * Implementation of [IMapHelper.clearMarkers]. Clears all markers along with the map containing it
     */
    override fun clearMarkers() {
        mMarkers.entries.forEach { entry ->
            entry.value.remove()
        }
        mMarkers.clear()
    }

    /**
     * Wrapped call to [GoogleMap.clear]
     */
    fun clear() {
        mMap.clear()
    }

    /**
     * Implementation of [IMapHelper.getMyLocation]. If no location can be retrieved, it will emit (0.0, 0.0) through the
     * second parameter [retrieved]. If Permission is not given, it will not emit anything
     */
    override fun getMyLocation(retrieved: (Double, Double) -> Unit) {
        val locReq = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setMaxUpdateAgeMillis(0)
                .setDurationMillis(3000)
                .build()

        val locTask = try { mFusedLocService.getCurrentLocation(locReq, null) }
            catch (e: SecurityException) { e.printStackTrace(); null }
            catch (e: RuntimeExecutionException) { e.printStackTrace(); null }

        locTask?.let { task ->
            task.addOnCompleteListener { loc ->
                val result = try { loc.result } catch (e: RuntimeExecutionException) { e.printStackTrace(); null }
                if (result == null) {
                    retrieved.invoke(0.0, 0.0)
                } else {
                    retrieved.invoke(result.longitude, result.latitude)
                }
            }.addOnFailureListener {
                retrieved.invoke(0.0, 0.0)
            }.addOnCanceledListener {
                retrieved.invoke(0.0, 0.0)
            }
        }
    }

    /**
     * Implementation of [IMapHelper.getCenter]. Returns lon-lat pair
     */
    override fun getCenter(): Pair<Double, Double> {
        val center = mMap.cameraPosition.target
        return Pair(center.longitude, center.latitude)
    }

    /**
     * Implementation of [IMapHelper.changeDisplayTo].
     */
    override fun changeDisplayTo(d: IMapHelper.Display) {
        when (d) {
            IMapHelper.Display.STREET -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            IMapHelper.Display.SATELLITE -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        }
    }

    override fun pause() {
        // Not implemented
    }

    override fun resume() {
        // Not implemented
    }

    /**
     * Implementation of [IMapHelper.setOnMarkerClickListener]. It will emit only existing identifier from [mMarkers].
     * The identifier is placed by [IMapHelper.addMarker]
     */
    override fun setOnMarkerClickListener(c: Context, f: (String?, data: Any?) -> Unit) {
        mMarkerCollection.setOnMarkerClickListener { marker: Marker ->
            if (marker.tag != null) {
                val pair = try { marker.tag as Pair<String, Any?> } catch (e: ClassCastException) { null }
                f.invoke(pair?.first, pair?.second)
            }
            false
        }
    }

    /**
     * Implementation of [IMapHelper.getMapScale].
     */
    override fun getMapScale(): Double {
        val zoom = mMap.cameraPosition.zoom.toInt()
        return try { IMapHelper.ZoomToScale[zoom]?: -1.0 } catch (e: Exception) { -1.0 }
    }

    /**
     * Implementation of [IMapHelper.getZoomLevel]
     */
    override fun getZoomLevel(): Int =  mMap.cameraPosition.zoom.toInt()

    /**
     * Implementation of [IMapHelper.highlightMarker]
     */
    override fun highlightMarker(id: String) {
        if (MMConstants.UseMarkerUri) return
        val marker = mMarkers[id]
        if (marker != null) {
            val tag = try { marker.tag as Pair<String, Any?> } catch (e: ClassCastException) { null }
            if (tag != null) {
                val message = try { tag.second as Message? } catch (e: ClassCastException) { null }

                if (message != null) {
                    // Remove the highlight from the last selected marker
                    clearHighlights()

                    // Highlight the new one
                    val highlighted = ResourceProxy.getMarker(mCtx, message.colorString, message.category.markerId, true)
                    replaceMarkerDrawable(id, highlighted)
                    marker.zIndex = 1000f
                    mLastHighlightedMarker = tag
                }
            }
        }
    }

    /**
     * Implementation of [IMapHelper.clearHighlights]
     */
    override fun clearHighlights() {
        if (MMConstants.UseMarkerUri) return

        mLastHighlightedMarker?.let { last ->
            val lastHighlightedMsg = try { last.second as Message? } catch (e: ClassCastException) { null }
            lastHighlightedMsg?.let { lastMsg ->
                val original = ResourceProxy.getMarker(mCtx, lastMsg.colorString, lastMsg.category.markerId)
                replaceMarkerDrawable(last.first, original)
                mLastHighlightedMarker = null
            }
        }
    }

    /**
     * Replace the marker icon with the given drawable
     *
     * @param id marker's identifier from [addMarker]
     * @param with the replacement icon in Drawable
     */
    private fun replaceMarkerDrawable(id: String, with: Drawable) {
        val marker = mMarkers[id]
        if (marker != null) {
            val icon = BitmapDescriptorFactory.fromBitmap(drawableToBitmap(with))
            marker.setIcon(icon)
        }
    }

    /**
     * Implementation of [IMapHelper.getMapExtent]. (top-left lon-lat), (bottom-right lon-lat)
     */
    override fun getMapExtent(): Pair<Pair<Double, Double>, Pair<Double, Double>> {
        val vis = mMap.projection.visibleRegion
        return Pair(
                Pair(vis.farLeft.longitude, vis.farLeft.latitude),
                Pair(vis.nearRight.longitude, vis.nearRight.latitude))
    }

    /**
     * Implementation of [IMapHelper.setMapPanListener]. The [locChange] will emit values when the map is moved either
     * by user or by the map itself (e.g. from [moveTo] method)
     */
    override fun setMapPanListener(c: Context, onStartMoving: (Int) -> Unit, locChange: (Double, Double) -> Unit) {
        mMap.setOnCameraMoveStartedListener { moveType ->
            bUserPanMap = when (moveType) {
                GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE -> {
                    onStartMoving.invoke(moveType)
                    true
                }
                else -> false
            }
        }
        mMap.setOnCameraIdleListener {
            if (bUserPanMap) {
                val center = getCenter()
                locChange.invoke(center.first, center.second)
            }
        }
    }

    /**
     * Implementation of [IMapHelper.setMapClickListener]
     */
    override fun setMapClickListener(c: Context, clickLoc: (Double, Double) -> Unit) {
        mMap.setOnMapClickListener { ll ->
            clickLoc.invoke(ll.longitude, ll.latitude)
        }
    }
}