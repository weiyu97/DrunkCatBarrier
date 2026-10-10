package com.drunkcat.barrier.risk

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gadgetbridge 数据桥接（路径三，极客模式）。
 *
 * Gadgetbridge 是一个开源的手环管理应用，将手环数据本地存储。
 * 通过它可读取到比标准 BLE 更丰富的健康数据（心率、HRV、睡眠等）。
 *
 * **为什么标注为极客模式：**
 * Android 不允许普通应用读取其他应用的私有数据库。
 * 本实现采用「用户主动导出」方案：
 * 1. 用户在 Gadgetbridge 中导出数据为文件
 * 2. 通过 SAF 选择该文件导入
 * 3. 本应用只读解析，不写入
 *
 * 这避免了申请 root、ADB 授权等高门槛权限。
 *
 * 隐私：导入的数据仅本地解析，不上传，可随时清除。
 */
class GadgetbridgeProvider(private val context: Context) : WearableProvider {

    override val name: String = "Gadgetbridge 导入"

    /** 最近一次导入的心率（0 表示无数据） */
    @Volatile var lastHeartRate: Int = 0
        private set

    /** 是否已导入过数据 */
    @Volatile private var imported = false

    override fun hasPermission(): Boolean = true  // SAF 无需预授权

    override fun start() {
        // 数据由用户主动导入，无需后台采集
    }

    override fun stop() {
    }

    override fun isConnected(): Boolean = imported && lastHeartRate > 0

    /**
     * 解析 Gadgetbridge 导出的文件。
     *
     * Gadgetbridge 支持导出为多种格式，这里解析最常见的
     * 「活动样本」JSON 或 CSV，从中提取最近的心率值。
     *
     * @param uri 用户通过 SAF 选择的文件
     * @return 成功解析的心率值数量
     */
    suspend fun importFromUri(uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() } ?: return@withContext 0

            val count = parseContent(text)
            if (count > 0) imported = true
            count
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 从文本内容中提取心率。
     *
     * 支持两种格式：
     * - CSV：包含 HEART_RATE 列
     * - JSON：包含 heartRate 字段
     *
     * 采用宽容解析：只找数字，不依赖严格的 schema。
     */
    private fun parseContent(text: String): Int {
        var count = 0

        // 尝试 CSV：找 HEART_RATE 列
        val lines = text.lines()
        if (lines.isNotEmpty() && lines.first().contains(",")) {
            val header = lines.first().split(",")
            val hrIndex = header.indexOfFirst {
                it.contains("HEART", ignoreCase = true) ||
                it.contains("心率", ignoreCase = true)
            }
            if (hrIndex >= 0) {
                lines.drop(1).forEach { line ->
                    val cols = line.split(",")
                    if (cols.size > hrIndex) {
                        val hr = cols[hrIndex].trim().toIntOrNull()
                        if (hr != null && hr in 30..220) {
                            lastHeartRate = hr
                            count++
                        }
                    }
                }
            }
        }

        // 尝试 JSON：找 heartRate / heart_rate 字段
        if (count == 0) {
            val regex = Regex(""""(?:heartRate|heart_rate|HR)"\s*:\s*(\d+)""")
            regex.findAll(text).forEach { match ->
                val hr = match.groupValues[1].toIntOrNull()
                if (hr != null && hr in 30..220) {
                    lastHeartRate = hr
                    count++
                }
            }
        }

        return count
    }

    /** 清除导入的数据 */
    fun clear() {
        lastHeartRate = 0
        imported = false
    }
}
