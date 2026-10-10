package com.drunkcat.barrier.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager

/**
 * Device Owner 管理：状态检测、激活引导、应用锁定。
 */
class DeviceOwnerManager(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(context, DrunkCatAdminReceiver::class.java)

    // ---------- 状态 ----------

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    fun isAdminActive(): Boolean = dpm.isAdminActive(admin)

    private fun hasNoAccounts(): Boolean =
        android.accounts.AccountManager.get(context).accounts.isEmpty()

    fun getActivationCommand(): String =
        "adb shell dpm set-device-owner ${context.packageName}/.admin.DrunkCatAdminReceiver"

    fun buildState(): DeviceState = DeviceState(
        isDeviceOwner = isDeviceOwner(),
        isAdminActive = isAdminActive(),
        hasAccounts = !hasNoAccounts(),
        sdkInt = Build.VERSION.SDK_INT,
        activationCommand = getActivationCommand(),
        lockedPackages = getLockedPackages(),
        safeBootDisabled = isUserRestrictionSet(UserManager.DISALLOW_SAFE_BOOT),
        factoryResetDisabled = isUserRestrictionSet(UserManager.DISALLOW_FACTORY_RESET),
        inLockTaskMode = isInLockTaskMode()
    )

    // ---------- 应用锁定 ----------

    fun setLockedPackages(packages: List<String>) {
        if (!isDeviceOwner()) return
        dpm.setLockTaskPackages(admin, packages.toTypedArray())
    }

    fun clearLockedPackages() {
        if (!isDeviceOwner()) return
        dpm.setLockTaskPackages(admin, emptyArray())
    }

    fun getLockedPackages(): List<String> {
        if (!isDeviceOwner()) return emptyList()
        return dpm.getLockTaskPackages(admin).toList()
    }

    fun hideApp(packageName: String): Boolean {
        if (!isDeviceOwner()) return false
        return dpm.setApplicationHidden(admin, packageName, true)
    }

    fun unhideApp(packageName: String): Boolean {
        if (!isDeviceOwner()) return false
        return dpm.setApplicationHidden(admin, packageName, false)
    }

    fun isAppHidden(packageName: String): Boolean {
        if (!isDeviceOwner()) return false
        return dpm.isApplicationHidden(admin, packageName)
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
    val isAdminActive: Boolean,
    val hasAccounts: Boolean,
    val sdkInt: Int,
    val activationCommand: String,
    val lockedPackages: List<String>,
    val safeBootDisabled: Boolean,
    val factoryResetDisabled: Boolean,
    val inLockTaskMode: Boolean
)
