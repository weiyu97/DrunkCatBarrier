package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.admin.DeviceState

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
    onOpenCheckup: () -> Unit
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

                    // 已缔结：有 PIN 且（Device Owner 模式 或 无障碍降级模式已开启）
                    val sealed = hasPin && (state.isDeviceOwner || protectionActive)
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

                    if (!state.isDeviceOwner) {
                        GlassPrimaryButton(
                            text = "缔结 Device-Owner 完整封印",
                            onClick = onRequestAdmin
                        )
                        Spacer(Modifier.height(9.dp))
                    }
                    GlassSecondaryButton(
                        text = "开启堕天契约仪式",
                        onClick = onStartContract
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ========== 健康检测 ==========
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle("结界戒律健康检测")

                    CheckItem(
                        label = if (state.isDeviceOwner) "Device-Owner 完整封印"
                        else "无障碍拦截（降级）",
                        status = if (state.isDeviceOwner) "已缔结"
                        else if (accessibilityOn) "已开启" else "未开启",
                        ok = state.isDeviceOwner || accessibilityOn,
                        icon = UiIconType.SHIELD
                    )
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
