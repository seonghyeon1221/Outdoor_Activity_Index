야외활동 지수 (2팀) - Windows 배포판
=====================================

먼저 GUIDE.pdf 를 열어 주세요. 처음부터 끝까지 순서대로 적혀 있어요.

[빠른 시작]  (Java 17+, Tomcat 11, MySQL 이 설치된 PC)
  0. 이 zip 을 반드시 [압축 풀기] 한 뒤 사용하세요. (권장 위치: C:\outdoor-index-windows)
  1. 1-tomcat-war\INSTALL.bat 더블클릭
       → Tomcat 폴더 자동 찾기 → MySQL 비밀번호 입력 → 배포 → 브라우저 자동 열림
       → 주소: http://localhost:8080/outdoor/
  2. Tomcat 쪽이 잘 안 되면: 2-standalone\SETUP.bat → START.bat
       → 주소: http://localhost:8090/

[문제가 생기면]
  CHECK.bat 더블클릭 → 같은 폴더에 check-result.txt 가 생겨요. (서비스키·비밀번호는 가려짐)

[폴더]
  1-tomcat-war\    outdoor.war (Tomcat 배포 파일), INSTALL.bat, 수동 설치용 설정 예시
  2-standalone\    Tomcat 없이 실행 (SETUP.bat / START.bat / STOP.bat)
  tools\           설치·점검 스크립트, 팀 공용 서비스키(defaults.properties)
  data\api-saved\  인터넷이 안 될 때 보여줄 저장 데이터
  docs\            MySQL 스키마(schema-mysql.sql)

※ 서비스키가 들어 있으니 이 파일을 외부(깃허브, 공개 게시판)에 올리지 마세요.
