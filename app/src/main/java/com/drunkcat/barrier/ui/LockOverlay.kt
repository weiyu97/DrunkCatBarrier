package com.drunkcat.barrier.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.drunkcat.barrier.data.ContractTime
import com.drunkcat.barrier.service.AppWatchService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 无障碍悬浮锁屏层（纯 View 实现，不依赖 Compose-in-service）。
 *
 * 关键：用 [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY] 直接由
 * 无障碍服务绘制全屏锁屏。该窗口类型不受 MIUI/ColorOS 的「后台弹出界面」
 * 限制，永远压在最上层，且不被返回手势关闭（没有 Activity 可被 finish）。
 *
 * 交互：本层只负责遮挡和入口；点击「通过清醒验证解除」后，以用户前台点击
 * 的方式启动 [LockScreenActivity] 完成验证，避免 MIUI 拦截后台 Activity 启动。
 */
object LockOverlay {

    @Volatile
    private var currentRoot: FrameLayout? = null

    /** 时钟刷新用，remove 时必须停掉，否则 Activity 关闭后仍会持有 View 引用 */
    private val clockHandler = Handler(Looper.getMainLooper())

    /** 挂起期间保留恢复锁屏所需的状态（遮罩必须让位给验证 Activity，否则会压在它上面） */
    @Volatile
    private var pendingPackage: String = ""
    @Volatile
    private var pendingUntil: Long = 0L
    @Volatile
    private var pendingValid: Boolean = false

    /** 是否已显示锁屏层 */
    fun isShowing(): Boolean = currentRoot != null

    /**
     * 让出屏幕给验证 Activity：移除遮罩但记住状态，便于验证未通过时恢复。
     * 仅移除视图，[pendingValid] 保持为 true。
     */
    fun suspend(context: Context, attemptedPackage: String, untilMs: Long) {
        pendingPackage = attemptedPackage
        pendingUntil = untilMs
        pendingValid = true
        remove(context)
    }

    /**
     * 恢复遮罩。[restoreContext] 必须是无障碍服务上下文
     * （TYPE_ACCESSIBILITY_OVERLAY 只有服务上下文有权添加）。
     */
    fun restore(restoreContext: Context) {
        if (!pendingValid) return
        pendingValid = false
        if (currentRoot != null) return
        show(restoreContext, pendingPackage, pendingUntil)
    }

    /** 清除挂起状态：验证通过或保护结束时调用，此后不会再恢复遮罩 */
    fun clearPending() {
        pendingValid = false
    }

    /**
     * 显示锁屏层。若已在显示则忽略，避免重复叠加。
     *
     * @param context 必须是 AccessibilityService 上下文（才有 TYPE_ACCESSIBILITY_OVERLAY 权限）
     * @param attemptedPackage 触发锁屏的应用包名（验证通过后会临时放行该应用）
     */
    fun show(context: Context, attemptedPackage: String, protectionUntilMs: Long = 0L) {
        android.util.Log.d("DrunkCat", "LockOverlay.show enter pkg=$attemptedPackage current=${currentRoot != null}")
        if (currentRoot != null) return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val root = FrameLayout(context)
        root.setBackgroundColor(Color.parseColor("#D9000000"))

        // 居中卡片容器
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(context, 24), dp(context, 28), dp(context, 24), dp(context, 28))
            background = roundedRect(
                fillColor = Color.parseColor("#1A0C0618"),
                strokeColor = Color.parseColor("#99FF4678"),
                strokeWidthPx = dp(context, 2),
                cornerRadiusPx = dp(context, 20)
            )
            val lp = FrameLayout.LayoutParams(dp(context, 312), FrameLayout.LayoutParams.WRAP_CONTENT)
            lp.gravity = Gravity.CENTER
            layoutParams = lp
        }

        val title = TextView(context).apply {
            text = "结界封锁中"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            letterSpacing = 0.15f
        }
        val subtitle = TextView(context).apply {
            text = "DRUNKCAT BARRIER ACTIVE"
            textSize = 10f
            setTextColor(Color.parseColor("#99FFFFFF"))
            gravity = Gravity.CENTER
            letterSpacing = 0.25f
        }
        val status = TextView(context).apply {
            text = statusText(protectionUntilMs)
            textSize = 13f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
        }

        val btnVerify = TextView(context).apply {
            text = "通过清醒验证解除"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 14), 0, dp(context, 14))
            background = roundedRect(
                fillColor = Color.parseColor("#FF4678"),
                cornerRadiusPx = dp(context, 12)
            )
            setOnClickListener {
                // 用户主动点击 → 先让出屏幕（遮罩是无障碍悬浮层，会盖住 Activity），
                // 再前台启动验证 Activity，MIUI 不会拦截。
                try {
                    suspend(context, attemptedPackage, protectionUntilMs)
                    context.startActivity(
                        Intent(context, LockScreenActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra(LockScreenActivity.EXTRA_ATTEMPTED_PACKAGE, attemptedPackage)
                        }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DrunkCat", "LockOverlay start LockScreenActivity failed: ${e.message}")
                    // 启动失败必须把遮罩还回来，否则用户被困在系统界面里
                    restore(context)
                }
            }
        }

        val btnEmergency = TextView(context).apply {
            text = "紧急呼叫"
            textSize = 14f
            setTextColor(Color.parseColor("#FF8FA7"))
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 12), 0, dp(context, 12))
            background = roundedRect(
                fillColor = Color.TRANSPARENT,
                strokeColor = Color.parseColor("#66FF4678"),
                strokeWidthPx = dp(context, 1),
                cornerRadiusPx = dp(context, 12)
            )
            setOnClickListener {
                AppWatchService.grantEmergencyAccess(60_000L)
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:110")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: Exception) { }
            }
        }

        val spacer = View(context).apply { layoutParams = LinearLayout.LayoutParams(1, dp(context, 8)) }

        card.addView(title)
        card.addView(subtitle)
        card.addView(spacer)
        card.addView(status)
        card.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(1, dp(context, 20)) })
        card.addView(btnVerify)
        card.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(1, dp(context, 10)) })
        card.addView(btnEmergency)

        // 顶部大号时钟：结界遮罩是全屏不透明的，盖住后用户就看不到系统状态栏时钟了
        val clock = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 52f
            letterSpacing = 0.02f
            includeFontPadding = false
        }
        val clockDate = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#B3FFFFFF"))
            textSize = 14f
            letterSpacing = 0.08f
            includeFontPadding = false
        }
        val clockBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(clock)
            addView(clockDate, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(context, 4) })
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = dp(context, 28)
            }
        }

        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("M月d日 EEEE", Locale.CHINA)
        val tick = object : Runnable {
            override fun run() {
                val now = Date()
                clock.text = timeFmt.format(now)
                clockDate.text = dateFmt.format(now)
                clockHandler.removeCallbacks(this)
                clockHandler.postDelayed(this, 1000L - (now.time % 1000L))
            }
        }

        root.addView(clockBox)
        root.addView(card)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0
        params.title = "DrunkCatBarrierLock"

        try {
            wm.addView(root, params)
            currentRoot = root
            clockHandler.removeCallbacksAndMessages(null)
            clockHandler.post(tick)
            android.util.Log.d("DrunkCat", "LockOverlay.show added OK")
        } catch (e: Exception) {
            android.util.Log.e("DrunkCat", "LockOverlay.show failed: ${e.message}")
            android.util.Log.e("DrunkCat", android.util.Log.getStackTraceString(e))
        }
    }

    /** 移除锁屏层 */
    fun remove(context: Context) {
        clockHandler.removeCallbacksAndMessages(null)
        val root = currentRoot ?: return
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeView(root)
        } catch (e: Exception) {
            android.util.Log.e("DrunkCat", "LockOverlay.remove failed: ${e.message}")
        } finally {
            currentRoot = null
        }
    }

    private fun statusText(until: Long): String {
        return if (until > 0L) {
            "将在 ${ContractTime.label(until)} 自动解除\n${ContractTime.remaining(until)}"
        } else {
            "保护进行中"
        }
    }

    private fun dp(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun roundedRect(
        fillColor: Int,
        strokeColor: Int? = null,
        strokeWidthPx: Int = 0,
        cornerRadiusPx: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx.toFloat()
            setColor(fillColor)
            if (strokeColor != null && strokeWidthPx > 0) {
                setStroke(strokeWidthPx, strokeColor)
            }
        }
    }
}
