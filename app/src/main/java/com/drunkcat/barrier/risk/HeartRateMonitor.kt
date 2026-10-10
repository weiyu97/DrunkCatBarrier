package com.drunkcat.barrier.risk

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 心率监测器（标准 BLE 心率服务）。
 *
 * 兼容所有支持标准 Heart Rate Service (0x180D) 的手环/手表：
 * 小米、华为、Amazfit、佳明、Polar 等。
 *
 * 设计：
 * - 扫描并连接第一个广播心率服务的设备
 * - 订阅 Heart Rate Measurement 特征，实时接收心率
 * - 连接断开自动重试
 *
 * 隐私：心率数据仅本地使用，不上传，不记录原始序列。
 *
 * 重要说明：心率不能直接判断醉酒。酒精、运动、压力、咖啡因
 * 都会影响心率，因此它只作为风险评分的一个**弱信号**，
 * 不能单独触发保护。
 */
class HeartRateMonitor(private val context: Context) {

    /** 标准心率服务 UUID */
    private val HR_SERVICE = java.util.UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    /** 心率测量特征 UUID */
    private val HR_MEASUREMENT = java.util.UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    /** 客户端特征配置描述符 */
    private val CLIENT_CHAR_CONFIG = java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var gatt: BluetoothGatt? = null
    private var scanning = false

    /** 最近的心率值（用于外部读取） */
    @Volatile var lastHeartRate: Int = 0
        private set

    /** 基线心率（静息，用于判断异常） */
    @Volatile private var baselineHr: Int = 0
    private val recentHr = ArrayDeque<Int>()

    /** 上次上报异常的时间，避免频繁触发 */
    @Volatile private var lastReportAt: Long = 0L

    fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scan = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            val conn = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            return scan && conn
        }
        @Suppress("DEPRECATION")
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission()) return
        if (scanning || gatt != null) return

        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE)
            as? BluetoothManager ?: return
        val adapter = bm.adapter ?: return
        if (!adapter.isEnabled) return

        val scanner = adapter.bluetoothLeScanner ?: return
        scanning = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE)
                as? BluetoothManager
            bm?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) { }
        scanning = false
        try {
            gatt?.close()
        } catch (e: Exception) { }
        gatt = null
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val r = result ?: return
            // 检查是否广播心率服务
            val hasHr = r.scanRecord?.serviceUuids?.any { it.uuid == HR_SERVICE } == true
            if (!hasHr) return

            // 找到设备，停止扫描，建立连接
            try {
                val bm = context.getSystemService(Context.BLUETOOTH_SERVICE)
                    as? BluetoothManager
                bm?.adapter?.bluetoothLeScanner?.stopScan(this)
            } catch (e: Exception) { }
            scanning = false

            try {
                gatt = r.device.connectGatt(context, true, gattCallback)
            } catch (e: Exception) { }
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                try {
                    g?.discoverServices()
                } catch (e: Exception) { }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt?, status: Int) {
            val service = g?.getService(HR_SERVICE) ?: return
            val ch = service.getCharacteristic(HR_MEASUREMENT) ?: return
            try {
                g.setCharacteristicNotification(ch, true)
                val desc = ch.getDescriptor(CLIENT_CHAR_CONFIG)
                if (desc != null) {
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    g.writeDescriptor(desc)
                }
            } catch (e: Exception) { }
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val c = characteristic ?: return
            if (c.uuid != HR_MEASUREMENT) return
            val value = c.value ?: return
            handleHrValue(value)
        }
    }

    /**
     * 解析心率测量值。
     * 格式：第 0 字节为 flags，bit0 决定心率是 8 位还是 16 位。
     */
    private fun handleHrValue(value: ByteArray) {
        if (value.isEmpty()) return
        val flags = value[0].toInt()
        val hr = if ((flags and 0x01) == 0) {
            // 8 位
            if (value.size < 2) return
            value[1].toInt() and 0xFF
        } else {
            // 16 位
            if (value.size < 3) return
            (value[1].toInt() and 0xFF) or ((value[2].toInt() and 0xFF) shl 8)
        }

        if (hr in 30..220) {
            lastHeartRate = hr
            analyze(hr)
        }
    }

    /**
     * 异常分析。
     *
     * 醉酒时心率通常升高（血管扩张、代谢加快），
     * 但运动和紧张也会升高，因此仅当「静息状态下持续偏高」才上报。
     */
    private fun analyze(hr: Int) {
        recentHr.addLast(hr)
        if (recentHr.size > 20) recentHr.removeFirst()

        // 建立基线：取前 10 次的平均值
        if (baselineHr == 0 && recentHr.size >= 10) {
            baselineHr = recentHr.average().toInt()
        }

        // 判断：相比基线升高 30% 以上，且持续（最近 5 次均偏高）
        if (baselineHr > 0 && recentHr.size >= 5) {
            val recent5 = recentHr.toList().takeLast(5)
            val allElevated = recent5.all { it > baselineHr * 1.3 }
            if (allElevated) {
                val now = System.currentTimeMillis()
                if (now - lastReportAt > 10 * 60 * 1000L) {
                    lastReportAt = now
                    RiskMonitor.onHeartRateAnomaly(
                        "基线 $baselineHr，当前 $hr"
                    )
                }
            }
        }
    }
}
