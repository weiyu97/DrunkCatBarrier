# 保留 Device Admin Receiver
-keep class com.drunkcat.barrier.admin.DrunkCatAdminReceiver { *; }

# 保留无障碍服务
-keep class com.drunkcat.barrier.service.AppWatchService { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin 协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
