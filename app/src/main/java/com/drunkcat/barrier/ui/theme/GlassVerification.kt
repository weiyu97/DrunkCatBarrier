package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.verify.Challenge
import com.drunkcat.barrier.verify.CognitiveVerifier
import com.drunkcat.barrier.verify.DotMemoryGenerator
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 琉璃风验证流程宿主。
 *
 * 从「算术 / 成语 / 点阵」三种题型中随机抽 2 种，全部通过才算通过。
 */
@Composable
fun GlassVerificationFlow(
    verifier: CognitiveVerifier,
    targetAppName: String = "",
    onResult: (passed: Boolean, reason: String) -> Unit
) {
    // 随机抽 2 种题型（1=算术 2=成语 3=点阵）
    val kinds = remember {
        listOf(1, 2, 3).shuffled(Random(System.currentTimeMillis())).take(2)
    }
    var stage by remember { mutableIntStateOf(0) }

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
            GlassHeader(statusText = "邪眼试炼 · ${stage + 1} / 2")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 13.dp)
            ) {
                // 说明为什么被锁——避免用户困惑
                if (targetAppName.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            UiIcon(
                                type = UiIconType.SHIELD,
                                size = 15.dp,
                                tint = Glass.Primary
                            )
                            Text(
                                text = "  「$targetAppName」已被结界封印",
                                fontSize = 12.5.sp,
                                color = Glass.OnBg,
                                fontFamily = FontFamily.Serif
                            )
                        }
                        Text(
                            text = "通过下方两项试炼即可临时使用 5 分钟。",
                            fontSize = 11.5.sp,
                            color = Glass.OnSurfaceVariant,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(top = 6.dp, start = 20.dp)
                        )
                    }
                }

                when (kinds.getOrNull(stage)) {
                    1 -> GlassArithmetic(
                        challenge = remember { verifier.newArithmeticChallenge() },
                        onFinished = { passed, reason ->
                            if (passed) {
                                if (stage == 0) stage = 1
                                else onResult(true, "")
                            } else onResult(false, reason)
                        }
                    )
                    2 -> GlassIdiom(
                        challenge = remember { verifier.newIdiomChallenge() },
                        onFinished = { passed, reason ->
                            if (passed) {
                                if (stage == 0) stage = 1
                                else onResult(true, "")
                            } else onResult(false, reason)
                        }
                    )
                    else -> GlassDotMemory(
                        challenge = remember { verifier.newDotMemoryChallenge() },
                        onFinished = { passed ->
                            if (passed) {
                                if (stage == 0) stage = 1
                                else onResult(true, "")
                            } else onResult(false, "点阵记忆未通过")
                        }
                    )
                }
            }
        }
    }
}

// ==================== 算术选择题 ====================

@Composable
private fun GlassArithmetic(
    challenge: Challenge.Arithmetic,
    onFinished: (passed: Boolean, reason: String) -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var remainingMs by remember { mutableLongStateOf(challenge.problems[0].timeLimitMs) }

    val problem = challenge.problems.getOrNull(index)

    LaunchedEffect(index) {
        remainingMs = challenge.problems[index].timeLimitMs
        while (remainingMs > 0 && selected == null) {
            delay(100)
            remainingMs -= 100
        }
        if (selected == null) {
            wrong++
            if (wrong >= 2) {
                onFinished(false, "算术未通过（对 $correct，错 $wrong）")
            } else if (index + 1 >= challenge.problems.size) {
                val need = challenge.problems.size - 1
                onFinished(correct >= need, if (correct >= need) "" else "算术答对不足 $need 题")
            } else {
                index++
                selected = null
            }
        }
    }

    if (problem == null) {
        val need = challenge.problems.size - 1
        onFinished(correct >= need, if (correct >= need) "" else "算术答对不足 $need 题")
        return
    }

    ChoiceLayout(
        title = "邪眼试炼 · 算术",
        progress = "第 ${index + 1} / ${challenge.problems.size} 题",
        remainMs = remainingMs,
        correct = correct,
        wrong = wrong,
        question = problem.question,
        options = problem.options.map { it.toString() },
        correctIndex = problem.correctIndex,
        selected = selected,
        onSelect = { idx ->
            selected = idx
            if (idx == problem.correctIndex) correct++ else wrong++
            if (wrong >= 2) {
                onFinished(false, "算术未通过（对 $correct，错 $wrong）")
            } else if (index + 1 >= challenge.problems.size) {
                val need = challenge.problems.size - 1
                onFinished(correct >= need, if (correct >= need) "" else "算术答对不足 $need 题")
            } else {
                index++
                selected = null
            }
        }
    )
}

// ==================== 成语选择题 ====================

@Composable
private fun GlassIdiom(
    challenge: Challenge.Idiom,
    onFinished: (passed: Boolean, reason: String) -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var remainingMs by remember { mutableLongStateOf(challenge.problems[0].timeLimitMs) }

    val problem = challenge.problems.getOrNull(index)

    LaunchedEffect(index) {
        remainingMs = challenge.problems[index].timeLimitMs
        while (remainingMs > 0 && selected == null) {
            delay(100)
            remainingMs -= 100
        }
        if (selected == null) {
            wrong++
            if (wrong >= 2) {
                onFinished(false, "成语未通过（对 $correct，错 $wrong）")
            } else if (index + 1 >= challenge.problems.size) {
                val need = challenge.problems.size - 1
                onFinished(correct >= need, if (correct >= need) "" else "成语答对不足 $need 题")
            } else {
                index++
                selected = null
            }
        }
    }

    if (problem == null) {
        val need = challenge.problems.size - 1
        onFinished(correct >= need, if (correct >= need) "" else "成语答对不足 $need 题")
        return
    }

    ChoiceLayout(
        title = "邪眼试炼 · 成语填空",
        progress = "第 ${index + 1} / ${challenge.problems.size} 题",
        remainMs = remainingMs,
        correct = correct,
        wrong = wrong,
        question = problem.question,
        options = problem.options.map { it.toString() },
        correctIndex = problem.correctIndex,
        selected = selected,
        onSelect = { idx ->
            selected = idx
            if (idx == problem.correctIndex) correct++ else wrong++
            if (wrong >= 2) {
                onFinished(false, "成语未通过（对 $correct，错 $wrong）")
            } else if (index + 1 >= challenge.problems.size) {
                val need = challenge.problems.size - 1
                onFinished(correct >= need, if (correct >= need) "" else "成语答对不足 $need 题")
            } else {
                index++
                selected = null
            }
        }
    )
}

// ==================== 通用选择题布局 ====================

@Composable
private fun ChoiceLayout(
    title: String,
    progress: String,
    remainMs: Long,
    correct: Int,
    wrong: Int,
    question: String,
    options: List<String>,
    correctIndex: Int,
    selected: Int?,
    onSelect: (Int) -> Unit
) {
    Column {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            CardTitle(title)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    progress,
                    fontSize = 12.sp,
                    color = Glass.OnSurfaceVariant,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    "剩余 ${(remainMs / 1000.0).let { (it * 10).toInt() / 10.0 }} 秒",
                    fontSize = 12.sp,
                    color = if (remainMs < 5000) Glass.Primary else Glass.OnSurfaceVariant,
                    fontFamily = FontFamily.Serif
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = question,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Glass.OnBg,
                    letterSpacing = 4.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("正确 $correct", fontSize = 12.sp, color = Color(0xFF6DFFB0), fontFamily = FontFamily.Serif)
                Text("错误 $wrong / 2", fontSize = 12.sp, color = Glass.Primary, fontFamily = FontFamily.Serif)
            }
        }

        Spacer(Modifier.height(12.dp))

        options.forEachIndexed { idx, label ->
            val isSelected = selected == idx
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0x33FF3264) else Color(0x0AFFFFFF))
                    .border(
                        1.dp,
                        if (isSelected) Color(0x8CFF4678) else Color(0x38AA82FF),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = selected == null) { onSelect(idx) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                    color = Glass.OnBg,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

// ==================== 点阵记忆 ====================

@Composable
private fun GlassDotMemory(
    challenge: Challenge.DotMemory,
    onFinished: (passed: Boolean) -> Unit
) {
    var phase by remember { mutableStateOf(0) }
    var tapped by remember { mutableStateOf<List<Int>>(emptyList()) }

    LaunchedEffect(challenge.id) {
        delay(challenge.displayDurationMs)
        phase = 1
    }

    Column {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            CardTitle("邪眼试炼 · 点阵记忆")
            Text(
                text = if (phase == 0)
                    "记住以下 ${DotMemoryGenerator.LIT_COUNT} 个位置"
                else
                    "按顺序点击还原（已点 ${tapped.size} / ${challenge.pattern.size}）",
                fontSize = 12.sp,
                color = Glass.OnSurfaceVariant,
                fontFamily = FontFamily.Serif
            )

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0 until 3) {
                            val cell = row * 3 + col
                            val highlighted = phase == 0 && challenge.pattern.contains(cell)
                            val isTapped = tapped.contains(cell)
                            val tapIdx = tapped.indexOf(cell)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (highlighted) Glass.Secondary
                                        else if (isTapped) Glass.Primary
                                        else Color(0x1AFFFFFF)
                                    )
                                    .border(1.dp, Color(0x38AA82FF), CircleShape)
                                    .clickable(enabled = phase == 1) {
                                        val next = tapped + cell
                                        tapped = next
                                        if (next.size == challenge.pattern.size) {
                                            onFinished(next == challenge.pattern)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isTapped && tapIdx >= 0) {
                                    Text(
                                        text = "${tapIdx + 1}",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Serif
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
