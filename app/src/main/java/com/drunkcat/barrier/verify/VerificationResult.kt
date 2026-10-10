package com.drunkcat.barrier.verify

sealed class VerificationResult {
    object Passed : VerificationResult()
    data class Failed(
        val reason: String,
        val cooldownMs: Long
    ) : VerificationResult()
}
