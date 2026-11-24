#!/bin/bash
# 공통 함수 (run.sh / setup.sh / stop.sh 에서 불러 씀)

MAVEN_VERSION="3.9.9"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

ok()   { printf "  \033[32m✔\033[0m %s\n" "$*"; }
info() { printf "  \033[34m•\033[0m %s\n" "$*"; }
warn() { printf "  \033[33m!\033[0m %s\n" "$*"; }
fail() { printf "  \033[31m✖\033[0m %s\n" "$*"; }

# .env 에서 값 하나 읽기:  env_get KEY [기본값]
env_get() {
    local key="$1" def="$2" val=""
    if [ -f "$PROJECT_DIR/.env" ]; then
        val=$(grep -E "^[[:space:]]*(export[[:space:]]+)?${key}=" "$PROJECT_DIR/.env" | tail -1 | sed -E "s/^[[:space:]]*(export[[:space:]]+)?${key}=//; s/^[\"']//; s/[\"'][[:space:]]*$//; s/[[:space:]]*$//")
    fi
    [ -n "$val" ] && echo "$val" || echo "$def"
}

# 자바 메이저 버전 (예: 21)
java_major() {
    "$1" -version 2>&1 | grep -m1 'version "' | sed -E 's/.*version "([0-9]+)(\.[0-9]+)*.*/\1/; s/^1\.([0-9]+).*/\1/'
}

# JDK 17 이상 찾기 → JAVA_HOME 설정. 못 찾으면 1 반환
find_jdk() {
    local cand=()
    [ -n "$JAVA_HOME" ] && cand+=("$JAVA_HOME")
    if [ -x /usr/libexec/java_home ]; then
        local jh; jh=$(/usr/libexec/java_home -v 17+ 2>/dev/null) && cand+=("$jh")
    fi
    for p in /opt/homebrew/opt/openjdk@21 /opt/homebrew/opt/openjdk@17 /opt/homebrew/opt/openjdk \
             /usr/local/opt/openjdk@21 /usr/local/opt/openjdk@17 /usr/local/opt/openjdk; do
        [ -d "$p/libexec/openjdk.jdk/Contents/Home" ] && cand+=("$p/libexec/openjdk.jdk/Contents/Home")
    done
    for p in "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
             "/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" \
             "/Applications/IntelliJ IDEA CE.app/Contents/jbr/Contents/Home" \
             /Library/Java/JavaVirtualMachines/*/Contents/Home \
             "$HOME"/Library/Java/JavaVirtualMachines/*/Contents/Home; do
        [ -d "$p" ] && cand+=("$p")
    done
    if command -v javac >/dev/null 2>&1; then
        local jc; jc=$(command -v javac); jc=$(cd "$(dirname "$jc")" && pwd -P)/javac
        cand+=("$(dirname "$(dirname "$(readlink -f "$jc" 2>/dev/null || echo "$jc")")")")
    fi
    for h in "${cand[@]}"; do
        if [ -x "$h/bin/java" ] && [ -x "$h/bin/javac" ]; then
            local v; v=$(java_major "$h/bin/java")
            if [ -n "$v" ] && [ "$v" -ge 17 ] 2>/dev/null; then
                export JAVA_HOME="$h"
                export PATH="$JAVA_HOME/bin:$PATH"
                return 0
            fi
        fi
    done
    return 1
}

jdk_help() {
    fail "JDK 17 이상을 찾지 못했어요."
    echo
    echo "    설치 방법 (터미널에 붙여넣기):"
    echo "      brew install openjdk@21"
    echo
    echo "    설치 후 ./setup.sh (또는 환경설정.command) 를 한 번 실행해 주세요."
    echo "    Homebrew가 없다면: https://adoptium.net 에서 JDK 21 (macOS, aarch64) 설치"
}

# Maven 찾기 → MVN 변수. 없으면 프로젝트 안(.tools)에 자동으로 받음
find_maven() {
    if command -v mvn >/dev/null 2>&1; then MVN=$(command -v mvn); return 0; fi
    local local_mvn="$PROJECT_DIR/.tools/apache-maven-${MAVEN_VERSION}/bin/mvn"
    if [ -x "$local_mvn" ]; then MVN="$local_mvn"; return 0; fi
    info "Maven이 없어서 프로젝트 폴더(.tools)에 내려받을게요. (처음 한 번, 약 9MB)"
    mkdir -p "$PROJECT_DIR/.tools"
    if ! curl -fL --progress-bar "$MAVEN_URL" -o "$PROJECT_DIR/.tools/maven.tar.gz"; then
        fail "Maven 다운로드 실패. 인터넷 연결을 확인하거나 'brew install maven' 후 다시 실행하세요."
        return 1
    fi
    tar -xzf "$PROJECT_DIR/.tools/maven.tar.gz" -C "$PROJECT_DIR/.tools" && rm -f "$PROJECT_DIR/.tools/maven.tar.gz"
    MVN="$local_mvn"
    ok "Maven 준비 완료"
}

# 포트를 쓰는 프로세스 PID
port_pids() { lsof -nP -iTCP:"$1" -sTCP:LISTEN -t 2>/dev/null; }

# 이 폴더에서 켠 서버 프로세스 PID (다른 폴더의 복사본 서버는 건드리지 않음)
server_pids() {
    local pid cmd
    for pid in $(pgrep -f "com.outdoorindex.Launcher" 2>/dev/null); do
        cmd=$(ps -o command= -p "$pid" 2>/dev/null)
        case "$cmd" in
            *"-Doutdoor.home=$PROJECT_DIR "*) echo "$pid" ;;
            *"-Doutdoor.home="*) ;;                          # 다른 폴더의 서버
            *) [ "$PROJECT_DIR" = "$(cd "$(dirname "$0")" && pwd)" ] && [ ! -d "$PROJECT_DIR/../scripts" ] && echo "$pid" ;;  # 예전 방식으로 켠 서버
        esac
    done
}
