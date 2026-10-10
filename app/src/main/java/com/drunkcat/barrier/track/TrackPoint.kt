package com.drunkcat.barrier.track

import org.json.JSONObject

/**
 * 单个轨迹点。
 */
data class TrackPoint(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val provider: String,
    val placeName: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("t", timestamp)
        put("lat", latitude)
        put("lon", longitude)
        put("acc", accuracy.toDouble())
        put("spd", speed.toDouble())
        put("prov", provider)
        placeName?.let { put("place", it) }
    }

    companion object {
        fun fromJson(o: JSONObject): TrackPoint = TrackPoint(
            timestamp = o.getLong("t"),
            latitude = o.getDouble("lat"),
            longitude = o.getDouble("lon"),
            accuracy = o.optDouble("acc", 0.0).toFloat(),
            speed = o.optDouble("spd", 0.0).toFloat(),
            provider = o.optString("prov", "unknown"),
            placeName = o.optString("place").takeIf { it.isNotEmpty() }
        )
    }
}
