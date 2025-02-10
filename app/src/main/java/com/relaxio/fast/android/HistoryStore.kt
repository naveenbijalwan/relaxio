package com.relaxio.fast.android

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HistoryStore(context: Context) {
    private val appContext = context.applicationContext // ✅ Fix: Always use applicationContext

    private val prefs = context.getSharedPreferences("recommendation_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveRecommendation(topics: List<String>, userInput: Map<String, Float>, videoLinks: Map<String, List<String>>,stressScore: Int) {
        val history = getHistory().toMutableList()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val newRecord = RecommendationHistory(timestamp, topics, userInput,videoLinks,stressScore)

        history.add(0, newRecord) // ✅ Add to top of list (most recent first)

        prefs.edit().putString("history", gson.toJson(history)).apply()
    }

    fun getHistory(): List<RecommendationHistory> {
        val json = prefs.getString("history", "[]")
        return gson.fromJson(json, object : TypeToken<List<RecommendationHistory>>() {}.type) ?: listOf()
    }
    fun clearHistory() {
        prefs.edit().clear().apply() // ✅ Clear all saved history
    }
}

data class RecommendationHistory(
    val timestamp: String,
    val topics: List<String>,
    val userInput: Map<String, Float>,
    val videoLinks: Map<String, List<String>>, // ✅ Save recommended video links
    val stressScore: Int

)
