# ⚡ 빠른 시작 가이드

## 1️⃣ 텔레그램 봇 설정 (5분)

### Step 1: 봇 생성
1. 텔레그램 앱 열기
2. [@BotFather](https://t.me/botfather) 검색
3. `/newbot` 명령 실행
4. 봇 이름 입력 (예: "My Claude Bot")
5. 봇 username 입력 (예: "my_claude_bot")
6. **봇 토큰 복사** (예: `1234567890:ABCdefGHIjklMNOpqrsTUVwxyz`)

### Step 2: Chat ID 확인
1. [@userinfobot](https://t.me/userinfobot) 검색
2. 메시지 전송 (아무거나)
3. **Chat ID 복사** (예: `123456789`)

### Step 3: 봇 활성화
1. 생성한 봇 찾기
2. `/start` 명령 전송

---

## 2️⃣ 환경변수 설정 (2분)

### macOS/Linux

터미널에서 실행:

```bash
# 환경변수 설정
echo 'export TELEGRAM_BOT_TOKEN="1234567890:ABCdefGHIjklMNOpqrsTUVwxyz"' >> ~/.bashrc
echo 'export TELEGRAM_CHAT_ID="123456789"' >> ~/.bashrc

# zsh 사용자는 .zshrc
echo 'export TELEGRAM_BOT_TOKEN="1234567890:ABCdefGHIjklMNOpqrsTUVwxyz"' >> ~/.zshrc
echo 'export TELEGRAM_CHAT_ID="123456789"' >> ~/.zshrc

# 적용
source ~/.bashrc  # 또는 source ~/.zshrc
```

**⚠️ 주의**: 위의 토큰과 Chat ID를 본인의 것으로 교체하세요!

### 확인

```bash
echo $TELEGRAM_BOT_TOKEN
echo $TELEGRAM_CHAT_ID
```

---

## 3️⃣ 프로젝트 빌드 & 실행 (3분)

### IntelliJ IDEA 사용 (권장)

1. **프로젝트 열기**
   - IntelliJ IDEA 실행
   - "Open" → `claudeping` 폴더 선택
   - Gradle 자동 import 대기

2. **실행**
   - `ClaudePingApplication.kt` 파일 열기
   - 파일 왼쪽의 ▶️ 버튼 클릭
   - 또는 `Shift + F10`

3. **확인**
   - 콘솔에 "Started ClaudePingApplication" 출력 확인
   - `http://localhost:8080` 실행 중

### 터미널 사용

```bash
# Gradle Wrapper 권한 부여 (최초 1회)
chmod +x gradlew

# 빌드 & 실행
./gradlew bootRun
```

**⚠️ Gradle Wrapper가 없는 경우**:
```bash
# IntelliJ로 프로젝트를 한 번 열면 자동 생성됨
# 또는 Gradle 설치 필요
brew install gradle  # macOS
gradle wrapper
```

---

## 4️⃣ Claude Code Hook 설정 (2분)

### Step 1: Hook 디렉토리 생성

```bash
mkdir -p ~/.claude/hooks
```

### Step 2: Hook 스크립트 링크

```bash
# 프로젝트 경로를 본인의 경로로 수정
ln -sf ~/Documents/git-repo/my-temp/hooks/ask-user-hook.sh \
       ~/.claude/hooks/ask-user-hook.sh
```

### Step 3: Claude 설정 파일 수정

```bash
# 설정 파일이 없으면 생성
mkdir -p ~/.claude
touch ~/.claude/settings.json
```

`~/.claude/settings.json` 파일 내용:

```json
{
  "hooks": {
    "user-prompt-submit": "/Users/본인계정/.claude/hooks/ask-user-hook.sh"
  }
}
```

**⚠️ 주의**: 경로를 본인의 홈 디렉토리로 수정하세요!

---

## 5️⃣ 테스트 (2분)

### 1. 서버 상태 확인

```bash
curl http://localhost:8080/api/hook/health
```

**예상 출력**:
```json
{"status":"UP","service":"claudeping"}
```

### 2. 수동 테스트

```bash
curl -X POST http://localhost:8080/api/hook/ask \
  -H "Content-Type: application/json" \
  -d '{
    "event": "ask_user",
    "question": "테스트 질문입니다. 승인하시겠습니까?",
    "context": {}
  }'
```

**결과**:
1. 텔레그램에서 알림 수신
2. 버튼 클릭 (승인 또는 거부)
3. 터미널에 응답 출력

### 3. Claude Code와 함께 테스트

Claude Code를 실행하고 작업을 수행하면, 승인이 필요한 경우 자동으로 텔레그램 알림이 전송됩니다!

---

## 🎉 완료!

이제 Claude Code가 질문할 때마다 텔레그램으로 알림을 받고, 버튼 클릭으로 즉시 응답할 수 있습니다.

---

## 📱 사용 예시

```
📱 텔레그램:
━━━━━━━━━━━━━━━━━━━━
🤖 Claude 승인 요청

📝 질문:
파일을 수정하시겠습니까?

⏰ 시간: 2024-02-08T10:30:00

응답을 선택해주세요:

[✅ 승인]  [❌ 거부]
━━━━━━━━━━━━━━━━━━━━
```

버튼을 누르면:
- Claude Code가 즉시 계속 진행
- 텔레그램 메시지가 "✅ 응답 완료"로 업데이트

---

## 🔧 문제 해결

### 텔레그램 알림이 안 옴
```bash
# 환경변수 확인
echo $TELEGRAM_BOT_TOKEN
echo $TELEGRAM_CHAT_ID

# 봇에게 /start 메시지 전송했는지 확인
```

### Hook이 실행 안 됨
```bash
# 실행 권한 확인
chmod +x ~/.claude/hooks/ask-user-hook.sh

# 로그 확인
tail -f ~/.claude/claudeping.log
```

### 서버 연결 실패
```bash
# 서버 실행 중인지 확인
curl http://localhost:8080/api/hook/health

# 포트 충돌 확인
lsof -i :8080
```

---

## 📚 더 보기

- [README.md](README.md) - 전체 문서
- [문제 신고](https://github.com/your-repo/issues)

---

**즐거운 코딩 되세요! 🚀**
