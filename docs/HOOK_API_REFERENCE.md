# Claude Code Hook API 참조

ClaudePing에서 사용하는 Claude Code Hook 시스템의 상세 레퍼런스입니다.

---

## 📚 공식 문서

- **Hooks 가이드**: https://code.claude.com/docs/ko/hooks-guide
- **Hooks 참조**: https://code.claude.com/docs/ko/hooks

---

## 🎯 PermissionRequest Hook

### 개요

권한 대화상자가 표시될 때 실행되는 Hook입니다. 사용자 대신 자동으로 허용/거부하거나, 사용자에게 질문할 수 있습니다.

### 트리거 시점

Claude Code가 다음 작업 수행 시 권한을 요청할 때:
- **Bash**: 셸 명령 실행
- **Write**: 파일 생성
- **Edit**: 파일 수정
- **Read**: 파일 읽기
- **기타 도구**: Task, Glob, Grep, WebFetch 등

---

## 📥 입력 형식 (stdin)

Hook은 stdin을 통해 JSON 데이터를 받습니다:

```json
{
  "session_id": "abc123",
  "transcript_path": "/Users/.../.claude/projects/.../session.jsonl",
  "cwd": "/Users/.../project",
  "permission_mode": "default",
  "hook_event_name": "PermissionRequest",
  "tool_name": "Bash",
  "tool_input": {
    "command": "npm install",
    "description": "Install dependencies"
  },
  "tool_use_id": "toolu_01ABC123..."
}
```

### 공통 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `session_id` | string | 세션 고유 ID |
| `transcript_path` | string | 대화 기록 파일 경로 |
| `cwd` | string | 현재 작업 디렉토리 |
| `permission_mode` | string | 권한 모드 (default, plan, etc.) |
| `hook_event_name` | string | Hook 이벤트 이름 |

### PermissionRequest 전용 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `tool_name` | string | 도구 이름 (Bash, Write, Edit 등) |
| `tool_input` | object | 도구별 입력 파라미터 |
| `tool_use_id` | string | 도구 사용 고유 ID |

### tool_input 예시

**Bash**:
```json
{
  "command": "ls -la",
  "description": "List all files"
}
```

**Write/Edit**:
```json
{
  "file_path": "/path/to/file.txt",
  "content": "file content here"
}
```

**Read**:
```json
{
  "file_path": "/path/to/file.txt"
}
```

---

## 📤 출력 형식 (stdout)

### 방법 1: Exit Code (간단)

가장 간단한 방법:

```bash
# 통과 (정상 권한 시스템 처리)
exit 0

# 거부 (stderr를 Claude에 표시)
echo "거부 이유" >&2
exit 2
```

### 방법 2: JSON 출력 (고급, 권장)

더 정교한 제어를 위한 JSON 출력:

#### 승인 (Allow)

```json
{
  "hookSpecificOutput": {
    "hookEventName": "PermissionRequest",
    "decision": {
      "behavior": "allow"
    }
  }
}
```

**Exit code**: `0`

#### 거부 (Deny)

```json
{
  "hookSpecificOutput": {
    "hookEventName": "PermissionRequest",
    "decision": {
      "behavior": "deny",
      "message": "거부 이유를 Claude에 전달",
      "interrupt": false
    }
  }
}
```

**Exit code**: `0` (JSON 사용 시)

#### 도구 입력 수정 후 승인

```json
{
  "hookSpecificOutput": {
    "hookEventName": "PermissionRequest",
    "decision": {
      "behavior": "allow",
      "updatedInput": {
        "command": "npm run lint --fix"
      }
    }
  }
}
```

---

## 🔧 설정 형식

### ~/.claude/settings.json

```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "Bash|Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "/path/to/hook-script.sh",
            "timeout": 600
          }
        ]
      }
    ]
  }
}
```

### Matcher 패턴

| 패턴 | 설명 | 예시 |
|------|------|------|
| `*` | 모든 도구 | 모든 권한 요청 |
| `Bash` | Bash만 | 셸 명령만 |
| `Write\|Edit` | Write 또는 Edit | 파일 수정만 |
| `.*` | 정규식 | `mcp__.*` (모든 MCP 도구) |

---

## 💡 ClaudePing 구현

### Hook 스크립트

[hooks/permission-request-hook.sh](../hooks/permission-request-hook.sh)

**주요 기능**:
1. stdin에서 JSON 파싱 (`jq` 사용)
2. 질문 생성 (도구 타입별)
3. Spring Boot API로 텔레그램 알림 전송
4. 사용자 응답 대기
5. JSON 출력으로 결과 반환

### API 엔드포인트

- **POST** `/api/hook/ask`
  - Input: `{"event": "permission_request", "question": "...", "context": {...}}`
  - Output: `{"approved": true/false, "message": "...", "requestId": "..."}`

### 처리 흐름

```
1. Claude → PermissionRequest Hook 트리거
2. Hook → stdin JSON 파싱
3. Hook → Spring Boot API 호출
4. Spring Boot → TelegramService 알림 전송
5. 사용자 → 텔레그램 버튼 클릭
6. TelegramService → ApprovalService 응답 전달
7. Spring Boot → Hook에 HTTP 응답
8. Hook → JSON 출력 (allow/deny)
9. Claude → 작업 계속 or 중단
```

---

## 🚨 에러 처리

### HTTP 요청 실패

```bash
if [ "$HTTP_CODE" != "200" ]; then
    echo "API 호출 실패" >&2
    exit 2  # 거부
fi
```

### 타임아웃

```json
{
  "type": "command",
  "command": "~/.claude/hooks/permission-request-hook.sh",
  "timeout": 600
}
```

### 로깅

```bash
LOG_FILE="${HOME}/.claude/claudeping.log"
log "Message"
```

---

## 🔍 디버깅

### Claude Code 디버그 모드

```bash
claude --debug
```

Hook 실행 상세 로그 확인 가능.

### 로그 확인

```bash
# Hook 로그
tail -f ~/.claude/claudeping.log

# Spring Boot 로그
# (실행 터미널에서 확인)
```

### 수동 테스트

```bash
echo '{"tool_name":"Bash","tool_input":{"command":"ls"}}' | \
  ~/.claude/hooks/permission-request-hook.sh
```

---

## 📖 참고 자료

- [Claude Code 공식 Hooks 문서](https://code.claude.com/docs/ko/hooks)
- [ClaudePing 설정 가이드](./SETUP_GUIDE.md)
- [예제 설정 파일](../examples/claude-settings-example.json)
