package com.uhstudio.pillreminder.data.dao

import androidx.room.*
import com.uhstudio.pillreminder.data.model.Pill
import kotlinx.coroutines.flow.Flow

/**
 * 약 정보에 대한 데이터베이스 접근 인터페이스
 */
@Dao
interface PillDao {
    @Query("SELECT * FROM pills")
    fun getAllPills(): Flow<List<Pill>>

    @Query("SELECT * FROM pills")
    suspend fun getAllPillsOnce(): List<Pill>

    @Query("SELECT * FROM pills WHERE id = :pillId")
    suspend fun getPillById(pillId: String): Pill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPill(pill: Pill)

    @Update
    suspend fun updatePill(pill: Pill)

    @Delete
    suspend fun deletePill(pill: Pill)
} 