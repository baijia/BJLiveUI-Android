package com.baijiayun.live.ui.viewsupport;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.baijiayun.live.ui.R;

/**
 * Created by wangkangfei on 17/5/3.
 */

public class CountdownCircleView extends View {
    private float ratio = 1.0f;
    private Paint mPaint;
    private RectF oval;

    public CountdownCircleView(Context context) {
        super(context);
        init();
    }

    public CountdownCircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CountdownCircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(4);
        mPaint.setColor(getResources().getColor(R.color.live_blue));
        oval = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int x = getWidth();
        int y = getHeight();
        int radius = y / 2 - (int) (mPaint.getStrokeWidth());
        oval.set(x / 2 - radius, y / 2 - radius, x / 2 + radius, y / 2 + radius);
        //startAngle普通坐标系，-90为12点，sweepAngle if >0 -->顺时针 else <0 -->逆时针
        canvas.drawArc(oval, -90, 360.0f * ratio, false, mPaint);
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    public float getRatio() {
        return this.ratio;
    }
}
