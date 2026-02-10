FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Gradle wrapper와 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# 소스 코드 복사
COPY src src

# 빌드 (의존성 캐싱을 위해 별도 단계)
RUN ./gradlew build -x test --no-daemon

# 포트 노출
EXPOSE 8080

# 환경변수 설정
ENV TELEGRAM_BOT_TOKEN=""
ENV TELEGRAM_CHAT_ID=""

# 실행
CMD ["./gradlew", "bootRun", "--no-daemon"]
