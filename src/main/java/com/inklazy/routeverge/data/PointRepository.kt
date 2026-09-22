package com.inklazy.routeverge.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PointRepository(context: Context) {
    private val prefs = context.getSharedPreferences("campus_runner_points", Context.MODE_PRIVATE)

    fun getPoints(): List<SavedPoint> = runCatching {
        val array = JSONArray(prefs.getString(KEY_POINTS, "[]") ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(SavedPoint(item.getString("id"), item.getString("name"), RoutePoint(item.getDouble("lat"), item.getDouble("lng"))))
            }
        }
    }.getOrDefault(emptyList())

    fun savePoint(name: String?, point: RoutePoint): SavedPoint {
        val points = getPoints().toMutableList()
        val label = name?.trim().orEmpty().ifBlank {
            var index = points.size + 1
            while (points.any { it.name == "点位$index" }) index++
            "点位$index"
        }
        val saved = SavedPoint(UUID.randomUUID().toString(), label, point)
        points.add(0, saved)
        val array = JSONArray().apply { points.forEach { p -> put(JSONObject().apply { put("id", p.id); put("name", p.name); put("lat", p.point.latWgs84); put("lng", p.point.lngWgs84) }) } }
        prefs.edit().putString(KEY_POINTS, array.toString()).apply()
        return saved
    }

    fun updatePoint(id: String, name: String?, point: RoutePoint): SavedPoint? {
        val points = getPoints().toMutableList()
        val index = points.indexOfFirst { it.id == id }
        if (index < 0) return null
        val current = points[index]
        val label = name?.trim().orEmpty().ifBlank { current.name }
        val updated = current.copy(name = label, point = point)
        points[index] = updated
        write(points)
        return updated
    }

    fun deletePoint(id: String): Boolean {
        val points = getPoints().toMutableList()
        val removed = points.removeAll { it.id == id }
        if (removed) write(points)
        return removed
    }

    private fun write(points: List<SavedPoint>) {
        val array = JSONArray().apply { points.forEach { p -> put(JSONObject().apply { put("id", p.id); put("name", p.name); put("lat", p.point.latWgs84); put("lng", p.point.lngWgs84) }) } }
        prefs.edit().putString(KEY_POINTS, array.toString()).apply()
    }

    private companion object { const val KEY_POINTS = "points_json" }
}
