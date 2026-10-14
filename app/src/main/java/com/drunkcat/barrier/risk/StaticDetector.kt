package com.drunkcat.barrier.risk

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 手机静置检测。
 *
 * 与 ShakeDetector 正相反：检测「加速度长时间几乎不变」，
 * 对应把手机架在支架上、躺着刷视频的场景。
 *
 * 判定：连续 [stillWindowMs] 内，加速度模长相对重力的波动始终小于 [stillThreshold]，
 * 视为「手机静置」，上报一次信号。之后需离开静置状态再重新累计，避免重复触发。
 *
 * 隐私：仅使用加速度传感器，不涉及任何个人信息。
 */
class StaticDetector(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var listener: SensorEventListener? = null

    /** 持续静置判定窗口 */
    private val stillWindowMs = 3 * 60_000L

    /** 波动阈值（m/s²）：低于此值视为「几乎不动」 */
    private val stillThreshold = 0.6f

    /** 窗口内最大波动 */
    private var windowMaxDelta = 0f
    private var windowStart = 0L

    /** 当前是否处于已上报的静置状态（离开后才会重置） */
    @Volatile private var stillReported = false

    /** 退出静置后需重新累计的冷却 */
    private var lastReportAt = 0L

    fun start() {
        if (listener != null) return
        val sm = sensorManager ?: return
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                handleAccel(event.values)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        listener = l
        sm.registerListener(l, accel, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        listener?.let { sensorManager?.unregisterListener(it) }
        listener = null
        windowMaxDelta = 0f
        stillReported = false
    }

    private fun handleAccel(values: FloatArray) {
        if (values.size < 3) return
        val magnitude = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        val delta = abs(magnitude - 9.81f)
        val now = System.currentTimeMillis()

        if (windowStart == 0L) windowStart = now

        // 超出窗口：结算并重置窗口
        if (now - windowStart > stillWindowMs) {
            evaluate(now)
            windowStart = now
            windowMaxDelta = 0f
        }
        // 记录窗口内最大波动
        if (delta > windowMaxDelta) windowMaxDelta = delta
    }

    private fun evaluate(now: Long) {
        val isStill = windowMaxDelta < stillThreshold
        if (isStill && !stillReported) {
            // 离开静置状态后才允许再次上报
            stillReported = true
            lastReportAt = now
            RiskMonitor.onStaticForeground("手机静置（支架/躺卧）")
        } else if (!isStill) {
            // 手机动了，解除静置状态，允许下次重新判定
            stillReported = false
        }
    }
}
