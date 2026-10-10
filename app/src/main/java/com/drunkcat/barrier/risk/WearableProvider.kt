package com.drunkcat.barrier.risk

/**
 * 可穿戴设备数据源接口。
 *
 * 抽象不同接入方式，让风险评分与具体实现解耦：
 * - BleHeartRateProvider：标准 BLE 心率广播（路径一，已实现）
 * - GadgetbridgeProvider：通过 Gadgetbridge 本地数据库读取（路径三，预留）
 * - MiHealthProvider：小米运动健康开放平台（路径二，预留）
 *
 * 所有实现都必须遵守：
 * - 数据仅本地使用，不上传
 * - 只读，不修改可穿戴设备
 * - 可被随时停止
 */
interface WearableProvider {

    /** 数据源名称（用于 UI 展示） */
    val name: String

    /** 是否有权限访问 */
    fun hasPermission(): Boolean

    /** 开始采集 */
    fun start()

    /** 停止采集 */
    fun stop()

    /**
     * 当前是否已连接/可用。
     * 用于体检页展示状态。
     */
    fun isConnected(): Boolean
}
