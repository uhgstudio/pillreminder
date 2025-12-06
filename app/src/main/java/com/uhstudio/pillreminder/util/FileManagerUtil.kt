package com.uhstudio.pillreminder.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 파일 선택 및 I/O 유틸리티
 */
object FileManagerUtil {

    /**
     * 기본 파일명 생성
     */
    fun generateDefaultFileName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "pill_reminder_backup_$timestamp.json"
    }

    /**
     * URI에서 파일 내용 읽기
     */
    suspend fun readFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * URI에 파일 내용 쓰기
     */
    suspend fun writeToUri(context: Context, uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * MIME 타입
     */
    const val MIME_TYPE_JSON = "application/json"
}
