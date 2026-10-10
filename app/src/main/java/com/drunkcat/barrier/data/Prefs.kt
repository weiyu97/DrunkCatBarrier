package com.drunkcat.barrier.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "drunckat_prefs")

/**
 * 本地偏好存储。所有数据仅本地。
 */
class Prefs(private val context: Context) {

    private object Keys {
        val PIN = stringPreferencesKey("pin")
        val LOCKED = stringPreferencesKey("locked_packages")
        val WHITELIST = stringPreferencesKey("whitelist_packages")
        val CONTRACT_MODE = stringPreferencesKey("contract_mode")
        val PROTECTION_LEVEL = stringPreferencesKey("protection_level")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PROTECTION_ACTIVE = booleanPreferencesKey("protection_active")
        val PROTECTION_UNTIL = longPreferencesKey("protection_until")
        val AUTO_PROTECT = booleanPreferencesKey("auto_protect")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SKIP_AUTO_UNTIL = longPreferencesKey("skip_auto_until")
        val MAP_ENABLED = booleanPreferencesKey("map_enabled")
        val FAILURE_COUNT = intPreferencesKey("failure_count")
        val COOLDOWN_UNTIL = longPreferencesKey("cooldown_until")
    }

    // ---------- PIN ----------

    suspend fun setPin(pin: String) {
        context.dataStore.edit { it[Keys.PIN] = pin }
    }

    suspend fun getPin(): String? =
        context.dataStore.data.first()[Keys.PIN]

    fun pinFlow(): Flow<String?> =
        context.dataStore.data.map { it[Keys.PIN] }

    // ---------- 锁定包列表 ----------

    suspend fun setLockedPackages(packages: List<String>) {
        context.dataStore.edit { it[Keys.LOCKED] = packages.joinToString(",") }
    }

    suspend fun getLockedPackages(): List<String> =
        (context.dataStore.data.first()[Keys.LOCKED] ?: "")
            .split(",").filter { it.isNotBlank() }

    fun lockedFlow(): Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            (prefs[Keys.LOCKED] ?: "").split(",").filter { it.isNotBlank() }
        }

    // ---------- 白名单 ----------

    suspend fun setWhitelistPackages(packages: List<String>) {
        context.dataStore.edit { it[Keys.WHITELIST] = packages.joinToString(",") }
    }

    suspend fun getWhitelistPackages(): List<String> =
        (context.dataStore.data.first()[Keys.WHITELIST] ?: "")
            .split(",").filter { it.isNotBlank() }

    fun whitelistFlow(): Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            (prefs[Keys.WHITELIST] ?: "").split(",").filter { it.isNotBlank() }
        }

    // ---------- 契约模式 ----------

    suspend fun setContractMode(mode: String) {
        context.dataStore.edit { it[Keys.CONTRACT_MODE] = mode }
    }

    suspend fun getContractMode(): String =
        context.dataStore.data.first()[Keys.CONTRACT_MODE] ?: ContractMode.QUICK.name

    // ---------- 保护级别 ----------

    suspend fun setProtectionLevel(level: String) {
        context.dataStore.edit { it[Keys.PROTECTION_LEVEL] = level }
    }

    suspend fun getProtectionLevel(): String =
        context.dataStore.data.first()[Keys.PROTECTION_LEVEL] ?: ProtectionLevel.L1.name

    // ---------- 主题 ----------

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun getThemeMode(): String =
        context.dataStore.data.first()[Keys.THEME_MODE] ?: "GLASS"

    // ---------- 保护开关（供无障碍服务读取） ----------

    suspend fun setProtectionActive(active: Boolean) {
        context.dataStore.edit { it[Keys.PROTECTION_ACTIVE] = active }
    }

    val protectionFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.PROTECTION_ACTIVE] ?: false }

    // ---------- 保护到期时间 ----------

    /** 设置保护到期时间戳（0 表示不自动解除） */
    suspend fun setProtectionUntil(until: Long) {
        context.dataStore.edit { it[Keys.PROTECTION_UNTIL] = until }
    }

    suspend fun getProtectionUntil(): Long =
        context.dataStore.data.first()[Keys.PROTECTION_UNTIL] ?: 0L

    val protectionUntilFlow: Flow<Long> =
        context.dataStore.data.map { it[Keys.PROTECTION_UNTIL] ?: 0L }

    // ---------- 自动保护开关 ----------

    suspend fun setAutoProtectEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PROTECT] = enabled }
    }

    /** 默认开启自动保护 */
    val autoProtectFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_PROTECT] ?: true }

    // ---------- 首次启动引导 ----------

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    /** 是否已完成引导（默认 false，触发引导） */
    suspend fun isOnboardingDone(): Boolean =
        context.dataStore.data.first()[Keys.ONBOARDING_DONE] ?: false

    // ---------- 今晚不喝（暂停自动检测） ----------

    /** 暂停自动检测到指定时间戳（0 表示不暂停） */
    suspend fun setSkipAutoUntil(until: Long) {
        context.dataStore.edit { it[Keys.SKIP_AUTO_UNTIL] = until }
    }

    suspend fun getSkipAutoUntil(): Long =
        context.dataStore.data.first()[Keys.SKIP_AUTO_UNTIL] ?: 0L

    /** 当前是否处于暂停状态 */
    suspend fun isAutoSkipped(): Boolean {
        val until = getSkipAutoUntil()
        return until > 0L && System.currentTimeMillis() < until
    }

    // ---------- 地图底图开关（默认关闭，纯本地优先） ----------

    suspend fun setMapEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MAP_ENABLED] = enabled }
    }

    /** 默认关闭：保持纯本地，用户主动开启后才下载地图瓦片 */
    val mapEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.MAP_ENABLED] ?: false }

    // ---------- 失败计数与冷却 ----------

    suspend fun getFailureCount(): Int =
        context.dataStore.data.first()[Keys.FAILURE_COUNT] ?: 0

    suspend fun setFailureCount(n: Int) {
        context.dataStore.edit { it[Keys.FAILURE_COUNT] = n }
    }

    suspend fun getCooldownUntil(): Long =
        context.dataStore.data.first()[Keys.COOLDOWN_UNTIL] ?: 0L

    suspend fun setCooldownUntil(ts: Long) {
        context.dataStore.edit { it[Keys.COOLDOWN_UNTIL] = ts }
    }

    /** 验证通过后清零 */
    suspend fun resetFailureState() {
        context.dataStore.edit {
            it[Keys.FAILURE_COUNT] = 0
            it[Keys.COOLDOWN_UNTIL] = 0L
        }
    }
}

enum class ContractMode { QUICK, FULL }

enum class ProtectionLevel { L1, L2, L3 }
