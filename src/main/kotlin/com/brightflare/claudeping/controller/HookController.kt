package com.brightflare.claudeping.controller

import com.brightflare.claudeping.model.ApprovalRequest
import com.brightflare.claudeping.model.ApprovalResponse
import com.brightflare.claudeping.model.HookRequest
import com.brightflare.claudeping.service.ApprovalService
import com.brightflare.claudeping.service.TelegramService
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * Claude Hook으로부터 요청을 받는 컨트롤러
 */
@RestController
@RequestMapping("/api/hook")
class HookController(
    private val approvalService: ApprovalService,
    private val telegramService: TelegramService
) {

    /**
     * Claude에서 승인 요청이 왔을 때 호출되는 엔드포인트
     *
     * 1. 텔레그램으로 알림 전송
     * 2. 사용자 응답 대기 (블로킹)
     * 3. 응답 결과 반환
     */
    @PostMapping("/ask")
    fun handleApprovalRequest(@RequestBody hookRequest: HookRequest): ResponseEntity<Map<String, Any>> {
        logger.info { "Received hook request: ${hookRequest.event}" }

        return try {
            val approvalRequest = ApprovalRequest(
                question = hookRequest.question,
                context = hookRequest.context?.toString()
            )

            // 블로킹으로 응답 대기
            val response = runBlocking {
                // 텔레그램으로 알림 전송
                telegramService.sendApprovalRequest(approvalRequest)

                // 사용자 응답 대기 (타임아웃: 5분)
                approvalService.createAndWaitForApproval(approvalRequest)
            }

            // Claude로 결과 반환
            ResponseEntity.ok(
                mapOf(
                    "approved" to response.approved,
                    "message" to (response.message ?: if (response.approved) "승인되었습니다" else "거부되었습니다"),
                    "requestId" to approvalRequest.id
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to process approval request" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "approved" to false,
                    "message" to "타임아웃 또는 오류가 발생했습니다: ${e.message}",
                    "error" to true
                )
            )
        }
    }

    /**
     * 현재 대기 중인 요청 목록 조회 (디버깅용)
     */
    @GetMapping("/pending")
    fun getPendingRequests(): ResponseEntity<List<ApprovalRequest>> {
        val pending = approvalService.getPendingRequests()
        return ResponseEntity.ok(pending)
    }

    /**
     * 특정 요청 조회
     */
    @GetMapping("/request/{id}")
    fun getRequest(@PathVariable id: String): ResponseEntity<ApprovalRequest> {
        val request = approvalService.getRequest(id)
        return if (request != null) {
            ResponseEntity.ok(request)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 헬스체크 엔드포인트
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "claudeping"
            )
        )
    }
}
