package com.example.palmfingerdetection.overlay
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FingerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr){


    private val dimPaint = Paint().apply {
        color = Color.parseColor("#AA000000")  // Darker overlay for blur effect
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#00FF00")  // Green border for finger guide
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val counterPaint = Paint().apply {
        color = Color.WHITE
        textSize = 72f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }

    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val errorPaint = Paint().apply {
        color = Color.RED
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    /** How many fingers have been scanned so far (0–5) */
    var scannedCount: Int = 0
        set(value) { field = value; invalidate() }

    /** Total fingers to scan (always 5) */
    var totalFingers: Int = 5

    /** Which finger we're currently scanning */
    var currentFingerName: String = ""
        set(value) { field = value; invalidate() }

    var errorText: String = ""
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val ovalWidth = width * 0.35f    // Smaller than palm overlay
        val ovalHeight = height * 0.25f

        val ovalRect = RectF(
            cx - ovalWidth / 2f,
            cy - ovalHeight / 2f,
            cx + ovalWidth / 2f,
            cy + ovalHeight / 2f
        )

        // Dimmed background with cutout
        val path = Path().apply {
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addOval(ovalRect, Path.Direction.CCW)
        }
        canvas.drawPath(path, dimPaint)

        // Oval border
        canvas.drawOval(ovalRect, borderPaint)

        // ─── Scan counter (e.g., "2/5") ───
        canvas.drawText(
            "$scannedCount/$totalFingers",
            cx,
            cy - ovalHeight / 2f - 40f,   // Above the oval
            counterPaint
        )

        // ─── Current finger label ───
        if (currentFingerName.isNotEmpty()) {
            canvas.drawText(
                "Scan: $currentFingerName",
                cx,
                cy + ovalHeight / 2f + 60f,   // Below the oval
                labelPaint
            )
        }

        // ─── Error text at bottom ───
        if (errorText.isNotEmpty()) {
            canvas.drawText(errorText, cx, height - 60f, errorPaint)
        }
    }
}