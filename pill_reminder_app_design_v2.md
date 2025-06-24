# 💊 약알림 (Pill Reminder) - Android 앱 설계 문서

## 📱 개요
약 복용 알람을 위한 안드로이드 앱입니다. 사용자는 약을 등록하고, 요일/시간별 알람을 설정하며, 복용 여부를 캘린더로 확인할 수 있습니다.

---

## 🧩 주요 기능 정의

| 기능 | 설명 |
|------|------|
| 📷 약 등록 | 사진 촬영 또는 이미지 선택으로 약 등록 |
| ⏰ 알람 등록 | 요일/시간별로 알람 설정, 약별로 여러 알람 등록 가능 |
| ✅ 복용 체크 | 알람 발생 시 "복용 완료" 버튼 제공 → 캘린더에 기록 |
| 📅 복용 캘린더 | 복용 이력 캘린더에서 확인 가능 |

---

## 🗂 데이터 구조 설계

### 📄 Pill (약 정보)
```kotlin
data class Pill(
    val id: String,
    val name: String,
    val imageUri: String?,  // 사진 or 이미지 등록
    val memo: String?       // 선택사항
)
```

### ⏰ PillAlarm (알람 정보)
```kotlin
data class PillAlarm(
    val id: String,
    val pillId: String,
    val hour: Int,
    val minute: Int,
    val repeatDays: List<DayOfWeek>,  // 반복 요일
    val enabled: Boolean
)
```

### ✅ IntakeHistory (복용 기록)
```kotlin
data class IntakeHistory(
    val id: String,
    val pillId: String,
    val alarmId: String,
    val intakeTime: LocalDateTime
)
```

---

## 🎛 UI 플로우

```
[홈 화면]
 └─ [약 목록] + [약 추가 버튼]
       └─ 약 이미지 등록 (촬영 or 갤러리)
       └─ 이름, 메모 입력
       └─ 약 저장

 └─ [알람 관리]
       └─ 약 선택 → 알람 추가
           └─ 시간 설정
           └─ 반복 요일 선택
           └─ 저장

 └─ [캘린더 보기]
       └─ 날짜별 복용 여부 확인
```

---

## 🔔 알람 기능

- AlarmManager + BroadcastReceiver 사용
- Notification 클릭 시 "복용 완료" 버튼 제공
- 복용 완료 시 IntakeHistory에 기록 저장

---

## 🗃 데이터 저장 방식

- 로컬 저장 우선
  - Room (SQLite 기반) 사용
  - 추후 Firebase 등 클라우드 연동 가능

---

## 🧪 테스트 시나리오 예시

- [ ] 약 등록 시 사진 촬영 동작 확인
- [ ] 알람 여러 개 등록 가능 여부 확인
- [ ] 알람 시간 정확히 울림 여부
- [ ] 복용 완료 버튼 클릭 시 기록 저장 확인
- [ ] 캘린더에서 복용 여부 표시 확인

---

## 🔧 권한 설정

- CAMERA 권한
- READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE (API 29 이하)
- POST_NOTIFICATIONS (Android 13 이상)

---

## 📌 향후 확장 가능 항목

- Firebase 연동 통한 백업/복구
- Google Calendar 연동
- 가족/보호자 알림 공유 기능