package com.baijiayun.live.ui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.support.v4.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.live.ui.utils.DisplayUtils
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.viewmodels.impl.LPSpeakQueueViewModel

private const val ASPECT_RATIO_16_9: Double = 16.0 / 9

fun isPad(context: Context): Boolean {
    return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
}

fun getSpeaksVideoParams(context: Context): LinearLayout.LayoutParams = if (isPad(context)) {
    LinearLayout.LayoutParams(DisplayUtils.dip2px(context, 240f), DisplayUtils.dip2px(context, 135f))
} else {
    LinearLayout.LayoutParams(DisplayUtils.dip2px(context, 128f), DisplayUtils.dip2px(context, 72f))
}

/**
 * 获取屏幕高度(px)
 */
fun getScreenHeight(context: Context): Int {
    return context.resources.displayMetrics.heightPixels
}

/**
 * 获取屏幕宽度(px)
 */
fun getScreenWidth(context: Context): Int {
    return context.resources.displayMetrics.widthPixels
}

/**
 * 获取屏幕宽高比
 * @param context
 * @return
 */
fun getScreentApectRatio(context: Context): Double {
    return 1.0 * getScreenWidth(context) / getScreenHeight(context)
}

/**
 * 未加载view计算宽高
 * @param view
 * @return
 */
fun unDisplayViewSize(view: View): IntArray {
    val size = IntArray(2)
    val width = View.MeasureSpec.makeMeasureSpec(0,
            View.MeasureSpec.UNSPECIFIED)
    val height = View.MeasureSpec.makeMeasureSpec(0,
            View.MeasureSpec.UNSPECIFIED)
    view.measure(width, height)
    size[0] = view.measuredWidth
    size[1] = view.measuredHeight
    return size
}

/**
 * wrap_content view计算宽高
 * @param view
 * @return
 */
fun atMostViewSize(view: View): IntArray {
    val size = IntArray(2)
    val width = View.MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2,
            View.MeasureSpec.AT_MOST)
    val height = View.MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2,
            View.MeasureSpec.AT_MOST)
    view.measure(width, height)
    size[0] = view.measuredWidth
    size[1] = view.measuredHeight
    return size
}

/**
 * 16：9分辨率
 */
fun isAspectRatioNormal(context: Context): Boolean {
    return ASPECT_RATIO_16_9 == getScreentApectRatio(context)
}

/**
 * >16:9
 * 典型18：9
 */
fun isAspectRatioLarge(context: Context): Boolean {
    return getScreentApectRatio(context) > ASPECT_RATIO_16_9
}

/**
 * <16:9
 * 典型16：10
 */
fun isAspectRatioSmall(context: Context): Boolean {
    return getScreentApectRatio(context) < ASPECT_RATIO_16_9
}

//老师、ppt、辅助摄像头可以在mainVideo区域
fun isMainVideoItem(switchable: Switchable?, liveRoom: LiveRoom): Boolean {
    return (liveRoom.teacherUser != null && (switchable?.identity == liveRoom.teacherUser.userId
            || switchable?.identity == liveRoom.teacherUser.userId + "_1")) || switchable?.identity == "PPT"
            || switchable?.identity == LPSpeakQueueViewModel.FAKE_MIX_STREAM_USER_ID

}

//学生、ppt、辅助摄像头可在学生发言列表
fun isSpeakVideoItem(switchable: Switchable?, liveRoom: LiveRoom): Boolean {
    return liveRoom.teacherUser != null && switchable?.identity != liveRoom.teacherUser.userId
            && switchable?.identity != LPSpeakQueueViewModel.FAKE_MIX_STREAM_USER_ID
}

fun removeSwitchableFromParent(switchable: Switchable) {
    val view = switchable.view ?: return
    val viewParent = view.parent ?: return
    (viewParent as ViewGroup).removeView(view)
}

fun Fragment.canShowDialog(): Boolean = activity?.run {
    !isFinishing && !isDestroyed
} ?: false
