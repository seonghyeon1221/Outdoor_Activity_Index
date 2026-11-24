#!/bin/bash
# =========================================================
#  환경 설정 (처음 한 번)
#  1) JDK 17+ 확인 (없으면 Homebrew로 설치할지 물어봄)
#  2) .env 준비 (서비스키)
#  3) ~/.zshrc 에 환경변수 등록: JAVA_HOME, OUTDOOR_SERVICE_KEY, OUTDOOR_PORT
#  4) Maven 준비 + 첫 빌드
#  여러 번 실행해도 안전해요 (.zshrc 의 같은 블록을 새로 덮어씀)
# =========================================================
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR" || exit 1
source "$PROJECT_DIR/scripts/common.sh"

echo
echo "  ☀  야외활동 지수 환경 설정"
echo

# 0) Homebrew 경로 (Finder에서 실행할 때 PATH에 없을 수 있음)
for b in /opt/homebrew/bin /usr/local/bin; do [ -x "$b/brew" ] && export PATH="$b:$PATH"; done

# 1) JDK
if ! find_jdk; then
    if command -v brew >/dev/null 2>&1; then
        if [ "$OUTDOOR_ASSUME_YES" = "1" ]; then ans=y; info "JDK가 없어서 Homebrew로 openjdk@21 을 설치할게요."
        else read -r -p "  JDK가 없어요. Homebrew로 openjdk@21 을 설치할까요? [Y/n] " ans; fi
        if [ -z "$ans" ] || [[ "$ans" =~ ^[Yy]$ ]]; then
            brew install openjdk@21 || { fail "설치 실패"; exit 1; }
        fi
    fi
    if ! find_jdk; then jdk_help; exit 1; fi
fi
ok "JDK $(java_major "$JAVA_HOME/bin/java")  ($JAVA_HOME)"

# 2) .env
if [ ! -f .env ]; then cp .env.example .env; ok ".env 생성"; fi
KEY=$(env_get OUTDOOR_SERVICE_KEY "")
if [ -z "$KEY" ] && [ "$OUTDOOR_ASSUME_YES" != "1" ]; then
    read -r -p "  공공데이터포털 서비스키(Decoding)를 붙여넣으세요: " KEY
    if [ -n "$KEY" ]; then
        if grep -q "^OUTDOOR_SERVICE_KEY=" .env; then
            sed -i '' "s|^OUTDOOR_SERVICE_KEY=.*|OUTDOOR_SERVICE_KEY=${KEY}|" .env 2>/dev/null || sed -i "s|^OUTDOOR_SERVICE_KEY=.*|OUTDOOR_SERVICE_KEY=${KEY}|" .env
        else
            echo "OUTDOOR_SERVICE_KEY=${KEY}" >> .env
        fi
    fi
fi
[ -n "$KEY" ] && ok "서비스키 ${KEY:0:6}…${KEY: -4}" || warn "서비스키 없이 진행 (나중에 .env 에 입력)"
PORT=$(env_get OUTDOOR_PORT 8080)

# 3) ~/.zshrc 환경변수 블록
RC="$HOME/.zshrc"
[ "$(basename "${SHELL:-zsh}")" = "bash" ] && RC="$HOME/.bash_profile"
touch "$RC"
BEGIN="# >>> outdoor-index (야외활동 지수) >>>"
END="# <<< outdoor-index (야외활동 지수) <<<"
TMP=$(mktemp)
awk -v b="$BEGIN" -v e="$END" '$0==b{skip=1} !skip{print} $0==e{skip=0}' "$RC" > "$TMP"
{
    cat "$TMP"
    echo "$BEGIN"
    echo "export JAVA_HOME=\"$JAVA_HOME\""
    echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    [ -n "$KEY" ] && echo "export OUTDOOR_SERVICE_KEY=\"$KEY\""
    echo "export OUTDOOR_PORT=\"$PORT\""
    echo "export OUTDOOR_HOME=\"$PROJECT_DIR\""
    echo "alias outdoor-start=\"\$OUTDOOR_HOME/run.sh\""
    echo "alias outdoor-stop=\"\$OUTDOOR_HOME/stop.sh\""
    echo "$END"
} > "$RC"
rm -f "$TMP"
ok "환경변수 등록: $RC"
info "  JAVA_HOME, OUTDOOR_SERVICE_KEY, OUTDOOR_PORT=$PORT, OUTDOOR_HOME"
info "  새 터미널에서 'outdoor-start' / 'outdoor-stop' 으로도 켜고 끌 수 있어요."

# 3-1) MySQL 상태 확인 (설치·설정은 바꾸지 않고 알려주기만)
DB_URL=$(env_get OUTDOOR_DB_URL "")
if [ -n "$DB_URL" ]; then
    DB_PORT=$(echo "$DB_URL" | sed -nE 's#.*://[^:/]+:([0-9]+).*#\1#p'); DB_PORT=${DB_PORT:-3306}
    if command -v mysql >/dev/null 2>&1; then
        ok "MySQL 클라이언트: $(mysql --version 2>/dev/null | head -1)"
    else
        warn "mysql 명령을 찾지 못했어요. (설치: brew install mysql)"
    fi
    if command -v brew >/dev/null 2>&1; then
        SVC=$(brew services list 2>/dev/null | grep -E "^mysql" | awk '{print $1" "$2}')
        [ -n "$SVC" ] && info "brew 서비스: $SVC"
    fi
    if [ -n "$(port_pids "$DB_PORT")" ]; then
        ok "MySQL 서버가 ${DB_PORT}번 포트에서 실행 중"
    else
        warn "${DB_PORT}번 포트에 MySQL 서버가 없어요. (실행: brew services start mysql)"
    fi
fi

# 4) 실행 파일 권한 + macOS 다운로드 격리 해제
chmod +x run.sh stop.sh setup.sh scripts/*.sh ./*.command 2>/dev/null
xattr -dr com.apple.quarantine "$PROJECT_DIR" 2>/dev/null
ok "실행 권한 설정"

# 5) Maven + 첫 빌드
find_maven || exit 1
info "첫 빌드 중... (라이브러리를 받느라 1~2분 걸려요)"
rm -rf target/lib
if "$MVN" -q -DskipTests package; then
    ok "빌드 완료"
else
    fail "빌드 실패. 위 메시지를 확인해 주세요."; exit 1
fi

echo
ok "설정 끝! 이제 '서버시작.command' 를 더블클릭하거나 ./run.sh 를 실행하세요."
echo "     (이미 열려 있는 터미널에는 'source $RC' 를 실행해야 환경변수가 적용돼요)"
echo
