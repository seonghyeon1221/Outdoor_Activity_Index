#!/bin/bash
# 처음 한 번: JDK 확인, 서비스키, 환경변수(~/.zshrc) 등록, 첫 빌드
cd "$(dirname "$0")" && OUTDOOR_ASSUME_YES=1 ./setup.sh 2>&1 | tee setup.log
echo; echo "  [setup 종료] 이 창은 닫아도 돼요."
