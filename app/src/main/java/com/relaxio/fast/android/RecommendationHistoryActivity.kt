package com.relaxio.fast.android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.relaxio.fast.android.databinding.ActivityRecommendationBinding
import com.relaxio.fast.android.databinding.VideoCardBinding

class RecommendationHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecommendationBinding
    private var savedVideoLinks: Map<String, List<String>> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecommendationBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Retrieve video links from intent
        val savedVideoLinksJson = intent.getStringExtra("savedVideoLinks")
        savedVideoLinks = Gson().fromJson(savedVideoLinksJson, object : TypeToken<Map<String, List<String>>>() {}.type)


        val stressScore = intent.getIntExtra("stressScore", 0)
        val savedVideoLinks: Map<String, List<String>> = Gson().fromJson(savedVideoLinksJson, object : TypeToken<Map<String, List<String>>>() {}.type)
        val relaxationLevel = 100 - stressScore

        // Update Relaxio Index view
        binding.stressRingView.setProgress(relaxationLevel)
        binding.stressScoreText.text = "$relaxationLevel\n${getRelaxationMessage(stressScore)}"

        // Populate video cards from saved video links
        savedVideoLinks.forEach { (topic, videoLinks) ->
            addCard(topic, videoLinks)
        }

        binding.backButton.setOnClickListener {
            finish() // Simply go back to WelcomeActivity without saving history
        }
    }

    // ✅ Override Android system back press to go to WelcomeActivity
    override fun onBackPressed() {
        finish() // Simply go back to WelcomeActivity without saving history
    }

    private fun getRelaxationMessage(score: Int): String {
        return when {
            score >= 70 -> "Excellent"
            score in 40..69 -> "Good"
            else -> "Needs Improvement"
        }
    }

    private fun addCard(topic: String, videoLinks: List<String>) {
        val cardBinding = VideoCardBinding.inflate(LayoutInflater.from(this), binding.recommendationsContainer, false)

        val rTopic =  topic.replace("([a-z])([A-Z])".toRegex(), "$1 $2")

        // Set the topic as the title of the recommendation card
        cardBinding.videoTitle.text = rTopic

        if (videoLinks.isNotEmpty()) {
            var currentVideoIndex = 0
            cardBinding.loadButton.visibility = View.VISIBLE
            cardBinding.loadButton.setOnClickListener {
                loadVideo(cardBinding, videoLinks[currentVideoIndex])
                cardBinding.youtubeWebView.visibility = View.VISIBLE
                cardBinding.nextButton.visibility = if (videoLinks.size > 1) View.VISIBLE else View.GONE
            }

            cardBinding.nextButton.setOnClickListener {
                if (currentVideoIndex < videoLinks.size - 1) {
                    currentVideoIndex++
                    loadVideo(cardBinding, videoLinks[currentVideoIndex])
                    if (currentVideoIndex == videoLinks.size - 1) {
                        cardBinding.nextButton.visibility = View.GONE
                    }
                }
            }
        } else {
            cardBinding.videoTitle.text = "$topic\nNo videos available."
        }

        binding.recommendationsContainer.addView(cardBinding.root)
    }

    private fun loadVideo(cardBinding: VideoCardBinding, videoLink: String) {
        val videoId = extractVideoId(videoLink)
        val videoUrl = "https://www.youtube.com/embed/$videoId"
        cardBinding.youtubeWebView.settings.javaScriptEnabled = true
        cardBinding.youtubeWebView.loadUrl(videoUrl)
    }

    private fun extractVideoId(link: String): String {
        return link.substringAfterLast("/")
    }
}
