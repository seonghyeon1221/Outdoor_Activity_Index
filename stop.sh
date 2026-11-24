#!/bin/bash
# 야외활동 지수 서버 종료
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR" || exit 1
source "$PROJECT_DIR/scripts/common.sh"
PIDS=$(server_pids)
if [ -z "$PIDS" ]; then
    info "켜져 있는 서버가 없어요."
else
    kill $PIDS 2>/dev/null; sleep 2
    [ -n "$(server_pids)" ] && kill -9 $(server_pids) 2>/dev/null
    ok "서버를 껐어요."
fi
