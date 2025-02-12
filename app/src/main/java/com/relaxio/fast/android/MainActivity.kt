package com.relaxio.fast.android

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var answerOptions: RadioGroup
    private lateinit var continueButton: Button
    private lateinit var nextButton: Button
    private lateinit var skipButton: Button
    private lateinit var youtubeWebView: WebView
    private lateinit var confettiContainer: FrameLayout
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var questionSection: LinearLayout
    private lateinit var answerSection: ScrollView
    private lateinit var UTUURL: String

    private var questionsList: List<VideoDataStoreSerializable.QuestionData> = listOf()
    private val userResponses = mutableMapOf<String, MutableMap<String, Float>>()
    private val coveredParameters = mutableSetOf<String>()
    private val askedQuestions = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener { v -> onBackPressed() }
        backButton.visibility = View.VISIBLE

        // Initialize views
        questionTextView = findViewById(R.id.questionTextView)
        answerOptions = findViewById(R.id.answerOptions)
        continueButton = findViewById(R.id.continueButton)
        skipButton = findViewById(R.id.skipButton)
        nextButton = findViewById(R.id.nextButton)
        youtubeWebView = findViewById(R.id.youtubeWebViewMain)
        confettiContainer = findViewById(R.id.confettiContainer)
        questionSection = findViewById(R.id.questionSection)
        answerSection = findViewById(R.id.answerSection)
        UTUURL = getString(R.string.utubeurl)

        // Initialize background music
        /*mediaPlayer = MediaPlayer.create(this, R.raw.gentle_music)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()*/

        // Set up listeners
        skipButton.setOnClickListener { skipCurrentQuestion() }
        nextButton.setOnClickListener { showNextBestQuestion() }

        // Load questions and display the first one
        questionsList = VideoDataStore.getQuestions().shuffled()
        if (questionsList.isEmpty()) {
            showErrorMessage("No questions found.")
        } else {
            showNextBestQuestion()
        }
    }

    private fun showNextBestQuestion() {
        clearPreviousAnimations()

        val nextQuestion = getNextBestQuestion() ?: run {
            checkCompletion()
            return
        }

        questionSection.visibility = View.VISIBLE
        answerSection.visibility = View.VISIBLE
        askedQuestions.add(nextQuestion.question)
        questionTextView.text = nextQuestion.question
        questionTextView.textSize = 28f

        skipButton.visibility = View.VISIBLE
        nextButton.visibility = View.GONE
        continueButton.visibility = View.GONE
        youtubeWebView.visibility = View.GONE  // Hide the WebView initially
        resetAnswerOptions()

        // Add answer options
        nextQuestion.options.forEach { option ->
            val radioButton = RadioButton(this).apply {
                text = option.text
                textSize = 24f
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    storeUserResponse(option.scores)
                    triggerConfettiEffect()  // Trigger confetti effect
                    playRandomVideoBeforeNextQuestion(option.scores)  // Play video before proceeding
                }
            }
            answerOptions.addView(radioButton)
        }
    }

    private fun playRandomVideoBeforeNextQuestion(scores: Map<String, Float>) {
        val closestVideoUrl = findClosestVideo(scores)
        val randomVideoUrl = closestVideoUrl //VideoDataStore.videoData.random().first

        skipButton.visibility = View.GONE
        questionSection.visibility = View.GONE
        answerSection.visibility = View.GONE

        youtubeWebView.settings.javaScriptEnabled = true
        youtubeWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                skipButton.visibility = View.GONE  // Hide skip button when video is playing
                nextButton.visibility = View.VISIBLE  // Show continue button when video ends
            }
        }

        nextButton.visibility = View.VISIBLE
        youtubeWebView.visibility = View.VISIBLE
        val videoId = randomVideoUrl.substringAfterLast("/")
        val videoUrl = "$UTUURL$videoId"
        youtubeWebView.loadUrl(videoUrl)
    }

    private fun findClosestVideo(userParams: Map<String, Float>): String {
        val videoData = VideoDataStore.videoData

        var minDistance = Double.MAX_VALUE
        var closestVideoUrl = ""

        for (video in videoData) {
            var distance = 0.0

            for ((param, userValue) in userParams) {
                    val videoValue = video.fourth[param] ?: 0.0
                    distance += (userValue.toFloat() - videoValue.toFloat()) * (userValue.toFloat() - videoValue.toFloat())
            }

            if (distance < minDistance) {
                minDistance = distance
                closestVideoUrl = video.first
            }
        }

        return closestVideoUrl
    }

    private fun triggerConfettiEffect() {
        for (i in 0..5) {
            val confetti = TextView(this).apply {
                text = "🎉"
                textSize = Random.nextInt(20, 36).toFloat()
                setTextColor(Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)))
                x = Random.nextInt(0, confettiContainer.width).toFloat()
                y = 0f
            }
            confettiContainer.addView(confetti)

            val animator = ObjectAnimator.ofFloat(confetti, "translationY", 0f, confettiContainer.height.toFloat())
            animator.duration = Random.nextLong(1000, 3000)
            animator.addUpdateListener {
                if (it.animatedFraction == 1f) confettiContainer.removeView(confetti)
            }
            animator.start()
        }
    }

    private fun resetAnswerOptions() {
        answerOptions.removeAllViews()
        answerOptions.clearAnimation()
    }

    private fun skipCurrentQuestion() {
        showNextBestQuestion()
    }

    private fun clearPreviousAnimations() {
        questionTextView.clearAnimation()
        youtubeWebView.clearAnimation()
        confettiContainer.removeAllViews()
    }

    private fun getNextBestQuestion(): VideoDataStoreSerializable.QuestionData? {
        val parameterToQuestions = mutableMapOf<String, MutableList<VideoDataStoreSerializable.QuestionData>>()

        for (question in questionsList) {
            if (askedQuestions.contains(question.question)) continue // Skip already asked questions

            for (option in question.options) {
                for (parameter in option.scores.keys) {
                    parameterToQuestions.getOrPut(parameter) { mutableListOf() }.add(question)
                }
            }
        }

        val missingParameters = requiredParameters().filter { it !in coveredParameters }
        if (missingParameters.isEmpty()) return null // All parameters covered

        val nextParameter = missingParameters.random() // Pick a random missing parameter
        val possibleQuestions = parameterToQuestions[nextParameter] ?: return null

        return possibleQuestions.random() // Pick a random question from available
    }


    private fun storeUserResponse(optionScores: Map<String, Float>) {
        for ((category, value) in optionScores) {
            coveredParameters.add(category)

            if (!userResponses.containsKey(category)) {
                userResponses[category] = mutableMapOf("sum" to 0f, "count" to 0f)
            }
            userResponses[category]!!["sum"] = userResponses[category]!!["sum"]!! + value
            userResponses[category]!!["count"] = userResponses[category]!!["count"]!! + 1f
        }
    }
    private fun checkCompletion() {
        val finalScores = userResponses.mapValues { (_, values) ->
            (values["sum"] ?: 0f) / (values["count"] ?: 1f)
        }

        if (finalScores.keys.containsAll(requiredParameters())) {
            continueButton.visibility = View.VISIBLE
            skipButton.visibility = View.GONE  // Hide Skip Button when showing Continue Button
            continueButton.setOnClickListener {
                val stressScore = calculateStressScore(finalScores)
                val intent = Intent(this, RecommendationActivity::class.java)
                intent.putExtra("userInput", HashMap(finalScores))
                intent.putExtra("stressScore", stressScore)
                startActivity(intent)
            }
        }
    }

    private fun calculateStressScore(finalScores: Map<String, Float>): Int {
        val negativeFactors = mapOf(
            "Stress Relief" to 2f,
            "Relaxation" to 2f,
            "Anxiety Management" to 3f,
            "Sleep Aid" to 1f
        )

        val positiveFactors = mapOf(
            "Motivation" to 1f,
            "Focus Improvement" to 1f,
            "Mindfulness" to 1f,
            "Positive Thinking" to 1f,
            "Breathing Exercises" to 1f,
            "Energizing" to 1f
        )

        var negativeSum = 0f
        var positiveSum = 0f

        negativeFactors.forEach { (category, weight) ->
            negativeSum += (finalScores[category] ?: 0f) * weight
        }

        positiveFactors.forEach { (category, weight) ->
            positiveSum += (finalScores[category] ?: 0f) * weight
        }

        val maxNegativeScore = negativeFactors.values.sum() * 5
        val maxPositiveScore = positiveFactors.values.sum() * 5

        return (50 + ((negativeSum / maxNegativeScore) * 50) - ((positiveSum / maxPositiveScore) * 50))
            .toInt().coerceIn(0, 100)
    }

    private fun requiredParameters() = listOf(
        "Stress Relief", "Relaxation", "Anxiety Management", "Sleep Aid",
        "Motivation", "Focus Improvement", "Mindfulness", "Positive Thinking",
        "Breathing Exercises", "Energizing"
    )

    private fun showErrorMessage(message: String) {
        questionTextView.text = message
    }

    override fun onDestroy() {
        super.onDestroy()
        //mediaPlayer.release()  // Release media player when activity is destroyed
    }
}