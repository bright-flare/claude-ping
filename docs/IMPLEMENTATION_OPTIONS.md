# ClaudePing 구현 방법 가이드

Claude Code와 텔레그램을 연동하는 **3가지 방법**을 제공합니다.

---

## 🎯 방법 비교

| 방법 | 난이도 | 안정성 | 자동화 | Claude Code 의존성 |
|------|--------|--------|--------|---------------------|
| **1. Native Hook** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 높음 (Hook API 필요) |
| **2. Wrapper 스크립트** | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | 낮음 |
| **3. Interactive Wrapper** | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | 중간 |

---

## 🔧 방법 1: Native Hook (권장, Hook 지원 시)

### 개요
Claude Code의 공식 Hook 시스템을 사용하는 방법.

### 장점
- ✅ 가장 안정적
- ✅ Claude와 완벽한 통합
- ✅ 자동화 100%

### 단점
- ❌ Claude Code가 Hook API를 지원해야 함
- ❌ 설정이 복잡할 수 있음

### 설정 방법

#### 1) Hook 스크립트 설치
```bash
mkdir -p ~/.claude/hooks
ln -sf $(pwd)/hooks/ask-user-hook.sh ~/.claude/hooks/ask-user-hook.sh
chmod +x ~/.claude/hooks/ask-user-hook.sh
```

#### 2) Claude 설정
```bash
# ~/.claude/settings.json
{
  "hooks": {
    "before_command": "~/.claude/hooks/ask-user-hook.sh",
    "before_file_edit": "~/.claude/hooks/ask-user-hook.sh"
  }
}
```

#### 3) 테스트
```bash
# Spring Boot 실행
./gradlew bootRun

# Claude 실행
claude "파일을 수정해주세요"

# 텔레그램 알림 확인
```

### 문제 해결
- Hook이 실행 안 됨 → [CLAUDE_HOOK_SETUP.md](./CLAUDE_HOOK_SETUP.md) 참고
- 환경변수 확인 → `~/.claude/claudeping.log` 로그 확인

---

## 🔧 방법 2: Wrapper 스크립트 (간단하고 확실)

### 개요
Claude 명령어를 래핑하여 실행 전 승인 요청.

### 장점
- ✅ 설정이 매우 간단
- ✅ Claude Code 버전 무관
- ✅ 안정적

### 단점
- ❌ Claude 대신 wrapper를 실행해야 함
- ❌ Claude 내부의 세부 질문은 감지 못함

### 설정 방법

#### 1) 스크립트 설치
```bash
chmod +x scripts/claude-wrapper.sh

# PATH에 추가 (optional)
sudo ln -s $(pwd)/scripts/claude-wrapper.sh /usr/local/bin/claude-ping
```

#### 2) 사용법
```bash
# Spring Boot 실행
./gradlew bootRun

# Wrapper로 Claude 실행
./scripts/claude-wrapper.sh "파일을 수정해주세요"

# 또는 PATH에 추가했다면
claude-ping "파일을 수정해주세요"
```

#### 3) Alias 설정 (편의성)
```bash
# ~/.bashrc or ~/.zshrc
alias claude='~/path/to/claudeping/scripts/claude-wrapper.sh'

# 적용
source ~/.bashrc
```

이제 `claude` 명령어가 자동으로 텔레그램 승인을 거칩니다!

---

## 🔧 방법 3: Interactive Wrapper (고급)

### 개요
Claude의 출력을 실시간 파싱하여 질문을 감지.

### 장점
- ✅ Claude 내부의 질문도 감지 가능
- ✅ 실시간 처리
- ✅ 자동화 수준 높음

### 단점
- ❌ 복잡한 구현
- ❌ 패턴 매칭에 의존
- ❌ 안정성이 낮을 수 있음

### 설정 방법

#### 1) 스크립트 설치
```bash
chmod +x scripts/claude-interactive-wrapper.sh
```

#### 2) 질문 패턴 커스터마이징
```bash
# scripts/claude-interactive-wrapper.sh 편집
QUESTION_PATTERNS=(
    "Do you want"
    "Should I"
    "Would you like"
    # 원하는 패턴 추가
)
```

#### 3) 사용법
```bash
# Spring Boot 실행
./gradlew bootRun

# Interactive Wrapper로 실행
./scripts/claude-interactive-wrapper.sh "작업 수행"
```

#### 4) Alias 설정
```bash
alias claude='~/path/to/claudeping/scripts/claude-interactive-wrapper.sh'
```

---

## 📊 사용 시나리오별 추천

### 시나리오 1: Claude Code Hook 지원 확인됨
→ **방법 1: Native Hook** 사용

### 시나리오 2: 간단하게 빠르게 시작
→ **방법 2: Wrapper 스크립트** 사용

### 시나리오 3: Claude 내부 질문도 처리 필요
→ **방법 3: Interactive Wrapper** 사용

### 시나리오 4: Hook 지원 불확실
→ **방법 2**로 시작 → 필요시 **방법 1** or **방법 3**로 전환

---

## 🧪 테스트 방법

### 1. Spring Boot 서버 테스트
```bash
# 서버 실행
./gradlew bootRun

# 헬스체크
curl http://localhost:8080/api/hook/health

# 수동 승인 요청 테스트
curl -X POST http://localhost:8080/api/hook/ask \
  -H "Content-Type: application/json" \
  -d '{"event":"test","question":"테스트 질문입니다"}'

# 텔레그램에서 버튼 클릭 후 응답 확인
```

### 2. Hook 스크립트 테스트
```bash
export CLAUDE_QUESTION="테스트 질문"
./hooks/ask-user-hook.sh
```

### 3. Wrapper 테스트
```bash
./scripts/claude-wrapper.sh --version
```

---

## 🔄 전환 가이드

### 방법 2 → 방법 1
1. Claude Code Hook 지원 확인
2. `~/.claude/settings.json` 설정
3. Alias 제거

### 방법 2 → 방법 3
1. Alias를 `claude-interactive-wrapper.sh`로 변경
2. 질문 패턴 커스터마이징
3. 테스트

---

## 📚 다음 단계

1. 환경에 맞는 방법 선택
2. [QUICKSTART.md](../QUICKSTART.md)의 기본 설정 완료
3. 선택한 방법의 설정 진행
4. 테스트 및 검증

---

## 💡 팁

- **개발 중**: 방법 2 (간단)
- **프로덕션**: 방법 1 (안정적)
- **실험적**: 방법 3 (고급)

- 여러 방법을 동시에 설치해도 됩니다
- 필요에 따라 전환 가능합니다

---

**질문이나 문제가 있다면 [GitHub Issues](링크)에 등록해주세요!**
