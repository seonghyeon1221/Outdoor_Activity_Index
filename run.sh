#!/bin/bash
# =========================================================
#  야외활동 지수 서버 실행
#  - 빌드 → 서버 시작 → 브라우저 자동 열기
#  - 종료: 이 창에서 Ctrl + C  (또는 ./stop.sh)
# =========================================================
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR" || exit 1
source "$PROJECT_DIR/scripts/common.sh"
for b in /opt/homebrew/bin /usr/local/bin; do [ -x "$b/brew" ] && export PATH="$b:$PATH"; done

echo
echo "  ☀  야외활동 지수 서버 준비 중"
echo

# 1) .env 확인
if [ ! -f .env ]; then
    if [ -f .env.example ]; then cp .env.example .env; fi
    warn ".env 파일이 없어서 새로 만들었어요. OUTDOOR_SERVICE_KEY 를 채운 뒤 다시 실행하세요."
    open -e .env 2>/dev/null
    exit 1
fi
KEY=$(env_get OUTDOOR_SERVICE_KEY "")
[ -z "$KEY" ] && [ -z "$OUTDOOR_SERVICE_KEY" ] && warn "서비스키가 비어 있어요. (.env 의 OUTDOOR_SERVICE_KEY) — 화면에 키 오류가 표시됩니다."
# 포트는 이 폴더의 .env 값이 우선 (복사본마다 다른 포트를 쓸 수 있게)
PORT="$(env_get OUTDOOR_PORT "${OUTDOOR_PORT:-8080}")"
export OUTDOOR_PORT="$PORT"

# 2) JDK
if ! find_jdk; then jdk_help; exit 1; fi
ok "JDK $(java_major "$JAVA_HOME/bin/java")  ($JAVA_HOME)"

# 3) Maven
find_maven || exit 1
ok "Maven  ($MVN)"

# 4) 이미 켜진 서버 정리
if [ -n "$(server_pids)" ]; then
    info "이전에 켜 둔 서버를 끄고 다시 시작할게요."
    kill $(server_pids) 2>/dev/null; sleep 2
fi
if [ -n "$(port_pids "$PORT")" ]; then
    fail "${PORT}번 포트를 다른 프로그램이 쓰고 있어요. (PID: $(port_pids "$PORT" | tr '\n' ' '))"
    echo "    .env 의 OUTDOOR_PORT 를 다른 번호(예: 8090)로 바꾸고 다시 실행하세요."
    exit 1
fi

# 5) 빌드 (처음에는 라이브러리를 받느라 1~2분 걸려요)
info "빌드 중..."
rm -rf target/lib
if ! "$MVN" -q -DskipTests package; then
    fail "빌드 실패. 위의 오류 메시지를 확인해 주세요."
    exit 1
fi
ok "빌드 완료"

# 6) 서버가 뜨면 브라우저 열기
URL="http://localhost:${PORT}/"
(
    for i in $(seq 1 60); do
        if curl -s -o /dev/null "${URL}search"; then open "$URL" 2>/dev/null; exit 0; fi
        sleep 1
    done
) &

# 7) 서버 실행 (Ctrl + C 로 종료)
exec "$JAVA_HOME/bin/java" -Doutdoor.home="$PROJECT_DIR" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 \
     --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED \
     --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
     --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED \
     -cp "target/classes:target/lib/*" com.outdoorindex.Launcher
