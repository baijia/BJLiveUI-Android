package com.baijiayun.live.ui

import android.content.Context
import android.content.res.Configuration
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import android.widget.LinearLayout
import com.baijiayun.live.ui.speakerlist.item.Switchable
import com.baijiayun.live.ui.utils.DisplayUtils
import com.baijiayun.livecore.context.LiveRoom
import com.baijiayun.livecore.viewmodels.impl.LPSpeakQueueViewModel

val ASPECT_RATIO_4_3: Double = 4.0 / 3
val ASPECT_RATIO_16_9: Double = 16.0 / 9
val ASPECT_RATIO_18_9 = 2.0

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

fun isAspectRatio_16_9(context: Context): Boolean {
    return ASPECT_RATIO_16_9 == getScreentApectRatio(context)
}

fun isAspectRatio_18_9(context: Context): Boolean {
    return getScreentApectRatio(context) > ASPECT_RATIO_16_9
}

fun isAspectRatio_16_10(context: Context): Boolean {
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
