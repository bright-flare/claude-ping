# ClaudePing 설정 가이드

Claude Code의 공식 **PermissionRequest Hook**을 사용하여 텔레그램 승인 시스템을 설정합니다.

---

## 📋 사전 준비

### 1. 텔레그램 봇 생성

1. [@BotFather](https://t.me/botfather)에서 봇 생성
2. 봇 토큰 복사 저장
3. [@userinfobot](https://t.me/userinfobot)에서 Chat ID 확인
4. 생성한 봇에게 `/start` 전송

### 2. 환경변수 설정

```bash
# ~/.bashrc 또는 ~/.zshrc에 추가
export TELEGRAM_BOT_TOKEN="your-bot-token-here"
export TELEGRAM_CHAT_ID="your-chat-id-here"

# 적용
source ~/.bashrc  # 또는 source ~/.zshrc
```

### 3. jq 설치 (JSON 파싱용)

```bash
# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq

# 확인
jq --version
```

---

## 🚀 설치 단계

### Step 1: Spring Boot 애플리케이션 실행

```bash
cd ~/path/to/claudeping

# 빌드 & 실행
./gradlew bootRun

# 또는 백그라운드 실행
nohup ./gradlew bootRun > claudeping.log 2>&1 &
```

**서버 확인**:
```bash
curl http://localhost:8080/api/hook/health
# 예상 출력: {"status":"UP","service":"claudeping"}
```

---

### Step 2: Hook 스크립트 설치

**2가지 버전 제공** - 환경에 맞게 선택하세요!

📖 **상세 비교**: [Hook 스크립트 버전 비교](./HOOK_SCRIPT_COMPARISON.md)

---

#### Option A: Python 버전 (권장 - jq 불필요) 🐍

**장점**: Python만 있으면 됨 (대부분 기본 설치), 추가 도구 불필요

```bash
# Python 확인 (3.6+ 필요)
python3 --version

# Hook 디렉토리 생성
mkdir -p ~/.claude/hooks

# 심볼릭 링크 생성
ln -sf ~/path/to/claudeping/hooks/permission-request-hook.py \
       ~/.claude/hooks/permission-request-hook.py

# 실행 권한 확인
chmod +x ~/.claude/hooks/permission-request-hook.py
```

**테스트**:
```bash
echo '{"tool_name":"Bash","tool_input":{"command":"ls","description":"List files"}}' | \
  ~/.claude/hooks/permission-request-hook.py
```

---

#### Option B: Bash 버전 (jq 필요) 💻

**장점**: 빠른 성능, 시스템 도구 선호

```bash
# jq 설치
# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq

# Hook 디렉토리 생성
mkdir -p ~/.claude/hooks

# 심볼릭 링크 생성
ln -sf ~/path/to/claudeping/hooks/permission-request-hook.sh \
       ~/.claude/hooks/permission-request-hook.sh

# 실행 권한 확인
chmod +x ~/.claude/hooks/permission-request-hook.sh
```

**테스트**:
```bash
echo '{"tool_name":"Bash","tool_input":{"command":"ls","description":"List files"}}' | \
  ~/.claude/hooks/permission-request-hook.sh
```

---

텔레그램에서 알림을 받고 버튼을 클릭하면 응답이 출력됩니다.

---

### Step 3: Claude Code 설정

#### 방법 A: `/hooks` 명령으로 설정 (권장)

1. Claude Code 실행:
   ```bash
   claude
   ```

2. `/hooks` 명령 입력

3. `PermissionRequest` 이벤트 선택

4. `+ Add new matcher…` 클릭
   - Matcher: `*` (모든 도구)
   - 또는 `Bash|Write|Edit` (특정 도구만)

5. `+ Add new hook…` 클릭
   - **Python 버전**: `~/.claude/hooks/permission-request-hook.py`
   - **Bash 버전**: `~/.claude/hooks/permission-request-hook.sh`

6. 저장 위치: `User settings` 선택

7. `Esc`로 나가기

#### 방법 B: 수동 설정 파일 편집

`~/.claude/settings.json` 파일 생성/편집:

**Python 버전 사용 시**:
```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "~/.claude/hooks/permission-request-hook.py",
            "timeout": 600
          }
        ]
      }
    ]
  }
}
```

**Bash 버전 사용 시**:
```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "~/.claude/hooks/permission-request-hook.sh",
            "timeout": 600
          }
        ]
      }
    ]
  }
}
```

**Matcher 옵션**:
- `*`: 모든 도구 (권장)
- `Bash`: Bash 명령만
- `Write|Edit`: 파일 수정만
- `Bash|Write|Edit|Read`: 특정 도구들만

---

## ✅ 동작 확인

### 1. Claude Code 실행

```bash
claude
```

### 2. 권한이 필요한 작업 요청

```
> 현재 디렉토리의 파일 목록을 보여줘
```

Claude가 `Bash` 도구를 사용하려고 하면:
1. **Hook 트리거** → Spring Boot API 호출
2. **텔레그램 알림** 수신 📱
3. **버튼 클릭** (✅ 승인 or ❌ 거부)
4. **결과 반영** → Claude 계속 진행 or 중단

### 3. 로그 확인

```bash
# Hook 실행 로그
tail -f ~/.claude/claudeping.log

# Spring Boot 로그
# (실행 중인 터미널에서 확인)
```

---

## 🎯 고급 설정

### 특정 도구만 Hook

특정 작업에만 텔레그램 승인을 요청하려면:

```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "~/.claude/hooks/permission-request-hook.sh"
          }
        ]
      }
    ]
  }
}
```

### 여러 Hook 조합

```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "~/.claude/hooks/permission-request-hook.sh"
          }
        ]
      },
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "~/.claude/hooks/permission-request-hook.sh"
          }
        ]
      }
    ]
  }
}
```

### 프로젝트별 설정

특정 프로젝트에서만 Hook을 사용하려면:

`.claude/settings.json` (프로젝트 루트에 생성):

```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "~/.claude/hooks/permission-request-hook.sh"
          }
        ]
      }
    ]
  }
}
```

---

## 🐛 문제 해결

### Hook이 실행되지 않음

**확인 사항**:
```bash
# 1. 실행 권한
ls -la ~/.claude/hooks/permission-request-hook.sh
# -rwxr-xr-x 여야 함

# 2. jq 설치 확인
which jq

# 3. Spring Boot 실행 확인
curl http://localhost:8080/api/hook/health

# 4. Claude Code 설정 확인
cat ~/.claude/settings.json
```

### 텔레그램 알림이 안 옴

**확인 사항**:
```bash
# 1. 환경변수 확인
echo $TELEGRAM_BOT_TOKEN
echo $TELEGRAM_CHAT_ID

# 2. 봇에게 /start 메시지 전송했는지 확인

# 3. API 수동 테스트
curl -X POST http://localhost:8080/api/hook/ask \
  -H "Content-Type: application/json" \
  -d '{"event":"test","question":"테스트 질문"}'

# 텔레그램에서 알림이 와야 함
```

### 타임아웃 발생

`~/.claude/settings.json`에서 타임아웃 증가:

```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "~/.claude/hooks/permission-request-hook.sh",
            "timeout": 900
          }
        ]
      }
    ]
  }
}
```

### 디버그 모드

```bash
# Claude Code 디버그 모드로 실행
claude --debug

# Hook 실행 상세 로그 확인
```

---

## 📚 추가 리소스

- [Claude Code Hooks 공식 문서](https://code.claude.com/docs/ko/hooks)
- [Claude Code Hooks 가이드](https://code.claude.com/docs/ko/hooks-guide)
- [ClaudePing README](../README.md)
- [구현 방법 비교](./IMPLEMENTATION_OPTIONS.md)

---

## 🎉 완료!

이제 Claude Code가 권한을 요청할 때마다 텔레그램으로 알림을 받고, 버튼 클릭으로 즉시 응답할 수 있습니다!

**테스트 예시**:
```bash
claude
> 현재 디렉토리에 test.txt 파일을 만들어줘
```

📱 텔레그램 알림 → ✅ 승인 클릭 → Claude가 파일 생성! 🎯
