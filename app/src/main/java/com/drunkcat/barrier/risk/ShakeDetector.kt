package com.drunkcat.barrier.risk

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 步态不稳检测。
 *
 * 通过加速度计检测「持续晃动」特征：
 * 醉酒者的手部控制不稳，手机加速度变化比清醒者剧烈且频繁。
 *
 * 判定：30 秒滑动窗口内，加速度模长突变（> 3 m/s²）出现次数 ≥ 15 次。
 * 该信号仅作为风险评分的一项，不单独触发保护。
 *
 * 隐私：仅使用加速度传感器，不涉及任何个人信息。
 */
class ShakeDetector(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var listener: SensorEventListener? = null

    /** 突变时间戳队列（滑动窗口） */
    private val spikes = ArrayDeque<Long>()

    /** 窗口长度 */
    private val windowMs = 30_000L

    /** 判定阈值 */
    private val spikeThreshold = 3.0f
    private val spikeCountThreshold = 15

    /** 上次上报时间，避免频繁触发 */
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
        spikes.clear()
    }

    private fun handleAccel(values: FloatArray) {
        if (values.size < 3) return
        val x = values[0]
        val y = values[1]
        val z = values[2]
        // 加速度模长（去除重力后约等于运动加速度；此处用总模长减去 9.8 的绝对值近似）
        val magnitude = sqrt(x * x + y * y + z * z)
        val delta = abs(magnitude - 9.81f)

        val now = System.currentTimeMillis()
        if (delta > spikeThreshold) {
            spikes.addLast(now)
        }
        // 清理窗口外
        while (spikes.isNotEmpty() && now - spikes.first() > windowMs) {
            spikes.removeFirst()
        }

        // 达到阈值则上报（每 5 分钟最多一次）
        if (spikes.size >= spikeCountThreshold && now - lastReportAt > 5 * 60 * 1000L) {
            lastReportAt = now
            RiskMonitor.onDeviceShaking()
        }
    }
}
