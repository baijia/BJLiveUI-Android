package com.baijiayun.live.ui.base

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent
import com.baijiayun.livecore.alilog.AliYunLogHelper

class ViewPagerWrapper(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {
    constructor(context: Context) : this(context, null)

    var routerViewModel: RouterViewModel? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (currentItem == 0 && childCount == 0) {
            AliYunLogHelper.getInstance().addErrorLog("ViewPager IndexOutOfBounds " + routerViewModel?.liveRoom?.partnerConfig?.liveFeatureTabs +
                    "\n  enterRoomSuccess:" + routerViewModel?.actionNavigateToMain?.value)
            return false
        }
        return super.onTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (currentItem == 0 && childCount == 0) {
            AliYunLogHelper.getInstance().addErrorLog("ViewPager IndexOutOfBounds " + routerViewModel?.liveRoom?.partnerConfig?.liveFeatureTabs +
                    "\n  enterRoomSuccess:" + routerViewModel?.actionNavigateToMain?.value)
            return false
        }
        return super.onInterceptTouchEvent(ev)
    }
}