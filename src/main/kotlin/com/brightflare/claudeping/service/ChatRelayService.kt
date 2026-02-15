package com.brightflare.claudeping.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * 텔레그램 메시지를 Claude(또는 LLM 백엔드)로 중계하는 서비스
 */
@Service
class ChatRelayService(
    webClientBuilder: WebClient.Builder,
    @Value("\${bridge.relay.url:}") private val relayUrl: String,
    @Value("\${bridge.relay.token:}") private val relayToken: String,
    @Value("\${bridge.relay.timeout-seconds:90}") private val timeoutSeconds: Long
) {

    private val webClient: WebClient = webClientBuilder.build()

    fun sendUserMessage(
        chatId: Long,
        text: String,
        username: String? = null,
        firstName: String? = null,
        lastName: String? = null
    ): String {
        if (relayUrl.isBlank()) {
            return """
                ⚠️ 아직 채팅 릴레이가 설정되지 않았어요.
                환경변수 `CLAUDE_RELAY_URL`을 설정해줘.
                (예: http://host.docker.internal:18789/api/chat)
            """.trimIndent()
        }

        val payload = mapOf(
            "chatId" to chatId,
            "message" to text,
            "username" to username,
            "firstName" to firstName,
            "lastName" to lastName
        )

        return try {
            val request = webClient.post()
                .uri(relayUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .let { spec ->
                    if (relayToken.isNotBlank()) {
                        spec.header("Authorization", "Bearer $relayToken")
                    } else spec
                }
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map::class.java)

            val response = request.block(Duration.ofSeconds(timeoutSeconds)) ?: emptyMap<String, Any>()
            val reply = response["reply"] ?: response["message"] ?: response["text"]

            (reply?.toString() ?: "응답은 받았지만 표시할 메시지가 없어요.")
        } catch (e: Exception) {
            logger.error(e) { "Failed to relay chat message" }
            "❌ Claude 릴레이 호출 중 오류가 발생했어: ${e.message}"
        }
    }
}
