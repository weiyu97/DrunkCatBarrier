package com.drunkcat.barrier

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.admin.DeviceOwnerManager
import com.drunkcat.barrier.admin.DeviceState
import com.drunkcat.barrier.admin.DrunkCatAdminReceiver
import com.drunkcat.barrier.data.ContractMode
import com.drunkcat.barrier.data.InstalledApps
import com.drunkcat.barrier.data.Prefs
import com.drunkcat.barrier.service.LockService
import com.drunkcat.barrier.ui.AppPickerScreen
import com.drunkcat.barrier.ui.ContractScreen
import com.drunkcat.barrier.ui.PinSetScreen
import com.drunkcat.barrier.ui.PinVerifyScreen
import com.drunkcat.barrier.ui.Screen
import com.drunkcat.barrier.ui.WhitelistScreen
import com.drunkcat.barrier.ui.theme.DrunkCatTheme
import com.drunkcat.barrier.ui.theme.GlassAppListScreen
import com.drunkcat.barrier.ui.theme.GlassContractScreen
import com.drunkcat.barrier.ui.theme.GlassPinSetScreen
import com.drunkcat.barrier.ui.theme.GlassPinVerifyScreen
import com.drunkcat.barrier.ui.theme.GlassReviewScreen
import com.drunkcat.barrier.ui.theme.GlassVerificationFlow
import com.drunkcat.barrier.ui.theme.ThemeExtras
import com.drunkcat.barrier.ui.theme.ThemeMode
import com.drunkcat.barrier.ui.theme.UiIcon
import com.drunkcat.barrier.ui.theme.UiIconType
import com.drunkcat.barrier.verify.CognitiveVerifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var manager: DeviceOwnerManager
    private lateinit var prefs: Prefs

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = DeviceOwnerManager(this)
        prefs = Prefs(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // 请求定位权限（用于保护期间的轨迹记录）
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.GLASS) }
            LaunchedEffect(Unit) {
                themeMode = try {
                    ThemeMode.valueOf(prefs.getThemeMode())
                } catch (e: Exception) {
                    ThemeMode.GLASS
                }
            }
            DrunkCatTheme(mode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(
                        manager = manager,
                        prefs = prefs,
                        themeMode = themeMode,
                        onToggleTheme = {
                            val next = if (themeMode == ThemeMode.GLASS)
                                ThemeMode.MINIMAL else ThemeMode.GLASS
                            themeMode = next
                            this@MainActivity.lifecycleScope.launch {
                                prefs.setThemeMode(next.name)
                            }
                        },
                        onRequestAdmin = { requestDeviceAdmin() },
                        onToast = { toast(it) }
                    )
                }
            }
        }
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(this@MainActivity, DrunkCatAdminReceiver::class.java)
            )
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "醉猫结界需要设备管理权限以防止被卸载。"
            )
        }
        startActivity(intent)
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

/**
 * 构建 LockTask 允许名单：
 * 本应用 + 永久白名单 + 用户自定义白名单
 */
private fun buildAllowedPackages(
    context: Context,
    userWhitelist: Set<String>
): List<String> {
    val result = mutableSetOf(context.packageName)
    result.addAll(com.drunkcat.barrier.data.PermanentWhitelist.PACKAGES)
    result.addAll(userWhitelist)
    return result.toList()
}

/** 打开系统无障碍设置页 */
private fun openAccessibilitySettings(context: Context) {
    try {
        context.startActivity(
            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) { }
}

/** 检测本应用的无障碍服务是否已开启 */
private fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(":").any {
        it.contains(context.packageName) && it.contains("AppWatchService")
    }
}

@Composable
private fun AppRoot(
    manager: DeviceOwnerManager,
    prefs: Prefs,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onRequestAdmin: () -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.HOME) }
    var state by remember { mutableStateOf(manager.buildState()) }
    var apps by remember { mutableStateOf<List<InstalledApps.AppInfo>>(emptyList()) }
    var whitelist by remember { mutableStateOf<Set<String>>(emptySet()) }
    var locked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pin by remember { mutableStateOf<String?>(null) }
    var contractMode by remember { mutableStateOf(ContractMode.QUICK) }
    var protectionActive by remember { mutableStateOf(false) }
    var autoProtectEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        apps = InstalledApps.list(context)
        whitelist = prefs.getWhitelistPackages().toSet()
        locked = prefs.getLockedPackages().toSet()
        pin = prefs.getPin()
        contractMode = try {
            ContractMode.valueOf(prefs.getContractMode())
        } catch (e: Exception) {
            ContractMode.QUICK
        }
        protectionActive = try {
            prefs.protectionFlow.first()
        } catch (e: Exception) { false }
        autoProtectEnabled = try {
            prefs.autoProtectFlow.first()
        } catch (e: Exception) { true }
    }

    fun refresh() { state = manager.buildState() }

    when (screen) {
        Screen.HOME -> {
            val startContract: () -> Unit = {
                if (pin == null) {
                    onToast("请先设置 PIN")
                } else {
                    screen = Screen.CONTRACT
                }
            }
            val stopProtection: () -> Unit = {
                scope.launch {
                    prefs.setProtectionActive(false)
                    protectionActive = false
                    // 清空风险信号，避免刚解除又被自动触发
                    com.drunkcat.barrier.risk.RiskMonitor.reset()
                    // 恢复被隐藏的应用 + 清空 LockTask 允许名单
                    locked.forEach { manager.unhideApp(it) }
                    manager.clearAllowedPackages()
                    prefs.setProtectionUntil(0L)
                    LockService.stop(context)
                    refresh()
                    onToast("保护已停止")
                }
            }

            if (themeMode == ThemeMode.GLASS) {
                com.drunkcat.barrier.ui.theme.GlassHomeScreen(
                    state = state,
                    appsCount = apps.size,
                    lockedCount = locked.size,
                    whitelistCount = whitelist.size,
                    hasPin = pin != null,
                    themeMode = themeMode,
                    onToggleTheme = onToggleTheme,
                    onRefresh = { refresh() },
                    onRequestAdmin = onRequestAdmin,
                    onCopyCommand = {
                        val cm = context
                            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("adb", state.activationCommand))
                        onToast("命令已复制")
                    },
                    onPickApps = { screen = Screen.APP_PICKER },
                    onWhitelist = { screen = Screen.WHITELIST },
                    onSetPin = { screen = Screen.PIN_SET },
                    onStartContract = startContract,
                    onTestVerify = { screen = Screen.VERIFICATION },
                    onStopProtection = stopProtection,
                    protectionActive = protectionActive,
                    autoProtectEnabled = autoProtectEnabled,
                    onToggleAutoProtect = {
                        val next = !autoProtectEnabled
                        autoProtectEnabled = next
                        scope.launch { prefs.setAutoProtectEnabled(next) }
                        onToast(if (next) "自动检测已开启" else "自动检测已关闭")
                    },
                    accessibilityOn = isAccessibilityEnabled(context),
                    onOpenAccessibility = { openAccessibilitySettings(context) },
                    onNavContract = startContract,
                    onNavTrial = { screen = Screen.VERIFICATION },
                    onNavReview = { screen = Screen.REVIEW }
                )
            } else {
                HomeScreen(
                    state = state,
                    appsCount = apps.size,
                    lockedCount = locked.size,
                    whitelistCount = whitelist.size,
                    hasPin = pin != null,
                    themeMode = themeMode,
                    onToggleTheme = onToggleTheme,
                    onRefresh = { refresh() },
                    onRequestAdmin = onRequestAdmin,
                    onCopyCommand = {
                        val cm = context
                            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("adb", state.activationCommand))
                        onToast("命令已复制")
                    },
                    onPickApps = { screen = Screen.APP_PICKER },
                    onWhitelist = { screen = Screen.WHITELIST },
                    onSetPin = { screen = Screen.PIN_SET },
                    onStartContract = startContract,
                    onTestVerify = { screen = Screen.VERIFICATION },
                    onStopProtection = stopProtection
                )
            }
        }

        Screen.APP_PICKER -> {
            if (themeMode == ThemeMode.GLASS) {
                GlassAppListScreen(
                    title = "封印名单",
                    subtitle = "选择要被结界锁定的应用。醉酒后打开它们需通过试炼。",
                    apps = apps,
                    initialSelected = locked,
                    onSave = { sel ->
                        scope.launch {
                            locked = sel
                            prefs.setLockedPackages(sel.toList())
                            screen = Screen.HOME
                        }
                    },
                    onBack = { screen = Screen.HOME }
                )
            } else {
                AppPickerScreen(
                    apps = apps,
                    initialLocked = locked,
                    onSave = { sel ->
                        scope.launch {
                            locked = sel
                            prefs.setLockedPackages(sel.toList())
                            screen = Screen.HOME
                        }
                    },
                    onBack = { screen = Screen.HOME }
                )
            }
        }

        Screen.WHITELIST -> {
            if (themeMode == ThemeMode.GLASS) {
                GlassAppListScreen(
                    title = "结界白名单",
                    subtitle = "这些应用在结界开启期间仍可自由使用。",
                    apps = apps,
                    initialSelected = whitelist,
                    onSave = { sel ->
                        scope.launch {
                            whitelist = sel
                            prefs.setWhitelistPackages(sel.toList())
                            screen = Screen.HOME
                        }
                    },
                    onBack = { screen = Screen.HOME }
                )
            } else {
                WhitelistScreen(
                    apps = apps,
                    initialWhitelist = whitelist,
                    permanentPackages = emptySet(),
                    onSave = { sel ->
                        scope.launch {
                            whitelist = sel
                            prefs.setWhitelistPackages(sel.toList())
                            screen = Screen.HOME
                        }
                    },
                    onBack = { screen = Screen.HOME }
                )
            }
        }

        Screen.PIN_SET -> {
            if (themeMode == ThemeMode.GLASS) {
                GlassPinSetScreen(
                    onSet = { newPin ->
                        scope.launch {
                            prefs.setPin(newPin)
                            pin = newPin
                            screen = Screen.HOME
                            onToast("PIN 已设置")
                        }
                    },
                    onCancel = { screen = Screen.HOME }
                )
            } else {
                PinSetScreen(
                    onSet = { newPin ->
                        scope.launch {
                            prefs.setPin(newPin)
                            pin = newPin
                            screen = Screen.HOME
                            onToast("PIN 已设置")
                        }
                    },
                    onCancel = { screen = Screen.HOME }
                )
            }
        }

        Screen.PIN_VERIFY -> {
            val expected = pin
            if (expected == null) {
                screen = Screen.HOME
            } else {
                if (themeMode == ThemeMode.GLASS) {
                    GlassPinVerifyScreen(
                        expected = expected,
                        onSuccess = { onToast("验证通过"); screen = Screen.HOME },
                        onFailure = { onToast("PIN 错误 3 次"); screen = Screen.HOME },
                        onCancel = { screen = Screen.HOME }
                    )
                } else {
                    PinVerifyScreen(
                        expected = expected,
                        onSuccess = { onToast("验证通过"); screen = Screen.HOME },
                        onFailure = { onToast("PIN 错误 3 次"); screen = Screen.HOME },
                        onCancel = { screen = Screen.HOME }
                    )
                }
            }
        }

        Screen.CONTRACT -> {
            if (themeMode == ThemeMode.GLASS) {
                GlassContractScreen(
                    endTimeLabel = "明早 8:00",
                    initialMode = contractMode,
                    onConfirm = { chosenMode ->
                        scope.launch {
                            contractMode = chosenMode
                            prefs.setContractMode(chosenMode.name)
                            // 开启保护开关，AppWatchService 据此拦截
                            prefs.setProtectionActive(true)
                            protectionActive = true
                            // 设置到期时间：下一个早上 8:00
                            prefs.setProtectionUntil(
                                com.drunkcat.barrier.data.ContractTime.nextMorning8()
                            )
                            // Device Owner 路径：
                            // 1. 设置 LockTask 允许名单（本应用 + 永久白名单 + 用户白名单）
                            // 2. 隐藏被锁应用
                            if (state.isDeviceOwner) {
                                val allowed = buildAllowedPackages(context, whitelist)
                                manager.setAllowedPackages(allowed)
                                locked.forEach { manager.hideApp(it) }
                            }
                            LockService.start(context)
                            refresh()
                            screen = Screen.HOME
                            onToast("契约已缔结，保护已开启")
                        }
                    },
                    onCancel = { screen = Screen.HOME }
                )
            } else {
                ContractScreen(
                    mode = contractMode,
                    endTimeLabel = "明早 8:00",
                    onConfirm = {
                        scope.launch {
                            prefs.setContractMode(contractMode.name)
                            if (state.isDeviceOwner) {
                                val allowed = buildAllowedPackages(context, whitelist)
                                manager.setAllowedPackages(allowed)
                                locked.forEach { manager.hideApp(it) }
                            }
                            LockService.start(context)
                            refresh()
                            screen = Screen.HOME
                            onToast("保护已开启")
                        }
                    },
                    onCancel = { screen = Screen.HOME }
                )
            }
        }

        Screen.VERIFICATION -> {
            GlassVerificationFlow(
                verifier = CognitiveVerifier(),
                onResult = { passed, reason ->
                    onToast(if (passed) "试炼通过" else "试炼失败：$reason")
                    screen = Screen.HOME
                }
            )
        }

        Screen.REVIEW -> {
            GlassReviewScreen()
        }
    }
}

@Composable
private fun HomeScreen(
    state: DeviceState,
    appsCount: Int,
    lockedCount: Int,
    whitelistCount: Int,
    hasPin: Boolean,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onRequestAdmin: () -> Unit,
    onCopyCommand: () -> Unit,
    onPickApps: () -> Unit,
    onWhitelist: () -> Unit,
    onSetPin: () -> Unit,
    onStartContract: () -> Unit,
    onTestVerify: () -> Unit,
    onStopProtection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = com.drunkcat.barrier.R.drawable.logo_app
                ),
                contentDescription = "醉猫结界",
                modifier = Modifier
                    .size(56.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("醉猫结界", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "清醒时设一道门，醉酒时少一次社死",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusRow("Device Owner", state.isDeviceOwner)
                StatusRow("设备管理已激活", state.isAdminActive)
                Text("已安装应用：$appsCount", fontSize = 14.sp)
                Text("已锁定应用：$lockedCount", fontSize = 14.sp)
                Text("自定义白名单：$whitelistCount", fontSize = 14.sp)
                StatusRow("已设置 PIN", hasPin)
                Text("LockTask 模式：${state.inLockTaskMode}", fontSize = 14.sp)
                Text("Android SDK：${state.sdkInt}", fontSize = 14.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                Text("刷新状态")
            }
            OutlinedButton(
                onClick = onToggleTheme,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (themeMode == ThemeMode.GLASS) "切简约" else "切琉璃")
            }
        }

        if (!state.isDeviceOwner) {
            Button(onClick = onRequestAdmin, modifier = Modifier.fillMaxWidth()) {
                Text("请求设备管理权限")
            }
            Text(
                text = state.activationCommand,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        ThemeExtras.codeBg(themeMode),
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            )
            Button(onClick = onCopyCommand, modifier = Modifier.fillMaxWidth()) {
                Text("复制激活命令")
            }
        }

        Text("配置", style = MaterialTheme.typography.titleSmall)
        Button(onClick = onPickApps, modifier = Modifier.fillMaxWidth()) {
            Text("选择要锁定的 App（$lockedCount）")
        }
        Button(onClick = onWhitelist, modifier = Modifier.fillMaxWidth()) {
            Text("白名单（$whitelistCount）")
        }
        Button(onClick = onSetPin, modifier = Modifier.fillMaxWidth()) {
            Text(if (hasPin) "修改 PIN" else "设置 PIN")
        }

        Text("运行", style = MaterialTheme.typography.titleSmall)
        Button(onClick = onStartContract, modifier = Modifier.fillMaxWidth()) {
            Text("开启喝酒模式")
        }
        OutlinedButton(onClick = onStopProtection, modifier = Modifier.fillMaxWidth()) {
            Text("停止保护")
        }
        OutlinedButton(onClick = onTestVerify, modifier = Modifier.fillMaxWidth()) {
            Text("测试清醒验证")
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        UiIcon(
            type = if (ok) UiIconType.CHECK else UiIconType.CROSS,
            size = 16.dp,
            tint = if (ok) Color(0xFF34C759) else Color(0xFFFF3B5C)
        )
        Text(
            text = "  $label",
            fontSize = 15.sp
        )
    }
}
