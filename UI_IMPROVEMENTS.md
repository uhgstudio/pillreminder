# UI/UX 개선 분석 보고서

약 알림 앱의 전체 UI/UX를 분석한 결과입니다.

---

## 1. AlarmsScreen (알람 탭)

### 현재 문제점

#### 1.1 누락된 핵심 기능
- **알람 추가 버튼 없음**: FAB(FloatingActionButton)이나 다른 알람 추가 방법이 없음
- **알람 삭제 기능 없음**: 개별 알람을 삭제할 수 없음
- **알람 편집 기능 없음**: 기존 알람을 수정할 수 없음

#### 1.2 요일 표시 문제
- **Line 83**: `it.name`으로 영어(MONDAY, TUESDAY)가 표시됨
- **개선 필요**: 한국어로 "월, 화, 수, 목, 금, 토, 일" 표시

#### 1.3 정보 부족
- 어떤 약의 알람인지 알 수 없음 (pillId만 표시)
- 다음 알람 시간, 남은 시간 등 유용한 정보 없음

#### 1.4 접근성 문제
- Switch에 contentDescription 없음
- 시간 표시의 접근성 레이블 없음

### 개선 방안
```kotlin
// 요일 한국어 변환 함수 추가
fun DayOfWeek.toKorean(): String = when (this) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}

// FAB 추가
FloatingActionButton(onClick = { /* 알람 추가 화면으로 이동 */ })
```

---

## 2. AddAlarmScreen (알람 추가)

### 현재 문제점

#### 2.1 요일 선택 UI 문제
- **Line 142**: `day.name.take(1)`로 영어 첫 글자만 표시 (M, T, W, T, F, S, S)
- 사용자가 어떤 요일인지 구분하기 어려움

#### 2.2 시간 선택기 불편
- 커스텀 NumberPicker 사용 (위/아래 버튼으로만 조작)
- Android의 Material TimePicker 사용 권장

#### 2.3 UX 문제
- 저장 실패 시 피드백 없음
- 요일 버튼 크기가 작음 (터치 영역 부족)
- 선택된 요일이 없을 때 안내 메시지 없음

#### 2.4 접근성 문제
- DayButton에 contentDescription 없음
- NumberPicker 버튼에 접근성 레이블 없음

### 개선 방안
```kotlin
// 요일 버튼 한국어로 변경
val koreanDays = listOf("월", "화", "수", "목", "금", "토", "일")
DayOfWeek.values().forEachIndexed { index, day ->
    DayButton(
        text = koreanDays[index],
        // ...
    )
}

// Material TimePicker 사용
val timePickerState = rememberTimePickerState()
TimePicker(state = timePickerState)
```

---

## 3. HomeScreen (홈 탭)

### 현재 문제점

#### 3.1 이미지 처리
- **Line 134-143**: imageUri가 null일 때 빈 공간만 표시
- placeholder 이미지 필요

#### 3.2 레이아웃 문제
- **Line 131**: `height(80.dp)` 고정으로 긴 메모가 잘림
- 리스트 상단/하단 패딩 부족
- 첫 번째 아이템이 화면 상단에 붙어 있음

#### 3.3 UX 문제
- 삭제 버튼이 오른쪽 상단에 있어 실수 유발 가능
- 스와이프 삭제 미지원
- 각 약의 다음 알람 시간 표시 없음

#### 3.4 접근성 문제
- AsyncImage의 contentDescription이 null

### 개선 방안
```kotlin
// 이미지 placeholder 추가
AsyncImage(
    model = imageUri,
    contentDescription = pill.name,
    placeholder = painterResource(R.drawable.ic_pill_placeholder),
    error = painterResource(R.drawable.ic_pill_placeholder)
)

// 리스트 패딩 추가
LazyColumn(
    contentPadding = PaddingValues(vertical = 8.dp)
)
```

---

## 4. PillDetailScreen (약 상세)

### 현재 문제점

#### 4.1 요일 표시 문제
- **Line 185**: `it.name`으로 영어 표시

#### 4.2 누락된 기능
- 알람 삭제 기능 없음
- 알람 편집 기능 없음
- 알람 on/off 토글 없음
- 복용 기록 표시 없음

#### 4.3 레이아웃 문제
- **스크롤 미지원**: Column 사용으로 알람이 많으면 스크롤 안 됨
- pill이 null일 때 빈 화면 (로딩 인디케이터 없음)

#### 4.4 접근성 문제
- AsyncImage의 contentDescription이 null

### 개선 방안
```kotlin
// LazyColumn으로 변경하여 스크롤 지원
LazyColumn {
    item { /* 약 정보 */ }
    items(alarms) { alarm -> /* 알람 아이템 */ }
}

// 로딩 상태 표시
if (pill == null) {
    CircularProgressIndicator()
} else {
    // 컨텐츠 표시
}
```

---

## 5. AddPillScreen (약 추가/편집)

### 현재 문제점

#### 5.1 코드 중복
- takePhoto() 함수 (Line 91-98)와 Button onClick (Line 170-191)에 유사 로직 중복

#### 5.2 레이아웃 문제
- **스크롤 미지원**: 작은 화면에서 입력 필드가 키보드에 가려짐
- 버튼 레이블이 좁은 화면에서 줄바꿈될 수 있음

#### 5.3 UX 문제
- 저장 완료 피드백 없음 (Toast/Snackbar)
- showImageSelectionDialog 코드가 있지만 사용되지 않음

#### 5.4 접근성 문제
- AsyncImage의 contentDescription이 null

### 개선 방안
```kotlin
// 스크롤 지원 추가
Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
)

// 저장 완료 Snackbar
val snackbarHostState = remember { SnackbarHostState() }
// 저장 후
snackbarHostState.showSnackbar("저장되었습니다")
```

---

## 6. strings.xml 개선 필요

### 추가 필요한 문자열
```xml
<!-- 요일 -->
<string name="day_mon">월</string>
<string name="day_tue">화</string>
<string name="day_wed">수</string>
<string name="day_thu">목</string>
<string name="day_fri">금</string>
<string name="day_sat">토</string>
<string name="day_sun">일</string>

<!-- 알람 관련 -->
<string name="btn_add_alarm">알람 추가</string>
<string name="btn_delete_alarm">알람 삭제</string>
<string name="dialog_delete_alarm_title">알람 삭제</string>
<string name="dialog_delete_alarm_message">이 알람을 삭제하시겠습니까?</string>
<string name="msg_select_day">요일을 선택해주세요</string>

<!-- 상태 메시지 -->
<string name="loading">로딩 중...</string>
<string name="msg_saved">저장되었습니다</string>
<string name="msg_deleted">삭제되었습니다</string>
```

---

## 우선순위별 작업 목록

### 우선순위 1 - 필수 (기능 문제)
| 상태 | 작업 | 파일 | 설명 |
|------|------|------|------|
| [x] | 요일 한국어 표시 | 전체 | 모든 화면에서 월화수목금토일 표시 |
| [x] | 알람 추가 FAB | AlarmsScreen.kt | 알람 탭에서 알람 추가할 수 있도록 |
| [x] | 알람 삭제 기능 | AlarmsScreen.kt, PillDetailScreen.kt | 개별 알람 삭제 |

### 우선순위 2 - 중요 (UX 개선)
| 상태 | 작업 | 파일 | 설명 |
|------|------|------|------|
| [ ] | Material TimePicker | AddAlarmScreen.kt | 시간 선택 UX 개선 |
| [x] | 스크롤 지원 | AddPillScreen.kt | 긴 컨텐츠 스크롤 |
| [x] | 이미지 placeholder | HomeScreen.kt | 이미지 없을 때 기본 아이콘 |
| [x] | 접근성 개선 | 일부 | contentDescription 추가 |

### 우선순위 3 - 개선
| 상태 | 작업 | 파일 | 설명 |
|------|------|------|------|
| [x] | 약 이름 표시 | AlarmsScreen.kt | 어떤 약의 알람인지 표시 |
| [ ] | 로딩 인디케이터 | PillDetailScreen.kt | 데이터 로딩 중 표시 |
| [ ] | 저장 피드백 | AddPillScreen.kt, AddAlarmScreen.kt | Snackbar로 결과 알림 |
| [ ] | 알람 편집 | PillDetailScreen.kt | 기존 알람 수정 기능 |

### 우선순위 4 - 향후
| 상태 | 작업 | 파일 | 설명 |
|------|------|------|------|
| [ ] | 스와이프 삭제 | HomeScreen.kt, AlarmsScreen.kt | 제스처로 삭제 |
| [ ] | 복용 기록 | PillDetailScreen.kt | 복용 이력 표시 |
| [ ] | 다음 알람 정보 | HomeScreen.kt | 각 약의 다음 알람 시간 |

---

## 공통 유틸리티 추가 제안

```kotlin
// util/DayOfWeekExtensions.kt
package com.uhstudio.pillreminder.util

import java.time.DayOfWeek

fun DayOfWeek.toKoreanShort(): String = when (this) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}

fun DayOfWeek.toKoreanFull(): String = when (this) {
    DayOfWeek.MONDAY -> "월요일"
    DayOfWeek.TUESDAY -> "화요일"
    DayOfWeek.WEDNESDAY -> "수요일"
    DayOfWeek.THURSDAY -> "목요일"
    DayOfWeek.FRIDAY -> "금요일"
    DayOfWeek.SATURDAY -> "토요일"
    DayOfWeek.SUNDAY -> "일요일"
}
```

---

## 참고: 수정이 필요한 파일 목록

1. `app/src/main/java/com/example/pillreminder/ui/alarms/AlarmsScreen.kt`
2. `app/src/main/java/com/example/pillreminder/ui/addAlarm/AddAlarmScreen.kt`
3. `app/src/main/java/com/example/pillreminder/ui/home/HomeScreen.kt`
4. `app/src/main/java/com/example/pillreminder/ui/pillDetail/PillDetailScreen.kt`
5. `app/src/main/java/com/example/pillreminder/ui/pill/AddPillScreen.kt`
6. `app/src/main/res/values/strings.xml`
7. (신규) `app/src/main/java/com/example/pillreminder/util/DayOfWeekExtensions.kt`
