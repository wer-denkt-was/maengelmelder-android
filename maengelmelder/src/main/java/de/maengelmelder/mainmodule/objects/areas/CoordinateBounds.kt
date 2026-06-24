package de.maengelmelder.mainmodule.objects.areas

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.VisibleRegion

class CoordinateBounds {
    private var longitudeEast = Double.MIN_VALUE
    private var longitudeWest = Double.MAX_VALUE
    private var latitudeNorth = Double.MIN_VALUE
    private var latitudeSouth = Double.MAX_VALUE

    private var empty = true

    fun CoordinatesBox() {}

    fun CoordinatesBox(longitudeEast: Double, longitudeWest: Double, latitudeNorth: Double, latitudeSouth: Double) {
        this.longitudeEast = longitudeEast
        this.longitudeWest = longitudeWest
        this.latitudeNorth = latitudeNorth
        this.latitudeSouth = latitudeSouth
        empty = false
    }

    fun CoordinatesBox(region: VisibleRegion) {
        latitudeNorth = region.latLngBounds.northeast.latitude
        latitudeSouth = region.latLngBounds.southwest.latitude
        longitudeEast = region.latLngBounds.northeast.longitude
        longitudeWest = region.latLngBounds.southwest.longitude
        empty = false
    }

    fun expand(point: Pair<Double, Double>) {
        latitudeSouth = if (latitudeSouth > point.second) point.second else latitudeSouth
        latitudeNorth = if (latitudeNorth < point.second) point.second else latitudeNorth
        longitudeWest = if (longitudeWest > point.first) point.first else longitudeWest
        longitudeEast = if (longitudeEast < point.first) point.first else longitudeEast
        empty = false
    }

    fun expand(lat: Double, lon: Double) {
        latitudeSouth = if (latitudeSouth > lat) lat else latitudeSouth
        latitudeNorth = if (latitudeNorth < lat) lat else latitudeNorth
        longitudeWest = if (longitudeWest > lon) lon else longitudeWest
        longitudeEast = if (longitudeEast < lon) lon else longitudeEast
        empty = false
    }

    fun getCenter(): Pair<Double, Double> {
        val longitude = (longitudeEast + longitudeWest) / 2
        val latitude = (latitudeNorth + latitudeSouth) / 2
        return Pair(longitude, latitude)
    }

    fun isEmpty(): Boolean {
        return empty
    }

    operator fun contains(point: Pair<Double, Double>): Boolean {
        val lat = (point.second in latitudeSouth..latitudeNorth
                || point.second in latitudeNorth..latitudeSouth)
        val longi = (point.first in longitudeWest..longitudeEast
                || point.first in longitudeEast..longitudeWest)
        return lat && longi
    }

    fun toLatLngBounds(): LatLngBounds {
        return LatLngBounds(LatLng(latitudeSouth, longitudeWest), LatLng(latitudeNorth, longitudeEast))
    }
}