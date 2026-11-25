# 야외 활동 지수 서비스 (Outdoor Activity Index)

지역을 검색하면 에어코리아 실시간 미세먼지와 기상청 날씨를 함께 가져와, 야외 활동을 해도 괜찮은지 **좋음 · 주의 · 나쁨**으로 알려 주는 웹 서비스입니다.

2조 OpenAPI 융합 팀 프로젝트 · 서버구축관리 · 조성현, 허규진, 오주은, 허윤호

## 두 API를 잇는 방법

시·도(검색어) → **에어코리아** 측정소별 미세먼지 → 측정소명 → `station_grid.csv` → 기상청 격자 (nx, ny) → **기상청** 초단기실황 · 단기예보 → 야외활동 지수 판정

`station_grid.csv`에는 17개 시·도 대표 지점과 전국 측정소 521곳의 격자 좌표가 시·도별로 들어 있습니다. 기상청 단기예보 조회서비스 참고문서의 격자 위경도 엑셀에서 같은 시·도의 읍·면·동 이름과 맞춰 만들었고, 표에 없는 측정소는 시·도 대표 지점을 씁니다.

## 기능

- 시·도 / 측정소 검색, 야외활동 지수와 계산 과정 표시
- 현재 미세먼지·날씨, 같은 시·도의 다른 측정소 목록
- 앞으로 12시간 시간대별 예보
- 최대 3곳 지역 비교
- MySQL 즐겨찾기 (추가 · 삭제 · 최근 3곳 비교)
- 판정 기준 화면

## 개발 환경

JDK 17 · Eclipse (Dynamic Web Project) · Apache Tomcat 11 · MySQL · JSP Model 2 · Gson

## 이클립스에서 실행하기

1. 이 저장소를 받아 Eclipse에서 Dynamic Web Project로 가져옵니다 (Java 소스 폴더 `src/main/java`, 웹 폴더 `src/main/webapp`).
2. `src/main/webapp/WEB-INF/api.properties.example`을 같은 폴더에 `api.properties`로 복사하고, `service.key`에 공공데이터포털 서비스 키(Decoding), `db.password`에 MySQL 비밀번호를 적습니다. 이 파일은 `.gitignore`에 들어 있어 깃헙에 올라가지 않습니다.
3. Run As → Run on Server로 Tomcat 11에서 실행하고 `http://localhost:8080/<프로젝트명>/search`에 접속합니다.

DB와 테이블은 처음 즐겨찾기를 사용할 때 자동으로 만들어지며, `outdoor_index.sql`로 미리 만들 수도 있습니다. 필요한 jar 5개는 `WEB-INF/lib`에 들어 있습니다.

## 폴더 구조

```
src/main/java
 ├─ station_grid.csv                  측정소명 → 격자 좌표 표
 └─ com/outdoorindex
     ├─ controller/MainController     /search, /compare, /favorites, /guide
     ├─ service/CardService           두 API 결합, 결과 카드
     ├─ service/AirService            에어코리아
     ├─ service/WeatherService        기상청
     ├─ service/IndexPolicy           지수 판정 기준
     ├─ dao/FavoriteDao               MySQL 즐겨찾기
     ├─ dto/Card                      결과 데이터 (record)
     └─ util/Api                      API 호출 · 오류 처리 · 설정
src/main/webapp
 ├─ index.jsp, css/style.css
 └─ WEB-INF/ web.xml, api.properties.example, views/*.jsp, lib/*.jar
outdoor_index.sql
```
