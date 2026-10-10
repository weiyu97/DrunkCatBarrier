package com.drunkcat.barrier.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 测试 Gadgetbridge 文件解析逻辑。
 *
 * 由于 GadgetbridgeProvider 的解析方法依赖 Context，
 * 这里提取等效的解析逻辑进行测试，确保格式兼容。
 */
class GadgetbridgeParseTest {

    /** 与 GadgetbridgeProvider.parseContent 等价的纯函数 */
    private fun parseHeartRate(text: String): Pair<Int, Int> {
        var lastHr = 0
        var count = 0

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
                            lastHr = hr
                            count++
                        }
                    }
                }
            }
        }

        if (count == 0) {
            val regex = Regex(""""(?:heartRate|heart_rate|HR)"\s*:\s*(\d+)""")
            regex.findAll(text).forEach { match ->
                val hr = match.groupValues[1].toIntOrNull()
                if (hr != null && hr in 30..220) {
                    lastHr = hr
                    count++
                }
            }
        }

        return lastHr to count
    }

    @Test
    fun `parses english csv header`() {
        val csv = "TIMESTAMP,HEART_RATE,STEPS\n1700000000,72,100\n1700000060,85,150"
        val (lastHr, count) = parseHeartRate(csv)
        assertEquals(2, count)
        assertEquals(85, lastHr)
    }

    @Test
    fun `parses chinese csv header`() {
        val csv = "时间,心率,步数\n1700000000,68,100"
        val (lastHr, count) = parseHeartRate(csv)
        assertEquals(1, count)
        assertEquals(68, lastHr)
    }

    @Test
    fun `parses json heartRate field`() {
        val json = """{"samples":[{"heartRate":75},{"heartRate":88}]}"""
        val (lastHr, count) = parseHeartRate(json)
        assertEquals(2, count)
        assertEquals(88, lastHr)
    }

    @Test
    fun `parses json heart_rate field`() {
        val json = """{"data":[{"heart_rate":92}]}"""
        val (lastHr, count) = parseHeartRate(json)
        assertEquals(1, count)
        assertEquals(92, lastHr)
    }

    @Test
    fun `ignores out of range values`() {
        val csv = "TIMESTAMP,HEART_RATE\n1700000000,500\n1700000060,20"
        val (_, count) = parseHeartRate(csv)
        assertEquals(0, count)
    }

    @Test
    fun `handles empty input`() {
        val (lastHr, count) = parseHeartRate("")
        assertEquals(0, count)
        assertEquals(0, lastHr)
    }
}
