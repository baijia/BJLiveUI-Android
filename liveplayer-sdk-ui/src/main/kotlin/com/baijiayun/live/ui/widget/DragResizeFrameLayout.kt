package com.baijiayun.live.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import com.baijiayun.live.ui.utils.DisplayUtils

/**
 * Created by yongjiaming on 2019-10-25
 * Describe:
 */
class DragResizeFrameLayout : FrameLayout {

    private var lastY = 0
    private var firstTouchY = 0
    private var dy = 0
    private var moveDy = 0
    private var threshold = 10
    var minHeight = 100
    var maxHeight = 600
    var status = Status.MINIMIZE
    private var onReSizeListener : OnResizeListener? = null

    constructor(ctx: Context) : super(ctx) {
        DragResizeFrameLayout(ctx, null)
    }

    constructor(ctx: Context, attributeSet: AttributeSet?) : super(ctx, attributeSet) {
        threshold = DisplayUtils.dip2px(ctx, 5.0f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = event.rawY.toInt()
                firstTouchY = lastY
            }
            MotionEvent.ACTION_MOVE -> {
                moveDy = lastY - event.rawY.toInt()
                layoutParams.height += moveDy
                if(layoutParams.height < minHeight){
                    layoutParams.height = minHeight
                }
                if(layoutParams.height > maxHeight){
                    layoutParams.height = maxHeight
                }
                parent.requestLayout()
                lastY = event.rawY.toInt()
            }
            MotionEvent.ACTION_UP -> {
                dy = firstTouchY - lastY
                //上滑并且未超过一半高度
                if (dy > 0 && layoutParams.height < maxHeight / 2) {
                    middle()
                    return true
                } else if (dy > 0 && layoutParams.height > maxHeight / 2) {
                    maximize()
                    return true
                } else if (dy < 0 && layoutParams.height > maxHeight / 2) {
                    middle()
                    return true
                } else if (dy < 0 && layoutParams.height < maxHeight / 2) {
                    minimize()
                    return true
                }
            }
            else -> {

            }
        }
        return super.onTouchEvent(event)
    }

    fun maximize() {
        layoutParams.height = maxHeight
        parent.requestLayout()
        if (status == Status.MAXIMIZE) {
            return
        }
        status = Status.MAXIMIZE
        onReSizeListener?.onMaximize()
    }

    fun minimize() {
        layoutParams.height = minHeight
        parent.requestLayout()
        if (status == Status.MINIMIZE) {
            return
        }
        status = Status.MINIMIZE
        onReSizeListener?.onMinimize()
    }

    fun middle() {
        layoutParams.height = maxHeight / 2
        parent.requestLayout()
        if (status == Status.MIDDLE) {
            return
        }
        status = Status.MIDDLE
        onReSizeListener?.onMiddle()
    }

    fun setOnResizeListener(resizeListener: OnResizeListener){
        this.onReSizeListener = resizeListener
    }

    enum class Status {
        MINIMIZE, MIDDLE, MAXIMIZE
    }

    interface OnResizeListener{
        fun onMaximize()

        fun onMiddle()

        fun onMinimize()
    }
}