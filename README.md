# ClaudePing 📱

Claude Code의 권한 요청을 텔레그램으로 받아서 응답할 수 있는 시스템입니다.

## ✨ 특징

- 🤖 **Claude Code 통합**: PermissionRequest Hook을 통한 완벽한 통합
- 📱 **텔레그램 알림**: 실시간 알림과 버튼으로 간편한 응답
- 🚀 **간편한 설치**: 원라이너 스크립트 또는 자동 설치
- 🐳 **Docker 지원**: 컨테이너 기반 배포 옵션
- 🔧 **유연한 설정**: 3가지 Hook 스크립트 옵션 (Simple/Python/Bash)

## 📋 아키텍처

```
Claude Code → Hook Script → Spring Boot API → Telegram Bot
                 ↑              ↓
                 └──── 응답 대기 ────┘
```

### 흐름 설명

1. **Claude Code**: 작업 권한 요청 (예: 파일 읽기, Bash 실행)
2. **Hook Script**: 요청을 Spring Boot API로 전달
3. **Spring Boot**: 텔레그램으로 알림 전송, 사용자 응답 대기
4. **Telegram Bot**: 사용자에게 질문과 버튼 표시
5. **사용자**: 버튼 클릭으로 승인/거부
6. **Spring Boot**: 응답을 Hook에 반환
7. **Hook**: Claude Code에 결과 전달
8. **Claude Code**: 승인 시 작업 수행, 거부 시 중단

## 🚀 빠른 시작

### 방법 1: 원라이너 설치 (권장)

```bash
curl -fsSL https://raw.githubusercontent.com/brightflare/claudeping/main/quick-install.sh | bash
```

### 방법 2: 자동 설치

```bash
# 1. 프로젝트 클론
git clone https://github.com/brightflare/claudeping.git
cd claudeping

# 2. 자동 설치 실행
./install.sh
```

### 방법 3: Docker Compose

```bash
# 1. .env 파일 생성
cat > .env << EOF
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_CHAT_ID=your_chat_id_here
EOF

# 2. Docker Compose 실행
docker-compose up -d

# 3. Hook 설정 (호스트에서)
ln -sf $(pwd)/hooks/permission-request-hook-simple.sh \
       ~/.claude/hooks/claudeping-hook
```

## 📋 사전 요구사항

### 기본 설치
- Java 21 이상
- curl

### Hook 버전별 요구사항
- **Simple Hook** (권장): curl만 필요
- **Python Hook**: Python 3.6+
- **Bash Hook**: bash + jq

### Docker 설치
- Docker & Docker Compose

## 🔧 설정

### 1. 텔레그램 봇 생성

1. [@BotFather](https://t.me/BotFather)에게 `/newbot` 명령 전송
2. 봇 이름과 사용자명 설정
3. 봇 토큰 복사 (예: `123456789:ABCdefGHIjklMNOpqrsTUVwxyz`)

### 2. Chat ID 확인

1. [@userinfobot](https://t.me/userinfobot)에게 메시지 전송
2. Chat ID 복사 (예: `987654321`)
3. 생성한 봇에게 `/start` 메시지 전송 (중요!)

### 3. Claude Code 설정

자동 설치 스크립트가 자동으로 설정하거나, 수동으로 `~/.claude/settings.json`에 추가:

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

## 🎯 사용법

### 1. 서비스 시작

```bash
# 직접 실행
./run.sh

# 또는 Gradle로
./gradlew bootRun

# 또는 Docker로
docker-compose up -d
```

### 2. Claude Code 사용

```bash
claude
> 현재 디렉토리의 파일 목록을 보여줘
```

### 3. 텔레그램에서 응답

Claude Code가 작업을 요청하면:
1. 텔레그램으로 알림 수신
2. 질문 내용 확인
3. **✅ 승인** 또는 **❌ 거부** 버튼 클릭
4. Claude가 선택에 따라 작업 수행

## 📖 상세 문서

- [설치 가이드](docs/SETUP_GUIDE.md) - 상세한 설치 방법
- [Hook API 레퍼런스](docs/HOOK_API_REFERENCE.md) - Claude Code Hook 명세
- [Hook 스크립트 비교](docs/HOOK_SCRIPT_COMPARISON.md) - 3가지 Hook 버전 비교
- [왜 Hook 스크립트가 필요한가?](docs/WHY_HOOK_SCRIPT.md) - 아키텍처 설명
- [Docker 가이드](docs/DOCKER_GUIDE.md) - Docker 배포 방법

## 🔍 문제 해결

### 서비스가 시작되지 않을 때

```bash
# 로그 확인
tail -f ~/.claude/claudeping.log

# 또는 Docker 로그
docker-compose logs -f claudeping
```

### 텔레그램 알림이 오지 않을 때

1. 봇 토큰과 Chat ID 확인
2. 봇에게 `/start` 메시지를 보냈는지 확인
3. 환경변수가 제대로 로드되었는지 확인

```bash
# 환경변수 확인
echo $TELEGRAM_BOT_TOKEN
echo $TELEGRAM_CHAT_ID
```

### Hook이 동작하지 않을 때

1. Hook 스크립트 실행 권한 확인
```bash
ls -l ~/.claude/hooks/claudeping-hook
chmod +x ~/.claude/hooks/claudeping-hook
```

2. Claude 설정 파일 확인
```bash
cat ~/.claude/settings.json
```

3. 서비스가 실행 중인지 확인
```bash
curl http://localhost:8080/api/hook/health
```

## 🛠️ 개발

### 로컬 개발 환경

```bash
# 의존성 설치 및 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 개발 모드 실행
./gradlew bootRun
```

### API 엔드포인트

- `GET /api/hook/health` - 헬스 체크
- `POST /api/hook/request` - 수동 승인 요청 (테스트용)
- `POST /api/claude-hook/permission-request` - Claude Hook 전용 엔드포인트

### API 테스트

```bash
# 헬스 체크
curl http://localhost:8080/api/hook/health

# 승인 요청 테스트
curl -X POST http://localhost:8080/api/hook/request \
  -H "Content-Type: application/json" \
  -d '{
    "question": "파일을 읽으시겠습니까?",
    "context": "test.txt 파일 읽기"
  }'
```

## 🤝 기여

기여는 언제나 환영합니다!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이센스

MIT License - 자유롭게 사용하세요!

## 💡 왜 ClaudePing인가?

Claude Code는 강력하지만, 터미널 앞에 있지 않으면 권한 요청에 응답할 수 없습니다. ClaudePing은 이 문제를 해결합니다:

- ☕ 커피를 마시면서도 Claude의 질문에 답할 수 있습니다
- 🏃 산책 중에도 작업을 승인할 수 있습니다
- 💼 회의 중에도 긴급한 배포를 승인할 수 있습니다

**어디서든, 언제든, Claude와 함께하세요!** 🚀

## 📞 지원

- 이슈: [GitHub Issues](https://github.com/brightflare/claudeping/issues)
- 문의: [GitHub Discussions](https://github.com/brightflare/claudeping/discussions)

---

Made with ❤️ by [brightflare](https://github.com/brightflare)
