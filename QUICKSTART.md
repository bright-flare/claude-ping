# ⚡ ClaudePing Quick Start (Docker + Telegram)

이 문서는 **처음 설치하는 사용자 기준**으로,
- 텔레그램 봇 생성
- Docker 실행
- Claude Hook 연결
- 승인/채팅 테스트
까지 한 번에 끝내는 가이드입니다.

---

## 0) 준비물

- Docker + Docker Compose
- Claude Code 사용 환경
- 텔레그램 앱

> 포트 기본값: `8080`

---

## 1) 텔레그램 봇 만들기 (약 3분)

### 1-1. BotFather에서 봇 생성
1. 텔레그램에서 [@BotFather](https://t.me/BotFather) 열기
2. `/newbot` 입력
3. 봇 이름/username 설정
4. 발급된 **BOT TOKEN** 복사

### 1-2. Chat ID 확인
1. [@userinfobot](https://t.me/userinfobot) 열기
2. 아무 메시지 전송
3. 표시되는 **Chat ID** 복사

### 1-3. 내 봇 활성화
1. 방금 만든 봇 대화창 열기
2. `/start` 전송 (필수)

---

## 2) 프로젝트 실행 (Docker)

```bash
git clone https://github.com/bright-flare/claude-ping.git
cd claude-ping
```

### 2-1. 환경파일 생성

```bash
cp .env.example .env
```

`.env`를 열어 최소 항목을 채워줘:

```env
TELEGRAM_BOT_TOKEN=여기에_봇_토큰
TELEGRAM_CHAT_ID=여기에_내_chat_id
TELEGRAM_CHAT_STRICT=true

# 텔레그램 일반 채팅 메시지를 보낼 릴레이 엔드포인트
CLAUDE_RELAY_URL=http://host.docker.internal:18789/api/chat
# CLAUDE_RELAY_TOKEN=선택
CLAUDE_RELAY_TIMEOUT_SECONDS=90
```

> `CLAUDE_RELAY_URL`은 네 환경의 실제 Claude/LLM 백엔드 주소로 바꿔야 해.

### 2-2. 컨테이너 실행

```bash
docker compose up -d --build
```

### 2-3. 헬스체크

```bash
curl http://localhost:8080/api/hook/health
```

예상 응답:

```json
{"status":"UP","service":"claudeping"}
```

---

## 3) Claude Code Hook 연결

### 3-1. Hook 링크

```bash
mkdir -p ~/.claude/hooks
ln -sf "$(pwd)/hooks/permission-request-hook-simple.sh" ~/.claude/hooks/claudeping-hook
chmod +x ~/.claude/hooks/claudeping-hook
```

### 3-2. Claude 설정

`~/.claude/settings.json`에 아래를 추가(없으면 생성):

```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "~/.claude/hooks/claudeping-hook",
            "timeout": 600
          }
        ]
      }
    ]
  }
}
```

---

## 4) 텔레그램 연결 테스트

### 4-1. 봇 명령 테스트
텔레그램에서 봇에게:
- `/start`
- `/help`
- `/health`

정상 응답이 오면 연결 OK.

### 4-2. 승인 버튼 테스트
아래 API 호출:

```bash
curl -X POST http://localhost:8080/api/hook/ask \
  -H "Content-Type: application/json" \
  -d '{
    "event": "permission_request",
    "question": "테스트 승인 요청입니다. 진행할까요?",
    "context": {"source":"quickstart"}
  }'
```

텔레그램에 ✅/❌ 버튼이 오고, 누르면 API 호출이 응답으로 종료되면 정상.

### 4-3. 일반 채팅 릴레이 테스트
텔레그램에서 봇에게 일반 메시지(예: `안녕`) 전송.
- 설정한 `CLAUDE_RELAY_URL`이 정상이면 답장 수신
- 미설정/오류면 안내 메시지 반환

---

## 5) 운영 기본 명령

```bash
# 로그 보기
docker compose logs -f claudeping

# 재시작
docker compose restart claudeping

# 중지
docker compose down
```

---

## 6) 자주 막히는 지점

1. **봇이 아무 응답이 없음**
   - BotFather 토큰 오타
   - 봇에 `/start` 안 보냄

2. **승인 요청이 텔레그램으로 안 옴**
   - `TELEGRAM_CHAT_ID` 불일치
   - `TELEGRAM_CHAT_STRICT=true` 상태에서 다른 채팅에서 테스트

3. **일반 채팅 답장이 안 옴**
   - `CLAUDE_RELAY_URL` 미설정 또는 접근 불가
   - 릴레이 응답 포맷이 `reply/message/text`를 반환하지 않음

---

## 7) 다음 단계

- 상세 문서: `README.md`
- Docker 심화: `docs/DOCKER_GUIDE.md`
- Hook 상세: `docs/HOOK_API_REFERENCE.md`

행복한 원격 승인/채팅 자동화 되길 🔥
