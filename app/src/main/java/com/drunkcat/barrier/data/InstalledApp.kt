package com.drunkcat.barrier.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * 已安装应用查询。
 *
 * 排除自身、系统核心包、无 launcher 入口的应用。
 */
object InstalledApps {

    data class AppInfo(
        val packageName: String,
        val label: String,
        val isSystem: Boolean
    )

    fun list(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = pm.queryIntentActivities(main, 0)

        val seen = mutableSetOf<String>()
        val result = mutableListOf<AppInfo>()

        for (ri in activities) {
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (!seen.add(pkg)) continue

            val ai: ApplicationInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            result.add(AppInfo(pkg, pm.getApplicationLabel(ai).toString(), isSystem))
        }

        return result.sortedBy { it.label.lowercase() }
    }
}
