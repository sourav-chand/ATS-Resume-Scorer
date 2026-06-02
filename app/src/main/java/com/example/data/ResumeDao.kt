package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeAnalysisDao {
    @Query("SELECT * FROM resume_analyses ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<ResumeAnalysis>>

    @Query("SELECT * FROM resume_analyses WHERE id = :id LIMIT 1")
    suspend fun getAnalysisById(id: Int): ResumeAnalysis?

    @Query("SELECT * FROM resume_analyses ORDER BY timestamp DESC LIMIT 1")
    fun getLatestAnalysis(): Flow<ResumeAnalysis?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: ResumeAnalysis): Long

    @Query("DELETE FROM resume_analyses WHERE id = :id")
    suspend fun deleteAnalysisById(id: Int)

    @Query("DELETE FROM resume_analyses")
    suspend fun deleteAll()
}
