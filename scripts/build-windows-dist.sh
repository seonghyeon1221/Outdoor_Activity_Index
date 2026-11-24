#!/bin/bash
# =========================================================
#  Windows 배포판 만들기 → dist/outdoor-index-windows.zip
#
#  mvn package 결과(target/classes, target/lib)로 아래를 한 zip 에 묶어요.
#    1-tomcat-war/  설치된 Tomcat 11 용 outdoor.war + INSTALL.bat
#    2-standalone/  Tomcat 없이 도는 단독 실행판 (SETUP / START / STOP)
#    tools/         설치 · 점검 스크립트, MySQL 연결 확인 도구
#    data/          인터넷이 안 될 때 쓸 저장 응답 (data/api-saved)
#    GUIDE.pdf      설치 가이드 (docs/WINDOWS_GUIDE.pdf)
#
#  서비스키는 .env 의 OUTDOOR_SERVICE_KEY 를 읽어 zip 안에만 넣어요.
#  dist/ 는 .gitignore 대상이라 깃허브에 올라가지 않아요. (zip 을 공개하지 마세요)
#
#  사용:  ./scripts/build-windows-dist.sh            (빌드 + 묶기)
#         ./scripts/build-windows-dist.sh --skip-build (이미 빌드된 target 사용)
# =========================================================
set -e
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"
source "$PROJECT_DIR/scripts/common.sh"
for b in /opt/homebrew/bin /usr/local/bin; do [ -x "$b/brew" ] && export PATH="$b:$PATH"; done

echo
echo "  ☀  Windows 배포판 만들기"
echo

# 1) JDK (WAR 안 클래스는 Java 17 바이트코드 — pom.xml 의 maven.compiler.release=17)
if ! find_jdk; then jdk_help; exit 1; fi
ok "JDK $(java_major "$JAVA_HOME/bin/java")  ($JAVA_HOME)"

# 2) 빌드
if [ "$1" != "--skip-build" ]; then
    find_maven || exit 1
    info "빌드 중..."
    rm -rf target/lib
    "$MVN" -q -DskipTests package || { fail "빌드 실패"; exit 1; }
    ok "빌드 완료"
fi
[ -d target/classes/com/outdoorindex ] || { fail "target/classes 가 없어요. --skip-build 없이 다시 실행하세요."; exit 1; }
[ -d target/lib ] || { fail "target/lib 가 없어요. --skip-build 없이 다시 실행하세요."; exit 1; }

# 3) 서비스키
KEY="$(env_get OUTDOOR_SERVICE_KEY "${OUTDOOR_SERVICE_KEY:-}")"
if [ -z "$KEY" ]; then
    warn ".env 에 OUTDOOR_SERVICE_KEY 가 없어요. 배포판 설치 때 키를 직접 입력하게 됩니다."
fi

STAGE="$PROJECT_DIR/dist/stage"
OUT="$STAGE/outdoor-index-windows"
rm -rf "$STAGE" && mkdir -p "$OUT"

# 4) 설치 스크립트 · 안내 파일 (deploy/windows 를 그대로 복사)
cp -R deploy/windows/. "$OUT/"
rm -f "$OUT/tools/DbCheck.java"
mkdir -p "$OUT/tools/dbcheck"
"$JAVA_HOME/bin/javac" --release 17 -cp "$OUT/tools/dbcheck" -sourcepath "$PROJECT_DIR/deploy/windows/tools" -d "$OUT/tools/dbcheck" "$PROJECT_DIR/deploy/windows/tools/DbCheck.java"
# 수동 설치용 예시 파일에 키 채우기 (CRLF 유지)
EX="$OUT/1-tomcat-war/outdoor-index.properties.example"
if [ -n "$KEY" ]; then
    KEY="$KEY" python3 - "$EX" <<'PY'
import os, sys
p = sys.argv[1]
t = open(p, encoding='utf-8', newline='').read().replace('__SERVICE_KEY__', os.environ['KEY'])
open(p, 'w', encoding='utf-8', newline='').write(t)
PY
else
    sed -i.bak 's/__SERVICE_KEY__//' "$EX" && rm -f "$EX.bak"
fi
# 설치 스크립트가 읽는 기본값
{
    printf '# 설치 스크립트가 쓰는 기본값 (팀 공용 서비스키)\r\n'
    printf '# 외부에 공유하지 마세요.\r\n'
    printf 'OUTDOOR_SERVICE_KEY=%s\r\n' "$KEY"
    printf 'OUTDOOR_DEMO_MODE=auto\r\n'
    printf 'OUTDOOR_CACHE_TTL_SECONDS=600\r\n'
} > "$OUT/tools/defaults.properties"
ok "설치 스크립트 준비"

# 5) WAR (Tomcat 이 이미 가진 라이브러리는 빼고 앱 라이브러리만)
WAR_DIR="$STAGE/war"
mkdir -p "$WAR_DIR/WEB-INF/classes" "$WAR_DIR/WEB-INF/lib"
cp -R src/main/webapp/. "$WAR_DIR/"
rm -f "$WAR_DIR/WEB-INF/lib/README.txt"
cp -R target/classes/. "$WAR_DIR/WEB-INF/classes/"
for j in target/lib/*.jar; do
    case "$(basename "$j")" in
        tomcat-*|ecj-*) ;;                       # Tomcat 11 에 이미 들어 있음
        *) cp "$j" "$WAR_DIR/WEB-INF/lib/" ;;
    esac
done
( cd "$WAR_DIR" && zip -qr -X "$OUT/1-tomcat-war/outdoor.war" . )
ok "outdoor.war ($(du -h "$OUT/1-tomcat-war/outdoor.war" | cut -f1))"

# 6) 단독 실행판 (내장 Tomcat 포함, Launcher 로 실행)
APP="$OUT/2-standalone/app"
mkdir -p "$APP/src/main" "$APP/target"
cp -R src/main/webapp "$APP/src/main/"
rm -f "$APP/src/main/webapp/WEB-INF/lib/README.txt"
cp -R target/classes "$APP/target/classes"
cp -R target/lib "$APP/target/lib"
ok "단독 실행판"

# 7) 저장 응답 · 문서
mkdir -p "$OUT/data/api-saved" "$OUT/docs"
if [ -d data/api-saved ]; then cp data/api-saved/*.json "$OUT/data/api-saved/" 2>/dev/null || true; fi
cp docs/schema-mysql.sql "$OUT/docs/"
[ -f docs/WINDOWS_GUIDE.pdf ] && cp docs/WINDOWS_GUIDE.pdf "$OUT/GUIDE.pdf"
ok "저장 응답 $(ls "$OUT/data/api-saved" | wc -l | tr -d ' ')개 · 가이드"

# 8) zip (파일 이름은 모두 영어 — 윈도우 압축 풀기에서 깨지지 않게)
mkdir -p dist
rm -f dist/outdoor-index-windows.zip
( cd "$STAGE" && zip -qr -X "$PROJECT_DIR/dist/outdoor-index-windows.zip" outdoor-index-windows )
rm -rf "$STAGE"
ok "완료 → dist/outdoor-index-windows.zip ($(du -h dist/outdoor-index-windows.zip | cut -f1))"
echo
warn "zip 안에 서비스키가 들어 있어요. 팀원에게만 전달하고 공개된 곳에 올리지 마세요."
echo
