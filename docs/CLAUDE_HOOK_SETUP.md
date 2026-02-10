# Claude Code Hook 설정 가이드

## ⚠️ 중요: Hook 지원 여부 확인

이 프로젝트는 Claude Code가 **Hook 시스템**을 지원한다고 가정하고 만들어졌습니다.
실제 사용 전에 Claude Code 문서를 확인해야 합니다.

---

## 📋 Hook 설정 단계

### 1. Claude Code Hook 지원 확인

```bash
# Claude Code 버전 확인
claude --version

# 설정 파일 위치 확인
ls -la ~/.claude/
```

Claude Code 공식 문서 확인:
- Hook 이벤트 타입
- 환경변수 전달 방식
- Exit code 처리 방식

---

### 2. Hook 스크립트 설치

```bash
# Hook 디렉토리 생성
mkdir -p ~/.claude/hooks

# 프로젝트의 Hook 스크립트 링크
ln -sf /path/to/claudeping/hooks/ask-user-hook.sh \
       ~/.claude/hooks/ask-user-hook.sh

# 실행 권한 부여
chmod +x ~/.claude/hooks/ask-user-hook.sh
```

---

### 3. Claude Code 설정 파일 수정

`~/.claude/settings.json` 파일 생성/수정:

#### 옵션 A: 직접 수정
```json
{
  "hooks": {
    "before_command": "~/.claude/hooks/ask-user-hook.sh",
    "before_file_edit": "~/.claude/hooks/ask-user-hook.sh",
    "before_dangerous_operation": "~/.claude/hooks/ask-user-hook.sh"
  }
}
```

#### 옵션 B: CLI로 설정 (Claude Code가 지원하는 경우)
```bash
claude config set hook.before_command ~/.claude/hooks/ask-user-hook.sh
```

---

### 4. 환경변수 전달 확인

Hook 스크립트는 다음 환경변수를 기대합니다:

```bash
CLAUDE_QUESTION     # Claude의 질문 내용
CLAUDE_CONTEXT      # 추가 컨텍스트 (optional)
CLAUDE_EVENT_TYPE   # 이벤트 타입 (optional)
```

**테스트 방법**:
```bash
# 수동으로 Hook 테스트
export CLAUDE_QUESTION="테스트 질문입니다"
~/.claude/hooks/ask-user-hook.sh
```

---

### 5. 동작 확인

1. Spring Boot 애플리케이션 실행
   ```bash
   cd /path/to/claudeping
   ./gradlew bootRun
   ```

2. Claude Code 실행
   ```bash
   claude "파일을 수정해주세요"
   ```

3. 텔레그램 알림 확인

---

## 🚨 문제 해결

### Hook이 실행되지 않는 경우

1. **실행 권한 확인**
   ```bash
   ls -la ~/.claude/hooks/ask-user-hook.sh
   # -rwxr-xr-x 여야 함
   ```

2. **로그 확인**
   ```bash
   tail -f ~/.claude/claudeping.log
   ```

3. **수동 테스트**
   ```bash
   # Hook 직접 실행
   export CLAUDE_QUESTION="테스트"
   ~/.claude/hooks/ask-user-hook.sh
   ```

4. **서버 연결 확인**
   ```bash
   curl http://localhost:8080/api/hook/health
   ```

---

## 🔄 대체 방안

Claude Code Hook이 지원되지 않는 경우:

### 방안 1: Claude Wrapper 스크립트
```bash
#!/bin/bash
# claude-with-approval.sh

# Spring Boot 서버에 질문 전송
curl -X POST http://localhost:8080/api/hook/ask \
  -H "Content-Type: application/json" \
  -d "{\"event\":\"manual\",\"question\":\"$1\"}"

# 승인되면 Claude 실행
if [ $? -eq 0 ]; then
    claude "$@"
fi
```

### 방안 2: stdout 파싱
Claude의 출력을 실시간으로 파싱하여 질문 감지

### 방안 3: Interactive Mode
Claude와의 대화를 중간에서 가로채기

---

## 📚 참고 문서

- Claude Code 공식 문서: [링크 확인 필요]
- Hook API 레퍼런스: [링크 확인 필요]
- 커뮤니티 포럼: [링크 확인 필요]

---

**중요**: 이 문서는 Claude Code Hook 시스템이 존재한다고 가정합니다.
실제 구현 전에 공식 문서를 반드시 확인하세요.
