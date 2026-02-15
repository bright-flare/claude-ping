package com.brightflare.claudeping.ui.dto

/**
 * Hook으로부터 받는 요청 데이터
 */
data class HookRequest(
    val event: String,           // 이벤트 타입 (예: "ask_user")
    val question: String,        // 질문 내용
    val context: Map<String, Any>? = null  // 추가 컨텍스트
)
