# 왜 Hook 스크립트가 필요한가?

이 문서는 "왜 API를 직접 호출하지 못하고 중간에 스크립트를 거쳐야 하는가?"에 대한 답변입니다.

---

## 🤔 질문

**"Hook을 Python이나 Shell로 만들어야 해? 현재 프로젝트의 API로는 불가능한가?"**

---

## 💡 핵심 답변

### Claude Code Hook은 "셸 명령"만 실행 가능

Claude Code Hook 설정:
```json
{
  "type": "command",
  "command": "실행할 셸 명령어"
}
```

**제한사항**:
- HTTP 엔드포인트를 직접 호출할 수 없음
- 오직 셸 명령(bash, python, curl 등)만 실행 가능
- stdin으로 JSON 입력, stdout으로 JSON 출력

**즉, API를 호출하려면 어떤 형태로든 "명령어"가 필요합니다.**

---

## 🔧 3가지 접근 방법

### 방법 1: 기존 방식 (Python/Bash Hook)

```
Claude → Hook Script (Python/Bash) → Spring Boot API → Telegram
          ↓ JSON 변환
          ↓ 질문 생성
          ↓ 응답 변환
```

**장점**:
- ✅ 완전한 제어
- ✅ 커스텀 로직 추가 가능
- ✅ 자세한 로깅
- ✅ 에러 처리 강력

**단점**:
- ❌ 스크립트 파일 필요
- ❌ Python/jq 의존성

**파일**:
- `hooks/permission-request-hook.py` (Python)
- `hooks/permission-request-hook.sh` (Bash + jq)

---

### 방법 2: ✨ API 직접 지원 (새로운 방법)

API가 Claude Hook 형식을 직접 처리하도록 수정:

```
Claude → 초간단 Hook (curl만) → Spring Boot API (Hook 형식 지원) → Telegram
```

**구현**:
1. **새 API 엔드포인트**: `/api/claude-hook/permission-request`
   - Claude Hook JSON을 그대로 받음
   - 질문 생성 로직 포함
   - Claude Hook 응답 형식으로 반환

2. **초간단 Hook 스크립트**:
```bash
#!/bin/bash
curl -X POST http://localhost:8080/api/claude-hook/permission-request \
  -H "Content-Type: application/json" \
  -d @- \
  --max-time 600
```

**장점**:
- ✅ 스크립트가 매우 간단 (5줄)
- ✅ Python/jq 불필요
- ✅ 로직이 API에 집중됨
- ✅ 유지보수 쉬움

**단점**:
- ⚠️ 여전히 Hook 스크립트 필요 (curl 실행용)
- ⚠️ API 코드 추가 필요

**파일**:
- `hooks/permission-request-hook-simple.sh` (5줄 curl 스크립트)
- `ClaudeHookController.kt` (새 API 엔드포인트)

---

### 방법 3: curl inline (가장 간단하지만 제한적)

```json
{
  "type": "command",
  "command": "curl -X POST http://localhost:8080/api/claude-hook/permission-request -H 'Content-Type: application/json' -d @-"
}
```

**장점**:
- ✅ 파일 없음
- ✅ 설정만으로 동작

**단점**:
- ❌ 에러 처리 없음
- ❌ 로깅 없음
- ❌ 디버깅 어려움
- ❌ 타임아웃 처리 복잡

---

## 📊 비교표

| 특징 | Python/Bash Hook | API 직접 지원 + 간단 Hook | curl inline |
|------|------------------|---------------------------|-------------|
| **스크립트 복잡도** | 높음 (100줄) | 매우 낮음 (5줄) | 없음 |
| **의존성** | Python 또는 jq | curl만 | curl만 |
| **유지보수** | 스크립트 수정 | API 수정 | 설정 변경 |
| **에러 처리** | 강력 | 중간 | 약함 |
| **로깅** | 자세함 | 중간 | 없음 |
| **커스터마이징** | 쉬움 | API에서 | 어려움 |
| **권장** | 고급 사용 | ⭐ 일반 사용 | 테스트용 |

---

## 🎯 결론

### Q: Hook 스크립트가 꼭 필요한가?

**A: 네, 어떤 형태로든 필요합니다.**

이유:
1. Claude Hook은 "셸 명령"만 실행
2. API를 호출하려면 `curl` 같은 명령 필요
3. 최소한 "curl을 실행하는 스크립트" 필요

### Q: 가장 간단한 방법은?

**A: 방법 2 (API 직접 지원 + 간단 Hook) 권장**

장점:
- 5줄짜리 Hook 스크립트
- 의존성 최소 (curl만)
- 로직이 API에 집중
- 에러 처리 적절

**구현**:
```bash
# 1. 새 API 사용 (이미 구현됨)
# ClaudeHookController 참고

# 2. 간단한 Hook 사용
ln -sf $(pwd)/hooks/permission-request-hook-simple.sh \
       ~/.claude/hooks/permission-request-hook.sh

# 3. 설정
{
  "hooks": {
    "PermissionRequest": [{
      "matcher": "*",
      "hooks": [{
        "type": "command",
        "command": "~/.claude/hooks/permission-request-hook.sh"
      }]
    }]
  }
}
```

---

## 🚀 최종 추천

### 일반 사용자

**방법 2** 사용:
- `hooks/permission-request-hook-simple.sh` (5줄)
- `ClaudeHookController` API
- curl만 있으면 됨

### 고급 사용자

**방법 1** 사용:
- 커스텀 로직 필요 시
- 복잡한 조건 처리
- 자세한 로깅 필요

### 빠른 테스트

**방법 3** 사용:
- 설정에 curl 직접 입력
- 프로토타입용

---

## 💡 핵심 정리

**Hook 스크립트는 필요하지만, 매우 간단하게 만들 수 있습니다!**

```bash
# 전체 Hook 스크립트 (5줄)
#!/bin/bash
curl -X POST http://localhost:8080/api/claude-hook/permission-request \
  -H "Content-Type: application/json" \
  -d @- \
  --max-time 600
```

**복잡한 로직은 API에서 처리**하고, Hook은 단순히 **API를 호출하는 브릿지 역할**만 합니다.

---

## 📖 관련 파일

- `hooks/permission-request-hook-simple.sh` - 간단 Hook (권장)
- `hooks/permission-request-hook.py` - 기존 Python Hook
- `hooks/permission-request-hook.sh` - 기존 Bash Hook
- `ClaudeHookController.kt` - Hook 형식 직접 지원 API

---

**더 간단해졌습니다! 🎉**
