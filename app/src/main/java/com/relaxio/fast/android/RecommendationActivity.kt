package com.relaxio.fast.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.relaxio.fast.android.databinding.ActivityRecommendationBinding
import com.relaxio.fast.android.databinding.VideoCardBinding
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class RecommendationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecommendationBinding
    private lateinit var similarityModel: TFLiteSimilarityModel
    private lateinit var annoyIndex: AnnoyIndex
    private var bestVideosByTopic = mutableMapOf<String, List<String>>()
    private val recommendedTopics = mutableListOf<String>()
    private lateinit var historyStore: HistoryStore
    private var stressScore: Int = 0
    private lateinit var UTUURL: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UTUURL = getString(R.string.utubeurl)

        // Set up View Binding
        binding = ActivityRecommendationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        historyStore = HistoryStore(applicationContext)

        // Load TensorFlow Lite model and Annoy index
        similarityModel = TFLiteSimilarityModel(this, "similarity_model.tflite")
        annoyIndex = AnnoyIndex(this, "annoy_index.ann")

        // Retrieve user input and stress score from intent
        val userInput = intent.getSerializableExtra("userInput") as? Map<String, Float> ?: emptyMap()
        stressScore = intent.getIntExtra("stressScore", 0)
        val relaxationLevel = 100 - stressScore

        // Update Relaxio Index view
        binding.stressRingView.setProgress(relaxationLevel)
        binding.stressScoreText.text = "$relaxationLevel\n${getRelaxationMessage(relaxationLevel)}"

        // Get topics and add recommendation cards
        val tags = VideoDataStore.getTopics()
        tags.keys.forEach { topic ->
            addCard(topic, tags, userInput)
            recommendedTopics.add(topic)
        }

        // Back button logic to save history and navigate to WelcomeActivity
        binding.backButton.setOnClickListener {
            saveHistory()
            navigateToWelcomeScreen()
        }
    }

    private fun getRelaxationMessage(score: Int): String {
        return when {
            score >= 70 -> "Excellent"
            score in 40..69 -> "Good"
            else -> "Needs Improvement"
        }
    }

    override fun onBackPressed() {
        saveHistory()
        navigateToWelcomeScreen()
    }

    private fun navigateToWelcomeScreen() {
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun saveHistory() {
        historyStore.saveRecommendation(recommendedTopics, emptyMap(), bestVideosByTopic, stressScore)
    }

    private fun addCard(
        topic: String,
        tags: Map<String, List<String>>,
        userInput: Map<String, Float>
    ) {
        val cardBinding = VideoCardBinding.inflate(LayoutInflater.from(this), binding.recommendationsContainer, false)

        val rTopic =  topic.replace("([a-z])([A-Z])".toRegex(), "$1 $2")
        // Set the topic as the title of the recommendation card
        cardBinding.videoTitle.text = rTopic

        updateCardUI(cardBinding, rTopic, "", isError = false)

        val bestVideos = tags[topic] ?: listOf()
        if (bestVideos.isNotEmpty()) {
            cardBinding.loadButton.visibility = View.VISIBLE
            cardBinding.loadButton.setOnClickListener {
                val queryVector = similarityModel.computeEmbedding(userInput.values.toFloatArray())

                queryVector?.let { vector ->
                    val bestVideos = annoyIndex.search(vector, 50, topic)

                    if (bestVideos.isNotEmpty()) {
                        bestVideosByTopic[topic] = bestVideos
                        loadVideo(cardBinding, bestVideos.first())
                        cardBinding.youtubeWebView.visibility = View.VISIBLE
                        cardBinding.nextButton.visibility = if (bestVideos.size > 1) View.VISIBLE else View.GONE
                        cardBinding.videoTitle.text = rTopic
                    } else {
                        updateCardUI(cardBinding, topic, "No videos available. Try again later.", isError = true)
                    }
                } ?: run {
                    updateCardUI(cardBinding, topic, "Error computing recommendation.", isError = true)
                }
            }
        }

        // Handle "Next" button
        cardBinding.nextButton.setOnClickListener {
            val videos = bestVideosByTopic[topic]
            if (!videos.isNullOrEmpty()) {
                var currentIndex = cardBinding.youtubeWebView.tag as? Int ?: 0
                if (currentIndex < videos.size - 1) {
                    currentIndex++
                    loadVideo(cardBinding, videos[currentIndex])
                    cardBinding.youtubeWebView.tag = currentIndex
                    cardBinding.nextButton.visibility =
                        if (currentIndex < videos.size - 1) View.VISIBLE else View.GONE
                }
            }
        }

        binding.recommendationsContainer.addView(cardBinding.root)
    }

    private fun updateCardUI(cardBinding: VideoCardBinding, title: String, message: String, isError: Boolean) {
        cardBinding.videoTitle.text = title
        cardBinding.videoMessage.text = message
        cardBinding.videoMessage.setTextColor(
            ContextCompat.getColor(
                this, if (isError) android.R.color.holo_red_dark else android.R.color.darker_gray
            )
        )
    }

    private fun loadVideo(cardBinding: VideoCardBinding, videoLink: String) {
        val videoId = extractVideoId(videoLink)
        val videoUrl = "$UTUURL$videoId"
        cardBinding.youtubeWebView.settings.javaScriptEnabled = true
        cardBinding.youtubeWebView.loadUrl(videoUrl)
    }

    private fun extractVideoId(link: String): String {
        return link.substringAfterLast("/")
    }
}
// Annoy Index Wrapper for Similarity Search with Topic Filtering
class AnnoyIndex(context: Context, indexFileName: String) {
    private val index: ByteBuffer
    private val videoData: List<VideoDataStoreSerializable.Quadruple<String, String, String, Map<String, Float>>> // Video data from VideoDataStore

    init {
        // Load Annoy index from assets
        val assetInputStream = context.assets.open(indexFileName)
        index = ByteBuffer.wrap(assetInputStream.readBytes())
        assetInputStream.close()


        // Load video data from VideoDataStore
        videoData = VideoDataStore.videoData
    }

    fun search(queryVector: FloatArray, k: Int, topic: String): List<String> {

        val topicVideos = videoData.filter { it.third == topic }


        val results = mutableListOf<Pair<Int, Float>>()
        for (i in topicVideos.indices) {
            val vector = getVectorFromIndex(i)
            val distance = euclideanDistance(queryVector, vector)
            results.add(i to distance)
        }

        return results.sortedBy { it.second }
            .take(k)
            .map { topicVideos[it.first].first }
    }

    private fun getVectorFromIndex(index: Int): FloatArray {

        val dimension = 10
        val offset = index * dimension * Float.SIZE_BYTES
        val vector = FloatArray(dimension)
        for (i in 0 until dimension) {
            vector[i] = this.index.getFloat(offset + i * Float.SIZE_BYTES)
        }
        return vector
    }

    private fun euclideanDistance(vector1: FloatArray, vector2: FloatArray): Float {
        return sqrt(vector1.zip(vector2) { v1, v2 -> (v1 - v2) * (v1 - v2) }.sum())
    }
}

class TFLiteSimilarityModel(context: Context, modelPath: String) {

    private var interpreter: Interpreter? = null

    init {
        try {
            val assetFileDescriptor = context.assets.openFd(modelPath)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            interpreter = Interpreter(mappedByteBuffer)
            Log.d("TFLite", "Model loaded successfully")
        } catch (e: Exception) {
            Log.e("TFLite", "Error loading model: ${e.localizedMessage}")
            interpreter = null
        }
    }

    fun computeEmbedding(inputVector: FloatArray): FloatArray? {
        if (interpreter == null) {
            Log.e("TFLite", "Interpreter is not initialized.")
            return null
        }

        return try {
            val inputShape = interpreter!!.getInputTensor(0).shape() // Validate shape
            val outputShape = interpreter!!.getOutputTensor(0).shape()

            Log.d("TFLite", "Input Shape: ${inputShape.contentToString()}")
            Log.d("TFLite", "Output Shape: ${outputShape.contentToString()}")

            // ✅ Trim or pad input vector to 10 floats
            val trimmedOrPaddedVector = if (inputVector.size > 10) {
                inputVector.copyOf(10) // Trim if larger
            } else {
                inputVector.copyOf(10) // Pad with zeros if smaller
            }

            // ✅ Create TensorBuffer of size [1, 10]
            val inputBuffer = TensorBuffer.createFixedSize(
                intArrayOf(1, 10),
                org.tensorflow.lite.DataType.FLOAT32
            )
            inputBuffer.loadArray(trimmedOrPaddedVector)

            val outputBuffer = TensorBuffer.createFixedSize(
                outputShape,
                org.tensorflow.lite.DataType.FLOAT32
            )

            interpreter!!.run(inputBuffer.buffer, outputBuffer.buffer.rewind())
            Log.d("TFLite", "Inference successful")
            outputBuffer.floatArray
        } catch (e: Exception) {
            Log.e("TFLite", "Error during inference: ${e.localizedMessage}")
            null
        }
    }

    fun close() {
        interpreter?.close()
        Log.d("TFLite", "Interpreter closed")
    }
}