# 🌤️ 야외활동 지수 (Outdoor Activity Index)

> 지역을 검색하면 **미세먼지 + 날씨**를 카드 한 장으로 보여주고,
> 두 정보를 합쳐 **"오늘 밖에 나가기 좋은지"** 를 `좋음 / 주의 / 나쁨` 으로 알려주는 웹 서비스

OpenAPI 융합 팀 프로젝트 (2팀) · 서버 구축 관리

```
📍 서울 중구                                     17:00 기준
┌────────┐
│  좋음   │  산책이나 운동하기 좋은 날이에요
└────────┘
미세먼지  PM10 45 (보통) · PM2.5 20 (보통)
날씨     ☀ 맑음 · 12℃ · 습도 45% · 강수 없음 · 바람 2.1m/s
```

---

## 📑 목차
1. [주요 기능](#-주요-기능)
2. [사용 API](#-사용-api)
3. [동작 흐름](#-동작-흐름)
4. [기술 스택 · 개발 환경](#-기술-스택--개발-환경)
5. [시작하기 (처음 받는 사람용)](#-시작하기-처음-받는-사람용)
6. [폴더 구조](#-폴더-구조)
7. [역할 분담](#-역할-분담)
8. [야외활동 지수 판정 기준](#-야외활동-지수-판정-기준)
9. [예외 처리](#-예외-처리)
10. [개발 규칙](#-개발-규칙)
11. [Git 협업 규칙](#-git-협업-규칙)
12. [진행 현황](#-진행-현황)
13. [문서](#-문서)

---

## ✨ 주요 기능

| 기능 | 설명 |
|---|---|
| 지역 검색 | 시·도 선택 (필수) + 측정소/구 이름 (선택) |
| 미세먼지 조회 | PM10, PM2.5 수치와 환경부 기준 등급 |
| 날씨 조회 | 하늘 상태, 기온, 습도, 강수형태, 풍속 |
| API 연결 (융합) | 에어코리아가 돌려준 **측정소**의 위치로 기상청 날씨를 조회 |
| 카드 결합 | 같은 위치의 미세먼지와 날씨를 한 장의 카드로 표시 |
| 다른 측정소 보기 | 에어코리아가 돌려준 같은 시·도 측정소 목록을 링크로 표시 → 누르면 그 측정소로 다시 조회 |
| 야외활동 지수 | 두 정보를 합쳐 좋음 / 주의 / 나쁨 판정 + 추천 문구 + 감점 사유 |
| 부분 실패 처리 | 한쪽 API가 실패해도 성공한 정보는 그대로 표시 |
| 확장 (선택) | 여러 지역 비교, 시간대별 추이 (구조만 준비) · DB 즐겨찾기/검색기록은 미사용 |

### 📋 과제 공통 요구사항 대응
| 교수님 요구사항 | 우리 프로젝트 |
|---|---|
| 서로 다른 2개 Open API 융합 (단순 나열 금지) | 에어코리아 결과(측정소명) → 기상청 입력(nx, ny) |
| 두 API가 좌표·검색어·ID로 연결 | 검색어(시·도) → ID(측정소명) → 좌표(격자) |
| 기술 스택 JSP + HttpClient + Gson | JSP + `java.net.http.HttpClient` + Gson |
| Model 1 또는 Model 2 선택 | **Model 2** (Servlet → Service → JSP) |
| 검색/입력 → 결과 화면 | `search.jsp` → `result.jsp` |
| 결과 없음·호출 실패 등 예외 처리 | [예외 처리](#-예외-처리) 표 참고 |
| 각 API 가입 후 인증키 발급 | 팀원 각자 발급, `api.properties` 로 관리 |
| 한글 검색은 URL 인코딩(UTF-8) | `URLEncoder.encode(값, UTF_8)` + 인코딩 필터 |
| DB(즐겨찾기·검색기록) 선택 | 이번에는 미사용 (확장 항목) |

---

## 🔌 사용 API

두 API 모두 [공공데이터포털](https://www.data.go.kr)에서 **활용신청**을 해야 사용할 수 있어요.
엔드포인트는 모두 같은 주소를 쓰고, **서비스키만 사람마다 달라요.**

### 1. 한국환경공단 에어코리아 대기오염정보
- **End Point** `https://apis.data.go.kr/B552584/ArpltnInforInqireSvc`

| 기능 | 설명 | 사용 |
|---|---|---|
| `/getCtprvnRltmMesureDnsty` | 시도별 실시간 측정정보 | ✅ 필수 |
| `/getMsrstnAcctoRltmMesureDnsty` | 측정소별 실시간 측정정보 | 선택 |
| `/getMinuDustFrcstDspth` | 대기질 예보통보 | 확장용 |
| `/getMinuDustWeekFrcstDspth` | 초미세먼지 주간예보 | 확장용 |
| `/getUnityAirEnvrnIdexSnstiveAboveMsrstnList` | 통합대기환경지수 나쁨 이상 측정소 | 사용 안 함 |

### 2. 기상청 단기예보 조회서비스
- **End Point** `https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0`

| 기능 | 설명 | 가져오는 값 | 사용 |
|---|---|---|---|
| `/getUltraSrtNcst` | 초단기실황 | 기온(T1H), 습도(REH), 강수형태(PTY), 풍속(WSD) | ✅ 필수 |
| `/getUltraSrtFcst` | 초단기예보 | 하늘상태(SKY) | ✅ 필수 |
| `/getVilageFcst` | 단기예보 | 시간별 기온·강수확률 | 확장용 |
| `/getFcstVersion` | 예보 버전 | - | 사용 안 함 |

> 하늘상태(SKY: 맑음/구름많음/흐림)는 **실황에는 없고 예보에만** 있어서 두 기능을 같이 써요.

### 🧪 브라우저로 먼저 테스트하기
코드 짜기 전에 아래 주소의 `{SERVICE_KEY}` 를 본인 키로 바꿔서 주소창에 넣어 보세요.
`"resultCode":"00"` 이 보이면 성공이에요.

**미세먼지 (서울 전체 측정소)**
```
https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty?serviceKey={SERVICE_KEY}&returnType=json&numOfRows=100&pageNo=1&sidoName=서울&ver=1.0
```

**현재 날씨 (서울 중구, nx=60 ny=127)** — `base_date` 는 오늘 날짜, `base_time` 은 한 시간 전 정각
```
https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst?serviceKey={SERVICE_KEY}&dataType=JSON&numOfRows=10&pageNo=1&base_date=20261118&base_time=1600&nx=60&ny=127
```

**하늘 상태** — `base_time` 은 한 시간 전 30분
```
https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst?serviceKey={SERVICE_KEY}&dataType=JSON&numOfRows=60&pageNo=1&base_date=20261118&base_time=1630&nx=60&ny=127
```

---

## 🔄 동작 흐름

### 🔗 융합 포인트
교수님 공통 요구사항: **"한 API의 결과가 다른 API의 입력으로 이어져야 함 (좌표·검색어·ID로 연결) — 단순 나열은 융합이 아님"**

우리 프로젝트의 연결 고리는 **측정소명(ID)** 이에요.
```
시·도 검색어 ─▶ ① 에어코리아 ─▶ 측정소명(ID) ─▶ 측정소 위치 표 ─▶ nx, ny ─▶ ② 기상청
                    │                                                        │
                    └──────── 미세먼지 ──────┐          ┌───── 날씨 ──────────┘
                                             ▼          ▼
                                       야외활동 지수 (좋음 / 주의 / 나쁨)
```
1. **검색어 연결** — 사용자가 고른 시·도로 에어코리아를 조회
2. **ID 연결** — 에어코리아가 돌려준 측정소명으로 기상청이 조회할 위치(nx, ny)를 결정
3. **시각화 연결** — 두 결과를 합쳐 어느 API에도 없는 새 지표(야외활동 지수)를 만듦
4. **검색어 재연결** — 에어코리아가 돌려준 측정소 목록이 다음 검색의 링크가 됨

### 요청 한 번의 흐름
```
사용자: 시·도 "서울" + 측정소 "중구" (측정소는 선택)
        │
        ▼
SearchController ────────────────▶ 시·도가 비었으면 search.jsp + 안내 문구
        │
        ▼
OutdoorCardService
        │
        ├─① AirQualityService   getCtprvnRltmMesureDnsty(sidoName=서울)
        │                       → 서울 측정소 목록 + 미세먼지 (0건이면 noResult.jsp)
        │                       → "중구" 측정소 선택 (미입력·불일치면 시·도 평균)
        │
        ├─  RegionService       API ①의 측정소명 "중구" → station_grid.csv → nx=60, ny=127
        │                       (표에 없으면 시·도 대표 지점)
        │
        ├─② WeatherService      getUltraSrtNcst + getUltraSrtFcst (nx, ny)
        │
        └─  OutdoorIndexPolicy  미세먼지 + 날씨 → 좋음 / 주의 / 나쁨
        │
        ▼
OutdoorCardDTO ──▶ result.jsp (카드 한 장 + 같은 시·도 다른 측정소 링크)
```

> API ①이 실패하면 측정소를 알 수 없으므로, 시·도 대표 지점으로 날씨만 보여줘요.

**왜 좌표 계산 대신 "측정소 위치 표"를 쓰나요?**
에어코리아 응답에는 측정소 이름만 있고, 기상청은 격자 번호(nx, ny)로 위치를 받아요.
기상청이 제공하는 격자 엑셀을 보고 측정소별 nx, ny를 `station_grid.csv` 로 만들어 두면, 복잡한 좌표 변환 공식 없이 표에서 찾기만 하면 돼요.

---

## 🛠 기술 스택 · 개발 환경

| 구분 | 사용 |
|---|---|
| 언어 | Java 17 이상 |
| 서버 | **Apache Tomcat 11** (Jakarta EE 11, Servlet 6.1) |
| 구조 | JSP + Servlet, Model2 (MVC) |
| HTTP 요청 | `java.net.http.HttpClient` (Java 내장) |
| JSON 파싱 | Gson |
| 화면 | JSP + JSTL 3.0 (스크립틀릿 사용 금지) |
| 인코딩 | UTF-8 통일 |

### 필요한 라이브러리 (`src/main/webapp/WEB-INF/lib/`)
| jar | 용도 |
|---|---|
| `gson-2.x.jar` | JSON → 자바 객체 변환 |
| `jakarta.servlet.jsp.jstl-api-3.0.x.jar` | JSTL API |
| `jakarta.servlet.jsp.jstl-3.0.x.jar` | JSTL 구현체 (GlassFish) |

> ⚠️ **Tomcat 11은 `jakarta.servlet.*` 패키지를 써요.** 인터넷 예제 대부분이 옛날 방식인 `javax.servlet.*` 이라 그대로 복사하면 실행이 안 돼요.
> JSTL 선언도 `<%@ taglib prefix="c" uri="jakarta.tags.core" %>` 로 써야 해요.

---

## 🚀 시작하기 (처음 받는 사람용)

### 1단계. API 키 발급
1. [공공데이터포털](https://www.data.go.kr) 회원가입 · 로그인
2. 아래 두 API를 검색해서 **활용신청**
   - `한국환경공단_에어코리아_대기오염정보`
   - `기상청_단기예보 ((구)_동네예보) 조회서비스`
3. 마이페이지 → 개발계정에서 **일반 인증키(Decoding)** 복사
4. 발급 직후에는 반영까지 시간이 걸릴 수 있어요 (1~2시간). 키 오류가 나면 조금 기다렸다 다시 해보세요.

> 키는 **각자 발급**받아요. 하나를 같이 쓰면 하루 호출 한도가 금방 차서 다 같이 테스트를 못 해요.

### 2단계. 프로젝트 받기
```bash
git clone https://github.com/seonghyeon1221/Outdoor_Activity_Index.git
cd Outdoor_Activity_Index
```

### 3단계. 설정 파일 만들기
```bash
cp src/main/resources/api.properties.example src/main/resources/api.properties
```
`api.properties` 를 열어서 `service.key=` 뒤에 본인 키를 붙여 넣어요.
이 파일은 `.gitignore` 에 등록되어 있어서 **깃허브에 올라가지 않아요.**

### 4단계. IDE 설정 (이클립스 기준)
1. `File → Import → Existing Projects` 또는 `Dynamic Web Project` 로 열기
2. `Window → Preferences → General → Workspace → Text file encoding` → **UTF-8**
3. `Window → Preferences → Server → Runtime Environments` → **Apache Tomcat v11.0** 추가
4. 프로젝트 우클릭 → `Properties → Project Facets` → Dynamic Web Module **6.1**, Java **17** 이상
5. 위 jar 3개를 `src/main/webapp/WEB-INF/lib` 에 넣기 (다운로드 주소는 같은 폴더의 `README.txt`)
   - Maven을 쓰면 jar를 따로 받을 필요 없이 `pom.xml` 이 자동으로 받아 줘요.

### 5단계. 실행
프로젝트 우클릭 → `Run As → Run on Server` → 브라우저에서
```
http://localhost:8080/Outdoor_Activity_Index/search
http://localhost:8080/Outdoor_Activity_Index/search?sido=서울&station=중구
```
(주소 가운데 `Outdoor_Activity_Index` 는 이클립스 프로젝트 이름이에요. 다르면 그 이름으로 바꾸세요.)

**Maven으로 실행할 때**
```bash
mvn package                      # target/outdoor-index.war 생성
cp target/outdoor-index.war {톰캣11 폴더}/webapps/
# http://localhost:8080/outdoor-index/search
```

### 자주 나는 오류
| 증상 | 원인 · 해결 |
|---|---|
| 화면에 "서비스 키에 문제가 있어…" | `api.properties` 가 없거나 키가 틀림. 발급 직후라면 1~2시간 뒤 다시 |
| `ClassNotFoundException: jakarta.servlet…` | Tomcat 10 미만 사용 중 → **Tomcat 11** 로 변경 |
| JSP에서 `jakarta.tags.core` 를 못 찾음 | `WEB-INF/lib` 에 JSTL jar 2개가 없음 |
| `NoClassDefFoundError: com/google/gson/Gson` | `WEB-INF/lib` 에 gson jar가 없음 |
| 한글이 `???` 로 깨짐 | 이클립스 Text file encoding이 UTF-8이 아님 |
| 날씨만 "현재 제공되는 데이터가 없습니다" | 기상청 발표 직후라 아직 자료가 없음 → 잠시 뒤 새로고침 |

---

## 📁 폴더 구조

✅ = 구현 완료 · 📦 = 각자 준비 · 🔒 = 각자 PC에만 · 괄호 = 담당자

```
Outdoor_Activity_Index/
├─ README.md                                   ✅
├─ pom.xml                                     ✅ (선택) Maven 빌드용
├─ .gitignore                                  ✅
├─ docs/
│  ├─ PROMPT.md                                ✅ AI 코드 생성용 요구사항 프롬프트
│  ├─ DTO_CONTRACT.md                          ✅ DTO 확정본 설명서
│  └─ TEAM_GUIDE.pdf                           ✅ 팀원용 가이드
│
└─ src/main/
   ├─ java/com/outdoorindex/
   │  ├─ controller/
   │  │  └─ SearchController.java              ✅ (A) 요청 받기 → 화면 전달
   │  ├─ filter/
   │  │  └─ EncodingFilter.java                ✅ (A) UTF-8 처리
   │  ├─ service/
   │  │  ├─ OutdoorCardService.java            ✅ (A) 두 결과 합치기 + 부분 실패
   │  │  ├─ WeatherService.java                ✅ (B) 초단기실황 + 초단기예보
   │  │  ├─ RegionService.java                 ✅ (C) 측정소명 → nx, ny 변환
   │  │  └─ AirQualityService.java             ✅ (C) 시도별 미세먼지 → 측정소 선택
   │  ├─ policy/
   │  │  └─ OutdoorIndexPolicy.java            ✅ (D) 판정 기준
   │  ├─ dto/                                  ✅ 확정본 (수정 금지)
   │  │  ├─ RegionDTO · AirQualityDTO · WeatherDTO · HourlyForecastDTO
   │  │  ├─ OutdoorIndexResult · OutdoorCardDTO
   │  │  └─ api/ AirKoreaResponse · KmaResponse
   │  ├─ enums/                                ✅ 확정본 (수정 금지)
   │  │  └─ DustGrade · ActivityLevel · SkyCode · PrecipitationType · ErrorCode
   │  ├─ exception/                            ✅ 확정본 (수정 금지)
   │  │  └─ OutdoorException · NoResultException · ApiResponseException
   │  │     ApiConnectionException · ApiParseException
   │  └─ util/
   │     ├─ ApiConfig.java                     ✅ (A) 설정 파일 읽기
   │     ├─ HttpClientUtil.java                ✅ (A) API 요청 공용 도구
   │     ├─ ApiResponseValidator.java          ✅ (A) resultCode 검사
   │     ├─ BaseTimeCalculator.java            ✅ (B) 발표 시간 계산
   │     └─ DustGradeCalculator.java           ✅ (D) 수치 → 등급
   ├─ resources/
   │  ├─ api.properties.example                ✅ 키 자리만 비운 템플릿
   │  ├─ api.properties                        🔒 각자 PC에만 (커밋 금지)
   │  └─ station_grid.csv                      ✅ (C) 시도, 측정소명, nx, ny
   └─ webapp/
      ├─ index.jsp                             ✅ (D) /search 로 이동
      ├─ css/style.css                         ✅ (D)
      └─ WEB-INF/
         ├─ web.xml                            ✅ (A)
         ├─ lib/                               📦 (A) jar 3개 — 각자 넣기 (README.txt 참고)
         └─ views/
            ├─ common/header.jsp · footer.jsp  ✅ (D)
            ├─ search.jsp                      ✅ (D) 검색 화면
            ├─ result.jsp                      ✅ (D) 결과 카드
            ├─ noResult.jsp                    ✅ (D) 검색 결과 없음
            └─ error.jsp                       ✅ (D) 오류 안내
```

---

## 👥 역할 분담

| 역할 | 담당 | 하는 일 | 만들 파일 |
|---|---|---|---|
| **A** 공통 기반 + 통합 | | 레포 관리, API 요청 공용 도구, 설정 읽기, 오류 처리, 최종 결합 | `SearchController` `EncodingFilter` `OutdoorCardService` `ApiConfig` `HttpClientUtil` `ApiResponseValidator` `web.xml` |
| **B** 날씨 (기상청) | | 발표 시간 계산, 실황 + 예보 조회, 코드 → 글자 변환 | `WeatherService` `BaseTimeCalculator` |
| **C** 미세먼지 + 측정소 위치 | | 시도별 미세먼지 조회, 측정소 선택 / 평균, 측정소 위치 표 작성, 측정소명 → nx·ny | `AirQualityService` `RegionService` `station_grid.csv` |
| **D** 판정 + 화면 | | 등급 계산, 야외활동 지수 판정, JSP 화면 4개 + CSS | `OutdoorIndexPolicy` `DustGradeCalculator` `*.jsp` `style.css` |

### 서로 주고받는 메서드 (이름 고정)
```java
// C — API ① 호출 → 측정소 목록 → 측정소 선택 → 위치 변환
List<AirQualityDTO> AirQualityService.getStations(String sido);          // 시·도 안 측정소 전체 (0건이면 NoResultException)
AirQualityDTO       AirQualityService.selectStation(List<AirQualityDTO> stations, String stationName);
                                                                         // 일치 측정소, 없거나 미입력이면 시·도 평균(sidoAverage=true)
RegionDTO           RegionService.findGrid(String sido, String stationName);
                                                                         // API ①의 측정소명 → nx, ny (표에 없으면 시·도 대표 지점)

// B — API ② 호출 (입력 nx, ny는 API ① 결과에서 나옴)
WeatherDTO              WeatherService.getCurrentWeather(int nx, int ny);
List<HourlyForecastDTO> WeatherService.getHourlyForecast(int nx, int ny); // 확장용, 지금은 빈 리스트

// D
DustGrade          DustGradeCalculator.pm10Grade(Integer value);
DustGrade          DustGradeCalculator.pm25Grade(Integer value);
OutdoorIndexResult OutdoorIndexPolicy.judge(AirQualityDTO air, WeatherDTO weather); // air나 weather가 null일 수 있음

// A — 공용 도구 (B·C가 호출)
String ApiConfig.getServiceKey();          // API 인증키 (Decoding 키 원본)
String ApiConfig.getAirKoreaEndpoint();    // 에어코리아 주소
String ApiConfig.getKmaEndpoint();         // 기상청 주소
String HttpClientUtil.get(String url);     // API 요청 → 응답 JSON 문자열 (연결 실패·타임아웃·XML 오류는 예외)
void   ApiResponseValidator.validate(String resultCode, String resultMsg); // "00"이 아니면 예외

// A — 위 순서대로 연결 (API ① → 측정소명 → nx, ny → API ② → 판정)
OutdoorCardDTO OutdoorCardService.getCard(String sido, String stationName);
```

---

## 📊 야외활동 지수 판정 기준

### 미세먼지 등급 (환경부 기준, ㎍/㎥)
| 등급 | PM10 | PM2.5 |
|---|---|---|
| 좋음 | 0 ~ 30 | 0 ~ 15 |
| 보통 | 31 ~ 80 | 16 ~ 35 |
| 나쁨 | 81 ~ 150 | 36 ~ 75 |
| 매우나쁨 | 151 이상 | 76 이상 |

PM10과 PM2.5 중 **더 나쁜 등급**을 대표 등급으로 사용해요.

### 날씨 감점 요소
- 비 · 눈이 옴 (강수형태 ≠ 0)
- 기온 0℃ 이하 또는 33℃ 이상
- 풍속 9m/s 이상

### 최종 판정
| 결과 | 조건 |
|---|---|
| 🟢 **좋음** | 미세먼지 좋음 ~ 보통 **그리고** 날씨 감점 없음 |
| 🟠 **주의** | 미세먼지 나쁨 **또는** 날씨 감점 1개 |
| 🔴 **나쁨** | 미세먼지 매우나쁨 **또는** 날씨 감점 2개 이상 |

> 기준값은 모두 `OutdoorIndexPolicy` 한 클래스에 상수로 모아요. 기준을 바꿀 땐 이 파일만 고치면 돼요.

---

## ⚠️ 예외 처리

| 상황 | 화면 | ErrorCode |
|---|---|---|
| 시·도를 선택하지 않음 | 검색 화면 + 안내 문구 | `EMPTY_INPUT` |
| 시·도 목록에 없는 값, 측정소 0건 | "검색 결과가 없습니다" | `REGION_NOT_FOUND` |
| 측정값이 `-` / 빈 값 | 해당 항목만 "정보 없음" | - |
| 서비스키 오류 | 해당 영역에 오류 문구 | `API_KEY_ERROR` |
| 하루 호출 한도 초과 | 해당 영역에 오류 문구 | `API_TRAFFIC_EXCEEDED` |
| 연결 실패 / 타임아웃 (연결 5초, 응답 10초) | 해당 영역에 오류 문구 | `API_CONNECTION_FAILED` / `API_TIMEOUT` |
| JSON 대신 XML 오류 응답, 파싱 실패 | 해당 영역에 오류 문구 | `API_PARSE_ERROR` |
| **한쪽 API만 실패** | **성공한 쪽은 정상 표시**, 실패한 쪽만 오류 | - |

---

## 📏 개발 규칙

1. **DTO · enum · exception 은 수정 금지** — 필요하면 단톡방에 먼저 말하고 A가 수정해요. ([docs/DTO_CONTRACT.md](docs/DTO_CONTRACT.md))
2. **API 키를 코드나 커밋에 절대 넣지 않기** — 키는 `api.properties` 에만.
3. **한글은 UTF-8** — IDE, JSP(`pageEncoding`, `contentType`), 요청 필터 모두 UTF-8. EUC-KR 금지.
4. **주석 달기** — 모든 클래스와 public 메서드 위에 "무슨 일을 하는지" 한 줄 이상.
5. **JSP 안에 자바 코드(`<% %>`) 금지** — EL(`${}`)과 JSTL(`<c:...>`)만 사용.
6. **값이 없을 때** — 숫자는 `null`, 코드 enum은 `UNKNOWN`, API 실패 시 해당 DTO는 `null` + 오류 코드.
7. **AI로 코드를 만들 때** — [docs/PROMPT.md](docs/PROMPT.md) 를 붙여 넣고, 끝에 아래처럼 **자기 담당 파일만** 요청하기
   ```
   나는 B(날씨) 담당이야. WeatherService, BaseTimeCalculator 만 만들어 줘.
   dto, enums, exception 패키지는 레포에 있는 확정본을 그대로 쓰고 수정하지 마.
   ```

---

## 🌿 Git 협업 규칙

### 브랜치
| 브랜치 | 용도 |
|---|---|
| `main` | 항상 실행되는 상태 유지. 직접 push 하지 않기 (A가 합침) |
| `feature/common` | A — 공통 기반, 통합 |
| `feature/weather` | B — 날씨 |
| `feature/air` | C — 지역 + 미세먼지 |
| `feature/view` | D — 판정 + 화면 |

### 작업 순서
```bash
git checkout main
git pull                          # 최신 코드 받기
git checkout -b feature/weather   # 처음 한 번만 (이후엔 git checkout feature/weather)

# ... 작업 ...

git add .
git commit -m "feat: 초단기실황 조회 구현"
git push origin feature/weather
```
그다음 깃허브에서 **Pull Request** 를 만들면 A가 확인하고 `main` 에 합쳐요.

### 커밋 메시지
| 머리말 | 의미 | 예시 |
|---|---|---|
| `feat:` | 기능 추가 | `feat: 시도별 미세먼지 조회` |
| `fix:` | 버그 수정 | `fix: 자정 base_date 계산 오류` |
| `docs:` | 문서 수정 | `docs: README 실행 방법 추가` |
| `style:` | 화면 · CSS | `style: 결과 카드 색상 변경` |
| `refactor:` | 동작 변화 없는 코드 정리 | `refactor: 파싱 로직 분리` |

> **자기 담당 파일만 수정하기** — 이것만 지켜도 충돌이 거의 안 생겨요.

---

## ✅ 진행 현황

**1단계 · 준비**
- [x] 요구사항 프롬프트 작성 (`docs/PROMPT.md`)
- [x] DTO · enum · exception 확정본
- [x] `.gitignore`, `api.properties.example`
- [ ] 팀원 전원 API 키 발급 + 브라우저 테스트 성공
- [ ] 개발 환경 통일 (JDK 17+, Tomcat 11, UTF-8)

**2단계 · 개발**
- [x] A: `HttpClientUtil`, `ApiConfig`, `ApiResponseValidator`, `EncodingFilter`
- [x] B: `BaseTimeCalculator`, `WeatherService`
- [x] C: `AirQualityService`, `station_grid.csv`(서울 25개 구 + 시·도 대표), `RegionService`
- [x] D: `DustGradeCalculator`, `OutdoorIndexPolicy`, JSP 화면 4개 + CSS
- [x] A: `OutdoorCardService`, `SearchController`, `web.xml`, `pom.xml`

**3단계 · 검증 · 마무리**
- [x] 로직 검증 (가짜 API 서버로 47개 항목 통과: 정상 / 시·도 평균 / 부분 매칭 / 키 오류(XML·401) / 트래픽 초과 / 깨진 JSON / 측정소 0건 / NO_DATA 재시도 / 타임아웃 / 하늘상태만 실패 / 빈 입력)
- [ ] 실제 키로 Tomcat 11에서 실행 확인
- [ ] `station_grid.csv` nx·ny 값을 기상청 격자 엑셀과 대조 + 다른 시·도 측정소 추가 (C)
- [ ] 정상 검색 / 빈 검색어 / 없는 지역 / 잘못된 키 / 미세먼지만 실패 / 날씨만 실패 화면 확인
- [ ] 시연 시나리오 준비

---

## 📚 문서

| 문서 | 내용 |
|---|---|
| [docs/TEAM_GUIDE.pdf](docs/TEAM_GUIDE.pdf) | 팀 가이드 (흐름, 역할, 약속, 용어) |
| [docs/PROMPT.md](docs/PROMPT.md) | AI 코드 생성용 전체 요구사항 |
| [docs/DTO_CONTRACT.md](docs/DTO_CONTRACT.md) | DTO 확정본 설명, JSP 사용 예 |

---

<sub>데이터 출처: 한국환경공단 에어코리아, 기상청 (공공데이터포털)</sub>
