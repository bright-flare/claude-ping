package com.brightflare.claudeping.infra.telegram

import com.brightflare.claudeping.application.port.ApprovalChannel
import com.brightflare.claudeping.application.service.ApprovalService
import com.brightflare.claudeping.application.service.BotConversationService
import com.brightflare.claudeping.application.service.IncomingMessageContext
import com.brightflare.claudeping.domain.model.ApprovalRequest
import com.brightflare.claudeping.domain.model.ApprovalResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

private val logger = KotlinLogging.logger {}

/**
 * 텔레그램 봇 서비스
 * - 승인/거부 콜백 처리
 * - 일반 텍스트 메시지 채팅 릴레이 처리
 */
@Component
class TelegramService(
    @Value("\${telegram.bot.token}") botToken: String,
    @Value("\${telegram.bot.username}") private val botUsername: String,
    @Value("\${telegram.chat.id}") private val defaultChatId: Long,
    @Value("\${telegram.chat.strict:true}") private val strictChatId: Boolean,
    private val approvalService: ApprovalService,
    private val conversationService: BotConversationService
) : TelegramLongPollingBot(botToken), ApprovalChannel {

    override val name: String = "telegram"

    override fun getBotUsername(): String = botUsername

    override fun onUpdateReceived(update: Update) {
        when {
            update.hasCallbackQuery() -> handleCallback(update)
            update.hasMessage() && update.message.hasText() -> handleTextMessage(update.message)
        }
    }

    private fun handleCallback(update: Update) {
        val callbackQuery = update.callbackQuery
        val data = callbackQuery.data
        val callbackMessage = callbackQuery.message
        val messageId = callbackMessage.messageId
        val chatId = callbackMessage.chatId
        val originalText = (callbackMessage as? Message)?.text ?: ""

        logger.info { "Received callback: $data" }

        // 콜백 데이터 파싱: "approve:request-id" or "reject:request-id"
        val parts = data.split(":")
        if (parts.size != 2) return

        val action = parts[0]
        val requestId = parts[1]

        val approved = action == "approve"
        val response = ApprovalResponse(
            approved = approved,
            message = if (approved) "승인되었습니다" else "거부되었습니다"
        )

        approvalService.respondToRequest(requestId, response)

        // 메시지 업데이트
        execute(
            EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(
                    """
                    ✅ 응답 완료

                    $originalText

                    👉 결과: ${if (approved) "승인" else "거부"}
                """.trimIndent()
                )
                .build()
        )

        execute(
            AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.id)
                .text(if (approved) "✅ 승인되었습니다" else "❌ 거부되었습니다")
                .build()
        )
    }

    private fun handleTextMessage(message: Message) {
        val chatId = message.chatId
        val text = message.text?.trim().orEmpty()
        val from = message.from

        if (text.isBlank()) return

        if (strictChatId && defaultChatId > 0 && chatId != defaultChatId) {
            logger.warn { "Rejected message from unauthorized chatId=$chatId" }
            sendMessageToChat(chatId, "이 봇은 허용된 Chat ID에서만 동작해요.")
            return
        }

        val reply = conversationService.handleMessage(
            IncomingMessageContext(
                platform = name,
                chatId = chatId,
                text = text,
                username = from?.userName,
                firstName = from?.firstName,
                lastName = from?.lastName
            )
        )

        sendMessageToChat(chatId, reply)
    }

    /**
     * 승인 요청을 텔레그램으로 전송
     */
    override fun sendApprovalRequest(request: ApprovalRequest) {
        val keyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(
                listOf(
                    InlineKeyboardButton.builder()
                        .text("✅ 승인")
                        .callbackData("approve:${request.id}")
                        .build(),
                    InlineKeyboardButton.builder()
                        .text("❌ 거부")
                        .callbackData("reject:${request.id}")
                        .build()
                )
            )
            .build()

        val message = """
            🤖 Claude 승인 요청

            📝 질문:
            ${request.question}

            ${request.context?.let { "📎 컨텍스트:\n$it\n" } ?: ""}
            ⏰ 시간: ${request.timestamp}

            응답을 선택해주세요:
        """.trimIndent()

        execute(
            SendMessage.builder()
                .chatId(defaultChatId.toString())
                .text(message)
                .replyMarkup(keyboard)
                .build()
        )
    }

    /**
     * 기본 chatId로 일반 메시지 전송
     */
    fun sendMessage(message: String) {
        sendMessageToChat(defaultChatId, message)
    }

    private fun sendMessageToChat(chatId: Long, message: String) {
        execute(
            SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .build()
        )
    }
}
