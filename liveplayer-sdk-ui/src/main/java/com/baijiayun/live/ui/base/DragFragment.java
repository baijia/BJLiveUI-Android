package com.baijiayun.live.ui.base;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.baijiayun.livecore.utils.DisplayUtils;


/**
 * Created by yangjingming on 2018/6/7.
 */

public class DragFragment extends FrameLayout{
    private int lastX = 0;
    private int lastY = 0;
    private int x1 = 0;
    private int x2 = 0;

    private int screenWidth = 10;
    private int screenHeight = 10;
    private Context context;
    private int dx;
    private int dy;
    RelativeLayout.LayoutParams lpFeedback = new RelativeLayout.LayoutParams(
            DisplayUtils.dip2px(getContext(), 224), ViewGroup.LayoutParams.WRAP_CONTENT);

    public DragFragment(Context context, AttributeSet attrs) {
        super(context, attrs);
        initScreenParam(context);
        this.context = context;
    }

    private void initScreenParam(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        screenWidth = metric.widthPixels;
        screenHeight = metric.heightPixels;

        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            screenHeight -= statusBarHeight;
        }
    }

    public void configurationChanged() {
        initScreenParam(context);
    }



    private int threshold = 0;

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent event){
//        return true;
//    }
//
//    @Override
//    public boolean dispatchTouchEvent(MotionEvent event){
//        return false;
//    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                dx = (int) event.getRawX() - lastX;
                dy = (int) event.getRawY() - lastY;

//                Log.e("onGlobal", "dx:" + dx + "-dy:" + dy);
                int left = getLeft() + dx;
                int top = getTop() + dy;
                int right = getRight() + dx;
                int bottom = getBottom() + dy;
                if (left < 0) {
                    left = 0;
                    right = left + getWidth();
                }
                if (right > screenWidth) {
                    right = screenWidth;
                    left = right - getWidth();
                }
                if (top < 0) {
                    top = 0;
                    bottom = top + getHeight();
                }
                if (bottom > screenHeight) {
                    bottom = screenHeight;
                    top = screenHeight - getHeight();
                }
                layout(left, top, right, bottom);
                lpFeedback.setMargins(left, top, 0, 0);
                setLayoutParams(lpFeedback);
                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
//                Log.e("onGlobal", getLeft() + ":" + getTop() + ":" + getRight() + ":" + getBottom());
                threshold = Math.max(threshold, Math.abs(dx) + Math.abs(dy));
                break;
            case MotionEvent.ACTION_UP:
                if (threshold > 10) {
                    threshold = 0;
                    return true;
                }
        }
        return super.onTouchEvent(event);
    }
}
