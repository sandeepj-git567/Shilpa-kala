package com.shilpakala

import android.graphics.*

object ImageProcessor {

    fun addHeritageBranding(
        original: Bitmap,
        artisanName: String,
        productName: String,
        woodType: String,
        price: String
    ): Bitmap {
        // Scale down if too large (max 1080px wide for performance)
        val scaled = scaleBitmap(original, 1080)
        val result = scaled.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val w = result.width.toFloat()
        val h = result.height.toFloat()

        drawBottomGradient(canvas, w, h)
        drawHeritageBadge(canvas, w, h)
        drawPriceBadge(canvas, w, price)
        drawProductInfo(canvas, w, h, artisanName, productName, woodType)
        drawWatermark(canvas, w, h)

        return result
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    private fun drawBottomGradient(canvas: Canvas, w: Float, h: Float) {
        val gradient = LinearGradient(
            0f, h * 0.50f, 0f, h,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#EE0D2B1E")),
            null,
            Shader.TileMode.CLAMP
        )
        val paint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, h * 0.50f, w, h, paint)
    }

    private fun drawHeritageBadge(canvas: Canvas, w: Float, h: Float) {
        val margin = w * 0.04f
        val badgeHeight = h * 0.062f
        val badgeWidth = w * 0.74f
        val top = h * 0.04f

        // Badge background — saffron
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E07B29")
        }
        val rect = RectF(margin, top, margin + badgeWidth, top + badgeHeight)
        canvas.drawRoundRect(rect, 14f, 14f, bgPaint)

        // Tri-color left accent bar (India flag colors)
        val accentWidth = 8f
        val saffronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF9933") }
        val whitePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val greenPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#138808") }
        val third = badgeHeight / 3f
        canvas.drawRect(margin, top, margin + accentWidth, top + third, saffronPaint)
        canvas.drawRect(margin, top + third, margin + accentWidth, top + 2 * third, whitePaint)
        canvas.drawRect(margin, top + 2 * third, margin + accentWidth, top + badgeHeight, greenPaint)

        // Badge text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = badgeHeight * 0.50f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
            setShadowLayer(2f, 0f, 1f, Color.parseColor("#44000000"))
        }
        canvas.drawText(
            "  🌿 HANDMADE IN KARNATAKA",
            margin + accentWidth + w * 0.02f,
            top + badgeHeight * 0.67f,
            textPaint
        )
    }

    private fun drawPriceBadge(canvas: Canvas, w: Float, price: String) {
        val badgeWidth = w * 0.28f
        val badgeHeight = w * 0.11f
        val margin = w * 0.04f
        val top = w * 0.04f
        val left = w - margin - badgeWidth

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2D6A4F")
        }
        val rect = RectF(left, top, left + badgeWidth, top + badgeHeight)
        canvas.drawRoundRect(rect, 14f, 14f, bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = badgeHeight * 0.52f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("₹$price", left + badgeWidth / 2f, top + badgeHeight * 0.68f, textPaint)
    }

    private fun drawProductInfo(
        canvas: Canvas, w: Float, h: Float,
        artisanName: String, productName: String, woodType: String
    ) {
        val margin = w * 0.05f

        // Product name (large, bold)
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = h * 0.048f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
            setShadowLayer(10f, 0f, 2f, Color.parseColor("#AA000000"))
        }
        canvas.drawText(productName, margin, h - h * 0.125f, namePaint)

        // Wood type
        val woodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DDFFFFFF")
            textSize = h * 0.030f
            textAlign = Paint.Align.LEFT
            setShadowLayer(6f, 0f, 1f, Color.parseColor("#88000000"))
        }
        canvas.drawText("$woodType  •  Natural dyes", margin, h - h * 0.080f, woodPaint)

        // Artisan name
        val artisanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BBFFFFFF")
            textSize = h * 0.026f
            textAlign = Paint.Align.LEFT
            setShadowLayer(4f, 0f, 1f, Color.parseColor("#88000000"))
        }
        canvas.drawText("By $artisanName", margin, h - h * 0.038f, artisanPaint)
    }

    private fun drawWatermark(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#66FFFFFF")
            textSize = h * 0.018f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("SHILPA-KALA ✦", w - w * 0.04f, h - h * 0.018f, paint)
    }
}
