> 📌 개발을 시작할 때 AI 코드 생성에 쓴 요구사항 프롬프트예요. 이후 확장 기능(비교 · 시간대별 추이 · MySQL)과 `.env` 설정 방식이 추가됐어요. 현재 상태는 [README](../README.md) 를 보세요.

너는 JSP 기반 풀스택 개발자야. 아래 요구사항과 개발 규칙을 반드시 지켜서 코드를 작성해 줘.

※ dto, enums, exception 패키지는 이미 확정되어 레포에 있음 (docs/DTO_CONTRACT.md 참고). 이 클래스들은 수정하지 말고 그대로 import 해서 사용할 것.

## 만들 서비스
사용자가 지역(시·도)을 검색하면, 해당 지역의 현재 미세먼지 수치와 날씨를 하나의 카드로 묶어 보여주고,
두 지표를 종합한 "야외활동 지수"(좋음 / 주의 / 나쁨)와 추천 문구를 함께 제시하는 웹 서비스.
(결과 예시: 서울 중구 · 미세먼지 45(보통) · 맑음 12° · 야외활동 '좋음')

## 과제 공통 요구사항 (교수님 자료 — 반드시 지킬 것)
- 서로 다른 2개 Open API를 '융합'해 하나의 동작하는 웹 서비스로 구현
- 단순히 두 결과를 나란히 보여주는 것은 융합이 아님
  → 한 API의 결과가 다른 API의 입력·시각화로 이어져야 함 (좌표·검색어·ID로 연결)
- 기술 스택: JSP + HttpClient + Gson / 웹 모델: Model 2 선택
- 검색/입력 → 결과 화면 반드시 구현
- 결과 없음·호출 실패 등 예외 처리
- 한글 검색은 URL 인코딩(UTF-8)
- DB(즐겨찾기·검색기록)는 선택 사항 (이번에는 미사용, 확장 고려에만 포함)

## 사용 API (공공데이터포털, 아래 2개만 사용 — 다른 API 추가 금지)
1. 한국환경공단 에어코리아 대기오염정보
   - End Point: https://apis.data.go.kr/B552584/ArpltnInforInqireSvc
   - 사용 기능: /getCtprvnRltmMesureDnsty (시도별 실시간 측정정보 조회)
     · 주요 파라미터: serviceKey, returnType=json, numOfRows=100, pageNo=1, sidoName, ver=1.0
     · sidoName 값: 서울, 부산, 대구, 인천, 광주, 대전, 울산, 경기, 강원, 충북, 충남, 전북, 전남, 경북, 경남, 제주, 세종
     · 응답 items의 stationName, pm10Value, pm25Value, pm10Grade, pm25Grade, dataTime 사용
   - 확장용(이번엔 메서드 틀만): /getMinuDustFrcstDspth (대기질 예보통보)

2. 기상청 단기예보 조회서비스
   - End Point: https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
   - 사용 기능
     · /getUltraSrtNcst (초단기실황): T1H(기온), REH(습도), PTY(강수형태), WSD(풍속)
     · /getUltraSrtFcst (초단기예보): SKY(하늘상태) — 현재 시각과 가장 가까운 fcstTime 값 사용
   - 공통 파라미터: serviceKey, dataType=JSON, numOfRows, pageNo, base_date(yyyyMMdd), base_time(HHmm), nx, ny
   - 코드표
     · SKY: 1 맑음, 3 구름많음, 4 흐림
     · PTY: 0 없음, 1 비, 2 비/눈, 3 눈, 5 빗방울, 6 빗방울눈날림, 7 눈날림
   - 확장용(이번엔 메서드 틀만): /getVilageFcst (단기예보, 시간대별 추이)

## 두 API 연결 방식 (융합: API ①의 결과가 API ②의 입력으로 이어짐)
- 연결 고리: 에어코리아(API ①)가 돌려준 측정소명(stationName, ID)
  → 측정소 위치 표에서 기상청 격자(nx, ny)로 변환 → 기상청(API ②) 조회
- 측정소 위치 표(station_grid.csv)를 resources에 두고 서버 시작 시 1번만 메모리에 로드
  · 컬럼: sido, stationName, nx, ny  (stationName이 빈 행 = 해당 시·도 대표 지점)
  · nx, ny 값은 기상청 단기예보 활용가이드 첨부 "격자 위경도" 엑셀에서 가져옴
  · 서울은 측정소명이 구 이름과 같음 (중구, 종로구 …)
  · 우선 서울 25개 측정소 + 17개 시·도 대표 행을 채운 샘플 CSV를 만들어 줄 것
- 흐름
  1) 사용자 입력: 시·도(필수) + 측정소/구 이름(선택)
  2) API ① getCtprvnRltmMesureDnsty(sidoName=sido) → 시·도 안의 측정소 목록 + 미세먼지
     · 측정소/구 이름을 입력했으면 stationName이 같은(또는 포함하는) 측정소 선택
     · 입력하지 않았거나 일치하는 측정소가 없으면 시·도 내 측정소 PM10/PM2.5 평균 사용, 화면에 "시·도 평균" 표시
     · 응답 측정소 목록(stationNames)은 결과 화면에 "같은 시·도 다른 측정소 보기" 링크로 표시
       → 클릭하면 그 측정소로 다시 조회 (API ① 결과가 다음 검색어로 이어짐)
  3) API ①이 돌려준 stationName → station_grid.csv → nx, ny
     · 표에 없는 측정소이거나 시·도 평균이면 시·도 대표 지점의 nx, ny 사용
  4) API ② getUltraSrtNcst + getUltraSrtFcst(nx, ny) → 같은 위치의 날씨
  5) 두 결과 → OutdoorIndexPolicy → 야외활동 지수 (두 API 결과를 하나의 새 지표로 시각화)
  6) 하나의 OutdoorCardDTO에 합쳐서 View로 전달
- API ①이 실패하면 측정소를 알 수 없으므로 시·도 대표 지점으로 날씨만 조회 (부분 실패 처리)

## base_time 계산 규칙 (BaseTimeCalculator 한 클래스에 모을 것)
- 초단기실황: 매시 정각 발표. 현재 분이 40분 미만이면 1시간 전 정각 사용
- 초단기예보: 매시 30분 발표. 현재 분이 45분 미만이면 1시간 전 30분 사용
- 자정 전후로 시간을 되돌릴 때 base_date도 하루 전으로 바뀌게 처리
- 기준 시간대는 Asia/Seoul로 고정
- 결과가 비어 있으면 한 단계 이전 base_time으로 1회 재시도

## 기술 스택
- JSP + Servlet (Model2 / MVC 패턴), Tomcat 11 (jakarta.servlet 패키지, Servlet 6.1), JDK 17 이상
- HttpClient: Java 11 이상 내장 java.net.http.HttpClient
- JSON 파싱: Gson
- View: JSP + JSTL (JSP 안에 스크립틀릿 사용 금지, EL과 JSTL만)

## 개발 규칙
1. 패키지는 com.outdoorindex 아래 역할별로 나눌 것 (아래 "파일 목록" 그대로)
2. 모든 클래스와 public 메서드 위에 용도 주석(한국어)을 달 것
3. 한글은 UTF-8로 통일 (EUC-KR 금지)
   - EncodingFilter에서 request/response 모두 UTF-8
   - API 파라미터는 URLEncoder.encode(값, StandardCharsets.UTF_8)
   - JSP는 pageEncoding, contentType 모두 UTF-8
4. API 서비스키와 엔드포인트는 코드에 쓰지 말고 api.properties에서 읽을 것
   - 키는 Decoding 키 기준으로 저장하고 URLEncoder로 1번만 인코딩 (이중 인코딩 금지)
   - api.properties는 .gitignore 대상, 키 자리를 비운 api.properties.example만 커밋

## 야외활동 지수 (판정 기준은 OutdoorIndexPolicy 한 클래스에 상수로 모을 것)
- 미세먼지 등급 (환경부 기준)
  PM10: 좋음 0~30 / 보통 31~80 / 나쁨 81~150 / 매우나쁨 151 이상
  PM2.5: 좋음 0~15 / 보통 16~35 / 나쁨 36~75 / 매우나쁨 76 이상
  → PM10과 PM2.5 중 더 나쁜 등급을 대표 등급으로 사용
- 날씨 감점 요소: 강수형태 ≠ 0, 기온 0℃ 이하 또는 33℃ 이상, 풍속 9m/s 이상
- 최종 판정
  좋음: 미세먼지 좋음~보통 + 날씨 감점 없음
  주의: 미세먼지 나쁨 또는 날씨 감점 1개
  나쁨: 미세먼지 매우나쁨 또는 날씨 감점 2개 이상
- 판정 결과마다 추천 문구와 감점 사유 목록을 함께 반환
- 미세먼지 값이 없으면 날씨만으로 판정하고 "미세먼지 정보 없이 판정" 표시

## 예외 처리 (상황별로 다른 안내 문구, ErrorCode enum으로 관리)
- 시·도를 선택하지 않았거나 입력이 공백뿐인 경우
- 시·도 목록에 없는 값이거나 API ① 결과 측정소가 0건 → "검색 결과가 없습니다" (noResult.jsp)
- 측정값이 "-" / null / 빈 문자열 → 해당 항목만 "정보 없음"
- resultCode가 "00"이 아닌 경우 (키 오류, 트래픽 초과 등) → resultMsg 로그 + 사용자 안내
- 연결 실패 / 타임아웃 (connect 5초, request 10초)
- JSON 파싱 실패, 응답이 "<"로 시작하는 XML 오류 응답
- 한쪽 API만 실패하면 성공한 쪽은 보여주고, 실패한 쪽 영역에만 오류 문구 표시

## 확장 고려 (이번에는 구조만)
- WeatherService에 getHourlyForecast(nx, ny) → List<HourlyForecastDTO> 메서드 틀
- AirQualityService에 getDustForecast() 메서드 틀
- OutdoorCardDTO에 List<HourlyForecastDTO> hourlyForecasts 필드 (지금은 빈 리스트)
- 여러 지역 비교: 같은 시·도의 측정소 여러 곳 카드를 나란히 보여줄 수 있게 getCard()를 측정소별로 반복 호출 가능한 구조
- DB(즐겨찾기·검색기록): 선택 사항, 이번에는 구현하지 않음

## 파일 목록 (이 이름 그대로 만들 것)
src/main/java/com/outdoorindex/
- controller/SearchController.java          @WebServlet("/search"), GET=검색화면, 파라미터 있으면 결과
- filter/EncodingFilter.java                 @WebFilter("/*")
- service/OutdoorCardService.java            두 API 결과 결합, 부분 실패 처리
- service/RegionService.java                 station_grid.csv 로드, 측정소명 → nx, ny 변환
- service/AirQualityService.java             시도별 실시간 조회 + 측정소 선택/평균 + 측정소 목록
- service/WeatherService.java                초단기실황 + 초단기예보(SKY)
- policy/OutdoorIndexPolicy.java             판정 기준 상수 + judge()
- dto/RegionDTO.java, AirQualityDTO.java, WeatherDTO.java, HourlyForecastDTO.java
- dto/OutdoorIndexResult.java, OutdoorCardDTO.java
- dto/api/AirKoreaResponse.java, KmaResponse.java   (Gson 매핑용)
- enums/DustGrade.java, ActivityLevel.java, SkyCode.java, PrecipitationType.java, ErrorCode.java
- exception/OutdoorException.java (최상위), NoResultException.java, ApiResponseException.java,
  ApiConnectionException.java, ApiParseException.java
- util/ApiConfig.java, HttpClientUtil.java, ApiResponseValidator.java, BaseTimeCalculator.java, DustGradeCalculator.java
src/main/resources/
- api.properties, api.properties.example, station_grid.csv
src/main/webapp/
- index.jsp (→ /search 리다이렉트), css/style.css
- WEB-INF/web.xml, WEB-INF/lib/
- WEB-INF/views/search.jsp, result.jsp, noResult.jsp, error.jsp, common/header.jsp, common/footer.jsp

## 결과물
- 프로젝트 폴더 구조
- 각 파일의 전체 코드
- api.properties.example 내용
- station_grid.csv 샘플
- 어노테이션 매핑 설명 (web.xml에는 에러 페이지와 welcome-file만)
- 실행 방법 (Tomcat 11, JDK 17 이상, 필요한 .jar: gson, jakarta.servlet.jsp.jstl-api, jakarta.servlet.jsp.jstl 구현체)
- 브라우저로 API를 먼저 테스트하는 예시 URL (키 자리는 {SERVICE_KEY})
