package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.admin.DeviceState
import com.drunkcat.barrier.data.ProtectionLevel
import com.drunkcat.barrier.risk.RiskMonitor
import com.drunkcat.barrier.service.WarningState
import com.drunkcat.barrier.track.TrackStore
import kotlinx.coroutines.delay

/**
 * 琉璃风首页（祭坛）。
 *
 * 布局：
 * - GlassHeader（极光 + 魔法阵 + Logo）
 * - 结界状态祭坛卡片
 * - 结界戒律健康检测卡片
 * - 配置卡片（应用选择 / 白名单 / PIN / 主题）
 * - 运行卡片（开启 / 停止 / 测试验证）
 * - 底部导航
 */
@Composable
fun GlassHomeScreen(
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
    onStopProtection: () -> Unit,
    protectionActive: Boolean,
    onShowLockScreen: () -> Unit,
    autoProtectEnabled: Boolean,
    autoSkipped: Boolean,
    onToggleAutoProtect: () -> Unit,
    onToggleSkipTonight: () -> Unit,
    accessibilityOn: Boolean,
    notificationOk: Boolean,
    onOpenAccessibility: () -> Unit,
    onNavContract: () -> Unit,
    onNavTrial: () -> Unit,
    onNavReview: () -> Unit,
    onOpenCheckup: () -> Unit,
    levelName: String,
    onLevelChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05030C), Color(0xFF0C0618), Color(0xFF080412))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassHeader(modifier = Modifier.zIndex(2f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 13.dp)
            ) {
                // ========== 结界状态祭坛 ==========
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = Color(0x59FF4678)
                ) {
                    CardTitle("结界状态祭坛")

                    // ========== 风险分环形读数（每秒刷新） ==========
                    var riskScore by remember { mutableIntStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            riskScore = RiskMonitor.currentScore()
                            delay(1000)
                        }
                    }

                    val ringColor = when {
                        riskScore >= 60 -> Color(0xFFFF4678)        // 红
                        riskScore >= 40 -> Color(0xFFFFD24D)        // 黄
                        riskScore > 0   -> Color(0xFF6DFFB0)        // 绿
                        else            -> Glass.Primary
                    }
                    val textColor = when {
                        riskScore >= 60 -> Color(0xFFFF4678)
                        riskScore >= 40 -> Color(0xFFFFD24D)
                        riskScore > 0   -> Color(0xFFFFFFFF)
                        else            -> Color(0xFF888899)
                    }
                    val hintText = when {
                        riskScore >= 60 -> "⚠ 接近触发，建议手动开启"
                        riskScore >= 40 -> "风险中等，注意节奏"
                        riskScore > 0   -> "有轻度信号"
                        else            -> "当前无风险"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.size(52.dp)) {
                            val strokeWidth = 5.dp.toPx()
                            val ringSize = size.minDimension - strokeWidth
                            val topLeft = Offset(
                                (size.width - ringSize) / 2f,
                                (size.height - ringSize) / 2f
                            )
                            val rect = Size(ringSize, ringSize)
                            // 背景环
                            drawArc(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0x22FFFFFF), Color(0x22FFFFFF))
                                ),
                                size = rect,
                                topLeft = Offset(topLeft.x, topLeft.y),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            // 前景环
                            val sweep = (riskScore / 100f) * 360f
                            drawArc(
                                brush = Brush.horizontalGradient(listOf(ringColor, ringColor)),
                                size = rect,
                                topLeft = Offset(topLeft.x, topLeft.y),
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                        }

                        Spacer(Modifier.size(12.dp))

                        Column {
                            Text(
                                text = "$riskScore / 100",
                                color = Glass.Primary,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                lineHeight = 24.sp
                            )
                            Text(
                                text = hintText,
                                color = textColor,
                                fontFamily = FontFamily.Serif,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // ========== 预警取消后的手动提醒横条 ==========
                    var needManual by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            needManual = WarningState.cancelledThisSession
                            delay(500)
                        }
                    }

                    if (needManual && !protectionActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x33FF4678))
                                .padding(horizontal = 10.dp, vertical = 9.dp)
                                .clickable { onStartContract() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        Color(0xFFFF4678),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(
                                text = "刚才检测到风险，要现在手动开启保护吗？",
                                color = Color(0xFFFFCCCC),
                                fontFamily = FontFamily.Serif,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "→",
                                color = Color(0xFFFF4678),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // 已缔结：有 PIN 且（有管理权限 或 无障碍降级已开启）
                    val sealed = hasPin && (state.hasManagement || protectionActive)
                    Box(modifier = Modifier.padding(bottom = 9.dp)) {
                        PillTag(
                            text = if (sealed) "† 结界已缔结 †" else "† 结界尚未缔结 †",
                            color = if (sealed) Color(0xFF6DFFB0) else Glass.Primary
                        )
                    }

                    Text(
                        text = if (sealed)
                            "契约已立，封印就位。今夜的一切冲动，皆被结界拦下。\n所有记忆仅留存于本机，不会外泄。"
                        else
                            "愚民，先完成权限献祭，再来缔结堕天契约。\n所有记忆仅留存于本机，不会外泄。",
                        fontSize = 12.5.sp,
                        lineHeight = 22.sp,
                        color = Glass.OnSurfaceVariant,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 13.dp)
                    )

                    GlassPrimaryButton(
                        text = "开启堕天契约仪式",
                        onClick = onStartContract
                    )

                    // ========== 保护期间轨迹预览 ==========
                    if (protectionActive) {
                        val context = LocalContext.current
                        var pointCount by remember { mutableIntStateOf(0) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                pointCount = try {
                                    TrackStore(context).readDay(System.currentTimeMillis()).size
                                } catch (e: Exception) { 0 }
                                delay(3000)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "保护进行中 · 已记录 $pointCount 个轨迹点",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Glass.OnSurfaceVariant,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    Spacer(Modifier.height(9.dp))

                    if (!state.hasManagement) {
                        GlassSecondaryButton(
                            text = "高级：建立工作资料封印",
                            onClick = onRequestAdmin
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "仅隔离工作资料内的应用。拦截主用户应用请用下方「无障碍拦截」。",
                            fontSize = 10.5.sp,
                            lineHeight = 16.sp,
                            color = Glass.OnSurfaceVariant,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ========== 健康检测 ==========
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle("结界戒律健康检测")

                    CheckItem(
                        label = "无障碍拦截",
                        status = if (accessibilityOn) "已开启" else "未开启",
                        ok = accessibilityOn,
                        icon = UiIconType.SHIELD
                    )
                    if (state.isDeviceOwner) {
                        CheckItem(
                            label = "Device-Owner 完整封印",
                            status = "已缔结",
                            ok = true,
                            icon = UiIconType.SHIELD
                        )
                    }
                    if (state.isProfileOwner) {
                        CheckItem(
                            label = "Profile-Owner（仅限工作资料）",
                            status = "已缔结",
                            ok = true,
                            icon = UiIconType.SHIELD
                        )
                    }
                    CheckItem(
                        label = "清醒试炼 PIN",
                        status = if (hasPin) "已设置" else "未设置",
                        ok = hasPin,
                        icon = UiIconType.KEY
                    )
                    CheckItem(
                        label = "封印名单",
                        status = "${lockedCount} 个应用",
                        ok = lockedCount > 0,
                        icon = UiIconType.PACKAGE
                    )
                    CheckItem(
                        label = "契约通告权限",
                        status = "正常",
                        ok = true,
                        icon = UiIconType.BELL
                    )

                    // 健康问题提示
                    if (!notificationOk) {
                        Row(
                            modifier = Modifier.padding(top = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UiIcon(
                                type = UiIconType.WARNING,
                                size = 14.dp,
                                tint = Glass.Tertiary
                            )
                            Text(
                                text = " 通知权限未开启，预警将无法提醒你。",
                                fontSize = 11.5.sp,
                                color = Glass.Tertiary,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    GlassSecondaryButton(
                        text = "一键体检",
                        onClick = onOpenCheckup
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ========== 配置 ==========
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle("封印配置")

                    // 保护级别三档切换
                    val currentLevel = try { ProtectionLevel.valueOf(levelName) } catch (e: Exception) { ProtectionLevel.STANDARD }
                    val levels = listOf(ProtectionLevel.LITE, ProtectionLevel.STANDARD, ProtectionLevel.STRICT)
                    val nextLevel = levels[(levels.indexOf(currentLevel) + 1) % levels.size]

                    GlassSecondaryButton(
                        text = "保护级别：${currentLevel.label}（点击切换）",
                        onClick = { onLevelChange(nextLevel.name) }
                    )
                    Text(
                        text = currentLevel.description,
                        fontSize = 10.5.sp,
                        lineHeight = 16.sp,
                        color = Glass.OnSurfaceVariant,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(top = 2.dp, bottom = 9.dp)
                    )

                    GlassSecondaryButton(
                        text = if (autoProtectEnabled) "自动检测：已开启" else "自动检测：已关闭",
                        onClick = onToggleAutoProtect
                    )
                    Spacer(Modifier.height(9.dp))
                    GlassSecondaryButton(
                        text = if (autoSkipped) "今晚不喝：已开启" else "今晚不喝：关闭",
                        onClick = onToggleSkipTonight
                    )
                    Spacer(Modifier.height(9.dp))
                    GlassSecondaryButton(
                        text = "封印名单（$lockedCount）",
                        onClick = onPickApps
                    )
                    Spacer(Modifier.height(9.dp))
                    GlassSecondaryButton(
                        text = "白名单（$whitelistCount）",
                        onClick = onWhitelist
                    )
                    Spacer(Modifier.height(9.dp))
                    GlassSecondaryButton(
                        text = if (hasPin) "重设清醒试炼 PIN" else "设置清醒试炼 PIN",
                        onClick = onSetPin
                    )
                    Spacer(Modifier.height(9.dp))
                    GlassSecondaryButton(
                        text = if (themeMode == ThemeMode.GLASS) "切换简约主题" else "切换琉璃主题",
                        onClick = onToggleTheme
                    )
                    Spacer(Modifier.height(9.dp))
                    GlassSecondaryButton(
                        text = if (accessibilityOn) "无障碍拦截已开启" else "开启无障碍拦截",
                        onClick = onOpenAccessibility
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ========== 运行 ==========
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle("结界运行")

                    GlassPrimaryButton(
                        text = "开启喝酒模式",
                        onClick = onStartContract
                    )
                    Spacer(Modifier.height(9.dp))
                    if (protectionActive) {
                        GlassSecondaryButton(
                            text = "进入锁屏",
                            onClick = onShowLockScreen
                        )
                        Spacer(Modifier.height(9.dp))
                    }
                    GlassGhostButton(
                        text = "停止保护",
                        onClick = onStopProtection
                    )
                    Spacer(Modifier.height(9.dp))
                    GlassSecondaryButton(
                        text = "测试清醒验证",
                        onClick = onTestVerify
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            GlassNavBar(
                items = DefaultNavItems,
                current = "altar",
                onSelect = { id ->
                    when (id) {
                        "altar" -> onRefresh()
                        "contract" -> onNavContract()
                        "trial" -> onNavTrial()
                        "review" -> onNavReview()
                    }
                }
            )
        }
    }
}
