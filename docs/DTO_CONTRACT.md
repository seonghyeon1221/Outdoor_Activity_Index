# DTO 확정본 안내 (Tomcat 11 / JDK 17 이상)

> 2026-11-24 최신화 — 팀원 코드(B: `WeatherService`·`BaseTimeCalculator`, C: `AirQualityService`)를 합친 현재 코드 기준이에요.

이 폴더의 18개 파일은 **팀 공용 약속**이에요. 이 파일들의 이름과 필드를 기준으로 4명이 각자 코드를 짜요.

## 규칙 3가지
1. **이 파일들은 A만 수정해요.** 필드를 추가하거나 바꿔야 하면 단톡방에 먼저 말하고, A가 고쳐서 다시 올려요.
2. **AI로 코드를 만들 때는 이 파일들을 프롬프트에 같이 붙여 넣고** "이 클래스는 수정하지 말고 그대로 사용해"라고 적어요.
3. **값이 없을 때의 규칙**
   - 숫자(Integer, Double)는 `null`
   - 코드(DustGrade, SkyCode, PrecipitationType)는 `UNKNOWN`
   - API가 실패하면 `OutdoorCardDTO`의 `air`나 `weather`는 `null`이고, 대신 `airError`나 `weatherError`에 오류 코드가 들어가요.

## 파일 목록과 누가 쓰는지

| 파일 | 만드는 사람 (값 채우기) | 쓰는 사람 |
|---|---|---|
| `dto/RegionDTO` | C (RegionService, 측정소명 → nx·ny) | A, B, 화면 |
| `dto/AirQualityDTO` | C (AirQualityService) | D (판정, 화면) |
| `dto/WeatherDTO` | B (WeatherService) | D (판정, 화면) |
| `dto/HourlyForecastDTO` | B (`getHourlyForecast`, 단기예보 12시간) | A (시간별 지수), 화면 |
| `dto/OutdoorIndexResult` | D (OutdoorIndexPolicy) | 화면 |
| `dto/OutdoorCardDTO` | A (OutdoorCardService) | 화면 (`${card}`) |
| `dto/api/AirKoreaResponse` | C (Gson 변환) | C |
| `dto/api/KmaResponse` | B (Gson 변환) | B |
| `enums/DustGrade` | D | C, D, 화면 |
| `enums/ActivityLevel` | D | D, 화면 |
| `enums/SkyCode` · `PrecipitationType` | B | B, D, 화면 |
| `enums/ErrorCode` | A | 모두 |
| `exception/*` (5개) | A | 모두 (예외 던질 때) |

## 서로 주고받는 메서드 (이름 고정)

```java
// C — API ① 호출 → 측정소 목록 → 측정소 선택 → 위치 변환
List<AirQualityDTO> AirQualityService.getStations(String sido);          // 시·도 안 측정소 전체 (0건이면 NoResultException)
AirQualityDTO       AirQualityService.selectStation(List<AirQualityDTO> stations, String stationName);
                                                                         // 일치 측정소, 없거나 미입력이면 시·도 평균(sidoAverage=true)
RegionDTO           RegionService.findGrid(String sido, String stationName);
                                                                         // API ①의 측정소명 → nx, ny (표에 없으면 시·도 대표 지점)

// B — API ② 호출 (입력 nx, ny는 API ① 결과에서 나옴) · static 메서드
static WeatherDTO              WeatherService.getCurrentWeather(int nx, int ny);   // 초단기실황 + 초단기예보(하늘). 하늘만 실패하면 sky=UNKNOWN
static List<HourlyForecastDTO> WeatherService.getHourlyForecast(int nx, int ny);   // 단기예보(getVilageFcst) 앞으로 12시간

// D
DustGrade          DustGradeCalculator.pm10Grade(Integer value);
DustGrade          DustGradeCalculator.pm25Grade(Integer value);
OutdoorIndexResult OutdoorIndexPolicy.judge(AirQualityDTO air, WeatherDTO weather); // air나 weather가 null일 수 있음

// A — 공용 도구 (B·C가 호출)
String ApiConfig.getServiceKey();          // API 인증키 (Decoding 키 원본)
String ApiConfig.getAirKoreaEndpoint();    // 에어코리아 주소
String ApiConfig.getKmaEndpoint();         // 기상청 주소
String HttpClientUtil.get(String url);     // API 요청 → 응답 JSON 문자열
void   ApiResponseValidator.validate(String resultCode, String resultMsg); // "00"이 아니면 예외

// B — 발표 시각 (BaseTime: getBaseDate() "yyyyMMdd", getBaseTime() "HHmm")
BaseTime BaseTimeCalculator.forObservation();   // 초단기실황 (매시 40분 이후 그 시각 정시)
BaseTime BaseTimeCalculator.forForecast();      // 초단기예보 (매시 45분 이후 그 시각 30분)
BaseTime BaseTimeCalculator.forVillage();       // 단기예보 (02·05·08·11·14·17·20·23시 + 10분)
BaseTime BaseTimeCalculator.previous(BaseTime t);        // 데이터 없음(03)일 때 한 시간 전
BaseTime BaseTimeCalculator.previousVillage(BaseTime t); // 단기예보 직전 발표 (3시간 전)
// 위 메서드는 LocalDateTime now 를 받는 버전도 있음 (테스트용)

// C — 측정소 목록 (가나다순, 구·군·시 이름 측정소 먼저) · 빈 값 보정
List<String>  AirQualityService.getStationNames(List<AirQualityDTO> stations);
// selectStation 은 고른 측정소에 값이 없으면(통신장애 등) 그 값만 시·도 평균으로 채움 (filledFromAverage=true)

// A — 위 순서대로 연결 (API ① → 측정소명 → nx, ny → API ② → 판정)
OutdoorCardDTO OutdoorCardService.getCard(String sido, String stationName);                       // 시간대별 포함
OutdoorCardDTO OutdoorCardService.getCard(String sido, String stationName, boolean includeHourly); // 비교 화면은 false
```

## JSP에서 꺼내 쓰는 예
```jsp
${card.region.displayName}              → 서울 중구
<c:forEach var="st" items="${card.stationNames}"> … </c:forEach>  → 같은 시·도 다른 측정소 링크
${card.air.pm10}  ${card.air.pm10Grade.label}   → 45 보통
${card.weather.sky.label}  ${card.weather.temperature}℃   → 맑음 12.0℃
${card.index.level.label}  ${card.index.message}
${card.airError.message}                → 오류 안내 문구
${card.air.filledFromAverage}  ${card.air.statusNote}   → true  통신장애
<c:forEach var="h" items="${card.hourlyForecasts}"> ${h.hourLabel} ${h.temperature}° ${h.rainProbability}% ${h.level.label} </c:forEach>
${card.hourlySummary}  ${card.savedDataUsed}
```

## 지금 추가된 필드 (처음 확정본 이후)
| DTO | 필드 | 뜻 |
|---|---|---|
| `AirQualityDTO` | `statusNote` | 측정값이 없을 때 에어코리아가 준 이유 (`pm10Flag`, 예: 통신장애) |
| `AirQualityDTO` | `filledFromAverage` | 빈 값을 시·도 평균으로 채웠으면 true |
| `OutdoorCardDTO` | `stationNames`, `hourlyForecasts`, `hourlySummary`, `savedDataUsed` | 다른 측정소 목록, 시간대별 예보, 요약, 저장본 사용 여부 |
| `HourlyForecastDTO` | `windSpeed`, `dayLabel`, `level` | 풍속, 오늘·내일, 시간별 지수 |
| `dto/api/AirKoreaResponse.Item` | `pm10Flag`, `pm25Flag` | 측정 상태 플래그 |
| `CompareItemDTO` · `FavoriteDTO` · `SearchHistoryDTO` | — | 비교 · 즐겨찾기 · 최근 검색 (DB) |

## Tomcat 11 참고
- 패키지는 `jakarta.servlet.*` (javax 아님)
- JSTL 선언: `<%@ taglib prefix="c" uri="jakarta.tags.core" %>`
- 필요한 jar: `gson`, `jakarta.servlet.jsp.jstl-api` (3.0), `jakarta.servlet.jsp.jstl` (GlassFish 구현체 3.0)
