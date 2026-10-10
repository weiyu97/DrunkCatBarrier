# ============================================================
# 注意：当前 release 构建已关闭 R8（isMinifyEnabled = false）。
# 本文件保留以备将来重新启用混淆时使用。
#
# 若重新开启 R8，以下规则必须保留，否则会丢失功能。
# ============================================================

# ---------- 组件类（反射/系统实例化，绝不能删） ----------

# Device Admin Receiver
-keep class com.drunkcat.barrier.admin.DrunkCatAdminReceiver { *; }

# 无障碍服务
-keep class com.drunkcat.barrier.service.AppWatchService { *; }

# 前台服务
-keep class com.drunkcat.barrier.service.LockService { *; }

# Activity（系统通过 intent 启动）
-keep class com.drunkcat.barrier.MainActivity { *; }
-keep class com.drunkcat.barrier.ui.VerifyActivity { *; }
-keep class com.drunkcat.barrier.ui.WarningActivity { *; }

# 调试广播接收器（若有）
-keep class com.drunkcat.barrier.service.DebugTriggerReceiver { *; }

# ---------- Compose（R8 会破坏运行时和动画） ----------

-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-dontwarn androidx.compose.**

# Compose 编译器生成的 Composable 单例
-keep class **ComposableSingletons$* { *; }

# ---------- Kotlin 协程 ----------

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---------- 数据类（序列化/反射） ----------

-keep class com.drunkcat.barrier.data.** { *; }
-keep class com.drunkcat.barrier.track.** { *; }
-keep class com.drunkcat.barrier.report.** { *; }

# ---------- 枚举（values() 反射） ----------

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- 其他 ----------

# 保留注解
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# 不警告缺失的可选依赖
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlin.**
