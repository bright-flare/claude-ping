#!/bin/bash

# ClaudePing - 초간단 Hook (curl만 사용)
# API가 Claude Hook 형식을 직접 처리하도록 수정한 경우 사용

set -e

API_URL="http://localhost:8080/api/claude-hook/permission-request"
LOG_FILE="${HOME}/.claude/claudeping.log"

# 로그 함수
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE" 2>/dev/null || true
}

log "=== Simple Hook triggered ==="

# stdin 데이터를 그대로 API로 전달하고 응답 받기
RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d @- \
  --max-time 600 \
  "$API_URL")

# 응답 상태 확인
if [ -z "$RESPONSE" ]; then
    log "Empty response from API"
    echo '{"hookSpecificOutput":{"hookEventName":"PermissionRequest","decision":{"behavior":"deny","message":"API 응답 없음","interrupt":false}}}'
    exit 0
fi

log "Response: $RESPONSE"

# 응답 그대로 출력 (Claude가 읽음)
echo "$RESPONSE"
exit 0
