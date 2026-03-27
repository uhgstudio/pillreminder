# Design: YacTime (PillReminder) 프로젝트 개선

- Feature: project-improvement
- Created: 2026-03-06
- Status: Design
- Plan Reference: `docs/01-plan/features/project-improvement.plan.md`

---

## Phase 1: 긴급 버그 수정

### D-C7: AlarmRepository 유효성 검사 수정 (라이브 버그)

**파일**: `data/repository/AlarmRepository.kt`

**현재 코드** (line 74-104):
```kotlin
private fun validateAlarmInput(
    hour: Int,
    minute: Int,
    repeatDays: Set<DayOfWeek>
): ValidationResult<Unit> {
    // ...
    if (repeatDays.isEmpty()) {  // DAILY, INTERVAL_DAYS 등도 실패
        errors.add("최소 하나의 요일을 선택해야 합니다.")
    }
}
```

**수정 코드**:
```kotlin
private fun validateAlarmInput(
    hour: Int,
    minute: Int,
    scheduleType: ScheduleType,
    repeatDays: Set<DayOfWeek>,
    scheduleConfig: String?
): ValidationResult<Unit> {
    val errors = mutableListOf<String>()

    if (hour !in 0..23) {
        errors.add("시간은 0~23 사이여야 합니다. (입력값: $hour)")
        Timber.w("Invalid hour: $hour")
    }
    if (minute !in 0..59) {
        errors.add("분은 0~59 사이여야 합니다. (입력값: $minute)")
        Timber.w("Invalid minute: $minute")
    }

    // scheduleType에 따른 분기 검증
    when (scheduleType) {
        ScheduleType.WEEKLY -> {
            @Suppress("DEPRECATION")
            if (repeatDays.isEmpty() && scheduleConfig == null) {
                errors.add("최소 하나의 요일을 선택해야 합니다.")
                Timber.w("Empty repeatDays for WEEKLY alarm")
            }
        }
        ScheduleType.INTERVAL_DAYS -> {
            if (scheduleConfig == null) {
                errors.add("반복 간격 설정이 필요합니다.")
                Timber.w("Missing scheduleConfig for INTERVAL_DAYS")
            }
        }
        ScheduleType.SPECIFIC_DATES -> {
            if (scheduleConfig == null) {
                errors.add("특정 날짜 설정이 필요합니다.")
                Timber.w("Missing scheduleConfig for SPECIFIC_DATES")
            }
        }
        ScheduleType.DAILY -> { /* 추가 검증 불필요 */ }
        ScheduleType.CUSTOM -> { /* 향후 구현 */ }
    }

    return if (errors.isEmpty()) {
        ValidationResult.Valid(Unit)
    } else {
        ValidationResult.Invalid(errors)
    }
}
```

**호출부 수정** (line 111, 151):
```kotlin
// Before:
val validation = validateAlarmInput(alarm.hour, alarm.minute, alarm.repeatDays)

// After:
@Suppress("DEPRECATION")
val validation = validateAlarmInput(
    alarm.hour, alarm.minute, alarm.scheduleType, alarm.repeatDays, alarm.scheduleConfig
)
```

---

### D-C1: HomeViewModel 메모리 누수 수정

**파일**: `ui/home/HomeViewModel.kt`

**현재 코드** (line 81-88, 138-179):
```kotlin
init {
    loadTodayAlarms()
    viewModelScope.launch {
        _todayAlarms.collect {  // 영구 collector
            loadTodayStats()     // 내부에서 또 영구 collector 생성
        }
    }
}

private fun loadTodayStats() {
    viewModelScope.launch {
        // ...
        histories.collect { historyList ->  // 영구 collector (매번 새로 추가됨)
            // ...
        }
    }
}
```

**수정 코드**:
```kotlin
init {
    loadTodayAlarms()
    viewModelScope.launch {
        _todayAlarms.collectLatest {
            updateTodayStats()  // one-shot 함수로 변경
        }
    }
}

private suspend fun updateTodayStats() {
    try {
        val today = LocalDateTime.now()
        val startOfDay = today.toLocalDate().atStartOfDay()
        val endOfDay = today.toLocalDate().plusDays(1).atStartOfDay()

        val historyList = historyDao.getHistoryForDateOnce(startOfDay, endOfDay)
        val totalAlarms = _todayAlarms.value.size

        val uniqueTaken = historyList
            .filter { it.status == IntakeStatus.TAKEN }
            .distinctBy { it.alarmId }
            .size

        val uniqueSkipped = historyList
            .filter { it.status == IntakeStatus.SKIPPED }
            .distinctBy { it.alarmId }
            .size

        val adherenceRate = if (totalAlarms > 0) {
            (uniqueTaken.toFloat() / totalAlarms.toFloat()) * 100f
        } else 0f

        _todayStats.value = IntakeStats(
            totalCount = totalAlarms,
            takenCount = uniqueTaken,
            skippedCount = uniqueSkipped,
            adherenceRate = adherenceRate
        )
    } catch (e: Exception) {
        Timber.e(e, "Failed to update today stats")
        _todayStats.value = IntakeStats()
    }
}

fun refreshData() {
    loadTodayAlarms()
    // loadTodayStats() 제거 - _todayAlarms collectLatest가 자동 트리거
}
```

**DAO 추가 필요** (`IntakeHistoryDao.kt`):
```kotlin
@Query("""
    SELECT * FROM intake_history
    WHERE intakeTime >= :startOfDay AND intakeTime < :endOfDay
    ORDER BY intakeTime DESC
""")
suspend fun getHistoryForDateOnce(startOfDay: LocalDateTime, endOfDay: LocalDateTime): List<IntakeHistory>
```

**추가 import**: `kotlinx.coroutines.flow.collectLatest`

---

### D-C3: Background Activity Start 수정

**파일**: `receiver/AlarmReceiver.kt`

**현재 코드** (line 187-208):
```kotlin
// 전체 화면 알람 Activity 실행
try {
    context.startActivity(alarmActivityIntent)  // Android 10+에서 실패 가능
} catch (e: Exception) { ... }
```

**수정 코드** - Notification의 `fullScreenIntent` 사용:
```kotlin
// fullScreenIntent용 PendingIntent 생성
val fullScreenPendingIntent = PendingIntent.getActivity(
    context,
    RequestCodeUtil.generateRequestCode(alarmId),
    alarmActivityIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

// 알림 빌드
val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
val pillName = pill.name

val notification = NotificationCompat.Builder(context, PillReminderApplication.ALARM_CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_pill_notification)
    .setContentTitle(context.getString(R.string.notification_pill_time))
    .setContentText(pillName)
    .setPriority(NotificationCompat.PRIORITY_MAX)
    .setCategory(NotificationCompat.CATEGORY_ALARM)
    .setAutoCancel(true)
    .setFullScreenIntent(fullScreenPendingIntent, true)
    .addAction(buildTakeAction(context, alarmId, pillId))
    .addAction(buildSkipAction(context, alarmId, pillId))
    .addAction(buildSnoozeAction(context, alarmId, pillId))
    .build()

notificationManager.notify(
    RequestCodeUtil.generateRequestCode(alarmId),
    notification
)
Timber.d("Full-screen notification posted for alarmId=$alarmId")
```

**헬퍼 메서드 추가**:
```kotlin
private fun buildTakeAction(context: Context, alarmId: String, pillId: String): NotificationCompat.Action {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        action = ACTION_TAKE_PILL
        putExtra(EXTRA_ALARM_ID, alarmId)
        putExtra(EXTRA_PILL_ID, pillId)
    }
    val pi = PendingIntent.getBroadcast(
        context,
        RequestCodeUtil.generateRequestCodeWithPrefix("take", alarmId),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return NotificationCompat.Action.Builder(
        R.drawable.ic_check, context.getString(R.string.btn_take), pi
    ).build()
}
// buildSkipAction, buildSnoozeAction도 동일 패턴
```

---

### D-C8: HomeScreen 하드코딩된 "08:00 AM" 수정

**파일**: `ui/home/HomeScreen.kt`

**현재 코드** (line 391):
```kotlin
text = stringResource(R.string.home_next_dose, "08:00 AM"),
```

**수정 코드**:
`StitchPillItem` 함수에 `nextDoseTime` 파라미터 추가:
```kotlin
// StitchPillItem에 파라미터 추가
@Composable
fun StitchPillItem(
    pill: Pill,
    nextDoseTime: String,  // 추가
    // ... 기존 파라미터들
)

// 사용 부분:
Text(
    text = stringResource(R.string.home_next_dose, nextDoseTime),
    // ...
)
```

**호출부**: `TodayAlarm.time`에서 시간 포맷:
```kotlin
val timeFormat = remember { DateTimeFormatter.ofPattern("hh:mm a") }

StitchPillItem(
    pill = todayAlarm.pill,
    nextDoseTime = todayAlarm.time.format(timeFormat),
    // ...
)
```

---

### D-C9: AlarmScreen 뒤로가기 아이콘 수정

**파일**: `ui/alarm/AlarmScreen.kt`

**현재 코드** (line 48):
```kotlin
imageVector = Icons.Default.Add,
```

**수정 코드**:
```kotlin
imageVector = Icons.AutoMirrored.Filled.ArrowBack,
```

---

### D-C11: ScheduleCalculator Division by Zero 가드

**파일**: `util/ScheduleCalculator.kt`

**현재 코드** (line 151):
```kotlin
val remainder = daysSinceStart % config.intervalDays
```

**수정 코드** (line 141 이후 추가):
```kotlin
if (config.intervalDays <= 0) {
    Timber.w("calculateIntervalDays: invalid intervalDays=${config.intervalDays}")
    return null
}

val remainder = daysSinceStart % config.intervalDays
```

---

## Phase 2: 데이터 안전성

### D-C4: DayOfWeekConverter 중복 삭제

**작업**: `data/converter/DayOfWeekConverter.kt` 파일 삭제
- `Converters.kt`에 동일 기능 존재 (`.filter { it.isNotBlank() }` 포함)
- 참조가 없음을 확인 후 삭제

---

### D-C5: Export/Import 데이터 손실 수정

**파일**: `data/export/ExportModels.kt`

**수정된 PillExport**:
```kotlin
data class PillExport(
    val id: String,
    val name: String,
    val imageUri: String?,
    val memo: String?,
    // 추가 필드
    val type: String = "Capsule",
    val dosage: String = "",
    val quantity: Int = 30,
    val lowStockThreshold: Int = 5
)
```

**수정된 PillAlarmExport**:
```kotlin
data class PillAlarmExport(
    val id: String,
    val pillId: String,
    val hour: Int,
    val minute: Int,
    val repeatDays: List<String>,
    val enabled: Boolean,
    val alarmSoundUri: String?,
    // 추가 필드
    val scheduleType: String = "WEEKLY",
    val scheduleConfig: String? = null,
    val startDate: String? = null,   // ISO 형식 LocalDate
    val endDate: String? = null,
    val createdAt: String? = null,   // ISO 형식 LocalDateTime
    val updatedAt: String? = null
)
```

**파일**: `data/export/EntityExtensions.kt`

**수정된 Pill.toExport()**:
```kotlin
fun Pill.toExport(): PillExport {
    return PillExport(
        id = this.id,
        name = this.name,
        imageUri = this.imageUri,
        memo = this.memo,
        type = this.type,
        dosage = this.dosage,
        quantity = this.quantity,
        lowStockThreshold = this.lowStockThreshold
    )
}
```

**수정된 PillExport.toPill()**:
```kotlin
fun PillExport.toPill(): Pill {
    return Pill(
        id = this.id,
        name = this.name,
        imageUri = this.imageUri,
        memo = this.memo,
        type = this.type,
        dosage = this.dosage,
        quantity = this.quantity,
        lowStockThreshold = this.lowStockThreshold
    )
}
```

**수정된 PillAlarm.toExport()**:
```kotlin
fun PillAlarm.toExport(): PillAlarmExport {
    return PillAlarmExport(
        id = this.id,
        pillId = this.pillId,
        hour = this.hour,
        minute = this.minute,
        @Suppress("DEPRECATION")
        repeatDays = this.repeatDays.map { it.name },
        enabled = this.enabled,
        alarmSoundUri = this.alarmSoundUri,
        scheduleType = this.scheduleType.name,
        scheduleConfig = this.scheduleConfig,
        startDate = this.startDate?.toString(),
        endDate = this.endDate?.toString(),
        createdAt = this.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = this.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}
```

**수정된 PillAlarmExport.toPillAlarm()**:
```kotlin
fun PillAlarmExport.toPillAlarm(): PillAlarm {
    return PillAlarm(
        id = this.id,
        pillId = this.pillId,
        hour = this.hour,
        minute = this.minute,
        repeatDays = this.repeatDays.mapNotNull { dayName ->
            try { DayOfWeek.valueOf(dayName) } catch (e: IllegalArgumentException) { null }
        }.toSet(),
        enabled = this.enabled,
        alarmSoundUri = this.alarmSoundUri,
        scheduleType = try {
            ScheduleType.valueOf(this.scheduleType)
        } catch (e: IllegalArgumentException) {
            ScheduleType.WEEKLY
        },
        scheduleConfig = this.scheduleConfig,
        startDate = this.startDate?.let { LocalDate.parse(it) },
        endDate = this.endDate?.let { LocalDate.parse(it) },
        createdAt = this.createdAt?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } ?: LocalDateTime.now(),
        updatedAt = this.updatedAt?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } ?: LocalDateTime.now()
    )
}
```

**ExportData 버전 업데이트**:
```kotlin
data class ExportData(
    val exportVersion: Int = 2,  // 1 -> 2
    // ...
)
```

---

### D-C6: REPLACE_ALL Import 수정

**파일**: `data/repository/ExportRepository.kt`

**현재 코드** (line 101-131): 기존 데이터를 삭제하지 않고 insert만 수행

**수정 코드**:
```kotlin
private suspend fun importWithReplace(data: ExportData, errors: MutableList<String>) {
    // 기존 데이터 전체 삭제 (CASCADE로 알람, 기록도 삭제됨)
    database.pillDao().deleteAllPills()
    Timber.d("importWithReplace: all existing data deleted")

    // 순서: Pills -> Alarms -> History (외래키 제약 준수)
    data.pills.forEach { pillExport ->
        try {
            database.pillDao().insertPill(pillExport.toPill())
        } catch (e: Exception) {
            errors.add("약 가져오기 실패 (${pillExport.name}): ${e.message}")
        }
    }
    // ... (Alarms, History 동일)
}
```

**PillDao에 추가 필요**:
```kotlin
@Query("DELETE FROM pills")
suspend fun deleteAllPills()
```

---

### D-C10: IntakeHistoryDao getIntakeDates 수정

**파일**: `data/dao/IntakeHistoryDao.kt`

**현재 코드** (line 69-77): `date(intakeTime)` -> `Flow<List<LocalDateTime>>` 타입 불일치

**수정 코드**: 반환 타입을 `Flow<List<String>>`으로 변경:
```kotlin
@Query("""
    SELECT DISTINCT date(intakeTime) as date
    FROM intake_history
    WHERE status = 'TAKEN'
    AND date(intakeTime) >= date(:startDate)
    AND date(intakeTime) <= date(:endDate)
""")
fun getIntakeDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<String>>
```

**호출부에서 파싱**:
```kotlin
val dates: Flow<List<LocalDate>> = dao.getIntakeDates(start, end)
    .map { strings -> strings.map { LocalDate.parse(it) } }
```

`@Transaction` 어노테이션 제거 (단순 쿼리이므로 불필요)

---

## Phase 3: 코루틴/메모리

### D-C2: AlarmReceiver/BootReceiver 구조적 코루틴

**파일**: `receiver/AlarmReceiver.kt`, `receiver/BootReceiver.kt`

**현재 코드**:
```kotlin
CoroutineScope(Dispatchers.IO).launch {  // 비구조적 - 라이프사이클 미연결
    try { ... }
    finally { pendingResult.finish() }
}
```

**수정 코드**:
```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()

    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.IO + job)

    scope.launch {
        try {
            withTimeout(9_000) {  // BroadcastReceiver 10초 제한보다 약간 짧게
                when (intent.action) {
                    ACTION_TAKE_PILL -> handleIntake(context, intent, IntakeStatus.TAKEN)
                    ACTION_SKIP_PILL -> handleIntake(context, intent, IntakeStatus.SKIPPED)
                    ACTION_SNOOZE -> handleSnooze(context, intent)
                    else -> showNotification(context, intent)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.e("AlarmReceiver timed out: action=${intent.action}")
        } catch (e: Exception) {
            Timber.e(e, "Error in AlarmReceiver: action=${intent.action}")
        } finally {
            pendingResult.finish()
            job.cancel()
        }
    }
}
```

**추가 import**: `kotlinx.coroutines.SupervisorJob`, `kotlinx.coroutines.withTimeout`, `kotlinx.coroutines.TimeoutCancellationException`

**BootReceiver도 동일 패턴 적용**

---

### D-W1: ImageUtil Bitmap recycle

**파일**: `util/ImageUtil.kt`

`saveImageToFile` 내 리사이즈 후 원본 recycle:
```kotlin
val bitmap = BitmapFactory.decodeStream(inputStream)
val resizedBitmap = if (bitmap.width > MAX_IMAGE_SIZE || bitmap.height > MAX_IMAGE_SIZE) {
    val resized = resizeBitmap(bitmap, MAX_IMAGE_SIZE)
    bitmap.recycle()  // 원본 recycle 추가
    resized
} else {
    bitmap
}

// 압축 후:
FileOutputStream(imageFile).use { out ->
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
}
resizedBitmap.recycle()  // 사용 완료 후 recycle
```

---

### D-W2: IntakeHistoryRepository 성능 개선

**파일**: `data/repository/IntakeHistoryRepository.kt`

**DAO에 쿼리 추가** (`IntakeHistoryDao.kt`):
```kotlin
@Query("""
    SELECT * FROM intake_history
    WHERE pillId = :pillId
    AND intakeTime >= :startDate AND intakeTime <= :endDate
    ORDER BY intakeTime DESC
""")
suspend fun getHistoryForPillAndDateRange(
    pillId: String, startDate: LocalDateTime, endDate: LocalDateTime
): List<IntakeHistory>
```

**Repository 수정**:
```kotlin
suspend fun calculateIntakeStats(pillId: String, startDate: LocalDateTime, endDate: LocalDateTime): IntakeStats {
    val filtered = historyDao.getHistoryForPillAndDateRange(pillId, startDate, endDate)
    // ... 계산 로직 (메모리 필터링 제거)
}
```

---

## Phase 4: 코드 정리 및 품질

### D-W6: getScheduleDescriptionInternal 중복 제거

**새 파일**: `util/ScheduleDescriptionUtil.kt`

```kotlin
package com.uhstudio.pillreminder.util

object ScheduleDescriptionUtil {
    fun getScheduleDescription(alarm: PillAlarm, context: Context): String {
        // AlarmScreen.kt:170, AlarmsScreen.kt:383, PillDetailScreen.kt:599의
        // getScheduleDescriptionInternal 로직을 여기로 이동
    }
}
```

3개 파일에서 `getScheduleDescriptionInternal` 함수 제거 후 `ScheduleDescriptionUtil.getScheduleDescription()` 호출로 대체

---

### D-W3: AlarmManagerUtil Dead Code 제거

**파일**: `util/AlarmManagerUtil.kt`

minSdk 26이므로 `Build.VERSION_CODES.M` (23) 분기 제거:
```kotlin
// Before:
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    alarmManager.setAlarmClock(...)
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    alarmManager.setAlarmClock(...)
} else {
    alarmManager.setExact(...)
}

// After:
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    alarmManager.setAlarmClock(...)
} else {
    alarmManager.setAlarmClock(...)
}
```

`AlarmReceiver.kt:132`의 동일한 dead code도 제거 (handleSnooze 내):
```kotlin
// Before:
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { ... } else { ... }

// After: (minSdk >= M)
alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
```

---

### D-W12: Dead 마이그레이션 코멘트 삭제

**파일**: `data/database/PillReminderDatabase.kt:166`

```kotlin
// Before:
// MIGRATION_9_10 removed (commented out)
private val MIGRATION_9_10 = object : Migration(9, 10) {

// After:
private val MIGRATION_9_10 = object : Migration(9, 10) {
```

---

### D-W10, D-W11: 미사용 파일 삭제

1. **`util/AlarmUtil.kt`** - 삭제 (참조 확인 후)
2. **`ui/pill/EditPillViewModel.kt`** - 삭제 (EditPillScreen이 AddPillViewModel 사용)
3. **`data/converter/DayOfWeekConverter.kt`** - 삭제 (Phase 2 D-C4)

---

### D-W4: runCatching 이름 충돌 해결

**파일**: `util/Result.kt`

```kotlin
// Before:
suspend fun <T> runCatching(block: suspend () -> T): Result<T> {

// After:
suspend fun <T> runSafely(block: suspend () -> T): Result<T> {
```

모든 호출부에서 `runCatching` -> `runSafely` 로 변경

---

### D-W5: AddPillScreen ScheduleType 전체 처리

**파일**: `ui/pill/AddPillScreen.kt`

**현재 코드** (line 492-496):
```kotlin
val scheduleConfig = when (scheduleType) {
    ScheduleType.DAILY -> ScheduleConfig.Daily
    ScheduleType.WEEKLY -> ScheduleConfig.Weekly.from(selectedDays)
    else -> ScheduleConfig.Daily  // INTERVAL_DAYS, SPECIFIC_DATES 무시
}
```

**수정 코드**:
```kotlin
val scheduleConfig = when (scheduleType) {
    ScheduleType.DAILY -> ScheduleConfig.Daily
    ScheduleType.WEEKLY -> ScheduleConfig.Weekly.from(selectedDays)
    ScheduleType.INTERVAL_DAYS -> ScheduleConfig.IntervalDays(
        intervalDays = intervalDays,
        startDate = startDate.toString()
    )
    ScheduleType.SPECIFIC_DATES -> ScheduleConfig.SpecificDates(
        dates = specificDates.map { it.toString() }
    )
    ScheduleType.CUSTOM -> ScheduleConfig.Daily
}
```

---

### D-W7: AddAlarmScreen 하드코딩된 한국어 추출

**파일**: `ui/addAlarm/AddAlarmScreen.kt`

`res/values/strings.xml`에 추가:
```xml
<string name="exact_alarm_permission_title">정확한 알람 권한 필요</string>
<string name="exact_alarm_permission_message">약 복용 알람이 정확한 시간에 울리려면 \"정확한 알람\" 권한이 필요합니다.\n\n설정으로 이동하여 권한을 허용해주세요.</string>
<string name="go_to_settings">설정으로 이동</string>
```

코드에서 `stringResource()` 사용으로 교체

---

### D-W8: SettingsScreen 버전 하드코딩 수정

**파일**: `ui/settings/SettingsScreen.kt:636`

```kotlin
// Before:
text = "1.0.5",

// After:
text = BuildConfig.VERSION_NAME,
```

`import com.uhstudio.pillreminder.BuildConfig` 추가

---

## Phase 5: UX 개선

### D-W9: isSaving Race Condition

**파일**: `AddAlarmViewModel.kt`, `AddPillViewModel.kt`

```kotlin
// Before:
private var isSaving = false

// After:
private val _isSaving = MutableStateFlow(false)
```

사용부:
```kotlin
fun save() {
    if (_isSaving.value) {
        Timber.w("이미 저장 중입니다")
        return
    }
    _isSaving.value = true
    viewModelScope.launch {
        try { ... }
        finally { _isSaving.value = false }
    }
}
```

---

### D-W13: CalendarScreen Flow 최적화

**파일**: `ui/calendar/CalendarScreen.kt`

```kotlin
// Before (매 리컴포지션마다 새 Flow):
val intakeHistory by viewModel.getIntakeHistoryForDate(selectedDate)
    .collectAsState(initial = emptyList())

// After:
val intakeHistoryFlow = remember(selectedDate) {
    viewModel.getIntakeHistoryForDate(selectedDate)
}
val intakeHistory by intakeHistoryFlow.collectAsState(initial = emptyList())
```

---

### D-W14: HomeScreen "View All" 미구현 처리

**파일**: `ui/home/HomeScreen.kt:135`

`clickable` 에 `onNavigateToAlarms` 콜백 연결 또는 clickable 제거:
```kotlin
// Option A: 연결
Modifier.clickable { onNavigateToAlarms() }

// Option B: 제거
// Modifier.clickable { /* View All */ } -> 삭제
```

---

## 수정 파일 요약

| Phase | 파일 | 작업 |
|-------|------|------|
| 1 | `data/repository/AlarmRepository.kt` | validateAlarmInput scheduleType 분기 |
| 1 | `ui/home/HomeViewModel.kt` | collectLatest + one-shot 쿼리 |
| 1 | `data/dao/IntakeHistoryDao.kt` | getHistoryForDateOnce 추가, getIntakeDates 수정 |
| 1 | `receiver/AlarmReceiver.kt` | fullScreenIntent 방식, 구조적 코루틴 |
| 1 | `ui/home/HomeScreen.kt` | 하드코딩 "08:00 AM" 수정 |
| 1 | `ui/alarm/AlarmScreen.kt` | Icons.Default.Add -> ArrowBack |
| 1 | `util/ScheduleCalculator.kt` | intervalDays <= 0 가드 |
| 2 | `data/converter/DayOfWeekConverter.kt` | 삭제 |
| 2 | `data/export/ExportModels.kt` | 필드 추가 |
| 2 | `data/export/EntityExtensions.kt` | 필드 매핑 추가 |
| 2 | `data/repository/ExportRepository.kt` | deleteAll 추가, 검증 최적화 |
| 2 | `data/dao/PillDao.kt` | deleteAllPills 추가 |
| 3 | `receiver/BootReceiver.kt` | 구조적 코루틴 |
| 3 | `util/ImageUtil.kt` | bitmap.recycle() 추가 |
| 3 | `data/repository/IntakeHistoryRepository.kt` | SQL 필터링 |
| 4 | `util/ScheduleDescriptionUtil.kt` | 신규 - 중복 함수 추출 |
| 4 | `util/AlarmManagerUtil.kt` | dead code 분기 제거 |
| 4 | `data/database/PillReminderDatabase.kt` | dead 코멘트 제거 |
| 4 | `util/AlarmUtil.kt` | 삭제 |
| 4 | `ui/pill/EditPillViewModel.kt` | 삭제 |
| 4 | `util/Result.kt` | runCatching -> runSafely |
| 4 | `ui/pill/AddPillScreen.kt` | ScheduleType 전체 처리 |
| 4 | `ui/addAlarm/AddAlarmScreen.kt` | 한국어 -> stringResource |
| 4 | `ui/settings/SettingsScreen.kt` | BuildConfig.VERSION_NAME |
| 4 | `res/values/strings.xml` | 문자열 리소스 추가 |
| 5 | `ui/addAlarm/AddAlarmViewModel.kt` | MutableStateFlow isSaving |
| 5 | `ui/pill/AddPillViewModel.kt` | MutableStateFlow isSaving |
| 5 | `ui/calendar/CalendarScreen.kt` | remember(selectedDate) |
| 5 | `ui/home/HomeScreen.kt` | View All 처리 |
| 5 | `ui/alarm/AlarmScreen.kt`, `AlarmsScreen.kt`, `PillDetailScreen.kt` | 중복 함수 제거 |

---

## 구현 순서

```
Phase 1 (긴급) ──────────────────────────────────
  D-C7  AlarmRepository 유효성 검사
  D-C1  HomeViewModel 메모리 누수
  D-C3  Background Activity Start
  D-C8  하드코딩된 "08:00 AM"
  D-C9  뒤로가기 아이콘
  D-C11 Division by Zero 가드

Phase 2 (데이터) ─────────────────────────────────
  D-C4  DayOfWeekConverter 삭제
  D-C5  Export 필드 추가
  D-C6  REPLACE_ALL 수정
  D-C10 getIntakeDates 타입 수정

Phase 3 (코루틴/메모리) ──────────────────────────
  D-C2  AlarmReceiver 구조적 코루틴
  D-W1  ImageUtil Bitmap recycle
  D-W2  IntakeHistoryRepository SQL 최적화

Phase 4 (코드 정리) ──────────────────────────────
  D-W6  getScheduleDescriptionInternal 추출
  D-W3  AlarmManagerUtil dead code 제거
  D-W12 Dead 코멘트 삭제
  D-W10/11 미사용 파일 삭제
  D-W4  runCatching -> runSafely
  D-W5  AddPillScreen ScheduleType
  D-W7  하드코딩 한국어 추출
  D-W8  BuildConfig.VERSION_NAME

Phase 5 (UX) ─────────────────────────────────────
  D-W9  isSaving StateFlow
  D-W13 CalendarScreen Flow 캐싱
  D-W14 View All 처리
```
