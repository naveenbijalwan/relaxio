package com.relaxio.fast.android

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.relaxio.fast.android.databinding.ActivityWelcomeBinding
import com.relaxio.fast.android.databinding.HistoryCardBinding
import java.text.SimpleDateFormat
import java.util.Locale

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var historyStore: HistoryStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add the animation section
        binding.animationContainer.addView(UltimateDynamicPatternView(this))

        historyStore = HistoryStore(applicationContext)

        // **Ensure VideoDataStore is initialized**
        VideoDataStore.initialize(this)

        // Show the motivational message
        //binding.motivationTextView.text = "Quick Relief, Powerful Boost—Feel the Change in less than 10 Minutes."

        binding.takeTestButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.clearHistoryButton.setOnClickListener {
            clearHistory()
        }

        displayHistory()
    }

    override fun onResume() {
        super.onResume()

        // Clear any old patterns before generating a new one
        binding.animationContainer.removeAllViews()

        // Dynamically add a fresh UltimateDynamicPatternView
        val patternView = UltimateDynamicPatternView(this)
        patternView.generateNewPatterns()  // Ensure new patterns are generated
        binding.animationContainer.addView(patternView)
    }

    private fun displayHistory() {
        val historyList = historyStore.getHistory()
        binding.historyContainer.removeAllViews()

        if (historyList.isEmpty()) {
            binding.clearHistoryButton.visibility = View.GONE
        } else {
            binding.clearHistoryButton.visibility = View.VISIBLE
            historyList.forEach { record ->
                addHistoryCard(record)
            }
        }
    }

    private fun addHistoryCard(record: RecommendationHistory) {
        val cardBinding = HistoryCardBinding.inflate(LayoutInflater.from(this), binding.historyContainer, false)

        // Convert timestamp to relative time
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val recordDate = sdf.parse(record.timestamp)
        val relativeTime = if (recordDate != null) {
            DateUtils.getRelativeTimeSpanString(
                recordDate.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
        } else {
            "Unknown time"
        }
        // Display the saved stress score on the card
        cardBinding.historyTitle.text =  "Mood Booster • $relativeTime"
        cardBinding.historyStressScore.text = "Relaxio Index: ${100 - record.stressScore}"

        // View details button to launch RecommendationHistoryActivity
        cardBinding.viewHistoryButton.setOnClickListener {
            val intent = Intent(this, RecommendationHistoryActivity::class.java)
            intent.putExtra("stressScore", record.stressScore)
            intent.putExtra("savedVideoLinks", Gson().toJson(record.videoLinks))
            startActivity(intent)
        }

        binding.historyContainer.addView(cardBinding.root)
    }

    private fun clearHistory() {
        historyStore.clearHistory()
        displayHistory() // Refresh the UI after clearing
    }
}
