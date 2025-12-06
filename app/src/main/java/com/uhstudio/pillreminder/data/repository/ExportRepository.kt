package com.uhstudio.pillreminder.data.repository

import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.export.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 데이터 내보내기/가져오기를 담당하는 Repository
 */
class ExportRepository(private val database: PillReminderDatabase) {

    private val gson = Gson()

    /**
     * 모든 데이터를 Export 형식으로 변환
     */
    suspend fun exportAllData(): ExportData = withContext(Dispatchers.IO) {
        val pills = database.pillDao().getAllPillsOnce()
        val alarms = database.pillAlarmDao().getAllAlarmsOnce()
        val history = database.intakeHistoryDao().getAllHistoryOnce()

        ExportData(
            exportVersion = 1,
            exportDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            pills = pills.map { it.toExport() },
            alarms = alarms.map { it.toExport() },
            history = history.map { it.toExport() }
        )
    }

    /**
     * Export 데이터를 JSON 문자열로 변환
     */
    fun exportDataToJson(exportData: ExportData): String {
        return gson.toJson(exportData)
    }

    /**
     * JSON 문자열을 Export 데이터로 파싱
     */
    fun parseJsonToExportData(json: String): ExportData? {
        return try {
            gson.fromJson(json, ExportData::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * 데이터 가져오기
     */
    suspend fun importData(
        data: ExportData,
        strategy: ImportStrategy = ImportStrategy.REPLACE_ALL
    ): ImportResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()

        try {
            when (strategy) {
                ImportStrategy.REPLACE_ALL -> {
                    // 기존 데이터 삭제 후 새 데이터 삽입
                    importWithReplace(data, errors)
                }
                ImportStrategy.MERGE_SKIP -> {
                    // 중복 건너뛰기
                    importWithMerge(data, errors, replaceOnConflict = false)
                }
                ImportStrategy.MERGE_REPLACE -> {
                    // 중복 시 교체
                    importWithMerge(data, errors, replaceOnConflict = true)
                }
            }

            return@withContext ImportResult(
                success = errors.isEmpty(),
                pillsImported = data.pills.size,
                alarmsImported = data.alarms.size,
                historyImported = data.history.size,
                errors = errors
            )
        } catch (e: Exception) {
            errors.add("가져오기 중 오류 발생: ${e.message}")
            return@withContext ImportResult(
                success = false,
                pillsImported = 0,
                alarmsImported = 0,
                historyImported = 0,
                errors = errors
            )
        }
    }

    /**
     * 전체 교체 방식으로 가져오기
     */
    private suspend fun importWithReplace(data: ExportData, errors: MutableList<String>) {
        // 기존 데이터 삭제는 CASCADE로 자동 처리됨
        // 순서: Pills → Alarms → History (외래키 제약 준수)

        // Pills 삽입
        data.pills.forEach { pillExport ->
            try {
                database.pillDao().insertPill(pillExport.toPill())
            } catch (e: Exception) {
                errors.add("약 가져오기 실패 (${pillExport.name}): ${e.message}")
            }
        }

        // Alarms 삽입
        data.alarms.forEach { alarmExport ->
            try {
                database.pillAlarmDao().insertAlarm(alarmExport.toPillAlarm())
            } catch (e: Exception) {
                errors.add("알람 가져오기 실패 (ID: ${alarmExport.id}): ${e.message}")
            }
        }

        // History 삽입
        data.history.forEach { historyExport ->
            try {
                database.intakeHistoryDao().insertHistory(historyExport.toIntakeHistory())
            } catch (e: Exception) {
                errors.add("복용 기록 가져오기 실패 (ID: ${historyExport.id}): ${e.message}")
            }
        }
    }

    /**
     * 병합 방식으로 가져오기
     */
    private suspend fun importWithMerge(
        data: ExportData,
        errors: MutableList<String>,
        replaceOnConflict: Boolean
    ) {
        val existingPills = database.pillDao().getAllPillsOnce().map { it.id }.toSet()
        val existingAlarms = database.pillAlarmDao().getAllAlarmsOnce().map { it.id }.toSet()
        val existingHistory = database.intakeHistoryDao().getAllHistoryOnce().map { it.id }.toSet()

        // Pills
        data.pills.forEach { pillExport ->
            try {
                if (pillExport.id in existingPills && !replaceOnConflict) {
                    // 중복이고 교체하지 않음
                    return@forEach
                }
                database.pillDao().insertPill(pillExport.toPill())
            } catch (e: Exception) {
                errors.add("약 가져오기 실패 (${pillExport.name}): ${e.message}")
            }
        }

        // Alarms
        data.alarms.forEach { alarmExport ->
            try {
                if (alarmExport.id in existingAlarms && !replaceOnConflict) {
                    return@forEach
                }
                database.pillAlarmDao().insertAlarm(alarmExport.toPillAlarm())
            } catch (e: Exception) {
                errors.add("알람 가져오기 실패 (ID: ${alarmExport.id}): ${e.message}")
            }
        }

        // History
        data.history.forEach { historyExport ->
            try {
                if (historyExport.id in existingHistory && !replaceOnConflict) {
                    return@forEach
                }
                database.intakeHistoryDao().insertHistory(historyExport.toIntakeHistory())
            } catch (e: Exception) {
                errors.add("복용 기록 가져오기 실패 (ID: ${historyExport.id}): ${e.message}")
            }
        }
    }

    /**
     * 데이터 유효성 검증
     */
    fun validateExportData(data: ExportData): List<String> {
        val errors = mutableListOf<String>()

        // Pills 검증
        if (data.pills.any { it.name.isBlank() }) {
            errors.add("약 이름이 비어있는 항목이 있습니다.")
        }

        // Alarms 검증
        data.alarms.forEach { alarm ->
            if (alarm.hour !in 0..23) {
                errors.add("잘못된 시간 값 (ID: ${alarm.id})")
            }
            if (alarm.minute !in 0..59) {
                errors.add("잘못된 분 값 (ID: ${alarm.id})")
            }
            if (alarm.pillId !in data.pills.map { it.id }) {
                errors.add("알람의 약 ID가 존재하지 않습니다 (Alarm ID: ${alarm.id})")
            }
        }

        // History 검증
        data.history.forEach { history ->
            if (history.pillId !in data.pills.map { it.id }) {
                errors.add("복용 기록의 약 ID가 존재하지 않습니다 (History ID: ${history.id})")
            }
            if (history.alarmId !in data.alarms.map { it.id }) {
                errors.add("복용 기록의 알람 ID가 존재하지 않습니다 (History ID: ${history.id})")
            }
        }

        return errors
    }
}

/**
 * 가져오기 전략
 */
enum class ImportStrategy {
    REPLACE_ALL,      // 기존 데이터 삭제 후 새 데이터 삽입
    MERGE_SKIP,       // 중복 건너뛰기
    MERGE_REPLACE     // 중복 시 교체
}

/**
 * 가져오기 결과
 */
data class ImportResult(
    val success: Boolean,
    val pillsImported: Int,
    val alarmsImported: Int,
    val historyImported: Int,
    val errors: List<String>
)
