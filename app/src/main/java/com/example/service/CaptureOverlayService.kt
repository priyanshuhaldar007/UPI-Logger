package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.UpiNoteLoggerApplication
import com.example.ocr.OcrProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class CaptureOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var overlayLayout: View? = null
    private var flashOverlay: View? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var screenWidth = 1080
    private var screenHeight = 1920
    private var screenDensity = 420

    private var isCapturing = false

    companion object {
        private const val TAG = "CaptureOverlayService"
        const val CHANNEL_ID = "upi_logger_capture_channel"
        const val NOTIFICATION_ID = 9101

        const val ACTION_START = "com.example.action.START_CAPTURE"
        const val ACTION_STOP = "com.example.action.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        // Cropping percentage constants for OCR optimization (tunable)
        const val SCREEN_A_HEADER_X_PERCENT = 0.20f
        const val SCREEN_A_HEADER_Y_PERCENT = 0.04f
        const val SCREEN_A_HEADER_WIDTH_PERCENT = 0.55f
        const val SCREEN_A_HEADER_HEIGHT_PERCENT = 0.10f

        const val SCREEN_A_AMOUNT_X_PERCENT = 0.18f
        const val SCREEN_A_AMOUNT_Y_PERCENT = 0.16f
        const val SCREEN_A_AMOUNT_WIDTH_PERCENT = 0.30f
        const val SCREEN_A_AMOUNT_HEIGHT_PERCENT = 0.10f

        const val SCREEN_B_TOP_Y_PERCENT = 0.30f
        const val SCREEN_B_BOTTOM_HEIGHT_PERCENT = 0.70f
        const val SCREEN_B_LEFT_WIDTH_PERCENT = 0.78f

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startService(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, CaptureOverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CaptureOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && resultData != null) {
                    startForegroundNotification()
                    initScreenMetrics()
                    initMediaProjection(resultCode, resultData)
                    createFloatingOverlay()
                    _isRunning.value = true
                } else {
                    Log.e(TAG, "Invalid resultCode or data for MediaProjection")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                cleanupAndStop()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UPI Capture Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active screen capture session for UPI Note Logger"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CaptureOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UPI Note Logger Active")
            .setContentText("Tap floating button to capture UPI screens")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Session", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @Suppress("DEPRECATION")
    private fun initScreenMetrics() {
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
    }

    private fun initMediaProjection(resultCode: Int, data: Intent) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, data)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d(TAG, "MediaProjection stopped by system")
                cleanupAndStop()
            }
        }, Handler(Looper.getMainLooper()))

        setupVirtualDisplay()
    }

    @SuppressLint("WrongConstant")
    private fun setupVirtualDisplay() {
        imageReader?.close()
        virtualDisplay?.release()

        imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "UpiScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    /**
     * Builds and attaches the floating draggable overlay window.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingOverlay() {
        if (overlayLayout != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = screenHeight / 3
        }

        // Create overlay container: pill shape containing Capture button and Close button
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 10, 14, 10)

            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(Color.parseColor("#F2001D36")) // Deep Navy with 95% opacity
                setStroke(3, Color.parseColor("#A8C8FB")) // Soft Blue border
            }
            background = backgroundDrawable
            elevation = 16f
        }

        // Two circular capture buttons labeled "A" and "B" placed side by side
        val density = resources.displayMetrics.density
        val btnSize = (38 * density).toInt()

        val buttonA = FrameLayout(this).apply {
            val circleDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0061A4")) // Professional Polish Brand Blue
            }
            background = circleDrawable
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                rightMargin = (6 * density).toInt()
            }

            val textA = TextView(this@CaptureOverlayService).apply {
                text = "A"
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            addView(textA)
        }

        val buttonB = FrameLayout(this).apply {
            val circleDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0061A4")) // Professional Polish Brand Blue
            }
            background = circleDrawable
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                rightMargin = (10 * density).toInt()
            }

            val textB = TextView(this@CaptureOverlayService).apply {
                text = "B"
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            addView(textB)
        }

        // Small Stop Button (X)
        val stopButton = FrameLayout(this).apply {
            val stopCircle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#003355")) // Navy accent
            }
            background = stopCircle
            val closeSize = (32 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(closeSize, closeSize)

            val closeIcon = TextView(this@CaptureOverlayService).apply {
                text = "✕"
                setTextColor(Color.parseColor("#D1E4FF"))
                textSize = 14f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            addView(closeIcon)

            setOnClickListener {
                cleanupAndStop()
            }
        }

        container.addView(buttonA)
        container.addView(buttonB)
        container.addView(stopButton)

        // Drag & Click handler for the container and capture buttons
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        val hitRectA = Rect()
        val hitRectB = Rect()

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        buttonA.getHitRect(hitRectA)
                        buttonB.getHitRect(hitRectB)
                        val touchX = event.x.toInt()
                        val touchY = event.y.toInt()

                        if (hitRectA.contains(touchX, touchY)) {
                            triggerCapture("SCREEN_A")
                        } else if (hitRectB.contains(touchX, touchY)) {
                            triggerCapture("SCREEN_B")
                        }
                    }
                    true
                }
                else -> false
            }
        }

        overlayLayout = container
        windowManager?.addView(container, params)
    }

    /**
     * Executes an on-demand single-frame screenshot, triggers OCR & parsing,
     * and updates Room.
     */
    private fun triggerCapture(expectedScreenType: String) {
        if (isCapturing) return
        isCapturing = true

        // Haptic feedback
        vibratePhone()

        // Quick visual flash on screen
        showVisualConfirmationFlash()

        serviceScope.launch {
            try {
                // Give VirtualDisplay a brief moment (80ms) to ensure latest frame is drawn
                delay(80)

                val bitmap = captureScreenBitmap()
                if (bitmap == null) {
                    Toast.makeText(applicationContext, "Capture failed: No image frame", Toast.LENGTH_SHORT).show()
                    isCapturing = false
                    return@launch
                }

                // Save bitmap to internal app storage
                val capturesDir = File(filesDir, "captures").apply { mkdirs() }
                val imageFile = File(capturesDir, "cap_${System.currentTimeMillis()}.png")
                withContext(Dispatchers.IO) {
                    FileOutputStream(imageFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                }

                // Run on-device ML Kit OCR on cropped copies
                val repo = (application as UpiNoteLoggerApplication).repository
                val (entry, isAutoMerged) = if (expectedScreenType == "SCREEN_A") {
                    val hX = (bitmap.width * SCREEN_A_HEADER_X_PERCENT).toInt().coerceIn(0, bitmap.width - 1)
                    val hY = (bitmap.height * SCREEN_A_HEADER_Y_PERCENT).toInt().coerceIn(0, bitmap.height - 1)
                    val hWidth = (bitmap.width * SCREEN_A_HEADER_WIDTH_PERCENT).toInt().coerceAtMost(bitmap.width - hX).coerceAtLeast(1)
                    val hHeight = (bitmap.height * SCREEN_A_HEADER_HEIGHT_PERCENT).toInt().coerceAtMost(bitmap.height - hY).coerceAtLeast(1)
                    val headerCrop = Bitmap.createBitmap(bitmap, hX, hY, hWidth, hHeight)

                    val aX = (bitmap.width * SCREEN_A_AMOUNT_X_PERCENT).toInt().coerceIn(0, bitmap.width - 1)
                    val aY = (bitmap.height * SCREEN_A_AMOUNT_Y_PERCENT).toInt().coerceIn(0, bitmap.height - 1)
                    val aWidth = (bitmap.width * SCREEN_A_AMOUNT_WIDTH_PERCENT).toInt().coerceAtMost(bitmap.width - aX).coerceAtLeast(1)
                    val aHeight = (bitmap.height * SCREEN_A_AMOUNT_HEIGHT_PERCENT).toInt().coerceAtMost(bitmap.height - aY).coerceAtLeast(1)
                    val amountCrop = Bitmap.createBitmap(bitmap, aX, aY, aWidth, aHeight)

                    var headerText = ""
                    var amountText = ""
                    try {
                        headerText = OcrProcessor.extractText(headerCrop)
                        amountText = OcrProcessor.extractText(amountCrop)
                    } finally {
                        if (headerCrop != bitmap) headerCrop.recycle()
                        if (amountCrop != bitmap) amountCrop.recycle()
                    }
                    Log.d(TAG, "Screen A Crops - Header: ${headerText.take(60)}, Amount: ${amountText.take(60)}")

                    val rawText = "$headerText\n$amountText".trim()
                    repo.processAndStoreCapture(
                        rawOcrText = rawText,
                        screenshotFilePath = imageFile.absolutePath,
                        expectedScreenType = expectedScreenType,
                        headerText = headerText,
                        amountText = amountText
                    )
                } else {
                    // Screen B (or other): crop to bottom 70% of height and left 78% of width
                    val startY = (bitmap.height * SCREEN_B_TOP_Y_PERCENT).toInt().coerceIn(0, bitmap.height - 1)
                    val cropWidth = (bitmap.width * SCREEN_B_LEFT_WIDTH_PERCENT).toInt().coerceIn(1, bitmap.width)
                    val cropHeight = (bitmap.height * SCREEN_B_BOTTOM_HEIGHT_PERCENT).toInt().coerceAtMost(bitmap.height - startY).coerceAtLeast(1)
                    val croppedBitmap = Bitmap.createBitmap(bitmap, 0, startY, cropWidth, cropHeight)

                    val rawText = try {
                        OcrProcessor.extractText(croppedBitmap)
                    } finally {
                        if (croppedBitmap != bitmap) {
                            croppedBitmap.recycle()
                        }
                    }
                    Log.d(TAG, "Extracted raw OCR: ${rawText.take(120)}...")

                    repo.processAndStoreCapture(
                        rawOcrText = rawText,
                        screenshotFilePath = imageFile.absolutePath,
                        expectedScreenType = expectedScreenType
                    )
                }

                // Feedback message
                val feedback = when {
                    isAutoMerged -> {
                        val refPart = if (entry.referenceNumber.isNotEmpty()) " #${entry.referenceNumber.takeLast(4)}" else ""
                        val notePart = if (entry.note.isNotEmpty()) " [Note: ${entry.note.take(15)}]" else ""
                        "✓ Merged with Ref$refPart$notePart"
                    }
                    entry.note.isNotEmpty() -> {
                        "✓ Captured Note: \"${entry.note.take(20)}\""
                    }
                    entry.amount.isNotEmpty() -> {
                        "✓ Captured Amount: ${entry.amount}"
                    }
                    else -> {
                        "✓ Captured Screenshot saved"
                    }
                }

                Toast.makeText(applicationContext, feedback, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error in capture flow: ${e.message}", e)
                Toast.makeText(applicationContext, "Capture error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isCapturing = false
            }
        }
    }

    private fun captureScreenBitmap(): Bitmap? {
        val reader = imageReader ?: return null
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image == null) {
                // Try again after tiny delay
                Thread.sleep(60)
                image = reader.acquireLatestImage()
            }
            if (image == null) return null

            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val fullBitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            fullBitmap.copyPixelsFromBuffer(buffer)

            return if (rowPadding != 0) {
                Bitmap.createBitmap(fullBitmap, 0, 0, screenWidth, screenHeight)
            } else {
                fullBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "captureScreenBitmap error: ${e.message}", e)
            return null
        } finally {
            image?.close()
        }
    }

    /**
     * Brief visual flash overlay to confirm capture without blocking screen.
     */
    private fun showVisualConfirmationFlash() {
        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val flashParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            val flash = View(this).apply {
                setBackgroundColor(Color.parseColor("#330061A4")) // Translucent brand blue flash
            }

            windowManager?.addView(flash, flashParams)

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    windowManager?.removeView(flash)
                } catch (_: Exception) {}
            }, 120)
        } catch (e: Exception) {
            Log.w(TAG, "Could not show flash: ${e.message}")
        }
    }

    private fun vibratePhone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (_: Exception) {}
    }

    private fun cleanupAndStop() {
        try {
            overlayLayout?.let {
                windowManager?.removeView(it)
                overlayLayout = null
            }
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}", e)
        }
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cleanupAndStop()
        serviceScope.cancel()
        super.onDestroy()
    }
}
