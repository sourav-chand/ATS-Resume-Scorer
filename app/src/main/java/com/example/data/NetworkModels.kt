package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<ContentJson>,
    val generationConfig: GenerationConfigJson? = null,
    val systemInstruction: ContentJson? = null
)

@JsonClass(generateAdapter = true)
data class ContentJson(
    val parts: List<PartJson>
)

@JsonClass(generateAdapter = true)
data class PartJson(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfigJson(
    val responseMimeType: String? = null,
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<CandidateJson>? = null
)

@JsonClass(generateAdapter = true)
data class CandidateJson(
    val content: ContentJson? = null
)

// The schema returned by Gemini representing the resume analysis result
@JsonClass(generateAdapter = true)
data class ResumeScanResult(
    val score: Int,
    val jobTitle: String,
    val missingKeywords: List<String>,
    val formattingPassed: List<String>,
    val formattingFailed: List<String>,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val recommendations: List<String>
)
