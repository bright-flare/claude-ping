package com.brightflare.claudeping.service

import com.brightflare.claudeping.model.ApprovalRequest

/**
 * 승인 요청을 사용자에게 전달하는 채널 추상화.
 *
 * 구현체 예시:
 * - TelegramApprovalChannel
 * - DiscordApprovalChannel
 */
interface ApprovalChannel {
    val name: String
    fun sendApprovalRequest(request: ApprovalRequest)
}
