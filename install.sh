#!/bin/bash

set -e

# ClaudePing 자동 설치 스크립트
# 모든 설정을 대화형으로 진행합니다

BOLD='\033[1m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BOLD}${GREEN}"
echo "╔═══════════════════════════════════════╗"
echo "║     ClaudePing 자동 설치 스크립트     ║"
echo "╚═══════════════════════════════════════╝"
echo -e "${NC}"

# 프로젝트 루트 디렉토리
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo -e "${YELLOW}📍 프로젝트 경로: $PROJECT_DIR${NC}\n"

# 1. 필수 도구 확인
echo -e "${BOLD}1️⃣  필수 도구 확인${NC}"

# Java 확인
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java가 설치되어 있지 않습니다.${NC}"
    echo "Java 21을 설치해주세요: https://adoptium.net/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}❌ Java 17 이상이 필요합니다. (현재: $JAVA_VERSION)${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Java $JAVA_VERSION${NC}"

# curl 확인
if ! command -v curl &> /dev/null; then
    echo -e "${RED}❌ curl이 설치되어 있지 않습니다.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ curl${NC}"

# 2. 텔레그램 봇 설정
echo -e "\n${BOLD}2️⃣  텔레그램 봇 설정${NC}"
echo -e "텔레그램 봇이 없다면 먼저 생성해주세요:"
echo -e "  1. @BotFather에게 /newbot 명령 전송"
echo -e "  2. 봇 토큰 복사"
echo -e "  3. @userinfobot에게 메시지 전송하여 Chat ID 확인"
echo -e "  4. 생성한 봇에게 /start 전송\n"

read -p "텔레그램 봇 토큰을 입력하세요: " BOT_TOKEN
if [ -z "$BOT_TOKEN" ]; then
    echo -e "${RED}❌ 봇 토큰은 필수입니다.${NC}"
    exit 1
fi

read -p "텔레그램 Chat ID를 입력하세요: " CHAT_ID
if [ -z "$CHAT_ID" ]; then
    echo -e "${RED}❌ Chat ID는 필수입니다.${NC}"
    exit 1
fi

# 3. 환경변수 파일 생성
echo -e "\n${BOLD}3️⃣  환경변수 설정${NC}"

cat > .env << EOF
# ClaudePing 환경변수
TELEGRAM_BOT_TOKEN=$BOT_TOKEN
TELEGRAM_CHAT_ID=$CHAT_ID
EOF

echo -e "${GREEN}✅ .env 파일 생성 완료${NC}"

# 셸 프로필에도 추가
SHELL_PROFILE=""
if [ -f "$HOME/.zshrc" ]; then
    SHELL_PROFILE="$HOME/.zshrc"
elif [ -f "$HOME/.bashrc" ]; then
    SHELL_PROFILE="$HOME/.bashrc"
fi

if [ -n "$SHELL_PROFILE" ]; then
    read -p "셸 프로필($SHELL_PROFILE)에 환경변수를 추가할까요? (y/n): " ADD_TO_PROFILE
    if [[ "$ADD_TO_PROFILE" =~ ^[Yy]$ ]]; then
        # 기존 설정 제거
        sed -i.bak '/# ClaudePing/d' "$SHELL_PROFILE"
        sed -i.bak '/TELEGRAM_BOT_TOKEN/d' "$SHELL_PROFILE"
        sed -i.bak '/TELEGRAM_CHAT_ID/d' "$SHELL_PROFILE"

        # 새 설정 추가
        echo "" >> "$SHELL_PROFILE"
        echo "# ClaudePing" >> "$SHELL_PROFILE"
        echo "export TELEGRAM_BOT_TOKEN=\"$BOT_TOKEN\"" >> "$SHELL_PROFILE"
        echo "export TELEGRAM_CHAT_ID=\"$CHAT_ID\"" >> "$SHELL_PROFILE"

        echo -e "${GREEN}✅ $SHELL_PROFILE 업데이트 완료${NC}"
        echo -e "${YELLOW}⚠️  새 터미널에서 환경변수가 적용됩니다.${NC}"
    fi
fi

# 4. Hook 스크립트 선택
echo -e "\n${BOLD}4️⃣  Hook 스크립트 선택${NC}"
echo "사용할 Hook 스크립트를 선택하세요:"
echo "  1) Simple  - curl만 필요 (권장)"
echo "  2) Python  - Python 3.6+ 필요"
echo "  3) Bash    - bash + jq 필요"

read -p "선택 (1-3) [1]: " HOOK_CHOICE
HOOK_CHOICE=${HOOK_CHOICE:-1}

case $HOOK_CHOICE in
    1)
        HOOK_SOURCE="hooks/permission-request-hook-simple.sh"
        HOOK_NAME="Simple Hook"
        ;;
    2)
        if ! command -v python3 &> /dev/null; then
            echo -e "${RED}❌ Python3가 설치되어 있지 않습니다.${NC}"
            exit 1
        fi
        HOOK_SOURCE="hooks/permission-request-hook.py"
        HOOK_NAME="Python Hook"
        ;;
    3)
        if ! command -v jq &> /dev/null; then
            echo -e "${RED}❌ jq가 설치되어 있지 않습니다.${NC}"
            echo "설치: brew install jq"
            exit 1
        fi
        HOOK_SOURCE="hooks/permission-request-hook.sh"
        HOOK_NAME="Bash Hook"
        ;;
    *)
        echo -e "${RED}❌ 잘못된 선택입니다.${NC}"
        exit 1
        ;;
esac

# Hook 디렉토리 생성 및 링크
mkdir -p "$HOME/.claude/hooks"
ln -sf "$PROJECT_DIR/$HOOK_SOURCE" "$HOME/.claude/hooks/claudeping-hook"
chmod +x "$HOME/.claude/hooks/claudeping-hook"

echo -e "${GREEN}✅ $HOOK_NAME 설치 완료${NC}"

# 5. Claude 설정
echo -e "\n${BOLD}5️⃣  Claude Code 설정${NC}"

CLAUDE_SETTINGS="$HOME/.claude/settings.json"

# 기존 설정 백업
if [ -f "$CLAUDE_SETTINGS" ]; then
    cp "$CLAUDE_SETTINGS" "$CLAUDE_SETTINGS.backup.$(date +%Y%m%d_%H%M%S)"
    echo -e "${YELLOW}⚠️  기존 설정을 백업했습니다.${NC}"
fi

# settings.json 생성 또는 업데이트
if [ ! -f "$CLAUDE_SETTINGS" ]; then
    # 새로 생성
    cat > "$CLAUDE_SETTINGS" << 'EOF'
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
EOF
    echo -e "${GREEN}✅ Claude 설정 파일 생성 완료${NC}"
else
    echo -e "${YELLOW}⚠️  Claude 설정 파일이 이미 존재합니다.${NC}"
    echo "수동으로 다음 내용을 추가해주세요:"
    echo ""
    echo '{
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
}'
fi

# 6. 실행 스크립트 생성
echo -e "\n${BOLD}6️⃣  실행 스크립트 생성${NC}"

cat > run.sh << 'EOF'
#!/bin/bash

# ClaudePing 실행 스크립트

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# .env 파일 로드
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "🚀 ClaudePing 시작 중..."
./gradlew bootRun
EOF

chmod +x run.sh

echo -e "${GREEN}✅ run.sh 생성 완료${NC}"

# 7. systemd 서비스 생성 (선택)
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo -e "\n${BOLD}7️⃣  Systemd 서비스 설치 (선택)${NC}"
    read -p "백그라운드 서비스로 설치할까요? (y/n): " INSTALL_SERVICE

    if [[ "$INSTALL_SERVICE" =~ ^[Yy]$ ]]; then
        SERVICE_FILE="$HOME/.config/systemd/user/claudeping.service"
        mkdir -p "$HOME/.config/systemd/user"

        cat > "$SERVICE_FILE" << EOF
[Unit]
Description=ClaudePing - Telegram Approval for Claude Code
After=network.target

[Service]
Type=simple
WorkingDirectory=$PROJECT_DIR
Environment="TELEGRAM_BOT_TOKEN=$BOT_TOKEN"
Environment="TELEGRAM_CHAT_ID=$CHAT_ID"
ExecStart=$PROJECT_DIR/gradlew bootRun
Restart=on-failure
RestartSec=10

[Install]
WantedBy=default.target
EOF

        systemctl --user daemon-reload
        systemctl --user enable claudeping.service

        echo -e "${GREEN}✅ Systemd 서비스 설치 완료${NC}"
        echo "시작: systemctl --user start claudeping"
        echo "중지: systemctl --user stop claudeping"
        echo "상태: systemctl --user status claudeping"
    fi
fi

# 8. 설치 완료
echo -e "\n${BOLD}${GREEN}"
echo "╔═══════════════════════════════════════╗"
echo "║        설치 완료! 🎉                  ║"
echo "╚═══════════════════════════════════════╝"
echo -e "${NC}"

echo -e "${BOLD}다음 단계:${NC}"
echo ""
echo "1️⃣  ClaudePing 실행:"
echo -e "   ${GREEN}./run.sh${NC}"
echo ""
echo "2️⃣  또는 직접 실행:"
echo -e "   ${GREEN}./gradlew bootRun${NC}"
echo ""
echo "3️⃣  테스트:"
echo -e "   ${GREEN}curl http://localhost:8080/api/hook/health${NC}"
echo ""
echo "4️⃣  Claude Code 사용:"
echo -e "   ${GREEN}claude${NC}"
echo -e "   ${GREEN}> 현재 디렉토리의 파일 목록을 보여줘${NC}"
echo ""
echo -e "${YELLOW}📱 텔레그램에서 알림을 받고 버튼을 클릭하세요!${NC}"
echo ""
echo -e "${BOLD}문서:${NC}"
echo "  - README.md"
echo "  - docs/SETUP_GUIDE.md"
echo "  - docs/HOOK_SCRIPT_COMPARISON.md"
echo ""
echo -e "${BOLD}문제 해결:${NC}"
echo "  - 로그: ~/.claude/claudeping.log"
echo "  - 백업: ~/.claude/settings.json.backup.*"
echo ""
