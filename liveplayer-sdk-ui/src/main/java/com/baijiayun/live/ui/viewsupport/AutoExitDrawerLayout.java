package com.baijiayun.live.ui.viewsupport;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by wangkangfei on 17/4/26.
 * 屏蔽drawer的处理键盘点击事件
 */

public class AutoExitDrawerLayout extends DrawerLayout {
    private int downX = -1, downY = -1,  dX, dY, initX = -1, initY = -1;
    private boolean isDownTouchRecorded = false;

    private static final int THRESHOLD_DRAGGER = 300;


    public AutoExitDrawerLayout(Context context) {
        super(context);
    }

    public AutoExitDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoExitDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

//    boolean isIntercept = false;
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        int configuration = getResources().getConfiguration().orientation;
//        if (configuration == Configuration.ORIENTATION_PORTRAIT)
//            return false;
//        switch (ev.getAction()){
//            case MotionEvent.ACTION_DOWN:{
//                initX = (int) ev.getX();
//                initY = (int) ev.getY();
//                isDownTouchRecorded = true;
//                return true;
//            }
//            case MotionEvent.ACTION_MOVE:{
//                if (isDownTouchRecorded){
//                    dX = ((int) ev.getX() - initX);
//                    dY = (int) (ev.getY() - initY);
//                    if (isDrawerOpen && dX > 0 && Math.abs(dY) < Math.abs(dX)){//右划
//                        isIntercept = true;
//                    }else{
//                        isIntercept = false;
//                    }
//                }
//                isDownTouchRecorded = false;
//                break;
//            }
//            case MotionEvent.ACTION_UP: {
//                isIntercept = false;
//                isDownTouchRecorded = false;
//                break;
//            }
//            default:
//                break;
//
//        }
//        return isIntercept;
//    }

    @Override
    public boolean onTouchEvent (MotionEvent event){
//        if (isIntercept && event.getAction() == MotionEvent.ACTION_MOVE){
//            return false;
//        }
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                downX = (int) event.getX();
                downY = (int) event.getY();
                if (downX > THRESHOLD_DRAGGER)
                    return false;
                return super.onTouchEvent(event);

            case MotionEvent.ACTION_MOVE:
                if (downX != -1 && downY != -1) {
                    dX = ((int) event.getX() - downX);
                    dY = (int) (event.getY() - downY);
                    downX = -1;
                    downY = -1;
                    if (isDrawerOpen){
                        if (dX < 0 && Math.abs(dY) < Math.abs(dX)){//左划
                            super.onTouchEvent(event);
                        }else if (Math.abs(dY) > Math.abs(dX)){
                            return super.onTouchEvent(event);
                        }else{
                            return false;
                        }
                    }else{
                        if (dX > 0 && Math.abs(dY) < Math.abs(dX)){//右划
                            return super.onTouchEvent(event);
                        }else{
                            return false;
                        }
                    }
                }
                return super.onTouchEvent(event);

            case MotionEvent.ACTION_UP: {
//                velocityTracker.clear();
                downX = -1;
                downY = -1;
                return super.onTouchEvent(event);
            }

            default:
                return super.onTouchEvent(event);
        }

    }
    private volatile boolean isDrawerOpen = true;
    @Override
    public boolean isDrawerOpen(View drawer){
        if (drawer.getTag() instanceof Boolean) {
            isDrawerOpen = (boolean) drawer.getTag();
            return isDrawerOpen;
        }
        return super.isDrawerOpen(drawer);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }
}
