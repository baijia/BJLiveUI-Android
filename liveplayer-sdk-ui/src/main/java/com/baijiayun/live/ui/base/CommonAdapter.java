package com.baijiayun.live.ui.base;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class CommonAdapter<T> extends BaseAdapter {
    protected Context mContext;
    protected List<T> mDatas;
    protected final int mItemLayoutId;

    public CommonAdapter(Context context, int itemLayoutId, List<T> mDatas) {
        this.mContext = context;
        this.mItemLayoutId = itemLayoutId;
        this.mDatas = mDatas == null ? new ArrayList<>() : mDatas;
    }

    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public T getItem(int position) {
        return mDatas.get(position);
    }

    public void setNewData(List<T> datas) {
        if (datas == null) {
            return;
        }
        this.mDatas = datas;
        notifyDataSetChanged();
    }

    public void addAll(List<T> datas) {
        this.mDatas.addAll(datas);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final CommonViewHolder viewHolder = getViewHolder(convertView, parent);
        convert(viewHolder, getItem(position),position);
        return viewHolder.getConvertView();

    }

    public abstract void convert(CommonViewHolder helper, T item,int position);

    private CommonViewHolder getViewHolder(View convertView, ViewGroup parent) {
        return CommonViewHolder.get(mContext, convertView, parent, mItemLayoutId);
    }
}
