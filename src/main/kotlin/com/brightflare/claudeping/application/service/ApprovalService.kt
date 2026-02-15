package com.brightflare.claudeping.application.service

import com.brightflare.claudeping.domain.model.ApprovalRequest
import com.brightflare.claudeping.domain.model.ApprovalResponse
import com.brightflare.claudeping.domain.model.ApprovalStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 승인 요청 관리 서비스
 * 요청의 생명주기와 상태를 관리하고, 응답을 대기합니다.
 */
@Service
class ApprovalService(
    @Value("\${approval.timeout.seconds:300}") private val timeoutSeconds: Long
) {

    // 활성 요청을 저장하는 맵 (request-id → deferred response)
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<ApprovalResponse>>()

    // 요청 히스토리 (디버깅 및 로깅용)
    private val requestHistory = ConcurrentHashMap<String, ApprovalRequest>()

    /**
     * 새로운 승인 요청을 생성하고 응답을 대기합니다.
     *
     * @return 사용자의 응답 (승인/거부)
     * @throws kotlinx.coroutines.TimeoutCancellationException 타임아웃 시
     */
    suspend fun createAndWaitForApproval(request: ApprovalRequest): ApprovalResponse {
        logger.info { "Creating approval request: ${request.id}" }

        // Deferred 생성 및 저장
        val deferred = CompletableDeferred<ApprovalResponse>()
        pendingRequests[request.id] = deferred
        requestHistory[request.id] = request

        return try {
            // 타임아웃 설정과 함께 응답 대기
            withTimeout(timeoutSeconds * 1000) {
                deferred.await()
            }
        } catch (e: Exception) {
            logger.error(e) { "Request ${request.id} failed or timed out" }
            request.status = ApprovalStatus.TIMEOUT
            throw e
        } finally {
            // 완료된 요청은 맵에서 제거
            pendingRequests.remove(request.id)
        }
    }

    /**
     * 사용자로부터 응답을 받았을 때 호출됩니다.
     */
    fun respondToRequest(requestId: String, response: ApprovalResponse) {
        logger.info { "Received response for request $requestId: approved=${response.approved}" }

        val deferred = pendingRequests[requestId]
        val request = requestHistory[requestId]

        if (deferred == null) {
            logger.warn { "No pending request found for ID: $requestId" }
            return
        }

        if (request != null) {
            request.status = if (response.approved) ApprovalStatus.APPROVED else ApprovalStatus.REJECTED
        }

        deferred.complete(response)
    }

    /**
     * 현재 대기 중인 요청 목록 조회
     */
    fun getPendingRequests(): List<ApprovalRequest> {
        return pendingRequests.keys.mapNotNull { requestHistory[it] }
    }

    /**
     * 특정 요청 조회
     */
    fun getRequest(requestId: String): ApprovalRequest? {
        return requestHistory[requestId]
    }

    /**
     * 요청 취소
     */
    fun cancelRequest(requestId: String, reason: String = "Cancelled") {
        logger.info { "Cancelling request $requestId: $reason" }

        val deferred = pendingRequests.remove(requestId)
        val request = requestHistory[requestId]

        request?.status = ApprovalStatus.TIMEOUT

        deferred?.completeExceptionally(
            RuntimeException("Request cancelled: $reason")
        )
    }
}
