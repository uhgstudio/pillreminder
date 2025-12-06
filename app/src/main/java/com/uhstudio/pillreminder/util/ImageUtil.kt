package com.uhstudio.pillreminder.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * 이미지 파일 관리를 위한 유틸리티 클래스
 */
object ImageUtil {
    private const val TAG = "ImageUtil"
    private const val IMAGES_DIR = "pill_images"
    private const val MAX_IMAGE_SIZE = 1024 // 최대 이미지 크기 (픽셀)
    private const val JPEG_QUALITY = 85 // JPEG 압축 품질

    /**
     * URI에서 이미지를 읽어와 앱 내부 저장소에 저장
     * @param context Context
     * @param sourceUri 원본 이미지 URI
     * @return 저장된 파일의 경로 또는 null
     */
    fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            inputStream?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                bitmap?.let { 
                    val resizedBitmap = resizeBitmap(it, MAX_IMAGE_SIZE)
                    saveImageToFile(context, resizedBitmap)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "이미지 저장 실패", e)
            null
        }
    }

    /**
     * 비트맵을 파일로 저장
     * @param context Context
     * @param bitmap 저장할 비트맵
     * @return 저장된 파일의 경로 또는 null
     */
    private fun saveImageToFile(context: Context, bitmap: Bitmap): String? {
        return try {
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val fileName = "pill_${UUID.randomUUID()}.jpg"
            val imageFile = File(imagesDir, fileName)

            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            }

            Log.d(TAG, "이미지 저장 완료: ${imageFile.absolutePath}")
            imageFile.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "이미지 파일 저장 실패", e)
            null
        }
    }

    /**
     * 비트맵 크기 조정
     * @param bitmap 원본 비트맵
     * @param maxSize 최대 크기
     * @return 크기 조정된 비트맵
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 저장된 이미지 파일 삭제
     * @param imagePath 삭제할 이미지 파일 경로
     */
    fun deleteImage(imagePath: String) {
        try {
            val file = File(imagePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "이미지 삭제 완료: $imagePath")
                } else {
                    Log.w(TAG, "이미지 삭제 실패: $imagePath")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "이미지 삭제 중 오류", e)
        }
    }

    /**
     * 이미지 파일이 존재하는지 확인
     * @param imagePath 확인할 이미지 파일 경로
     * @return 파일 존재 여부
     */
    fun imageExists(imagePath: String): Boolean {
        return try {
            File(imagePath).exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 앱 내부 저장소의 모든 이미지 파일 정리 (사용하지 않는 이미지 삭제)
     * @param context Context
     * @param usedImagePaths 현재 사용 중인 이미지 경로 목록
     */
    fun cleanupUnusedImages(context: Context, usedImagePaths: List<String>) {
        try {
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (!imagesDir.exists()) return

            val allImageFiles = imagesDir.listFiles() ?: return
            val usedPaths = usedImagePaths.toSet()

            allImageFiles.forEach { file ->
                if (file.absolutePath !in usedPaths) {
                    if (file.delete()) {
                        Log.d(TAG, "사용하지 않는 이미지 삭제: ${file.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "이미지 정리 중 오류", e)
        }
    }
} 