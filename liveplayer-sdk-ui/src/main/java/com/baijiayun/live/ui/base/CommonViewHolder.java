package com.baijiayun.live.ui.base;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CommonViewHolder {
    private final SparseArray<View> mViews;
    private View mConvertView;

    private CommonViewHolder(Context context, ViewGroup parent, int layoutId) {
        this.mViews = new SparseArray<>();
        mConvertView = LayoutInflater.from(context).inflate(layoutId, parent, false);
        mConvertView.setTag(this);
    }
    public static CommonViewHolder get(Context context, View convertView, ViewGroup parent, int layoutId) {
        if (convertView == null) {
            return new CommonViewHolder(context, parent, layoutId);
        }
        return (CommonViewHolder) convertView.getTag();
    }

    public <T extends View> T getView(int viewId) {

        View view = mViews.get(viewId);
        if (view == null) {
            view = mConvertView.findViewById(viewId);
            mViews.put(viewId, view);
        }
        return (T) view;
    }

    public View getConvertView() {
        return mConvertView;
    }
}