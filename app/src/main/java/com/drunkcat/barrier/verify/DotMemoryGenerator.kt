package com.drunkcat.barrier.verify

import kotlin.random.Random

/**
 * 点阵记忆挑战生成器。
 *
 * 3x3 网格，随机点亮 LIT_COUNT 个位置。
 */
object DotMemoryGenerator {

    const val GRID_SIZE = 9
    const val LIT_COUNT = 4

    fun generate(random: Random = Random.Default): Challenge.DotMemory {
        val cells = (0 until GRID_SIZE).shuffled(random).take(LIT_COUNT)
        return Challenge.DotMemory(
            id = "dot-${System.currentTimeMillis()}",
            pattern = cells
        )
    }
}
