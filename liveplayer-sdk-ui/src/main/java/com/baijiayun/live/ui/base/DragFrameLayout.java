package com.baijiayun.live.ui.base;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.baijiayun.live.ui.R;
import com.baijiayun.livecore.utils.DisplayUtils;


/**
 * Created by yangjingming on 2018/6/7.
 */

public class DragFrameLayout extends FrameLayout{
    private int lastX = 0;
    private int lastY = 0;
    private int x1 = 0;
    private int x2 = 0;

    private int screenWidth = 10;
    private int screenHeight = 10;
    private Context context;
    private int dx;
    private int dy;
    private ViewGroup mParent;
    private boolean useParentRect = false;

    RelativeLayout.LayoutParams lpFeedback = new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    public DragFrameLayout(Context context){
        this(context, null);
    }

    public DragFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initScreenParam(context);
        this.context = context;
        mParent = (ViewGroup)getParent();
    }

    private void initScreenParam(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
//        screenWidth = metric.widthPixels;
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
                if (useParentRect){
                    //如果选择使用父容器，则将范围调整为父容器的宽高
                    screenWidth = mParent.getWidth();
                    screenHeight = mParent.getHeight();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                dx = (int) event.getRawX() - lastX;
                dy = (int) event.getRawY() - lastY;

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
                    left = screenWidth - getWidth();
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
                lpFeedback.setMargins(left, top, 0,0);
                setLayoutParams(lpFeedback);
                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
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

    /**
     * 若要限制控件活动范围在父容器内，请调用此方法
     * @param parent 父容器
     */
    public void assignParent(ViewGroup parent){
        if (mParent == null) {
            mParent = parent;
            useParentRect = true;
        } else if (parent == null) {
            mParent = null;
        }
    }
}
