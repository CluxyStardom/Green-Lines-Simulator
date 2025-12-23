package com.example.greenlinessimulator

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import kotlin.random.Random

class GreenLineService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If the service is already running, remove the old overlay before creating a new one.
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }

        val colors = intent?.getStringArrayListExtra("colors")
        val lineCount = intent?.getIntExtra("lineCount", 10) ?: 10
        val orientation = intent?.getStringExtra("orientation") ?: "vertical"
        val randomize = intent?.getBooleanExtra("randomize", false) ?: false

        if (colors != null && colors.isNotEmpty()) {
            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // Use flags that allow drawing under the system bars without hiding them.
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            overlayView = OverlayView(this, colors, lineCount, orientation, randomize)

            // Tell the view to lay out as if the bars are hidden, which makes it extend underneath them.
            @Suppress("DEPRECATION")
            overlayView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

            windowManager.addView(overlayView, params)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            windowManager.removeView(it)
        }
        overlayView = null
    }

    private class OverlayView(
        context: Context,
        private val colors: ArrayList<String>,
        private val lineCount: Int,
        private val orientation: String,
        private val randomize: Boolean
    ) : View(context) {

        private val paint = Paint()
        private val linePositions = mutableListOf<Int>()
        private val lineThicknesses = mutableListOf<Int>()
        private var isInitialized = false

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (!isInitialized && w > 0 && h > 0) {
                initializeLines(w, h)
                isInitialized = true
            }
        }

        private fun initializeLines(width: Int, height: Int) {
            // Pre-calculate random positions and thicknesses using the correct, final dimensions of the view.
            val maxPosition: Int = if (orientation == "vertical") width else height

            for (i in 0 until lineCount) {
                if (randomize) {
                    linePositions.add(Random.nextInt(0, maxPosition))
                }
                lineThicknesses.add(Random.nextInt(6, 13))
            }

            if (randomize) {
                linePositions.sort()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!isInitialized) return // Don't draw until we have calculated positions

            val width = canvas.width
            val height = canvas.height

            if (orientation == "vertical") {
                if (randomize) {
                    linePositions.forEachIndexed { index, x ->
                        drawVerticalLine(canvas, x, height, colors[index % colors.size], lineThicknesses[index])
                    }
                } else {
                    val lineSpacing = width / lineCount
                    for (i in 0 until lineCount) {
                        drawVerticalLine(canvas, i * lineSpacing, height, colors[i % colors.size], lineThicknesses[i])
                    }
                }
            } else { // Horizontal
                if (randomize) {
                    linePositions.forEachIndexed { index, y ->
                        drawHorizontalLine(canvas, y, width, colors[index % colors.size], lineThicknesses[index])
                    }
                } else {
                    val lineSpacing = height / lineCount
                    for (i in 0 until lineCount) {
                        drawHorizontalLine(canvas, i * lineSpacing, width, colors[i % colors.size], lineThicknesses[i])
                    }
                }
            }
        }

        private fun drawVerticalLine(canvas: Canvas, x: Int, height: Int, colorString: String, thickness: Int) {
            paint.color = parseColor(colorString)
            canvas.drawRect(x.toFloat(), 0f, (x + thickness).toFloat(), height.toFloat(), paint)
        }

        private fun drawHorizontalLine(canvas: Canvas, y: Int, width: Int, colorString: String, thickness: Int) {
            paint.color = parseColor(colorString)
            canvas.drawRect(0f, y.toFloat(), width.toFloat(), (y + thickness).toFloat(), paint)
        }

        private fun parseColor(colorString: String): Int {
            return when (colorString) {
                "green" -> Color.GREEN
                "purple" -> Color.MAGENTA
                else -> Color.TRANSPARENT
            }
        }
    }
}