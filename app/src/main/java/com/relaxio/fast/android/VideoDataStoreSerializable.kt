package com.relaxio.fast.android

import java.io.Serializable

/**
 * Outer class VideoDataStoreSerializable matching Java's package and structure.
 */
class VideoDataStoreSerializable : Serializable {

    /**
     * Inner class Quadruple matching the Java class structure exactly.
     */
    data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L  // Same fixed ID as in Java
        }
    }


    /**
     * Other models (if needed)
     */
    data class QuestionData(
        val question: String,
        val options: List<OptionData>
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L  // Same fixed ID as in Java
        }
    }

    data class OptionData(
        val text: String,
        val scores: Map<String, Float>
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L  // Same fixed ID as in Java
        }
    }

}
