package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val sender: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ResumeRepository(database.resumeDao())

    // UI tab state: 0 = Dashboard, 1 = Upload, 2 = Analysis, 3 = Advice
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Resume list and active selection
    val allAnalyses: StateFlow<List<ResumeAnalysis>> = repository.allAnalyses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestAnalysis: StateFlow<ResumeAnalysis?> = repository.latestAnalysis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedAnalysis = MutableStateFlow<ResumeAnalysis?>(null)
    val selectedAnalysis: StateFlow<ResumeAnalysis?> = _selectedAnalysis.asStateFlow()

    // Scanning states
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    // Advice Chat States
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("model", "Hello! I am your AI career coach. Ask me advice on interview preparation, resume tuning, or cover letter drafts. How can I assist you today?")
        )
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    init {
        // Pre-populate DB if empty
        viewModelScope.launch(Dispatchers.IO) {
            repository.populateInitialDataIfEmpty()
            // Set initial selected analysis to latest if null
            repository.latestAnalysis.collect { latest ->
                if (_selectedAnalysis.value == null && latest != null) {
                    _selectedAnalysis.value = latest
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun selectAnalysis(analysis: ResumeAnalysis) {
        _selectedAnalysis.value = analysis
        _selectedTab.value = 2 // Auto navigate to Analysis Tab
    }

    fun clearScanError() {
        _scanError.value = null
    }

    // Delete a scanned resume
    fun deleteAnalysis(analysis: ResumeAnalysis) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(analysis.id)
            if (_selectedAnalysis.value?.id == analysis.id) {
                // Point selected analysis to latest available instead
                val remaining = repository.allAnalyses.first()
                _selectedAnalysis.value = remaining.firstOrNull()
            }
        }
    }

    // Scan a new Resume against a target job using real Gemini (or premium simulated fallback if key is placeholder)
    fun scanResume(fileName: String, resumeText: String, targetJob: String) {
        if (resumeText.isBlank()) {
            _scanError.value = "Resume text cannot be blank."
            return
        }

        val job = if (targetJob.trim().isBlank()) "Software Developer" else targetJob.trim()
        _isScanning.value = true
        _scanError.value = null

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val isDemoKey = apiKey.isBlank() || apiKey == "GEMINI_API_KEY" || apiKey == "MY_GEMINI_API_KEY"

                val scanResult = if (isDemoKey) {
                    // Demo mode simulation for robust operation if API key is not fully configured
                    simulateScanResult(job, resumeText)
                } else {
                    withContext(Dispatchers.IO) {
                        executeGeminiResumeScan(apiKey, resumeText, job)
                    }
                }

                if (scanResult != null) {
                    val entity = ResumeAnalysis(
                        fileName = fileName.ifBlank { "Scanned Resume (${job})" },
                        targetJob = job,
                        timestamp = System.currentTimeMillis(),
                        overallScore = scanResult.score,
                        missingKeywords = scanResult.missingKeywords.joinToString(", "),
                        formattingPassed = scanResult.formattingPassed.joinToString(","),
                        formattingFailed = scanResult.formattingFailed.joinToString(","),
                        strengths = scanResult.strengths.joinToString(","),
                        weaknesses = scanResult.weaknesses.joinToString(","),
                        recommendations = scanResult.recommendations.joinToString("\n")
                    )

                    val newId = withContext(Dispatchers.IO) {
                        repository.insert(entity)
                    }

                    // Retrieve the newly inserted item
                    val saved = withContext(Dispatchers.IO) {
                        repository.getAnalysisById(newId.toInt())
                    }
                    if (saved != null) {
                        _selectedAnalysis.value = saved
                    }
                    _selectedTab.value = 2 // Navigate to Report page
                } else {
                    _scanError.value = "Failed to parse analysis schema from Gemini. Please try again."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _scanError.value = "Connection error: ${e.localizedMessage ?: "Please check internet connection"}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    // Call the real Gemini API via Retrofit
    private suspend fun executeGeminiResumeScan(apiKey: String, resumeText: String, job: String): ResumeScanResult? {
        val systemPrompt = """
            You are CareerPulse, an advanced ATS (Applicant Tracking System) optimization advisor and professional HR recruiter. 
            Analyze the candidate's resume strictly in relation to the target job: "$job".
            
            You must evaluate:
            1. An overall score from 0 to 100 based on keyword density, formatting, and impact metrics.
            2. Critically missing ATS keywords (usually 3 to 6 high-value skills or tool names omitted).
            3. Standard formatting checklists: items that correspond to clean styling (single-page, structured headers, bulleted layout) that passed or failed.
            4. Detailed list of candidate strengths, weaknesses, and clear tactical action items.

            You MUST strictly return your evaluation in structured JSON matching this schema:
            {
              "score": Int (0 to 100),
              "jobTitle": "Target position name",
              "missingKeywords": ["string", "string", ...],
              "formattingPassed": ["string", "string", ...],
              "formattingFailed": ["string", "string", ...],
              "strengths": ["string", "string", ...],
              "weaknesses": ["string", ...],
              "recommendations": ["string", "string", ...]
            }
            Do not enclose in markdown blocks like ```json ... ```, just return the raw JSON object. Use double quotes.
        """.trimIndent()

        val prompt = "Candidate Resume Text:\n$resumeText\n\nTarget Job:\n$job"

        val request = GeminiRequest(
            contents = listOf(ContentJson(parts = listOf(PartJson(text = prompt)))),
            systemInstruction = ContentJson(parts = listOf(PartJson(text = systemPrompt))),
            generationConfig = GenerationConfigJson(
                responseMimeType = "application/json",
                temperature = 0.2
            )
        )

        val response = GeminiNetworkClient.apiService.analyzeResume(apiKey, request)
        val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: return null

        return GeminiNetworkClient.parseAnalysisResult(textResponse)
    }

    // Chat coach interaction using real Gemini REST client
    fun sendChatMessage(messageText: String) {
        if (messageText.isBlank() || _chatLoading.value) return

        val userMsg = ChatMessage("user", messageText)
        _chatHistory.value = _chatHistory.value + userMsg
        _chatLoading.value = true

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val isDemoKey = apiKey.isBlank() || apiKey == "GEMINI_API_KEY" || apiKey == "MY_GEMINI_API_KEY"

                val reply = if (isDemoKey) {
                    withContext(Dispatchers.IO) {
                        simulateCoachResponse(messageText)
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        executeGeminiChat(apiKey, messageText)
                    }
                }

                _chatHistory.value = _chatHistory.value + ChatMessage("model", reply)
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage("model", "I'm having trouble connecting to AI services right now to provide advice. Please check your network or Android Secret configurations.")
            } finally {
                _chatLoading.value = false
            }
        }
    }

    private suspend fun executeGeminiChat(apiKey: String, lastUserMessage: String): String {
        // Compile history briefly
        val contextPrompt = StringBuilder()
        contextPrompt.append("You are CareerPulse Coach - a high-end personal mentor, senior product leader, and engineering executive. ")
        contextPrompt.append("The user has currently selected a resume report for a ${selectedAnalysis.value?.targetJob ?: "professional role"} ")
        contextPrompt.append("which scored ${selectedAnalysis.value?.overallScore ?: 75}/100. Give elite, concise, and dynamic answers to their question.\n\n")

        // Include recent history
        val recentTurns = _chatHistory.value.takeLast(10)
        for (turn in recentTurns) {
            val label = if (turn.sender == "user") "User: " else "Coach: "
            contextPrompt.append(label).append(turn.content).append("\n")
        }
        contextPrompt.append("Coach: ")

        val request = GeminiRequest(
            contents = listOf(ContentJson(parts = listOf(PartJson(text = contextPrompt.toString())))),
            generationConfig = GenerationConfigJson(temperature = 0.7)
        )

        val response = GeminiNetworkClient.apiService.analyzeResume(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "I appreciate the query, but couldn't formulate a response. Can you rephrase that?"
    }

    // High quality mock values if the API key is not fully configured
    private fun simulateScanResult(job: String, text: String): ResumeScanResult {
        // Calculate a pseudo score based on resume text length to make it reactive
        val score = when {
            text.length < 150 -> 45
            text.length < 500 -> 68
            text.contains("Agile", ignoreCase = true) && text.contains("SQL", ignoreCase = true) -> 89
            else -> 74
        }

        val missing = mutableListOf<String>()
        if (!text.contains("Agile", ignoreCase = true)) missing.add("Agile Methodologies")
        if (!text.contains("SQL", ignoreCase = true)) missing.add("SQL Metrics / Analytics")
        if (!text.contains("A/B", ignoreCase = true)) missing.add("A/B Testing Experiments")
        if (missing.isEmpty()) {
            missing.add("KPI tracking")
            missing.add("Executive presentations")
        }

        val passed = listOf(
            "Contact section complete and valid",
            "Linear single-column formatting matches strict parser limits",
            "Job timelines correctly annotated chronologically",
            "Core skills highlighted under precise category tags"
        )

        val failed = if (score < 60) {
            listOf("Missing 4+ critical industry keyword tags", "Formatting contains dense double-column margins", "Lack of clear quantifiers in bullet achievements")
        } else if (score < 80) {
            listOf("3 critical ATS keywords omitted", "Omitted active leadership keywords", "Unstructured executive profile introduction")
        } else {
            listOf("No major formatting issues detected")
        }

        return ResumeScanResult(
            score = score,
            jobTitle = job,
            missingKeywords = missing,
            formattingPassed = passed,
            formattingFailed = failed,
            strengths = listOf(
                "Strong clear employment history detailing concrete deliverables",
                "Machine-readable structural layouts meet premium parser indices",
                "Excellent technical tool tagging inside structural tables"
            ),
            weaknesses = listOf(
                "Lack of direct revenue/customer quantitative impacts in early achievements",
                "High density in description statements slows scanning eye-lines"
            ),
            recommendations = listOf(
                "1. Direct-inject missing keywords: ${missing.joinToString(", ")} into career highlights.",
                "2. Restructure sentence bullets: Use 'Accomplished [X] as measured by [Y], by doing [Z]'.",
                "3. Compress experience bullet items down to 4 statements max per professional role."
            )
        )
    }

    private fun simulateCoachResponse(query: String): String {
        return when {
            query.contains("interview", ignoreCase = true) -> {
                "For a ${selectedAnalysis.value?.targetJob ?: "Product"} role, expect these high-probability interview questions:\n\n" +
                        "1. *'Walk me through a product or project launch that failed. What did you learn?'* (Tip: Focus on post-mortem ownership and structured iterative corrections).\n" +
                        "2. *'How do you handle prioritization conflicts between engineering teams and stakeholders?'* (Tip: Emphasize data-driven metrics, user ROI, and standard roadmap alignment).\n\n" +
                        "Would you like to simulate a mock response to one of these?"
            }
            query.contains("keyword", ignoreCase = true) || query.contains("add", ignoreCase = true) -> {
                "Looking at your report for high impact, you should seek to seed keyword tags directly into your active role descriptions, not just as a static list at the bottom. " +
                        "For example: instead of writing 'Responsible for product analytics', write 'Developed SQL Analytics pipelines to query A/B Testing results and drive user retention.' This satisfies both keyword matches and metric highlights."
            }
            query.contains("cover letter", ignoreCase = true) -> {
                "Absolutely! Here's a concise, high-impact template:\n\n" +
                        "*Dear Hiring Team,*\n\n" +
                        "I am reaching out to express my intense interest in the *${selectedAnalysis.value?.targetJob ?: "open"}* opening. Having recently scanned my credentials against your ATS framework, my profile maps excellently covering critical tracks like *${selectedAnalysis.value?.missingKeywords?.split(",")?.firstOrNull()?.trim() ?: "Agile leadership"}*.\n\n" +
                        "In my previous tenure, I owned core initiatives that boosted user performance parameters significantly. I'm eager to bring this structured, metric-driven execution to your team.\n\n" +
                        "Shall we customize this template with specific details from your work history?"
            }
            else -> {
                "Excellent question. In a competitive candidate landscape, the key to standing out is translating daily responsibilities into quantifiable business results. " +
                        "Do you have a specific point on your resume that feels weak or lacks a metric? Paste it here and we can rewrite it to be elite together!"
            }
        }
    }
}
