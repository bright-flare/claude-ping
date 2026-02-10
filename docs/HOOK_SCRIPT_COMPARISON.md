# Hook 스크립트 버전 비교

ClaudePing은 2가지 Hook 스크립트 버전을 제공합니다. 환경에 맞게 선택하세요!

---

## 📊 버전 비교

| 특징 | Bash 버전 | Python 버전 |
|------|-----------|-------------|
| **파일** | `permission-request-hook.sh` | `permission-request-hook.py` |
| **의존성** | `jq` 필요 | Python 3.6+ (표준 라이브러리만) |
| **설치** | `brew install jq` | 대부분 시스템에 기본 설치됨 |
| **성능** | 매우 빠름 | 빠름 |
| **가독성** | 보통 | 높음 |
| **유지보수** | 보통 | 쉬움 |
| **에러 처리** | 기본적 | 강력함 |
| **권장** | jq가 이미 있다면 | Python이 있다면 (대부분) |

---

## 🎯 선택 가이드

### Bash 버전을 선택하세요 (`permission-request-hook.sh`)

✅ **다음과 같은 경우**:
- jq가 이미 설치되어 있음
- 최고 성능이 필요함
- Bash 스크립팅에 익숙함
- 가벼운 의존성 선호

❌ **피해야 하는 경우**:
- jq 설치가 어려운 환경
- Python이 이미 있고 jq가 없음

### Python 버전을 선택하세요 (`permission-request-hook.py`)

✅ **다음과 같은 경우**:
- Python이 설치되어 있음 (대부분의 시스템)
- jq를 설치하고 싶지 않음
- 더 나은 에러 처리가 필요함
- 향후 기능 확장 계획이 있음

❌ **피해야 하는 경우**:
- Python이 설치되지 않은 환경
- 최소한의 의존성만 원함

---

## 🚀 설치 방법

### Option 1: Bash 버전

```bash
# 1. jq 설치
# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq

# Arch Linux
sudo pacman -S jq

# 2. Hook 설치
mkdir -p ~/.claude/hooks
ln -sf $(pwd)/hooks/permission-request-hook.sh \
       ~/.claude/hooks/permission-request-hook.sh
chmod +x ~/.claude/hooks/permission-request-hook.sh

# 3. Claude 설정
# ~/.claude/settings.json
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

### Option 2: Python 버전

```bash
# 1. Python 확인 (보통 이미 설치되어 있음)
python3 --version  # Python 3.6+ 필요

# 2. Hook 설치
mkdir -p ~/.claude/hooks
ln -sf $(pwd)/hooks/permission-request-hook.py \
       ~/.claude/hooks/permission-request-hook.py
chmod +x ~/.claude/hooks/permission-request-hook.py

# 3. Claude 설정
# ~/.claude/settings.json
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

---

## 🧪 테스트

### Bash 버전 테스트

```bash
echo '{"tool_name":"Bash","tool_input":{"command":"ls","description":"List files"}}' | \
  ~/.claude/hooks/permission-request-hook.sh
```

### Python 버전 테스트

```bash
echo '{"tool_name":"Bash","tool_input":{"command":"ls","description":"List files"}}' | \
  ~/.claude/hooks/permission-request-hook.py
```

둘 다 텔레그램 알림을 보내고 버튼 클릭 후 JSON 응답을 출력해야 합니다.

---

## 🔍 내부 동작 비교

### JSON 파싱

**Bash 버전**:
```bash
TOOL_NAME=$(echo "$INPUT_JSON" | jq -r '.tool_name // "Unknown"')
COMMAND=$(echo "$INPUT_JSON" | jq -r '.tool_input.command // "unknown"')
```

**Python 버전**:
```python
input_data = json.load(sys.stdin)
tool_name = input_data.get("tool_name", "Unknown")
command = tool_input.get("command", "unknown")
```

### API 호출

**Bash 버전**:
```bash
curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "$JSON_PAYLOAD" \
  --max-time 600 \
  "$API_URL"
```

**Python 버전**:
```python
import urllib.request
req = urllib.request.Request(
    API_URL,
    data=json.dumps(payload).encode('utf-8'),
    headers={'Content-Type': 'application/json'}
)
response = urllib.request.urlopen(req, timeout=600)
```

---

## 💡 추천

### 일반 사용자

**Python 버전 권장** 🐍
- 대부분의 시스템에 Python이 이미 있음
- 추가 설치 불필요
- 더 나은 에러 메시지

### DevOps/고급 사용자

**Bash 버전 권장** 💻
- 성능 우선
- 시스템 도구 선호
- jq는 유용한 도구

---

## 🔄 전환하기

언제든지 버전을 바꿀 수 있습니다:

```bash
# Bash → Python
rm ~/.claude/hooks/permission-request-hook.sh
ln -sf $(pwd)/hooks/permission-request-hook.py \
       ~/.claude/hooks/permission-request-hook.py

# ~/.claude/settings.json에서 경로만 변경
"command": "~/.claude/hooks/permission-request-hook.py"
```

```bash
# Python → Bash
rm ~/.claude/hooks/permission-request-hook.py
ln -sf $(pwd)/hooks/permission-request-hook.sh \
       ~/.claude/hooks/permission-request-hook.sh

# ~/.claude/settings.json에서 경로만 변경
"command": "~/.claude/hooks/permission-request-hook.sh"
```

---

## 📝 정리

### jq가 필요한 이유

**Bash 버전에서만 필요**:
- JSON 파싱을 위한 표준 도구
- Bash에는 내장 JSON 파서가 없음
- 안전하고 정확한 JSON 처리

**Python 버전은 불필요**:
- Python 표준 라이브러리에 `json` 모듈 내장
- 추가 도구 설치 불필요

### 결론

- **jq 있음** → Bash 버전 사용
- **jq 없음** → Python 버전 사용 (대부분의 경우)
- **Python도 없음** → jq 설치 후 Bash 버전 사용

대부분의 사용자는 **Python 버전**이 더 편리합니다! 🎯
