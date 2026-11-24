#!/bin/bash
# 더블클릭하면 터미널이 열리며 서버가 시작돼요. 끌 때는 그 창에서 Ctrl + C (또는 서버종료.command)
cd "$(dirname "$0")" && ./run.sh 2>&1 | tee run.log
