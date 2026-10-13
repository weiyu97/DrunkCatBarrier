package com.drunkcat.barrier.report

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 拦截日志。
 *
 * 记录每次「打开被锁 App 并触发验证」的事件，用于复盘。
 * 按天分文件，JSON Lines 格式，仅本地。
 */
class InterceptLog(private val context: Context) {

    data class Entry(
        val timestamp: Long,
        val packageName: String,
        val passed: Boolean,
        val reason: String = ""
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("t", timestamp)
            put("pkg", packageName)
            put("pass", passed)
            if (reason.isNotEmpty()) put("reason", reason)
        }

        companion object {
            fun fromJson(o: JSONObject): Entry = Entry(
                timestamp = o.getLong("t"),
                packageName = o.optString("pkg", ""),
                passed = o.optBoolean("pass", false),
                reason = o.optString("reason", "")
            )
        }
    }

    /** 紧急绕过事件的特殊包名，用于复盘识别 */
    companion object {
        const val EMERGENCY_BYPASS_PKG = "__EMERGENCY_BYPASS__"

        /**
         * 写入一条紧急绕过记录。
         * 复盘时据此识别"用户跳过验证直接进入"的事件。
         */
        suspend fun logEmergencyBypass(context: Context) {
            val log = InterceptLog(context)
            log.append(
                Entry(
                    timestamp = System.currentTimeMillis(),
                    packageName = EMERGENCY_BYPASS_PKG,
                    passed = true,
                    reason = "紧急跳过验证"
                )
            )
        }
    }

    private val dir: File
        get() = File(context.filesDir, "logs").apply { if (!exists()) mkdirs() }

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun fileFor(ts: Long) = File(dir, "${dayFormat.format(Date(ts))}.jsonl")

    suspend fun append(entry: Entry) = withContext(Dispatchers.IO) {
        fileFor(entry.timestamp).appendText(entry.toJson().toString() + "\n")
    }

    suspend fun readDay(dayTimestamp: Long): List<Entry> = withContext(Dispatchers.IO) {
        val f = fileFor(dayTimestamp)
        if (!f.exists()) return@withContext emptyList()
        f.readLines().mapNotNull {
            if (it.isBlank()) null
            else try { Entry.fromJson(JSONObject(it)) } catch (e: Exception) { null }
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
    }
}
