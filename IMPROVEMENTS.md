# PillReminder 앱 개선 사항

이 문서는 PillReminder 앱의 분석 결과와 개선 사항을 정리한 것입니다.

## 1단계: 긴급 수정 (컴파일/크래시 오류)

### 컴파일 오류 수정

| 파일 | 문제 | 해결 |
|------|------|------|
| `AddPillScreen.kt` | 정의되지 않은 `launcher` 변수 | `selectImage`로 수정 |
| `AddPillScreen.kt` | 누락된 import | `RoundedCornerShape`, `clip`, `FileProvider`, `Environment` 추가 |
| `PillDetailScreen.kt` | `Icons.Default.Alarm` import 누락 | import 추가 |

### 크래시 방지

| 항목 | 설명 |
|------|------|
| BootReceiver 구현 | AndroidManifest에 선언되어 있지만 클래스가 없어 크래시 발생 → `receiver/BootReceiver.kt` 생성 |
| FileProvider 설정 | 카메라 촬영 시 필요한 provider 및 `file_paths.xml` 설정 추가 |
| 중복 MainActivity 제거 | `ui/MainActivity.kt` (미완성 스텁) 삭제 |

### 데이터 무결성

| 파일 | 문제 | 해결 |
|------|------|------|
| `AddAlarmScreen.kt` | 빈 pillId 전달 | pillId 파라미터 추가 및 네비게이션에서 전달 |
| `HomeScreen.kt` | 네비게이션 타입 불일치 (Pill → String) | `onPillClick: (String) -> Unit`으로 변경 |

---

## 2단계: 핵심 기능 수정

### AlarmReceiver 비동기 처리 개선

**문제**: BroadcastReceiver에서 코루틴 사용 시 `onReceive()` 완료 전에 프로세스가 종료될 수 있음

**해결**: `goAsync()` 사용
```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 작업 수행
        } finally {
            pendingResult.finish()
        }
    }
}
```

### AlarmManagerUtil 개선

**문제점**:
- `setRepeating()` 사용 - Android 4.4+에서 부정확
- repeatDays 무시 - 항상 매일 반복

**개선 사항**:
1. `setAlarmClock()` / `setExactAndAllowWhileIdle()` 사용 (정확한 알람)
2. repeatDays 기반 다음 알람 시간 계산
3. 알람 발생 후 자동 재스케줄링
4. Android 버전별 분기 처리

```kotlin
// Android 12+ 정확한 알람 권한 확인
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (alarmManager.canScheduleExactAlarms()) {
        alarmManager.setAlarmClock(...)
    }
}
```

### 약 삭제 시 알람 취소

**문제**: 약 삭제 시 DB에서만 삭제되고 AlarmManager의 알람은 취소되지 않음

**해결**: `PillDetailViewModel.deletePill()`에서 알람 취소 추가
```kotlin
fun deletePill(pill: Pill, onComplete: () -> Unit) {
    viewModelScope.launch {
        val alarms = alarmDao.getAlarmsForPillOnce(pill.id)
        alarms.forEach { alarm ->
            alarmManagerUtil.cancelAlarm(alarm)
        }
        pillDao.deletePill(pill)
        onComplete()
    }
}
```

---

## 3단계: UX/UI 개선

### 알람 토글 구현

**위치**: `AlarmsScreen.kt`, `AlarmsViewModel.kt`

**기능**: 알람 활성화/비활성화 스위치 동작
- DB 업데이트 (`updateAlarmEnabled`)
- 활성화 시 알람 스케줄링
- 비활성화 시 알람 취소

### 약 편집 기능

**변경 파일**:
- `AddPillViewModel.kt` - `loadPill()`, `editingPill` 상태 추가
- `AddPillScreen.kt` - `pillId` 파라미터, 편집 모드 지원
- `PillDetailScreen.kt` - 편집 버튼 추가
- `MainActivity.kt` - `edit_pill/{pillId}` 라우트 추가

**기능**:
- 기존 약 정보 로드
- 폼 필드 초기화
- 저장 시 update 또는 insert 분기

### 삭제 확인 다이얼로그

**위치**: `HomeScreen.kt`

**기능**: 약 삭제 버튼 클릭 시 확인 다이얼로그 표시
```kotlin
pillToDelete?.let { pill ->
    AlertDialog(
        onDismissRequest = { pillToDelete = null },
        title = { Text(stringResource(R.string.dialog_delete_pill_title)) },
        text = { Text(stringResource(R.string.dialog_delete_pill_message)) },
        // ...
    )
}
```

---

## 4단계: 새 기능 추가

### 스누즈 기능

**위치**: `AlarmReceiver.kt`

**기능**: 알림에 "10분 후 알림" 버튼 추가

**구현**:
1. `ACTION_SNOOZE` 액션 추가
2. `handleSnooze()` 함수 - 10분 후 알람 재스케줄링
3. 알림에 스누즈 버튼 추가

```kotlin
private fun handleSnooze(context: Context, intent: Intent) {
    // 알림 제거
    notificationManager.cancel(alarmId.hashCode())

    // 10분 후 재알람
    val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000)
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        snoozeTime,
        pendingIntent
    )
}
```

**리소스 추가**:
- `strings.xml`: `btn_snooze` = "10분 후 알림"
- `drawable/ic_snooze.xml`: 스누즈 아이콘

---

## 추가된 DAO 메서드

### PillAlarmDao.kt

```kotlin
// BootReceiver용 - 모든 활성화된 알람 한 번에 가져오기
@Query("SELECT * FROM pill_alarms WHERE enabled = 1")
suspend fun getEnabledAlarmsOnce(): List<PillAlarm>

// 약 삭제 시 연관 알람 가져오기
@Query("SELECT * FROM pill_alarms WHERE pillId = :pillId")
suspend fun getAlarmsForPillOnce(pillId: String): List<PillAlarm>
```

---

## 파일 변경 목록

### 수정된 파일
- `app/src/main/java/com/example/pillreminder/MainActivity.kt`
- `app/src/main/java/com/example/pillreminder/receiver/AlarmReceiver.kt`
- `app/src/main/java/com/example/pillreminder/util/AlarmManagerUtil.kt`
- `app/src/main/java/com/example/pillreminder/ui/pill/AddPillScreen.kt`
- `app/src/main/java/com/example/pillreminder/ui/pill/AddPillViewModel.kt`
- `app/src/main/java/com/example/pillreminder/ui/pillDetail/PillDetailScreen.kt`
- `app/src/main/java/com/example/pillreminder/ui/pillDetail/PillDetailViewModel.kt`
- `app/src/main/java/com/example/pillreminder/ui/home/HomeScreen.kt`
- `app/src/main/java/com/example/pillreminder/ui/alarms/AlarmsScreen.kt`
- `app/src/main/java/com/example/pillreminder/ui/alarms/AlarmsViewModel.kt`
- `app/src/main/java/com/example/pillreminder/ui/addAlarm/AddAlarmScreen.kt`
- `app/src/main/java/com/example/pillreminder/data/dao/PillAlarmDao.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/xml/file_paths.xml`

### 생성된 파일
- `app/src/main/java/com/example/pillreminder/receiver/BootReceiver.kt`
- `app/src/main/res/drawable/ic_snooze.xml`

### 삭제된 파일
- `app/src/main/java/com/example/pillreminder/ui/MainActivity.kt` (중복)

---

## 향후 개선 가능 사항

### 우선순위 높음
- [ ] 로딩 상태 및 에러 처리 추가 (모든 ViewModel)
- [ ] 알람 편집 기능
- [ ] 복용량 추적 (1회 복용량 설정)

### 우선순위 중간
- [ ] 복약 통계/리포트 화면
- [ ] 놓친 약 재알림
- [ ] 알림 소리/진동 설정

### 우선순위 낮음
- [ ] Repository 패턴 도입
- [ ] 의존성 주입 (Hilt/Koin)
- [ ] 백업/복원 기능

---

## 빌드 및 테스트

```bash
# 디버그 빌드
./gradlew assembleDebug

# 기기에 설치
./gradlew installDebug

# 테스트 실행
./gradlew test
```
