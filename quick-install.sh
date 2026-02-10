#!/bin/bash

# ClaudePing 원라이너 설치 스크립트
# 사용법: curl -fsSL https://raw.githubusercontent.com/[username]/claudeping/main/quick-install.sh | bash

set -e

REPO_URL="https://github.com/bright-flare/claude-ping.git"
INSTALL_DIR="$HOME/.claudeping"

echo "🚀 ClaudePing 빠른 설치를 시작합니다..."

# Git 확인
if ! command -v git &> /dev/null; then
    echo "❌ Git이 설치되어 있지 않습니다."
    echo "설치: brew install git (macOS) 또는 apt install git (Linux)"
    exit 1
fi

# 프로젝트 클론
echo "📦 프로젝트 다운로드 중..."
if [ -d "$INSTALL_DIR" ]; then
    echo "기존 설치를 발견했습니다. 업데이트 중..."
    cd "$INSTALL_DIR"
    git pull
else
    git clone "$REPO_URL" "$INSTALL_DIR"
    cd "$INSTALL_DIR"
fi

# install.sh 실행
echo "🔧 설치 스크립트 실행 중..."
chmod +x install.sh
./install.sh

echo ""
echo "✅ 설치 완료!"
echo ""
echo "다음 단계:"
echo "  1. cd $INSTALL_DIR"
echo "  2. ./run.sh"
echo ""
