package com.brightflare.claudeping.application.service

import com.brightflare.claudeping.domain.model.ApprovalRequest
import com.brightflare.claudeping.domain.model.ApprovalResponse
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.springframework.stereotype.Service

/**
 * 승인 요청 오케스트레이션 담당.
 *
 * 1) 채널로 승인 요청 알림 발송
 * 2) 사용자 응답 대기
 */
@Service
class ApprovalOrchestrator(
    private val notificationService: ApprovalNotificationService,
    private val approvalService: ApprovalService
) {
    suspend fun requestApproval(request: ApprovalRequest): ApprovalResponse {
        // 외부 I/O (메시징) 호출 구간
        withContext(Dispatchers.IO) {
            notificationService.notify(request)
        }

        return approvalService.createAndWaitForApproval(request)
    }
}
