package com.baijiayun.live.ui.viewsupport;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * Created by Shubo on 2017/11/7.
 */

public class BJTouchHorizontalScrollView extends HorizontalScrollView {

    private PPTModeCheckListener listener;

    public BJTouchHorizontalScrollView(Context context) {
        super(context);
    }

    public BJTouchHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BJTouchHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BJTouchHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setListener(PPTModeCheckListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT || (listener != null && !listener.isPPTDrawing()))
            return super.onTouchEvent(ev);
        return !(listener != null && listener.isPPTDrawing()) && super.onTouchEvent(ev);
    }

    public interface PPTModeCheckListener{
        boolean isPPTDrawing();
    }
}
