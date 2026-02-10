#!/bin/bash

# ClaudePing - PermissionRequest Hook
# Claude Code가 권한 요청 시 텔레그램으로 알림을 보내고 응답을 받습니다.

set -e

# Spring Boot API 엔드포인트
API_URL="http://localhost:8080/api/hook/ask"

# 로그 파일
LOG_FILE="${HOME}/.claude/claudeping.log"
mkdir -p "$(dirname "$LOG_FILE")"

# 로그 함수
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

log "=== PermissionRequest Hook triggered ==="

# stdin에서 JSON 입력 읽기 (Claude Code가 전달)
INPUT_JSON=$(cat)

log "Input JSON: $INPUT_JSON"

# jq로 JSON 파싱
TOOL_NAME=$(echo "$INPUT_JSON" | jq -r '.tool_name // "Unknown"')
TOOL_INPUT=$(echo "$INPUT_JSON" | jq -c '.tool_input // {}')
SESSION_ID=$(echo "$INPUT_JSON" | jq -r '.session_id // "unknown"')
CWD=$(echo "$INPUT_JSON" | jq -r '.cwd // "unknown"')

log "Tool: $TOOL_NAME"
log "Input: $TOOL_INPUT"

# 질문 생성
case "$TOOL_NAME" in
    "Bash")
        COMMAND=$(echo "$TOOL_INPUT" | jq -r '.command // "unknown command"')
        DESCRIPTION=$(echo "$TOOL_INPUT" | jq -r '.description // "No description"')
        QUESTION="Bash 명령을 실행하시겠습니까?\n\n명령: $COMMAND\n설명: $DESCRIPTION"
        ;;
    "Write"|"Edit")
        FILE_PATH=$(echo "$TOOL_INPUT" | jq -r '.file_path // "unknown file"')
        QUESTION="파일을 수정하시겠습니까?\n\n파일: $FILE_PATH"
        ;;
    "Read")
        FILE_PATH=$(echo "$TOOL_INPUT" | jq -r '.file_path // "unknown file"')
        QUESTION="파일을 읽으시겠습니까?\n\n파일: $FILE_PATH"
        ;;
    *)
        QUESTION="$TOOL_NAME 작업을 수행하시겠습니까?"
        ;;
esac

log "Generated question: $QUESTION"

# API로 전송할 JSON 페이로드 생성
API_PAYLOAD=$(jq -n \
  --arg event "permission_request" \
  --arg question "$QUESTION" \
  --arg tool_name "$TOOL_NAME" \
  --argjson tool_input "$TOOL_INPUT" \
  --arg session_id "$SESSION_ID" \
  --arg cwd "$CWD" \
  '{
    event: $event,
    question: $question,
    context: {
      tool_name: $tool_name,
      tool_input: $tool_input,
      session_id: $session_id,
      cwd: $cwd,
      timestamp: (now | strftime("%Y-%m-%dT%H:%M:%SZ"))
    }
  }')

log "Sending request to ClaudePing API..."

# API 호출 (타임아웃 10분)
HTTP_CODE=$(curl -s -w "%{http_code}" -o /tmp/claudeping_response.json \
  -X POST \
  -H "Content-Type: application/json" \
  -d "$API_PAYLOAD" \
  --max-time 600 \
  "$API_URL")

log "HTTP response code: $HTTP_CODE"

# HTTP 응답 확인
if [ "$HTTP_CODE" != "200" ]; then
    ERROR_MSG="ClaudePing API 호출 실패 (HTTP $HTTP_CODE)"
    log "$ERROR_MSG"
    echo "$ERROR_MSG" >&2
    exit 2  # 차단 (stderr를 Claude에 표시)
fi

# 응답 파싱
RESPONSE=$(cat /tmp/claudeping_response.json)
log "API Response: $RESPONSE"

APPROVED=$(echo "$RESPONSE" | jq -r '.approved // false')
MESSAGE=$(echo "$RESPONSE" | jq -r '.message // "No message"')
REQUEST_ID=$(echo "$RESPONSE" | jq -r '.requestId // "unknown"')

log "Approved: $APPROVED, Message: $MESSAGE, RequestID: $REQUEST_ID"

# Claude Code에 JSON 응답 반환
if [ "$APPROVED" = "true" ]; then
    # 승인: JSON 출력으로 allow 전달
    OUTPUT=$(jq -n \
      --arg msg "$MESSAGE" \
      '{
        hookSpecificOutput: {
          hookEventName: "PermissionRequest",
          decision: {
            behavior: "allow"
          }
        }
      }')

    echo "$OUTPUT"
    log "Permission ALLOWED"
    exit 0
else
    # 거부: JSON 출력으로 deny 전달
    OUTPUT=$(jq -n \
      --arg msg "$MESSAGE" \
      '{
        hookSpecificOutput: {
          hookEventName: "PermissionRequest",
          decision: {
            behavior: "deny",
            message: $msg,
            interrupt: false
          }
        }
      }')

    echo "$OUTPUT"
    log "Permission DENIED"
    exit 0  # JSON 출력을 사용하므로 exit 0
fi
