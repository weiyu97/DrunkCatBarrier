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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableIntStateOf
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

    // 快捷方式指定的初始页面（Compose 状态，可被 onNewIntent 更新）
    private val initialScreenState =
        androidx.compose.runtime.mutableStateOf<Screen?>(null)

    private val REQUEST_PROVISION_PROFILE = 1001

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

        // 处理快捷方式意图
        handleShortcut(intent)

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
                        initialScreenState = initialScreenState,
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcut(intent)
    }

    /** 处理长按图标快捷方式 */
    private fun handleShortcut(intent: Intent?) {
        when (intent?.action) {
            "com.drunkcat.barrier.START_PROTECTION" -> {
                lifecycleScope.launch {
                    val p = Prefs(this@MainActivity)
                    // 套用默认锁定清单（若尚未配置）
                    if (p.getLockedPackages().isEmpty()) {
                        val installed = packageManager
                            .getInstalledApplications(0)
                            .map { it.packageName }
                        val defaults = com.drunkcat.barrier.data.DefaultLockList
                            .pickDefault(installed)
                        if (defaults.isNotEmpty()) p.setLockedPackages(defaults)
                    }
                    p.setProtectionActive(true)
                    p.setProtectionUntil(
                        com.drunkcat.barrier.data.ContractTime.nextMorning8()
                    )
                    LockService.start(this@MainActivity)
                    toast("保护已开启")
                }
            }
            "com.drunkcat.barrier.OPEN_CHECKUP" -> {
                initialScreenState.value = Screen.CHECKUP
            }
        }
    }

    /**
     * 请求管理权限。
     *
     * 优先引导用户创建工作资料（Profile Owner），
     * 无需电脑、无需删除个人账户，普通用户即可完成。
     */
    private fun requestDeviceAdmin() {
        // 若设备已支持工作资料，直接发起创建
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                ComponentName(this@MainActivity, DrunkCatAdminReceiver::class.java)
            )
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION,
                true
            )
        }
        try {
            startActivityForResult(intent, REQUEST_PROVISION_PROFILE)
        } catch (e: Exception) {
            // 设备不支持工作资料，降级到普通设备管理
            toast("当前设备不支持工作资料，改用基础设备管理")
            requestBasicAdmin()
        }
    }

    /** 基础设备管理（防卸载，能力有限） */
    private fun requestBasicAdmin() {
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
        try {
            startActivity(intent)
        } catch (e: Exception) {
            toast("无法请求设备管理权限")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PROVISION_PROFILE) {
            if (resultCode == RESULT_OK) {
                toast("工作资料已创建，封印就位")
            } else {
                toast("已取消创建")
            }
        }
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
    initialScreenState: androidx.compose.runtime.MutableState<Screen?>,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onRequestAdmin: () -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 体检页：定位权限请求器
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    // 体检页：蓝牙权限请求器（心率手环）
    val bluetoothPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    // 体检页：Device Owner 说明对话框
    var showDeviceOwnerDialog by remember { mutableStateOf(false) }

    var screen by remember { mutableStateOf(initialScreenState.value ?: Screen.HOME) }

    // 快捷方式切换页面时同步
    LaunchedEffect(initialScreenState.value) {
        initialScreenState.value?.let {
            screen = it
            initialScreenState.value = null
        }
    }
    var state by remember { mutableStateOf(manager.buildState()) }
    var apps by remember { mutableStateOf<List<InstalledApps.AppInfo>>(emptyList()) }
    var whitelist by remember { mutableStateOf<Set<String>>(emptySet()) }
    var locked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pin by remember { mutableStateOf<String?>(null) }
    var contractMode by remember { mutableStateOf(ContractMode.QUICK) }
    var protectionActive by remember { mutableStateOf(false) }
    var autoProtectEnabled by remember { mutableStateOf(true) }
    var autoSkipped by remember { mutableStateOf(false) }
    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
    var onboardingStep by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(com.drunkcat.barrier.data.ContractTime.DEFAULT_HOUR) }
    // 待验证的操作：非 null 时显示 PIN 验证界面
    var pendingStop by remember { mutableStateOf(false) }

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
        autoSkipped = try { prefs.isAutoSkipped() } catch (e: Exception) { false }
        onboardingDone = try { prefs.isOnboardingDone() } catch (e: Exception) { true }
        // 首次启动时自动申请通知权限（用于预警）
        android.util.Log.d("DrunkCat", "onboardingDone=$onboardingDone")
    }

    fun refresh() { state = manager.buildState() }

    // ===== 共享动作（在导航分支之前定义，供多处引用）=====

    // 实际执行停止
    val doStop: () -> Unit = {
        scope.launch {
            prefs.setProtectionActive(false)
            protectionActive = false
            com.drunkcat.barrier.risk.RiskMonitor.reset()
            locked.forEach { manager.unhideApp(it) }
            manager.clearAllowedPackages()
            prefs.setProtectionUntil(0L)
            LockService.stop(context)
            refresh()
            onToast("保护已停止")
        }
    }

    // 停止保护：保护中时需过 PIN 验证
    val stopProtection: () -> Unit = {
        if (protectionActive && pin != null) {
            pendingStop = true
        } else {
            doStop()
        }
    }

    // 开启契约
    val startContract: () -> Unit = {
        if (pin == null) {
            onToast("请先设置 PIN")
        } else {
            screen = Screen.CONTRACT
        }
    }

    // 读取偏好期间显示空白，避免首帧闪主页
    if (onboardingDone == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    if (onboardingDone == false) {
        com.drunkcat.barrier.ui.theme.GlassOnboarding(
            step = onboardingStep,
            accessibilityOn = isAccessibilityEnabled(context),
            hasPin = pin != null,
            onOpenAccessibility = { openAccessibilitySettings(context) },
            onSetPin = { screen = Screen.PIN_SET },
            onFinish = {
                if (onboardingStep < 2) {
                    onboardingStep++
                } else {
                    scope.launch {
                        prefs.setOnboardingDone()
                        onboardingDone = true
                    }
                }
            }
        )
        return
    }

    // 停止保护前的 PIN 验证
    if (pendingStop) {
        val expected = pin
        if (expected == null) {
            pendingStop = false
        } else {
            if (themeMode == ThemeMode.GLASS) {
                GlassPinVerifyScreen(
                    expected = expected,
                    onSuccess = {
                        pendingStop = false
                        doStop()
                    },
                    onFailure = {
                        pendingStop = false
                        onToast("PIN 错误，保护继续")
                    },
                    onCancel = { pendingStop = false }
                )
            } else {
                PinVerifyScreen(
                    expected = expected,
                    onSuccess = {
                        pendingStop = false
                        doStop()
                    },
                    onFailure = {
                        pendingStop = false
                        onToast("PIN 错误，保护继续")
                    },
                    onCancel = { pendingStop = false }
                )
            }
            return
        }
    }

    when (screen) {
        Screen.HOME -> {
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
                    autoSkipped = autoSkipped,
                    onToggleSkipTonight = {
                        val next = !autoSkipped
                        autoSkipped = next
                        scope.launch {
                            if (next) {
                                // 暂停到明早 8:00
                                prefs.setSkipAutoUntil(
                                    com.drunkcat.barrier.data.ContractTime.nextMorning8()
                                )
                            } else {
                                prefs.setSkipAutoUntil(0L)
                            }
                        }
                        onToast(if (next) "今晚不再自动检测" else "已恢复自动检测")
                    },
                    onToggleAutoProtect = {
                        val next = !autoProtectEnabled
                        autoProtectEnabled = next
                        scope.launch { prefs.setAutoProtectEnabled(next) }
                        onToast(if (next) "自动检测已开启" else "自动检测已关闭")
                    },
                    accessibilityOn = isAccessibilityEnabled(context),
                    notificationOk = com.drunkcat.barrier.service.HealthCheck
                        .check(context).notificationOk,
                    onOpenAccessibility = { openAccessibilitySettings(context) },
                    onNavContract = startContract,
                    onNavTrial = { screen = Screen.VERIFICATION },
                    onNavReview = { screen = Screen.REVIEW },
                    onOpenCheckup = { screen = Screen.CHECKUP }
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
                    initialMode = contractMode,
                    initialEndHour = endHour,
                    onConfirm = { chosenMode, chosenHour ->
                        scope.launch {
                            contractMode = chosenMode
                            endHour = chosenHour
                            prefs.setContractMode(chosenMode.name)
                            // 开启保护开关，AppWatchService 据此拦截
                            prefs.setProtectionActive(true)
                            protectionActive = true
                            // 设置到期时间
                            prefs.setProtectionUntil(
                                com.drunkcat.barrier.data.ContractTime.nextAt(chosenHour)
                            )
                            // 管理路径（Device Owner 或 Profile Owner）：
                            // 1. 设置 LockTask 允许名单
                            // 2. 隐藏被锁应用
                            if (state.hasManagement) {
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
                    endTimeLabel = com.drunkcat.barrier.data.ContractTime.label(
                        com.drunkcat.barrier.data.ContractTime.nextAt(endHour)
                    ),
                    onConfirm = {
                        scope.launch {
                            prefs.setContractMode(contractMode.name)
                            prefs.setProtectionActive(true)
                            protectionActive = true
                            prefs.setProtectionUntil(
                                com.drunkcat.barrier.data.ContractTime.nextAt(endHour)
                            )
                            if (state.hasManagement) {
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

        Screen.CHECKUP -> {
            val health = com.drunkcat.barrier.service.HealthCheck.check(context)
            val a11yOn = isAccessibilityEnabled(context)

            // 心率实时读取（体检页停留期间每秒刷新）
            var hr by remember { mutableIntStateOf(0) }
            var hrConnected by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                while (true) {
                    hr = com.drunkcat.barrier.service.AppWatchService.currentHeartRate
                    hrConnected = com.drunkcat.barrier.service.AppWatchService.heartRateConnected
                    kotlinx.coroutines.delay(1000)
                }
            }
            com.drunkcat.barrier.ui.theme.GlassCheckupScreen(
                items = listOf(
                    com.drunkcat.barrier.ui.theme.CheckupItem(
                        title = "无障碍服务",
                        desc = if (a11yOn) "已开启，可检测前台应用"
                        else "未开启，自动保护无法生效",
                        ok = a11yOn,
                        critical = true,
                        action = { openAccessibilitySettings(context) }
                    ),
                    com.drunkcat.barrier.ui.theme.CheckupItem(
                        title = "通知权限",
                        desc = if (health.notificationOk) "已授权，预警可正常提醒"
                        else "未授权，30 秒预警将静默失效",
                        ok = health.notificationOk,
                        critical = false,
                        action = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                try {
                                    context.startActivity(
                                        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(
                                                android.provider.Settings.EXTRA_APP_PACKAGE,
                                                context.packageName
                                            )
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (e: Exception) { }
                            }
                        }
                    ),
                    com.drunkcat.barrier.ui.theme.CheckupItem(
                        title = "清醒试炼 PIN",
                        desc = if (pin != null) "已设置"
                        else "未设置，无法阻止他人解除保护",
                        ok = pin != null,
                        critical = true,
                        action = { screen = Screen.PIN_SET }
                    ),
                    com.drunkcat.barrier.ui.theme.CheckupItem(
                        title = "封印名单",
                        desc = if (locked.isNotEmpty()) "已配置 ${locked.size} 个应用"
                        else "未配置，可套用默认清单",
                        ok = locked.isNotEmpty(),
                        critical = false,
                        action = { screen = Screen.APP_PICKER }
                    ),
                    com.drunkcat.barrier.ui.theme.CheckupItem(
                        title = "定位权限",
                        desc = if (health.locationOk) "已授权，可记录轨迹"
                        else "未授权，无法记录轨迹",
                        ok = health.locationOk,
                        critical = false,
                        action = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ),
                    com.drunkcat.barrier.ui.theme.CheckupItem(
                        title = "电池优化白名单",
                        desc = if (health.batteryOptimized) "已加入，服务可常驻"
                        else "未加入，后台可能被系统杀死",
                        ok = health.batteryOptimized,
                        critical = false,
                        action = {
                            try {
                                context.startActivity(
                                    Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (e: Exception) { }
                        }
                    ),
                    com.drunkcat.barrier.ui.theme.CheckupItem(
                        title = "心率手环",
                        desc = when {
                            hrConnected && hr > 0 -> "已连接，当前心率 $hr BPM"
                            health.bluetoothOk -> "已授权。请在手环上开启心率广播，App 会自动连接"
                            else -> "未授权，可选功能（用于心率辅助判断）"
                        },
                        ok = hrConnected && hr > 0,
                        critical = false,
                        action = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                bluetoothPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_SCAN,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    )
                                )
                            }
                        }
                    ),
                    com.drunkcat.barrier.ui.theme.CheckupItem(
                        title = "Device Owner 完整封印",
                        desc = if (state.isDeviceOwner) "已激活，最强锁定能力"
                        else "未激活，当前使用无障碍降级模式",
                        ok = state.isDeviceOwner,
                        critical = false,
                        action = { showDeviceOwnerDialog = true }
                    )
                ),
                onBack = { screen = Screen.HOME }
            )
        }
    }

    // Device Owner 激活说明对话框
    if (showDeviceOwnerDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeviceOwnerDialog = false },
            title = {
                Text(
                    "激活 Device Owner 完整封印",
                    fontFamily = FontFamily.Serif
                )
            },
            text = {
                Text(
                    "需要电脑配合，通过 ADB 命令激活：\n\n" +
                    "1. 确保设备无任何账户\n" +
                    "2. 开启 USB 调试\n" +
                    "3. 电脑上执行：\n\n" +
                    "adb shell dpm set-device-owner\n" +
                    "com.drunkcat.barrier/.admin.DrunkCatAdminReceiver\n\n" +
                    "激活后解锁最强锁定能力：LockTask 模式、隐藏应用、防卸载。",
                    fontFamily = FontFamily.Serif
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDeviceOwnerDialog = false }
                ) {
                    Text("知道了", fontFamily = FontFamily.Serif)
                }
            }
        )
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
