package com.relaxio.fast.android

import android.animation.ValueAnimator
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*
import kotlin.random.Random

class UltimateDynamicPatternView(context: android.content.Context) : View(context) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val neuroPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.parseColor("#3D85C6")
        isAntiAlias = true
    }
    private val backgroundPaint = Paint()
    private var animationProgress = 0f
    private val randomColorList = generateRandomColors()
    private var selectedPatterns = listOf<(Canvas, Float, Float, Float) -> Unit>()

    init {
        startAnimation()
        generateNewPatterns()
    }

    override fun onDraw(canvas: Canvas) {
        drawGradientBackground(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = (width / 2).toFloat()

        // Draw selected patterns in sequence
        for (pattern in selectedPatterns) {
            pattern.invoke(canvas, centerX, centerY, maxRadius)
        }
        invalidate()  // Continuously animate
    }

    fun generateNewPatterns() {
        selectedPatterns = generateRandomPatterns()  // Generate new random patterns each session
    }

    private fun drawGradientBackground(canvas: Canvas) {
        val gradient = RadialGradient(
            width / 2f, height / 2f, width / 1.5f,
            intArrayOf(Color.parseColor("#FFDEE9"), Color.parseColor("#B5FFFC")),
            null, Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
    }

    private fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat())
        animator.duration = 10000  // Slow evolution
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener {
            animationProgress = it.animatedValue as Float
            invalidate()  // Trigger redraw
        }
        animator.start()
    }

    private fun generateRandomColors(): List<Int> {
        val colorPool = listOf(
            Color.parseColor("#89CFF0"),  // Light Blue
            Color.parseColor("#98FB98"),  // Pale Green
            Color.parseColor("#FFB6C1"),  // Light Pink
            Color.parseColor("#FFD700"),  // Yellow
            Color.parseColor("#ADD8E6"),  // Pale Blue
            Color.parseColor("#FFA07A"),  // Light Coral
            Color.parseColor("#FFDAC1"),  // Light Peach
            Color.parseColor("#E6E6FA")   // Lavender
        )
        return List(15) { colorPool.random() }
    }

    private fun generateRandomPatterns(): List<(Canvas, Float, Float, Float) -> Unit> {
        val patterns = listOf(
            { canvas: Canvas, centerX: Float, centerY: Float, maxRadius: Float -> drawSpirals(canvas, centerX, centerY, maxRadius) },
            { canvas: Canvas, centerX: Float, centerY: Float, _: Float -> drawNeurographicArt(canvas) },
            { canvas: Canvas, centerX: Float, centerY: Float, _: Float -> drawFractalTree(canvas, centerX, centerY, 6) },
            { canvas: Canvas, centerX: Float, centerY: Float, _: Float -> drawMandalaPattern(canvas, centerX, centerY) },
            { canvas: Canvas, _: Float, _: Float, _: Float -> drawGridCircles(canvas) },
            { canvas: Canvas, centerX: Float, centerY: Float, _: Float -> drawLissajousCurves(canvas, centerX, centerY) },
            { canvas: Canvas, centerX: Float, centerY: Float, _: Float -> drawGoldenRatioSpiral(canvas, centerX, centerY) },
            { canvas: Canvas, _: Float, _: Float, _: Float -> drawVoronoiDiagram(canvas) },
            { canvas: Canvas, _: Float, _: Float, _: Float -> drawPerlinNoiseFlow(canvas) },
            { canvas: Canvas, _: Float, _: Float, _: Float -> drawHillsAndMountains(canvas) },
            { canvas: Canvas, _: Float, _: Float, _: Float -> drawRiver(canvas) },
            { canvas: Canvas, _: Float, _: Float, _: Float -> drawOceanWaves(canvas) },
            { canvas: Canvas, _: Float, _: Float, _: Float -> drawSnowfall(canvas) }
        )
        return patterns.shuffled().take(Random.nextInt(3, 7))
    }

    private fun drawSpirals(canvas: Canvas, centerX: Float, centerY: Float, maxRadius: Float) {
        val numPoints = 300
        val angleStep = (2 * Math.PI / numPoints).toFloat()

        for (i in 0 until 10) {
            paint.color = randomColorList[i]
            for (j in 0 until numPoints) {
                val angle = j * angleStep + animationProgress
                val radius = maxRadius * (j / numPoints.toFloat()) * (0.5f + 0.5f * sin(animationProgress + i))
                val x = (centerX + radius * cos(angle)).toFloat()
                val y = (centerY + radius * sin(angle)).toFloat()
                canvas.drawPoint(x, y, paint)
            }
        }
    }

    private fun drawNeurographicArt(canvas: Canvas) {
        val numPaths = 10  // Reduced number of paths for a cleaner look
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = min(width, height) / 2f

        for (i in 0 until numPaths) {
            // Start and end points are symmetrically placed around the center
            val angle = i * (2 * PI / numPaths).toFloat() + animationProgress
            val startX = centerX + maxRadius * cos(angle)
            val startY = centerY + maxRadius * sin(angle)
            val endX = centerX + maxRadius * cos(angle + PI.toFloat())
            val endY = centerY + maxRadius * sin(angle + PI.toFloat())

            // Control points for smoother curves
            val controlX1 = centerX + maxRadius * 0.5f * cos(angle + PI.toFloat() / 4)
            val controlY1 = centerY + maxRadius * 0.5f * sin(angle + PI.toFloat() / 4)
            val controlX2 = centerX + maxRadius * 0.5f * cos(angle - PI.toFloat() / 4)
            val controlY2 = centerY + maxRadius * 0.5f * sin(angle - PI.toFloat() / 4)

            // Create a smooth curve using cubic Bézier
            val path = Path().apply {
                moveTo(startX, startY)
                cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
            }

            // Use a gradient color for the stroke
            val gradient = LinearGradient(
                startX, startY, endX, endY,
                randomColorList[i % randomColorList.size],
                randomColorList[(i + 1) % randomColorList.size],
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawPath(path, paint)
        }
    }

    private fun drawFractalTree(canvas: Canvas, x: Float, y: Float, depth: Int) {
        if (depth == 0) return
        paint.color = randomColorList[depth % randomColorList.size]
        val branchLength = 100f * depth / 6
        val angleVariation = 30f + 5 * sin(animationProgress)

        val leftX = x - branchLength * cos(angleVariation)
        val leftY = y - branchLength * sin(angleVariation)
        val rightX = x + branchLength * cos(angleVariation)
        val rightY = y + branchLength * sin(angleVariation)

        canvas.drawLine(x, y, leftX.toFloat(), leftY.toFloat(), paint)
        canvas.drawLine(x, y, rightX.toFloat(), rightY.toFloat(), paint)

        drawFractalTree(canvas, leftX, leftY, depth - 1)
        drawFractalTree(canvas, rightX, rightY, depth - 1)
    }

    private fun drawGridCircles(canvas: Canvas) {
        val gridSize = Random.nextInt(3, 8)
        val spacing = (width / gridSize).toFloat()

        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val x = spacing / 2 + i * spacing
                val y = spacing / 2 + j * spacing
                val rippleRadius = 10f + 15 * sin(animationProgress + sqrt((i * j).toDouble()).toFloat())
                paint.color = randomColorList[(i + j) % randomColorList.size]
                canvas.drawCircle(x, y, rippleRadius, paint)
            }
        }
    }

    private fun drawMandalaPattern(canvas: Canvas, centerX: Float, centerY: Float) {
        val numLayers = Random.nextInt(5, 10)
        for (layer in 0 until numLayers) {
            paint.color = randomColorList[layer % randomColorList.size]
            val numPetals = 8 + layer * 3
            val radius = 50f + layer * 30
            val angleStep = (2 * Math.PI / numPetals).toFloat()

            for (j in 0 until numPetals) {
                val angle = j * angleStep + animationProgress
                val petalX = (centerX + radius * cos(angle)).toFloat()
                val petalY = (centerY + radius * sin(angle)).toFloat()
                canvas.drawCircle(petalX, petalY, 10f + 5 * sin(animationProgress), paint)
            }
        }
    }

    private fun drawLissajousCurves(canvas: Canvas, centerX: Float, centerY: Float) {
        val amplitudeX = width / 4f
        val amplitudeY = height / 4f
        val frequencyX = 3f
        val frequencyY = 2f
        val phase = animationProgress

        val path = Path()
        for (i in 0..360) {
            val angle = Math.toRadians(i.toDouble())
            val x = centerX + amplitudeX * sin(frequencyX * angle + phase).toFloat()
            val y = centerY + amplitudeY * sin(frequencyY * angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        paint.color = randomColorList.random()
        canvas.drawPath(path, paint)
    }

    private fun drawGoldenRatioSpiral(canvas: Canvas, centerX: Float, centerY: Float) {
        val goldenRatio = 1.618f
        var radius = 10f
        var angle = 0f

        val path = Path()
        path.moveTo(centerX, centerY)

        for (i in 0..150) {
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            path.lineTo(x, y)
            radius *= goldenRatio
            angle += Math.toRadians(15.0).toFloat()
        }
        paint.color = randomColorList.random()
        canvas.drawPath(path, paint)
    }

    private fun drawVoronoiDiagram(canvas: Canvas) {
        val numPoints = 20
        val points = List(numPoints) { PointF(Random.nextFloat() * width, Random.nextFloat() * height) }

        for (x in 0 until width step 10) {
            for (y in 0 until height step 10) {
                var closestPoint = points[0]
                var minDistance = Float.MAX_VALUE

                for (point in points) {
                    val distance = hypot((x - point.x), (y - point.y))
                    if (distance < minDistance) {
                        minDistance = distance
                        closestPoint = point
                    }
                }
                paint.color = randomColorList[(closestPoint.x + closestPoint.y).toInt() % randomColorList.size]
                canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
        }
    }

    private fun drawPerlinNoiseFlow(canvas: Canvas) {
        val gridSize = 20
        val spacing = width / gridSize

        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val x = i * spacing
                val y = j * spacing
                val angle = PerlinNoise.noise(x.toFloat(), y.toFloat(), animationProgress) * 2 * PI.toFloat()
                val dx = cos(angle) * 10f
                val dy = sin(angle) * 10f
                paint.color = randomColorList[(i + j) % randomColorList.size]
                canvas.drawLine(x.toFloat(), y.toFloat(), x + dx, y + dy, paint)
            }
        }
    }

    private fun drawHillsAndMountains(canvas: Canvas) {
        val numHills = 5
        val hillColors = listOf(
            Color.parseColor("#4CAF50"),  // Green
            Color.parseColor("#8BC34A"),  // Light Green
            Color.parseColor("#CDDC39")   // Lime
        )

        for (i in 0 until numHills) {
            val path = Path()
            path.moveTo(0f, height.toFloat())
            val peakX = Random.nextFloat() * width
            val peakY = height - Random.nextFloat() * (height / 2)
            path.quadTo(peakX, peakY, width.toFloat(), height.toFloat())
            fillPaint.color = hillColors[i % hillColors.size]
            canvas.drawPath(path, fillPaint)
        }
    }

    private fun drawRiver(canvas: Canvas) {
        val riverColor = Color.parseColor("#3F51B5")  // Blue
        fillPaint.color = riverColor

        val path = Path()
        path.moveTo(0f, height * 0.7f)
        path.quadTo(width * 0.3f, height * 0.6f + 50 * sin(animationProgress), width * 0.7f, height * 0.7f)
        path.quadTo(width * 1.2f, height * 0.8f, width.toFloat(), height * 0.7f)
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
        canvas.drawPath(path, fillPaint)
    }

    private fun drawOceanWaves(canvas: Canvas) {
        val waveColor = Color.parseColor("#03A9F4")  // Light Blue
        paint.color = waveColor

        val waveHeight = 20f
        val waveLength = width / 4f

        for (i in 0..width step 50) {
            val startY = height * 0.8f + waveHeight * sin(animationProgress + i / waveLength)
            val endY = height * 0.8f + waveHeight * sin(animationProgress + (i + 50) / waveLength)
            canvas.drawLine(i.toFloat(), startY, (i + 50).toFloat(), endY, paint)
        }
    }

    private fun drawSnowfall(canvas: Canvas) {
        val snowColor = Color.parseColor("#FFFFFF")  // White
        paint.color = snowColor

        val numFlakes = 100
        for (i in 0 until numFlakes) {
            val x = Random.nextFloat() * width
            val y = (Random.nextFloat() * height + animationProgress * 100) % height
            canvas.drawCircle(x, y, 5f, paint)
        }
    }
}

// Placeholder for Perlin Noise implementation
object PerlinNoise {
    fun noise(x: Float, y: Float, z: Float): Float {
        return Random.nextFloat()
    }
}