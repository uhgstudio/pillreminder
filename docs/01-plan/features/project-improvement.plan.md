# Plan: YacTime (PillReminder) 프로젝트 전체 분석 및 개선

- Feature: project-improvement
- Created: 2026-03-06
- Status: Plan

---

## 1. 프로젝트 개요

- **패키지**: `com.uhstudio.pillreminder`
- **아키텍처**: MVVM + Jetpack Compose
- **코틀린 파일 수**: 69개
- **SDK**: minSdk 26, targetSdk 36, compileSdk 36

---

## 2. 분석 결과 요약

| 영역 | 점수 | 주요 문제 |
|------|------|-----------|
| 코어 로직 (DB, Receiver, Util) | 62/100 | BroadcastReceiver 코루틴 누수, TypeConverter 충돌, Export 데이터 손실 |
| UI / ViewModel | 52/100 | HomeViewModel 메모리 누수, 하드코딩된 UI값, 아이콘 버그 |
| 전체 구조 | 양호 | MVVM 잘 지켜짐, 다만 Repository 계층 불일치 |

---

## 3. CRITICAL 이슈 (즉시 수정 필요)

### C-1. HomeViewModel 메모리 누수
- **파일**: `ui/home/HomeViewModel.kt:84-88, 138-179`
- **문제**: `_todayAlarms.collect` 내부에서 `loadTodayStats()`를 호출하고, `loadTodayStats`는 `histories.collect`로 무한 수집기를 생성. `refreshData()` 호출 시마다 새로운 영구 collector가 추가되어 메모리 누수 발생
- **수정**: `flatMapLatest` + `first()` 또는 one-shot DB 쿼리로 전환

### C-2. AlarmReceiver 코루틴 구조 문제
- **파일**: `receiver/AlarmReceiver.kt:39`, `receiver/BootReceiver.kt:21`
- **문제**: `CoroutineScope(Dispatchers.IO)`로 비구조적 코루틴 생성. `goAsync()`와 코루틴 수명이 연결되지 않아, 프로세스 종료 시 작업 소실
- **수정**: `goAsync()` + `SupervisorJob` + 타임아웃 연결, 또는 `WorkManager` 전환

### C-3. Background Activity Start 제한
- **파일**: `receiver/AlarmReceiver.kt:188-208`
- **문제**: Android 10+ (API 29)에서 `context.startActivity()` 백그라운드 시작이 제한됨. 알람 울릴 때 Activity가 안 뜰 수 있음
- **수정**: Notification의 `fullScreenIntent`를 통해 Activity 실행

### C-4. TypeConverter 중복 및 크래시
- **파일**: `data/converter/DayOfWeekConverter.kt:12-13` vs `Converters.kt`
- **문제**: `DayOfWeekConverter`는 `Converters`와 중복이며, 빈 문자열 입력 시 `IllegalArgumentException` 크래시 발생 (`filter { it.isNotBlank() }` 누락)
- **수정**: `DayOfWeekConverter.kt` 삭제 (중복 파일)

### C-5. Export/Import 데이터 손실
- **파일**: `data/export/EntityExtensions.kt:16-23, 35-45`
- **문제**: `Pill.toExport()`에서 `type`, `dosage`, `quantity`, `lowStockThreshold` 미포함. `PillAlarm.toExport()`에서 `scheduleType`, `scheduleConfig`, `startDate`, `endDate` 미포함. 백업/복원 시 중요 데이터 소실
- **수정**: Export 모델에 모든 필드 추가

### C-6. REPLACE_ALL Import가 기존 데이터를 삭제하지 않음
- **파일**: `data/repository/ExportRepository.kt:98-131`
- **문제**: `ImportStrategy.REPLACE_ALL`이 기존 데이터를 삭제하지 않고 REPLACE만 수행. import에 없는 기존 레코드가 남아있음
- **수정**: Room `@Transaction` 내에서 delete-all 후 insert

### C-7. AlarmRepository 유효성 검사 오류 (라이브 버그)
- **파일**: `data/repository/AlarmRepository.kt:111, 151`
- **문제**: `validateAlarmInput`이 `repeatDays`가 비어있으면 실패. 하지만 `DAILY`, `INTERVAL_DAYS`, `SPECIFIC_DATES` 타입은 `repeatDays`를 사용하지 않음. **WEEKLY가 아닌 알람은 생성/수정이 항상 실패**
- **수정**: `scheduleType`에 따라 분기하여 검증

### C-8. HomeScreen 하드코딩된 다음 복용 시간
- **파일**: `ui/home/HomeScreen.kt:391`
- **문제**: `"08:00 AM"`이 하드코딩되어 실제 알람 시간과 무관하게 항상 같은 시간 표시
- **수정**: `TodayAlarm` 객체의 실제 알람 시간 사용

### C-9. AlarmScreen 뒤로가기 아이콘 버그
- **파일**: `ui/alarm/AlarmScreen.kt:46-49`
- **문제**: 뒤로가기 버튼에 `Icons.Default.Add` ("+" 아이콘) 사용
- **수정**: `Icons.AutoMirrored.Filled.ArrowBack`로 변경

### C-10. IntakeHistoryDao 쿼리 반환 타입 불일치
- **파일**: `data/dao/IntakeHistoryDao.kt:70-77`
- **문제**: SQL `date(intakeTime)`은 `YYYY-MM-DD` 문자열을 반환하지만 `Flow<List<LocalDateTime>>`으로 수신. Room 버전에 따라 크래시 또는 빈 결과
- **수정**: `Flow<List<String>>`으로 반환 후 수동 파싱, 또는 쿼리 수정

### C-11. ScheduleCalculator Division by Zero
- **파일**: `util/ScheduleCalculator.kt:151`
- **문제**: `config.intervalDays`가 0이면 `daysSinceStart % config.intervalDays`에서 `ArithmeticException` 발생
- **수정**: `if (config.intervalDays <= 0) return null` 가드 추가

---

## 4. WARNING 이슈 (개선 권장)

### W-1. ImageUtil Bitmap 메모리 누수
- **파일**: `util/ImageUtil.kt:32-36, 78-96`
- **문제**: 리사이즈 시 원본 Bitmap `recycle()` 미호출로 OOM 가능
- **수정**: 리사이즈 후 원본 `recycle()`

### W-2. IntakeHistoryRepository 성능 문제
- **파일**: `data/repository/IntakeHistoryRepository.kt:147-148`
- **문제**: `getAllHistoryOnce()`로 전체 기록 로드 후 메모리에서 필터링. 장기 사용자에게 매우 비효율적
- **수정**: SQL에서 `pillId`와 날짜 범위로 직접 필터링

### W-3. AlarmManagerUtil 불필요한 SDK 분기
- **파일**: `util/AlarmManagerUtil.kt:73-89, 132`
- **문제**: minSdk 26이므로 `SDK >= M` (23) 체크는 항상 true. Dead code
- **수정**: 불필요한 분기 제거

### W-4. runCatching 이름 충돌
- **파일**: `util/Result.kt:147-153`
- **문제**: Kotlin stdlib의 `runCatching`과 이름 충돌
- **수정**: `runSafely`로 이름 변경

### W-5. AddPillScreen ScheduleType 미처리
- **파일**: `ui/pill/AddPillScreen.kt:492-496`
- **문제**: `else -> ScheduleConfig.Daily` fallback으로 `INTERVAL_DAYS`, `SPECIFIC_DATES` 타입 무시
- **수정**: 모든 `ScheduleType` 처리

### W-6. getScheduleDescriptionInternal 3중 복사
- **파일**: `AlarmScreen.kt:170`, `AlarmsScreen.kt:383`, `PillDetailScreen.kt:599`
- **문제**: ~85줄 함수가 3곳에 복사/붙여넣기 (총 255줄 중복)
- **수정**: 공통 유틸리티로 추출

### W-7. AddAlarmScreen 하드코딩된 한국어
- **파일**: `ui/addAlarm/AddAlarmScreen.kt:270-278`
- **문제**: 권한 다이얼로그에 "정확한 알람 권한 필요", "설정으로 이동" 하드코딩
- **수정**: `strings.xml`로 추출

### W-8. SettingsScreen 하드코딩 버전
- **파일**: `ui/settings/SettingsScreen.kt:636`
- **문제**: 버전 "1.0.5" 하드코딩
- **수정**: `BuildConfig.VERSION_NAME` 사용

### W-9. isSaving Race Condition
- **파일**: `AddAlarmViewModel.kt:40`, `AddPillViewModel.kt:53`
- **문제**: `isSaving` Boolean이 thread-safe하지 않음
- **수정**: `MutableStateFlow<Boolean>` 사용

### W-10. EditPillViewModel 미사용
- **파일**: `ui/pill/EditPillViewModel.kt`
- **문제**: `EditPillScreen`이 `AddPillViewModel`을 사용하여 `EditPillViewModel`은 dead code
- **수정**: 삭제 또는 활용

### W-11. AlarmUtil.kt 미사용
- **파일**: `util/AlarmUtil.kt`
- **문제**: 레거시 `repeatDays` 기반 유틸리티. 현재 `ScheduleCalculator`가 대체
- **수정**: 참조 확인 후 삭제

### W-12. Dead 마이그레이션 코멘트
- **파일**: `data/database/PillReminderDatabase.kt:166`
- **문제**: `// MIGRATION_9_10 removed` 코멘트 직후 실제 `MIGRATION_9_10` 정의 존재
- **수정**: 혼란스러운 코멘트 삭제

### W-13. CalendarScreen 매 리컴포지션마다 새 Flow 생성
- **파일**: `ui/calendar/CalendarScreen.kt:81-86`
- **문제**: `viewModel.getIntakeHistoryForDate(selectedDate)` 호출이 매번 새 Flow 생성
- **수정**: `remember(selectedDate)` 또는 ViewModel StateFlow로 전환

### W-14. HomeScreen "View All" 미구현
- **파일**: `ui/home/HomeScreen.kt:135`
- **문제**: `Modifier.clickable { /* View All */ }` 빈 핸들러
- **수정**: 네비게이션 구현 또는 제거

### W-15. BadgesScreen 전체 하드코딩
- **파일**: `ui/badges/BadgesScreen.kt:37-213`
- **문제**: 모든 텍스트가 영어 하드코딩, ViewModel 없음, 실제 데이터 미연동
- **수정**: 실제 데이터 연동 또는 TODO 표시

---

## 5. 미사용/Dead Code 목록

| 파일 | 이유 |
|------|------|
| `util/AlarmUtil.kt` | `ScheduleCalculator`로 대체됨 |
| `ui/pill/EditPillViewModel.kt` | `EditPillScreen`이 `AddPillViewModel` 사용 |
| `data/converter/DayOfWeekConverter.kt` | `Converters.kt`와 중복 |
| `EditAlarmViewModel.kt`의 수동 DayOfWeek 매핑 | `DayOfWeek.of(day)`/`dayOfWeek.value`로 대체 가능 |

---

## 6. 아키텍처 개선 사항

### 6-1. Repository 계층 불일치
- 일부 ViewModel은 DAO 직접 접근, 일부는 Repository 사용
- **권장**: 모든 ViewModel이 Repository를 통해 데이터 접근

### 6-2. 테스트 부재
- Unit test 파일이 하나도 없음
- **권장**: 최소한 ViewModel과 ScheduleCalculator에 대한 단위 테스트 추가

### 6-3. Accessibility 부족
- 대부분의 `Icon`에 `contentDescription = null`
- `NumberPicker` 커스텀 컴포넌트에 접근성 라벨 없음

---

## 7. 우선순위별 수정 계획

### Phase 1: 긴급 버그 수정 (Critical)
1. C-7: AlarmRepository 유효성 검사 (비-WEEKLY 알람 생성 불가 버그)
2. C-1: HomeViewModel 메모리 누수
3. C-3: Background Activity Start 수정
4. C-8: 하드코딩된 "08:00 AM"
5. C-9: 뒤로가기 아이콘 버그
6. C-11: Division by zero 가드

### Phase 2: 데이터 안전성
7. C-4: DayOfWeekConverter 중복 삭제
8. C-5: Export 데이터 손실 수정
9. C-6: REPLACE_ALL Import 수정
10. C-10: IntakeHistoryDao 반환 타입 수정

### Phase 3: 코루틴/메모리
11. C-2: AlarmReceiver/BootReceiver 구조적 코루틴
12. W-1: ImageUtil Bitmap recycle
13. W-2: IntakeHistoryRepository 성능 개선

### Phase 4: 코드 정리 및 품질
14. W-6: getScheduleDescriptionInternal 중복 제거
15. W-3, W-12: Dead code 제거 (AlarmManagerUtil 분기, 코멘트)
16. W-10, W-11: 미사용 파일 삭제
17. W-4: runCatching 이름 변경
18. W-5: AddPillScreen ScheduleType 처리
19. W-7, W-8: 하드코딩 문자열 추출

### Phase 5: UX 개선
20. W-9: isSaving thread safety
21. W-13: CalendarScreen Flow 최적화
22. W-14, W-15: 미구현 UI 처리

---

## 8. 긍정적 평가

- MVVM 아키텍처가 잘 분리되어 있음
- `ScheduleConfig` sealed class로 유연한 스케줄링 시스템
- Room 마이그레이션 v1~v11 완전함
- 6개 언어 다국어 지원
- Kotlin sealed class와 enum 적절한 활용
- Material3 테마 일관성
- Widget 지원
