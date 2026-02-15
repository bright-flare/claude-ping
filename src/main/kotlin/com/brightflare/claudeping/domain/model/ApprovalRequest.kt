package com.brightflare.claudeping.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Claude에서 받은 승인 요청
 */
data class ApprovalRequest(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val context: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    var status: ApprovalStatus = ApprovalStatus.PENDING
)

/**
 * 승인 상태
 */
enum class ApprovalStatus {
    PENDING,    // 대기 중
    APPROVED,   // 승인됨
    REJECTED,   // 거부됨
    TIMEOUT     // 타임아웃
}

/**
 * 사용자 응답
 */
data class ApprovalResponse(
    val approved: Boolean,
    val message: String? = null
)
