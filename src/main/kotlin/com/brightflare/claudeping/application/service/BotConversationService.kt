package com.brightflare.claudeping.application.service

import com.brightflare.claudeping.infra.relay.ChatRelayService
import org.springframework.stereotype.Service

/**
 * 채널 독립적인 대화 로직.
 *
 * Telegram/Discord 어댑터는 이 서비스를 호출해
 * 동일한 커맨드/릴레이 동작을 재사용할 수 있다.
 */
@Service
class BotConversationService(
    private val chatRelayService: ChatRelayService
) {

    fun handleMessage(context: IncomingMessageContext): String {
        val text = context.text.trim()

        return when (text) {
            "/start" -> {
                """
                🔥 ClaudePing 봇이 연결됐어.
                이제 일반 메시지를 보내면 Claude 릴레이로 전달할게.

                명령어:
                /help - 도움말
                /health - 연결 상태 확인
            """.trimIndent()
            }

            "/help" -> {
                """
                사용 방법:
                1) Claude Hook 승인 요청은 버튼(✅/❌)으로 처리
                2) 일반 텍스트는 Claude 릴레이로 전달

                필수 설정:
                - CLAUDE_RELAY_URL
                - (선택) CLAUDE_RELAY_TOKEN
            """.trimIndent()
            }

            "/health" -> "✅ bot alive"

            else -> chatRelayService.sendUserMessage(
                chatId = context.chatId,
                text = text,
                username = context.username,
                firstName = context.firstName,
                lastName = context.lastName
            )
        }
    }
}

data class IncomingMessageContext(
    val platform: String,
    val chatId: Long,
    val text: String,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)
