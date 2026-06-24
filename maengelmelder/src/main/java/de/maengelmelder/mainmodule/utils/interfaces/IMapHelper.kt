package de.maengelmelder.mainmodule.utils.interfaces

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable


/**
 * This interface is the base of any map implementation. Do inherit this method when you are implementing a different map engine (OSM, GMap, etc.)
 */
interface IMapHelper {

    companion object {
        val ZoomToScale: Map<Int, Double> = mapOf(
                1 to 591657550.500000,
                2 to 295828775.300000,
                3 to 147914387.600000,
                4 to 73957193.820000,
                5 to 36978596.910000,
                6 to 18489298.450000,
                7 to 9244649.227000,
                8 to 4622324.614000,
                9 to 2311162.307000,
                10 to 1155581.153000,
                11 to 577790.576700,
                12 to 288895.288400,
                13 to 144447.644200,
                14 to 72223.822090,
                15 to 36111.911040,
                16 to 18055.955520,
                17 to 9027.977761,
                18 to 4513.988880,
                19 to 2256.994440,
                20 to 1128.497220
        )
    }

    /**
     * @property Display enumeration to differentiate map display
     */
    enum class Display {
        STREET, SATELLITE
    }

    /**
     * This method should move the center of the map to the desired lat-lon
     *
     * @param lat latitude
     * @param lon longitude
     */
    fun moveTo(lat: Double, lon: Double)

    /**
     * This method should move the center of the map to the desired lat-lon and zoom level
     *
     * @param lat latitude
     * @param lon longitude
     * @param zoom zoom level
     */
    fun moveTo(lat: Double, lon: Double, zoom: Int)

    /**
     * This method should display a marker to a map on a location designated by lat-lon
     *
     * @param identifier the identifier to differentiate between markers.
     * @param drawable the marker image drawable
     * @param lat latitude
     * @param lon longitude
     * @param data extra data. Nullable
     * @param markerDescriptor text to be read out when clicking marker, for accessibility purpose
     */
    fun addMarker(identifier: String, drawable: Drawable, lat: Double, lon: Double, data: Any?, markerDescriptor: String?)

    /**
     * This method should draw polygon on top of the map with the defined list of coords as the corners
     *
     * @param identifier id
     * @param coords coordinates
     * @param fillColor color to fill the polygon
     * @param strokeColor edge color
     */
    fun addPolygon(identifier: String, coords: List<Pair<Double, Double>>, fillColor: Int, strokeColor: Int)

    /**
     * This method should remove all markers on the map.
     * Whether the markers are made invisible or removed completely is up to the implementation
     */
    fun clearMarkers()

    /**
     * This method should reliably return the user's current location through the given param,
     * be it from GPS, network, or even own caches.
     * When it fails, do not invoke the function param
     *
     */
    fun getMyLocation(retrieved: (Double, Double) -> Unit)

    /**
     * This method should return the center coordinate of the displayed portion of the map in a form of lon-lat
     *
     * @return a Pair of Doubles. First is longitude, second is latitude
     */
    fun getCenter(): Pair<Double, Double>

    /**
     * This method should change the map's display according to [Display] parameter
     *
     * @param d the type of display
     */
    fun changeDisplayTo(d: Display)

    /**
     * This method should pause the map. You can call this method on [android.app.Activity.onPause] method
     */
    fun pause()

    /**
     * This method should resume the map functionality. You can call this method on [android.app.Activity.onResume] method
     */
    fun resume()

    /**
     * This method should initiate a listener when a displayed marker is clicked
     *
     * @param c Context
     * @param f the function that emits a String and triggered every time a marker is clicked. This string is the identifier of the marker
     *
     * @see addMarker
     */
    fun setOnMarkerClickListener(c: Context, f: (String?, data: Any?) -> Unit)

    /**
     * Get the current scale of the map
     *
     * @return the map's scale in Double. The scale itself is a value of X from scale such as 1 : X.
     * E.g. if the current scale is 1 : 10000, the returned value is 10000.0
     */
    fun getMapScale(): Double

    /**
     * Returns the zoom level of the map
     *
     * @return Map's zoom level
     */
    fun getZoomLevel(): Int

    /**
     *  This method should highlight a chosen marker identified by the ID. If the marker does not exist,
     *  this method should not change anything
     *
     */
    fun highlightMarker(id: String)

    /**
     * This method clears all highlighted markers and reset them back to their normal state
     */
    fun clearHighlights()

    /**
     *
     * Get the map's extent in a form of (min lat-lon, max lat-lon). Below is the representation.
     * First pair is x-y of the min Point, 2nd pair is x-y of the max Point
     *
     * 1st pair
     * ^
     * +-----------+
     * |           |
     * |           |
     * |           |
     * +-----------+ -> 2nd pair
     *
     * @return Pair of lon-lat pairs. The first pair is the top-left lon-lat. The second one is the bottom-right lon-lat
     */
    fun getMapExtent(): Pair<Pair<Double, Double>, Pair<Double, Double>>

    /**
     * This method should emit the center location when the map is panned by user's finger
     *
     * @param c Context
     * @param onStartMoving triggered when the map starts moving by user gesture
     * @param locChange the listener that emits 2 Double values (1st is lon, 2nd is Lat) every time the map is panned.
     */
    fun setMapPanListener(c: Context, onStartMoving: (Int) -> Unit, locChange: (Double, Double) -> Unit)

    /**
     * This method should register a listener that listens on user's tap on the map (excluding marker/shapes/etc)
     * @param c Context
     * @param clickLoc location of the user's tap (Lon-Lat)
     */
    fun setMapClickListener(c: Context, clickLoc: (Double, Double) -> Unit)
}