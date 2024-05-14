package com.example.genie

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

class ROIRectangleView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = 0xFFFF0000.toInt() // 빨간색 윤곽선
        style = Paint.Style.STROKE
        strokeWidth = 5f // 윤곽선 두께
    }

    private var roiRect: Rect? = null

    fun setROIRect(rect: Rect) {
        roiRect = rect
        invalidate() // 뷰를 다시 그리도록 요청
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        roiRect?.let { rect ->
            canvas.drawRect(rect, paint)
        }
    }
}