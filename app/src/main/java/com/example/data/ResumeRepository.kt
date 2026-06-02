package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ResumeRepository(private val resumeDao: ResumeAnalysisDao) {
    val allAnalyses: Flow<List<ResumeAnalysis>> = resumeDao.getAllAnalyses()
    val latestAnalysis: Flow<ResumeAnalysis?> = resumeDao.getLatestAnalysis()

    suspend fun getAnalysisById(id: Int): ResumeAnalysis? {
        return resumeDao.getAnalysisById(id)
    }

    suspend fun insert(analysis: ResumeAnalysis): Long {
        return resumeDao.insertAnalysis(analysis)
    }

    suspend fun deleteById(id: Int) {
        resumeDao.deleteAnalysisById(id)
    }

    suspend fun populateInitialDataIfEmpty() {
        val currentList = resumeDao.getAllAnalyses().first()
        if (currentList.isEmpty()) {
            val now = System.currentTimeMillis()
            val hourMs = 60 * 60 * 1000L
            
            // Item 1: Senior Product Manager Resume v3
            val pmResume = ResumeAnalysis(
                fileName = "Senior Product Manager Resume v3",
                targetJob = "Senior Product Manager",
                timestamp = now - 45 * 60 * 1000L, // Scanned Today, 45m ago
                overallScore = 72,
                missingKeywords = "Wireframing, SQL Analytics, A/B testing",
                formattingPassed = "Machine-readable structure is excellent.,Single-page standard layout.,Contact details properly structured.,Clear chronological dates.",
                formattingFailed = "3 critical ATS keywords omitted.,Omitted action verbs in achievements.,Unstructured profile summary section.",
                strengths = "Solid employment timeline with direct progression.,Clean serif profile categorization formatting.,Highlights multi-platform features deployment.",
                weaknesses = "No quantified key metrics for version 2 growth.,Resume formatting utilizes multi-column margins resulting in potential parser confusion.,Summary is over-detailed.",
                recommendations = "Ensure to include critical missing keywords: Wireframing, SQL, A/B Testing.\nQuantify accomplishment sections using standard bullet metrics.\nRevise early experience blocks into 3 primary action statements.",
                isMock = true
            )

            // Item 2: Tech Lead Application - Google
            val techResume = ResumeAnalysis(
                fileName = "Tech Lead Application - Google",
                targetJob = "Tech Lead - Core Infrastructure",
                timestamp = now - 24 * hourMs, // Yesterday
                overallScore = 88,
                missingKeywords = "Cloud-native choreography, high throughput RPC endpoints",
                formattingPassed = "Chronological project detail layout.,High-contrast visual hierarchy.,Concise bullet action verbs.,Included direct github profile.,Zero multi-column parser bottlenecks.",
                formattingFailed = "Omitted detailed security clearances.,Minor generic fonts layout.",
                strengths = "Exceptional system design expertise demonstration.,Strong delivery accomplishments with precise percentages (e.g. 'Reduced latency by 42%').,Polished layout structure.",
                weaknesses = "Lacks detailed documentation on personal mentor stats.,Highlights legacy migration details over active modern stacks.",
                recommendations = "Integrate secure gRPC scaling terminologies.\nBroaden mentor statements with exact lead counts.\nEnsure direct alignment with Google core infrastructure specifications.",
                isMock = true
            )

            resumeDao.insertAnalysis(pmResume)
            resumeDao.insertAnalysis(techResume)
        }
    }
}
