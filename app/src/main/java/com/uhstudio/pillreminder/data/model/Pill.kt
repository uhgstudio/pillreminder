package com.example.pillreminder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 약 정보를 저장하는 데이터 클래스
 * @property id 약 고유 식별자
 * @property name 약 이름
 * @property imageUri 약 이미지 URI (선택사항)
 * @property memo 약에 대한 메모 (선택사항)
 */
@Entity(tableName = "pills")
data class Pill(
    @PrimaryKey
    val id: String,
    val name: String,
    val imageUri: String? = null,
    val memo: String? = null
) 