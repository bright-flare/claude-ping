#!/bin/bash

# Claude Wrapper with Telegram Approval
# Claude Code를 래핑하여 특정 키워드 감지 시 승인 요청

set -e

API_URL="http://localhost:8080/api/hook/ask"
LOG_FILE="${HOME}/.claude/claudeping.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

# 승인 요청 함수
ask_approval() {
    local question="$1"

    log "Requesting approval: $question"

    # API 호출
    RESPONSE=$(curl -s -X POST \
      -H "Content-Type: application/json" \
      -d "{\"event\":\"wrapper\",\"question\":\"$question\"}" \
      --max-time 600 \
      "$API_URL")

    APPROVED=$(echo "$RESPONSE" | grep -o '"approved":[^,}]*' | sed 's/"approved":\s*//')

    if [ "$APPROVED" = "true" ]; then
        log "Approved"
        return 0
    else
        log "Rejected"
        return 1
    fi
}

# 명령어 확인
if ask_approval "Claude를 실행하시겠습니까? 명령: claude $*"; then
    log "Executing claude $*"
    claude "$@"
else
    log "Execution cancelled by user"
    echo "❌ 사용자에 의해 취소되었습니다"
    exit 1
fi
