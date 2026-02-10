package com.brightflare.claudeping.service

import com.brightflare.claudeping.model.ApprovalRequest
import com.brightflare.claudeping.model.ApprovalResponse
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

private val logger = KotlinLogging.logger {}

/**
 * 텔레그램 봇 서비스
 */
@Service
class TelegramService(
    @Value("\${telegram.bot.token}") private val botToken: String,
    @Value("\${telegram.chat.id}") private val chatId: Long,
    private val approvalService: ApprovalService
) {

    private val bot = bot {
        token = botToken

        dispatch {
            callbackQuery {
                val data = callbackQuery.data
                val messageId = callbackQuery.message?.messageId

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
                    bot.editMessageText(
                        chatId = ChatId.fromId(chatId),
                        messageId = messageId!!,
                        text = """
                            ✅ 응답 완료

                            ${callbackQuery.message?.text}

                            👉 결과: ${if (approved) "승인" else "거부"}
                        """.trimIndent()
                    )

                    bot.answerCallbackQuery(
                        callbackQuery.id,
                        text = if (approved) "✅ 승인되었습니다" else "❌ 거부되었습니다"
                    )
                }
            }
        }
    }

    @PostConstruct
    fun start() {
        logger.info { "Starting Telegram bot..." }
        bot.startPolling()
    }

    /**
     * 승인 요청을 텔레그램으로 전송
     */
    suspend fun sendApprovalRequest(request: ApprovalRequest) {
        val keyboard = InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(
                    text = "✅ 승인",
                    callbackData = "approve:${request.id}"
                ),
                InlineKeyboardButton.CallbackData(
                    text = "❌ 거부",
                    callbackData = "reject:${request.id}"
                )
            )
        )

        val message = """
            🤖 Claude 승인 요청

            📝 질문:
            ${request.question}

            ${request.context?.let { "📎 컨텍스트:\n$it\n" } ?: ""}
            ⏰ 시간: ${request.timestamp}

            응답을 선택해주세요:
        """.trimIndent()

        val result = bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = message,
            replyMarkup = keyboard
        )

        if (result.isError) {
            logger.error { "Failed to send message: ${result.errorBody}" }
            throw RuntimeException("Failed to send Telegram message")
        } else {
            logger.info { "Approval request sent: ${request.id}" }
        }
    }

    /**
     * 일반 메시지 전송
     */
    fun sendMessage(message: String) {
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = message
        )
    }
}
