package com.baijiayun.live.ui.utils;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * Catch RecyclerView IndexOutOfBoundsException exception
 * Created by Shubo on 2017/5/23.
 */

public class LinearLayoutWrapManager extends LinearLayoutManager {

    public LinearLayoutWrapManager(Context context) {
        super(context);
    }

    public LinearLayoutWrapManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public LinearLayoutWrapManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state){
        try {
            return super.scrollVerticallyBy(dy, recycler, state);
        }catch (IndexOutOfBoundsException e){
            return 0;
        }
    }
}
