package com.example.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

/**
 * Firebase AI Logic (Gemini) repository providing live navigation assistance,
 * traffic/place insights, and conversational Arabic city guidance without API keys.
 */
class CityPulseAiRepository {

    private val systemPrompt = """
        أنت المساعد الذكي المدمج في تطبيق "نبض المدينة" (CityPulse) للخرائط الحية والاتجاهات.
        دورك:
        - مساعدة المستخدمين باللغة العربية الواضحة والودودة والمختصرة.
        - تحليل الأماكن القريبة الظاهرة على الخريطة (مطاعم، صيدليات، مقاهي، محطات صيانة، بلاغات حوادث وازدحام) بناءً على السياق المرفق.
        - تقديم نصائح وإرشادات ذكية أثناء القيادة والتنقل أو عند استكشاف المدن حول العالم.
        - إذا طُلب منك وصف طريق أو نصيحة مرورية، اعتمد على بيانات المسار المرفقة إن وجدت، ولا تخترع أسماء شوارع غير موجودة إذا لم تتوفر.
        - اجعل ردودك عملية ومنظمة ومناسبة للقراءة السريعة أو النطق الصوتي باللهجة العربية الفصحى المبسطة.
    """.trimIndent()

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(systemPrompt)
            }
        )
    }

    fun streamReply(
        userPrompt: String,
        liveContextSummary: String,
        history: List<AiChatMessage>
    ): Flow<String> = flow {
        val recentHistory = history
            .filter { !it.isError && it.text.isNotBlank() }
            .takeLast(6)
            .joinToString("\n") { msg ->
                val roleLabel = if (msg.isUser) "المستخدم" else "المساعد"
                "$roleLabel: ${msg.text.trim()}"
            }

        val fullPrompt = buildString {
            appendLine("=== السياق الحي من الخريطة ===")
            appendLine(liveContextSummary)
            if (recentHistory.isNotBlank()) {
                appendLine()
                appendLine("=== الحوار السابق ===")
                appendLine(recentHistory)
            }
            appendLine()
            appendLine("=== طلب المستخدم الآن ===")
            append(userPrompt.trim())
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MILLIS) {
                model.generateContentStream(fullPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("لم يُرجع الذكاء الاصطناعي استجابة نصية.")
            }
        } catch (e: CancellationException) {
            throw e
        }
    }

    private companion object {
        const val MODEL_NAME = "gemini-3.8-flash"
        const val AI_TIMEOUT_MILLIS = 60_000L
    }
}
