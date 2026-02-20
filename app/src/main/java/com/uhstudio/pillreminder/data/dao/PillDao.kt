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

    /**
     * 약 수량 감소 (복용 시 호출)
     */
    @Query("UPDATE pills SET quantity = quantity - 1 WHERE id = :pillId AND quantity > 0")
    suspend fun decrementQuantity(pillId: String)

    /**
     * 약 수량 업데이트
     */
    @Query("UPDATE pills SET quantity = :quantity WHERE id = :pillId")
    suspend fun updateQuantity(pillId: String, quantity: Int)

    /**
     * 재고 부족 약 목록 조회 (quantity <= lowStockThreshold)
     */
    @Query("SELECT * FROM pills WHERE quantity <= lowStockThreshold AND quantity > 0")
    fun getLowStockPills(): Flow<List<Pill>>
} 