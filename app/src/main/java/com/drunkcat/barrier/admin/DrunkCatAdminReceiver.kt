package com.drunkcat.barrier.admin

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DrunkCatAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "设备管理权限已激活", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "设备管理权限已停用", Toast.LENGTH_SHORT).show()
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        // 工作资料创建完成，确认 Profile Owner 身份
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as DevicePolicyManager
            val admin = android.content.ComponentName(context, DrunkCatAdminReceiver::class.java)
            if (dpm.isProfileOwnerApp(context.packageName)) {
                Toast.makeText(context, "工作资料已创建，封印就位", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) { }
    }
}
