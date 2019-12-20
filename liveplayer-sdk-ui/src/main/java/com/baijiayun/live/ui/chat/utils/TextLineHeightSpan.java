package com.baijiayun.live.ui.chat.utils;

import android.graphics.Paint;
import android.text.style.LineHeightSpan;

public class TextLineHeightSpan implements LineHeightSpan {
    private Paint.FontMetricsInt fontMetricsInt;
    private int moreHeight;

    public TextLineHeightSpan(Paint.FontMetricsInt fontMetricsInt, int moreHeight) {
        this.fontMetricsInt = fontMetricsInt;
        this.moreHeight = moreHeight;
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight, Paint.FontMetricsInt fm) {
        fm.top = fontMetricsInt.top - moreHeight / 2;
        fm.bottom = fontMetricsInt.bottom + moreHeight / 2;
        fm.ascent = fontMetricsInt.ascent - moreHeight / 2;
        fm.descent = fontMetricsInt.descent + moreHeight / 2;
    }
}
