package com.frame.app.platform.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.frame.app.platform.db.entity.ReferenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for saved reference profiles. Defined now (Phase 1 scaffold) so Phase 6's
 * library implementation has its persistence surface; unused until then.
 */
@Dao
interface ReferenceDao {

    @Query("SELECT * FROM reference_profiles ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<ReferenceEntity>>

    @Query("SELECT * FROM reference_profiles WHERE tags LIKE '%' || :tag || '%' ORDER BY createdAtMillis DESC")
    fun observeByTag(tag: String): Flow<List<ReferenceEntity>>

    @Query("SELECT * FROM reference_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReferenceEntity)

    @Query("DELETE FROM reference_profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}
