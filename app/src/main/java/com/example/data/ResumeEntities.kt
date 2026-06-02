package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resume_analyses")
data class ResumeAnalysis(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val targetJob: String,
    val timestamp: Long,
    val overallScore: Int,
    val missingKeywords: String, // comma-separated strings
    val formattingPassed: String, // newline-separated or comma-separated
    val formattingFailed: String,
    val strengths: String,
    val weaknesses: String,
    val recommendations: String,
    val isMock: Boolean = false
)
