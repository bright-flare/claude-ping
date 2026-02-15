package com.brightflare.claudeping.ui.controller

import com.brightflare.claudeping.domain.model.ApprovalRequest
import com.brightflare.claudeping.application.service.ApprovalOrchestrator
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * Claude Code Hook 형식을 직접 받는 컨트롤러
 * 중간 스크립트 없이 직접 호출 가능
 */
@RestController
@RequestMapping("/api/claude-hook")
class ClaudeHookController(
    private val approvalOrchestrator: ApprovalOrchestrator
) {

    /**
     * Claude Hook 형식으로 요청 받기
     * stdin 데이터를 그대로 받아서 처리
     */
    @PostMapping("/permission-request")
    fun handlePermissionRequest(@RequestBody hookInput: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        logger.info { "Received Claude Hook request: $hookInput" }

        return try {
            // Hook 입력에서 데이터 추출
            val toolName = hookInput["tool_name"] as? String ?: "Unknown"
            val toolInput = hookInput["tool_input"] as? Map<String, Any> ?: emptyMap()
            val sessionId = hookInput["session_id"] as? String ?: "unknown"
            val cwd = hookInput["cwd"] as? String ?: "unknown"

            // 도구 타입별 질문 생성
            val question = generateQuestion(toolName, toolInput)

            // 승인 요청 생성
            val approvalRequest = ApprovalRequest(
                question = question,
                context = buildContext(toolName, toolInput, sessionId, cwd)
            )

            // 블로킹으로 응답 대기
            val response = runBlocking {
                approvalOrchestrator.requestApproval(approvalRequest)
            }

            // Claude Hook 형식으로 응답
            val hookResponse = if (response.approved) {
                mapOf(
                    "hookSpecificOutput" to mapOf(
                        "hookEventName" to "PermissionRequest",
                        "decision" to mapOf(
                            "behavior" to "allow"
                        )
                    )
                )
            } else {
                mapOf(
                    "hookSpecificOutput" to mapOf(
                        "hookEventName" to "PermissionRequest",
                        "decision" to mapOf(
                            "behavior" to "deny",
                            "message" to (response.message ?: "거부되었습니다"),
                            "interrupt" to false
                        )
                    )
                )
            }

            ResponseEntity.ok(hookResponse)

        } catch (e: Exception) {
            logger.error(e) { "Failed to process permission request" }

            // 에러 시 거부 응답
            val errorResponse = mapOf(
                "hookSpecificOutput" to mapOf(
                    "hookEventName" to "PermissionRequest",
                    "decision" to mapOf(
                        "behavior" to "deny",
                        "message" to "서버 오류: ${e.message}",
                        "interrupt" to false
                    )
                )
            )

            ResponseEntity.ok(errorResponse)
        }
    }

    /**
     * 도구 타입별 질문 생성
     */
    private fun generateQuestion(toolName: String, toolInput: Map<String, Any>): String {
        return when (toolName) {
            "Bash" -> {
                val command = toolInput["command"] as? String ?: "unknown command"
                val description = toolInput["description"] as? String ?: "No description"
                "Bash 명령을 실행하시겠습니까?\n\n명령: $command\n설명: $description"
            }
            "Write", "Edit" -> {
                val filePath = toolInput["file_path"] as? String ?: "unknown file"
                "파일을 수정하시겠습니까?\n\n파일: $filePath"
            }
            "Read" -> {
                val filePath = toolInput["file_path"] as? String ?: "unknown file"
                "파일을 읽으시겠습니까?\n\n파일: $filePath"
            }
            else -> "$toolName 작업을 수행하시겠습니까?"
        }
    }

    /**
     * 컨텍스트 정보 생성
     */
    private fun buildContext(
        toolName: String,
        toolInput: Map<String, Any>,
        sessionId: String,
        cwd: String
    ): String {
        return """
            도구: $toolName
            세션: $sessionId
            경로: $cwd
            입력: $toolInput
        """.trimIndent()
    }
}
