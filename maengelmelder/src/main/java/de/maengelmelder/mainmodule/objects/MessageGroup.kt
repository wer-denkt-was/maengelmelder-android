package de.maengelmelder.mainmodule.objects

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import de.maengelmelder.mainmodule.objects.areas.CoordinateBounds
import de.maengelmelder.mainmodule.utils.GoogleMapHelper
import kotlin.math.pow

/**
 * This class handles message grouping and provides utility functions for location-based grouping
 */
class MessageGroup(initialMessage: Message) {

    companion object {
        val PREFIX_ID = "messagegroup"
    }

    /**
     * @property mMessages List of messages in the group
     * @property mMinLatitude min latitude of the boundary
     * @property mMinLongitude min longitude of the boundary
     * @property mMaxLatitude max latitude of the boundary
     * @property mMaxLongitude max longitude of the bounday
     * @property bIsDead whether the group is a dead group or not
     * @property bIsSmall
     *      whether the group is small or not.
     *      Small group also means that the distance of the 2 furthest points (messages) in the group is less than [LONGEST_DISTANCE_SMALL_GROUP_M].
     *      The group can also be manually set as "small"
     *
     * @property LONGEST_DISTANCE_SMALL_GROUP_M the longest distance of the bounds of the group to be assumed as small group
     */
    private val mMessages = arrayListOf(initialMessage)
    private var mMinLatitude = initialMessage.lat
    private var mMinLongitude = initialMessage.lon
    private var mMaxLatitude = initialMessage.lat
    private var mMaxLongitude = initialMessage.lat
    private var bIsDead = false
    private var bIsSmall = false

    private val LONGEST_DISTANCE_SMALL_GROUP_M = 25f

    var isAlive: Boolean
        get() = !bIsDead
        set(stat) { bIsDead = !stat }

    var isSmall: Boolean
        get() = doCheckIfSmall()
        set(stat) { bIsSmall = stat }

    val messages: List<Message> get() = mMessages

    val groupId: String get() {
        return messages.map { m -> m.serverId }.joinToString("-", "$PREFIX_ID-")
    }

    /**
     * Returns the display count
     */
    val groupMemberDisplayCount: Int get() {
        return if (mMessages.isEmpty() || mMessages.size == 1) mMessages.size
            else if (mMessages.size == 2) 2
            else 3
    }

    /**
     * Gets the center point (longitude-latitude) of the messages in the group, or (0.0, 0.0) if no messages in a group.
     */
    fun getCenter(): Pair<Double, Double> {
        var totalLat = 0.0
        var totalLon = 0.0
        mMessages.forEach { m ->
            totalLat += m.lat
            totalLon += m.lon
        }
        totalLat /= mMessages.size
        totalLon /= mMessages.size
        return Pair(totalLon, totalLat)
    }

    /**
     * Adds another group into this group and re-calculates the furthest points
     */
    fun concat(other: MessageGroup) {
        other.messages.forEach { m ->
            mMessages.add(m)
            mMinLatitude    = mMinLatitude.coerceAtMost(m.lat)
            mMinLongitude   = mMinLongitude.coerceAtMost(m.lon)
            mMaxLatitude    = mMaxLatitude.coerceAtLeast(m.lat)
            mMaxLongitude   = mMaxLongitude.coerceAtLeast(m.lon)
        }
    }

    /**
     * Check if this group overlaps with other groups within the given threshold.
     * By overlapping, it means that the distance between groups are less than the given threshold.
     * The areas of the groups do not have to necessarily overlap
     *
     * @param other the other [MessageGroup]
     * @param threshold maximum distance to be considered overlapping in meters
     * @param mapHelper instance of [GoogleMapHelper] to provide with projections
     *
     * @return true if they overlap, false otherwise
     */
    fun isOverlappingWith(other: MessageGroup, threshold: Double, mapHelper: GoogleMapHelper): Boolean {
        val thisCenter = getCenter()
        val thatCenter = other.getCenter()
        val proj = mapHelper.getProjection()

        val thisPoint = proj.toScreenLocation(LatLng(thisCenter.second, thisCenter.first))
        val thatPoint = proj.toScreenLocation(LatLng(thatCenter.second, thatCenter.first))

        val distance = (thisPoint.x.toDouble() - thatPoint.x.toDouble()).pow(2) +
                (thisPoint.y.toDouble() - thatPoint.y.toDouble()).pow(2)

        return distance <= threshold.pow(2)
    }

    /**
     * Returns the [CoordinateBounds] object of this group
     */
    fun getBounds(): CoordinateBounds {
        return CoordinateBounds().apply {
            mMessages.forEach { m -> expand(m.lat, m.lon) }
        }
    }

    /**
     * Check if it is a small group either by checking its flag (which can be setup manually)
     * or calculating the distance between 2 furthest points in the group
     */
    private fun doCheckIfSmall(): Boolean {
        if (bIsSmall) return bIsSmall

        var maxLat = -200.0
        var maxLon = -200.0
        var minLat = 200.0
        var minLon = 200.0

        mMessages.forEach { m ->
            minLat      = minLat.coerceAtMost(m.lat)
            minLon      = minLon.coerceAtMost(m.lon)
            maxLat      = maxLat.coerceAtLeast(m.lat)
            maxLon      = maxLon.coerceAtLeast(m.lon)
        }

        val loc1 = Location("1").apply {
            latitude = minLat
            longitude = minLon
        }
        val loc2 = Location("2").apply {
            latitude = maxLat
            longitude = maxLon
        }

        return loc1.distanceTo(loc2) < LONGEST_DISTANCE_SMALL_GROUP_M
    }
}