#!/bin/bash

# Interactive Claude Wrapper
# Claude의 출력을 실시간으로 파싱하여 질문 감지

API_URL="http://localhost:8080/api/hook/ask"
LOG_FILE="${HOME}/.claude/claudeping.log"

# 질문 패턴 (정규식)
QUESTION_PATTERNS=(
    "Do you want"
    "Should I"
    "Would you like"
    "May I"
    "Can I"
    "Shall I"
    "확인"
    "승인"
    "진행"
)

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

# 승인 요청 함수
ask_approval() {
    local question="$1"

    log "Question detected: $question"

    RESPONSE=$(curl -s -X POST \
      -H "Content-Type: application/json" \
      -d "{\"event\":\"interactive\",\"question\":\"$question\"}" \
      --max-time 600 \
      "$API_URL")

    APPROVED=$(echo "$RESPONSE" | grep -o '"approved":[^,}]*' | sed 's/"approved":\s*//')

    if [ "$APPROVED" = "true" ]; then
        echo "yes"  # Claude에게 yes 전달
        log "User approved"
    else
        echo "no"   # Claude에게 no 전달
        log "User rejected"
    fi
}

# Claude 실행 및 출력 파싱
log "Starting Claude with args: $*"

# Named pipe 생성
PIPE=$(mktemp -u)
mkfifo "$PIPE"

# Claude 실행 (백그라운드)
claude "$@" < "$PIPE" &
CLAUDE_PID=$!

# 출력 파싱 루프
exec 3>"$PIPE"  # pipe에 쓰기용 fd 열기

while IFS= read -r line; do
    echo "$line"  # 사용자에게 출력

    # 질문 패턴 감지
    for pattern in "${QUESTION_PATTERNS[@]}"; do
        if echo "$line" | grep -q "$pattern"; then
            log "Pattern matched: $pattern"

            # 승인 요청 및 결과 전달
            response=$(ask_approval "$line")
            echo "$response" >&3
            break
        fi
    done
done < <(claude "$@" 2>&1)

# 정리
exec 3>&-
rm -f "$PIPE"

wait $CLAUDE_PID
exit $?
