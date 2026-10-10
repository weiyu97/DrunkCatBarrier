package com.drunkcat.barrier.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager

/**
 * 权限管理器：Device Owner / Profile Owner / 无障碍降级。
 *
 * 三种模式的定位：
 * - **Profile Owner**（推荐给普通用户）：创建工作资料，无需删除个人账户，
 *   可管理工作资料内的应用。门槛最低。
 * - **Device Owner**（高级用户）：最强锁定，但要求设备无账户。
 * - **无障碍降级**（兜底）：所有设备可用，通过无障碍服务拦截。
 */
class DeviceOwnerManager(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(context, DrunkCatAdminReceiver::class.java)

    // ---------- 状态 ----------

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    /** 本应用是否为某个资料的 Profile Owner */
    fun isProfileOwner(): Boolean {
        return try {
            dpm.isProfileOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    /** 当前用户是否属于工作资料 */
    fun isInWorkProfile(): Boolean {
        return try {
            val um = context.getSystemService(Context.USER_SERVICE) as UserManager
            um.isManagedProfile
        } catch (e: Exception) {
            false
        }
    }

    fun isAdminActive(): Boolean = dpm.isAdminActive(admin)

    private fun hasNoAccounts(): Boolean =
        android.accounts.AccountManager.get(context).accounts.isEmpty()

    fun getActivationCommand(): String =
        "adb shell dpm set-device-owner ${context.packageName}/.admin.DrunkCatAdminReceiver"

    /** Profile Owner 的 ADB 激活命令（无需无账户条件） */
    fun getProfileOwnerCommand(): String =
        "adb shell dpm set-profile-owner ${context.packageName}/.admin.DrunkCatAdminReceiver"

    fun buildState(): DeviceState = DeviceState(
        isDeviceOwner = isDeviceOwner(),
        isProfileOwner = isProfileOwner(),
        isInWorkProfile = isInWorkProfile(),
        isAdminActive = isAdminActive(),
        hasAccounts = !hasNoAccounts(),
        sdkInt = Build.VERSION.SDK_INT,
        activationCommand = getActivationCommand(),
        profileOwnerCommand = getProfileOwnerCommand(),
        lockedPackages = getAllowedPackages(),
        safeBootDisabled = isUserRestrictionSet(UserManager.DISALLOW_SAFE_BOOT),
        factoryResetDisabled = isUserRestrictionSet(UserManager.DISALLOW_FACTORY_RESET),
        inLockTaskMode = isInLockTaskMode()
    )

    // ---------- LockTask 允许名单 ----------

    /**
     * 设置 LockTask 模式下**允许**运行的应用（白名单）。
     *
     * 注意：setLockTaskPackages 接收的是允许名单，不是禁止名单。
     * 禁止的应用应通过 setApplicationHidden 隐藏。
     *
     * 白名单应包含：本应用 + 永久白名单（电话/地图/打车等）+ 用户自定义白名单。
     */
    /** 是否具备管理权限（Device Owner 或 Profile Owner） */
    private fun hasManagement(): Boolean = isDeviceOwner() || isProfileOwner()

    fun setAllowedPackages(packages: List<String>) {
        if (!hasManagement()) return
        try {
            dpm.setLockTaskPackages(admin, packages.toTypedArray())
        } catch (e: Exception) { }
    }

    fun clearAllowedPackages() {
        if (!hasManagement()) return
        try {
            dpm.setLockTaskPackages(admin, emptyArray())
        } catch (e: Exception) { }
    }

    fun getAllowedPackages(): List<String> {
        if (!hasManagement()) return emptyList()
        return try {
            dpm.getLockTaskPackages(admin).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun hideApp(packageName: String): Boolean {
        if (!hasManagement()) return false
        return try {
            dpm.setApplicationHidden(admin, packageName, true)
        } catch (e: Exception) {
            false
        }
    }

    fun unhideApp(packageName: String): Boolean {
        if (!hasManagement()) return false
        return try {
            dpm.setApplicationHidden(admin, packageName, false)
        } catch (e: Exception) {
            false
        }
    }

    fun isAppHidden(packageName: String): Boolean {
        if (!hasManagement()) return false
        return try {
            dpm.isApplicationHidden(admin, packageName)
        } catch (e: Exception) {
            false
        }
    }

    // ---------- 用户限制 ----------

    fun disableSafeBoot() {
        if (!isDeviceOwner()) return
        dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
    }

    fun enableSafeBoot() {
        if (!isDeviceOwner()) return
        dpm.clearUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
    }

    fun disableFactoryReset() {
        if (!isDeviceOwner()) return
        dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
    }

    fun enableFactoryReset() {
        if (!isDeviceOwner()) return
        dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
    }

    private fun isUserRestrictionSet(key: String): Boolean {
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        return um.userRestrictions.containsKey(key)
    }

    // ---------- LockTask 模式 ----------

    fun isInLockTaskMode(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.lockTaskModeState != android.app.ActivityManager.LOCK_TASK_MODE_NONE
        } else false
    }

    // ---------- 判断某 App 是否可被锁定 ----------

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

data class DeviceState(
    val isDeviceOwner: Boolean,
    val isProfileOwner: Boolean,
    val isInWorkProfile: Boolean,
    val isAdminActive: Boolean,
    val hasAccounts: Boolean,
    val sdkInt: Int,
    val activationCommand: String,
    val profileOwnerCommand: String,
    val lockedPackages: List<String>,
    val safeBootDisabled: Boolean,
    val factoryResetDisabled: Boolean,
    val inLockTaskMode: Boolean
) {
    /** 是否拥有任一管理权限 */
    val hasManagement: Boolean get() = isDeviceOwner || isProfileOwner

    /** 当前模式描述 */
    val modeLabel: String
        get() = when {
            isDeviceOwner -> "Device Owner"
            isProfileOwner -> "Profile Owner"
            else -> "无障碍降级"
        }
}
