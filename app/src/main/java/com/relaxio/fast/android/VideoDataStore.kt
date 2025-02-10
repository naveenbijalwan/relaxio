package com.relaxio.fast.android

import android.content.Context
import java.io.ObjectInputStream
import java.io.ObjectStreamClass

object VideoDataStore {
    var videoData: List<VideoDataStoreSerializable.Quadruple<String, String, String, Map<String, Float>>> = emptyList()
    private var questions: List<VideoDataStoreSerializable.QuestionData> = emptyList()
    var cachedTopics: Map<String, List<String>> = emptyMap()

    fun initialize(context: Context) {
        if (videoData.isEmpty()) {
            videoData = loadBinaryData(context, "videoData.bin") ?: emptyList()
        }
        if (cachedTopics.isEmpty()) {
            cachedTopics = loadBinaryData(context, "tagsData.bin") ?: emptyMap()
        }
        if (questions.isEmpty()) {
            questions = loadBinaryData(context, "questionsData.bin") ?: emptyList()
        }
    }

    private fun <T> loadBinaryData(context: Context, fileName: String): T? {
        return try {
            context.assets.open(fileName).use { inputStream ->
                val objectInputStream = object : ObjectInputStream(inputStream) {
                    override fun resolveClass(desc: ObjectStreamClass): Class<*> {
                        return when (desc.name) {
                            "com.relaxio.android.VideoDataStoreSerializable\$Quadruple" -> VideoDataStoreSerializable.Quadruple::class.java
                            "com.relaxio.android.VideoDataStoreSerializable\$QuestionData" -> VideoDataStoreSerializable.QuestionData::class.java
                            "com.relaxio.android.VideoDataStoreSerializable\$OptionData" -> VideoDataStoreSerializable.OptionData::class.java
                            else -> super.resolveClass(desc)
                        }
                    }
                }
                objectInputStream.readObject() as T
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun getTopics(): Map<String, List<String>> = cachedTopics
    fun getQuestions(): List<VideoDataStoreSerializable.QuestionData> = questions
}