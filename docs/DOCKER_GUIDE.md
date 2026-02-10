# Docker 배포 가이드

Docker를 사용한 ClaudePing 배포 방법을 설명합니다.

---

## 📦 사전 요구사항

- Docker 20.10+
- Docker Compose 2.0+

### 설치 확인

```bash
docker --version
docker-compose --version
```

---

## 🚀 빠른 시작

### 1. 환경변수 설정

```bash
# .env 파일 생성
cat > .env << EOF
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_CHAT_ID=your_chat_id_here
EOF
```

### 2. 컨테이너 실행

```bash
# 백그라운드 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f claudeping
```

### 3. Hook 설정

Docker는 서비스만 실행하므로, Hook은 호스트에서 설정해야 합니다:

```bash
# Simple Hook 설치 (권장)
ln -sf $(pwd)/hooks/permission-request-hook-simple.sh \
       ~/.claude/hooks/claudeping-hook

chmod +x ~/.claude/hooks/claudeping-hook
```

### 4. Claude 설정

`~/.claude/settings.json`에 Hook 설정 추가:

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

## 🔧 Docker Compose 설정

### docker-compose.yml

```yaml
version: '3.8'

services:
  claudeping:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: claudeping
    ports:
      - "8080:8080"
    environment:
      - TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}
      - TELEGRAM_CHAT_ID=${TELEGRAM_CHAT_ID}
    env_file:
      - .env
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/hook/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    volumes:
      - ./logs:/app/logs
    networks:
      - claudeping-network

networks:
  claudeping-network:
    driver: bridge
```

### 주요 설정

- **Ports**: 8080 포트로 API 노출
- **Environment**: .env 파일에서 환경변수 로드
- **Restart**: 실패 시 자동 재시작
- **Healthcheck**: 30초마다 헬스 체크
- **Volumes**: 로그 파일 영속화
- **Networks**: 격리된 네트워크 사용

---

## 🛠️ Docker 명령어

### 기본 명령

```bash
# 컨테이너 시작
docker-compose up -d

# 컨테이너 중지
docker-compose down

# 재시작
docker-compose restart

# 로그 보기
docker-compose logs -f

# 상태 확인
docker-compose ps
```

### 빌드 & 업데이트

```bash
# 이미지 재빌드
docker-compose build --no-cache

# 재빌드 후 재시작
docker-compose up -d --build
```

### 컨테이너 접속

```bash
# 셸 접속
docker-compose exec claudeping sh

# 로그 파일 확인
docker-compose exec claudeping cat logs/application.log
```

### 클린업

```bash
# 컨테이너와 네트워크 제거
docker-compose down

# 볼륨까지 제거
docker-compose down -v

# 이미지까지 제거
docker-compose down --rmi all
```

---

## 🔍 문제 해결

### 1. 컨테이너가 시작되지 않을 때

```bash
# 로그 확인
docker-compose logs claudeping

# 상세 로그
docker-compose logs -f --tail=100 claudeping

# 컨테이너 상태 확인
docker-compose ps
```

### 2. 환경변수가 적용되지 않을 때

```bash
# 환경변수 확인
docker-compose config

# 컨테이너 내부 환경변수 확인
docker-compose exec claudeping env | grep TELEGRAM
```

### 3. 포트 충돌

```bash
# 8080 포트 사용 중인 프로세스 확인
lsof -i :8080

# docker-compose.yml에서 포트 변경
ports:
  - "8081:8080"  # 호스트 포트를 8081로 변경
```

### 4. 헬스 체크 실패

```bash
# 수동으로 헬스 체크
curl http://localhost:8080/api/hook/health

# 컨테이너 내부에서 확인
docker-compose exec claudeping curl http://localhost:8080/api/hook/health
```

### 5. Hook이 Docker 서비스에 연결되지 않을 때

Hook 스크립트의 API URL 확인:

```bash
# hooks/permission-request-hook-simple.sh
API_URL="http://localhost:8080/api/claude-hook/permission-request"
```

Docker가 다른 호스트나 포트에서 실행 중이면 URL 수정:

```bash
# 예: 다른 포트 사용 시
API_URL="http://localhost:8081/api/claude-hook/permission-request"

# 예: 원격 서버 사용 시
API_URL="http://192.168.1.100:8080/api/claude-hook/permission-request"
```

---

## 🌐 프로덕션 배포

### 1. 환경변수 보안

프로덕션에서는 .env 파일 대신 Docker Secrets 사용 권장:

```yaml
version: '3.8'

services:
  claudeping:
    # ...
    secrets:
      - telegram_bot_token
      - telegram_chat_id
    environment:
      - TELEGRAM_BOT_TOKEN=/run/secrets/telegram_bot_token
      - TELEGRAM_CHAT_ID=/run/secrets/telegram_chat_id

secrets:
  telegram_bot_token:
    file: ./secrets/telegram_bot_token.txt
  telegram_chat_id:
    file: ./secrets/telegram_chat_id.txt
```

### 2. 리버스 프록시 (Nginx)

```nginx
server {
    listen 80;
    server_name claudeping.example.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. HTTPS 설정 (Let's Encrypt)

```bash
# Certbot 설치
apt-get install certbot python3-certbot-nginx

# 인증서 발급
certbot --nginx -d claudeping.example.com
```

### 4. 로그 관리

```yaml
services:
  claudeping:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 5. 자동 재시작

```yaml
services:
  claudeping:
    restart: always
    deploy:
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
```

---

## 📊 모니터링

### 헬스 체크

```bash
# 주기적인 헬스 체크
watch -n 5 curl http://localhost:8080/api/hook/health
```

### 리소스 사용량

```bash
# 컨테이너 리소스 모니터링
docker stats claudeping

# 상세 정보
docker-compose top
```

### 로그 수집

```bash
# 로그 저장
docker-compose logs > claudeping-$(date +%Y%m%d).log

# 에러 로그만 필터링
docker-compose logs | grep ERROR > errors.log
```

---

## 🚀 성능 최적화

### 1. 멀티스테이지 빌드

Dockerfile은 이미 멀티스테이지 빌드를 사용 중:

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
# ... 빌드 단계는 이미 최적화됨
```

### 2. 이미지 크기 최적화

```bash
# 이미지 크기 확인
docker images | grep claudeping

# 불필요한 레이어 제거
docker-compose build --no-cache
```

### 3. 리소스 제한

```yaml
services:
  claudeping:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

---

## 🔄 업데이트 절차

```bash
# 1. 최신 코드 가져오기
git pull

# 2. 컨테이너 중지
docker-compose down

# 3. 이미지 재빌드
docker-compose build --no-cache

# 4. 컨테이너 재시작
docker-compose up -d

# 5. 로그 확인
docker-compose logs -f
```

---

## 🎯 Docker vs Native 비교

| 특징 | Docker | Native |
|------|--------|--------|
| 설치 난이도 | ⭐⭐ 쉬움 | ⭐⭐⭐ 중간 |
| 의존성 관리 | ✅ 격리됨 | ⚠️ 수동 관리 |
| 포트 충돌 | ✅ 쉽게 변경 | ⚠️ 수동 해결 |
| 업데이트 | ✅ 이미지 재빌드 | ⚠️ 수동 재빌드 |
| 성능 | ⭐⭐⭐⭐ 약간 오버헤드 | ⭐⭐⭐⭐⭐ 네이티브 |
| 이식성 | ✅ 어디서든 동일 | ⚠️ 환경 의존적 |

### 권장 사항

- **개발**: Native 설치 (빠른 반복 개발)
- **프로덕션**: Docker (안정성과 이식성)
- **테스트**: Docker (일관된 환경)

---

## 📖 추가 자료

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 레퍼런스](https://docs.docker.com/compose/)
- [Spring Boot Docker 가이드](https://spring.io/guides/topicals/spring-boot-docker/)

---

**Docker로 ClaudePing을 더 쉽게 배포하세요!** 🐳
