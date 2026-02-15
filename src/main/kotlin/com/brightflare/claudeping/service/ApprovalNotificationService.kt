package com.brightflare.claudeping.service

import com.brightflare.claudeping.model.ApprovalRequest
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * 등록된 모든 채널로 승인 요청을 브로드캐스트.
 *
 * 채널 추가(예: Discord) 시 구현체만 빈으로 추가하면 자동 포함된다.
 */
@Service
class ApprovalNotificationService(
    private val channels: List<ApprovalChannel>
) {

    fun notify(request: ApprovalRequest) {
        if (channels.isEmpty()) {
            throw IllegalStateException("No approval channels configured")
        }

        channels.forEach { channel ->
            try {
                channel.sendApprovalRequest(request)
                logger.info { "Approval request ${request.id} sent via ${channel.name}" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to send approval request ${request.id} via ${channel.name}" }
            }
        }
    }
}
