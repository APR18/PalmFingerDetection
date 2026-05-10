package com.example.palmfingerdetection.overlay
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
class PalmOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // Paint for the semi-transparent dark background
    private val dimPaint = Paint().apply {
        color = Color.parseColor("#88000000")  // Black at 53% opacity
        style = Paint.Style.FILL
    }

    // Paint for the oval border
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE          // Just the outline, not filled
        strokeWidth = 4f
        isAntiAlias = true                   // Smooth edges
    }

    // Paint for status text
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // Error message paint (red)
    private val errorPaint = Paint().apply {
        color = Color.RED
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    var statusText: String = "Place your palm in the oval"
        set(value) { field = value; invalidate() }  // invalidate() triggers redraw

    var errorText: String = ""
        set(value) { field = value; invalidate() }

    /**
     * onDraw() is called by the system whenever the view needs to be drawn.
     * We override it to draw our custom overlay.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f          // Center X
        val cy = height / 2f         // Center Y
        val ovalWidth = width * 0.75f
        val ovalHeight = height * 0.5f

        // Define the oval rectangle
        val ovalRect = RectF(
            cx - ovalWidth / 2f,
            cy - ovalHeight / 2f,
            cx + ovalWidth / 2f,
            cy + ovalHeight / 2f
        )

        // ─── Draw dimmed background with oval cutout ───
        // Path trick: draw a rectangle (full screen) and subtract an oval.
        // The result is the dimmed area around the oval.
        val path = Path().apply {
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addOval(ovalRect, Path.Direction.CCW)  // CCW = counter-clockwise = cutout
        }
        canvas.drawPath(path, dimPaint)

        // ─── Draw oval border ───
        canvas.drawOval(ovalRect, borderPaint)

        // ─── Draw status text at top ───
        canvas.drawText(statusText, cx, 80f, textPaint)

        // ─── Draw error text at bottom ───
        if (errorText.isNotEmpty()) {
            canvas.drawText(errorText, cx, height - 60f, errorPaint)
        }
    }

}