package com.baijiayun.live.ui.widget

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Created by yongjiaming on 2019-11-04
 * Describe:
 */
class GravityCompoundDrawable(val drawable: Drawable) : Drawable() {

    override fun setAlpha(alpha: Int) {

    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {

    }

    override fun getIntrinsicWidth() = drawable.intrinsicWidth

    override fun getIntrinsicHeight() = drawable.intrinsicHeight

    override fun draw(canvas: Canvas) {
        val halfCanvas = canvas.height / 2.0f
        var halfDrawable = drawable.intrinsicHeight / 2.0f
        halfDrawable -= halfDrawable / 3.0f
        // align to top
        canvas.save()
        canvas.translate(0.0f, -halfCanvas + halfDrawable)
        drawable.draw(canvas)
        canvas.restore()
    }

}