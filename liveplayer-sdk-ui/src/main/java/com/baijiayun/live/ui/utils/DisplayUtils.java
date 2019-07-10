package com.baijiayun.live.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;

/**
 * Created by Shubo on 2017/4/18.
 */

public class DisplayUtils {
    private static float mScreenDensity = 0.0f;

    /**
     * 获取屏幕宽的像素值
     *
     * @param context 上下文
     * @return 像素值
     */
    public static int getScreenWidthPixels(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    /**
     * 获取屏幕高的像素值
     *
     * @param context 上下文
     * @return 像素值
     */
    public static int getScreenHeightPixels(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * 获取 density
     *
     * @param context 上下文
     * @return density
     */
    public static float getScreenDensity(Context context) {
        if (mScreenDensity == 0.0f) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            mScreenDensity = dm.density;
        }
        return mScreenDensity;
    }

    /**
     * 获取通知栏高度
     *
     * @param context activity context
     * @return 高度
     */
    public static int getStatusBarHeight(Activity context) {
        Rect frame = new Rect();
        context.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        return frame.top;
    }
}
