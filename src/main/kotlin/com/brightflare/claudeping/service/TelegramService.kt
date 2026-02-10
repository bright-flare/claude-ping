package com.brightflare.claudeping.service

import com.brightflare.claudeping.model.ApprovalRequest
import com.brightflare.claudeping.model.ApprovalResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

private val logger = KotlinLogging.logger {}

/**
 * 텔레그램 봇 서비스
 */
@Component
class TelegramService(
    @Value("\${telegram.bot.token}") botToken: String,
    @Value("\${telegram.bot.username}") private val botUsername: String,
    @Value("\${telegram.chat.id}") private val chatId: Long,
    private val approvalService: ApprovalService
) : TelegramLongPollingBot(botToken) {

    override fun getBotUsername(): String = botUsername

    override fun onUpdateReceived(update: Update) {
        if (!update.hasCallbackQuery()) return

        val callbackQuery = update.callbackQuery
        val data = callbackQuery.data
        val callbackMessage = callbackQuery.message
        val messageId = callbackMessage.messageId
        val originalText = (callbackMessage as? org.telegram.telegrambots.meta.api.objects.Message)?.text ?: ""

        logger.info { "Received callback: $data" }

        // 콜백 데이터 파싱: "approve:request-id" or "reject:request-id"
        val parts = data.split(":")
        if (parts.size == 2) {
            val action = parts[0]
            val requestId = parts[1]

            val approved = action == "approve"
            val response = ApprovalResponse(
                approved = approved,
                message = if (approved) "승인되었습니다" else "거부되었습니다"
            )

            approvalService.respondToRequest(requestId, response)

            // 메시지 업데이트
            execute(EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text("""
                    ✅ 응답 완료

                    $originalText

                    👉 결과: ${if (approved) "승인" else "거부"}
                """.trimIndent())
                .build())

            execute(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.id)
                .text(if (approved) "✅ 승인되었습니다" else "❌ 거부되었습니다")
                .build())
        }
    }

    /**
     * 승인 요청을 텔레그램으로 전송
     */
    fun sendApprovalRequest(request: ApprovalRequest) {
        val keyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(listOf(
                InlineKeyboardButton.builder()
                    .text("✅ 승인")
                    .callbackData("approve:${request.id}")
                    .build(),
                InlineKeyboardButton.builder()
                    .text("❌ 거부")
                    .callbackData("reject:${request.id}")
                    .build()
            ))
            .build()

        val message = """
            🤖 Claude 승인 요청

            📝 질문:
            ${request.question}

            ${request.context?.let { "📎 컨텍스트:\n$it\n" } ?: ""}
            ⏰ 시간: ${request.timestamp}

            응답을 선택해주세요:
        """.trimIndent()

        execute(SendMessage.builder()
            .chatId(chatId.toString())
            .text(message)
            .replyMarkup(keyboard)
            .build())
    }

    /**
     * 일반 메시지 전송
     */
    fun sendMessage(message: String) {
        execute(SendMessage.builder()
            .chatId(chatId.toString())
            .text(message)
            .build())
    }
}
