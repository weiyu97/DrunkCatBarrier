package com.drunkcat.barrier.track

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 轨迹存储。
 *
 * 按天分文件，格式为 JSON Lines（每行一个 JSON 对象）。
 * 仅本地存储，不自动删除，用户可手动清理。
 */
class TrackStore(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "tracks").apply { if (!exists()) mkdirs() }

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun fileFor(timestamp: Long): File =
        File(dir, "${dayFormat.format(Date(timestamp))}.jsonl")

    /**
     * 追加一个轨迹点。
     */
    suspend fun append(point: TrackPoint) = withContext(Dispatchers.IO) {
        val f = fileFor(point.timestamp)
        f.appendText(point.toJson().toString() + "\n")
    }

    /**
     * 读取指定日期的轨迹点。
     */
    suspend fun readDay(dayTimestamp: Long): List<TrackPoint> = withContext(Dispatchers.IO) {
        val f = fileFor(dayTimestamp)
        if (!f.exists()) return@withContext emptyList()
        f.readLines().mapNotNull { line ->
            if (line.isBlank()) null
            else try {
                TrackPoint.fromJson(JSONObject(line))
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 列出所有有轨迹的日期文件。
     */
    suspend fun listDays(): List<File> = withContext(Dispatchers.IO) {
        dir.listFiles()?.sortedByDescending { it.name } ?: emptyList()
    }

    /**
     * 删除全部轨迹。
     */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
    }

    /**
     * 删除指定日期之前的轨迹。
     */
    suspend fun deleteBefore(timestamp: Long) = withContext(Dispatchers.IO) {
        val cutoff = dayFormat.format(Date(timestamp))
        dir.listFiles()?.forEach { f ->
            if (f.nameWithoutExtension < cutoff) f.delete()
        }
    }

    /**
     * 删除单个轨迹点（按时间戳）。
     */
    suspend fun deletePoint(point: TrackPoint) = withContext(Dispatchers.IO) {
        val f = fileFor(point.timestamp)
        if (!f.exists()) return@withContext
        val kept = f.readLines().filter { line ->
            try {
                val o = JSONObject(line)
                o.getLong("t") != point.timestamp
            } catch (e: Exception) {
                true
            }
        }
        f.writeText(kept.joinToString("\n", postfix = if (kept.isEmpty()) "" else "\n"))
    }
}
