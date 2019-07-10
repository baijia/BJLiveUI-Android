package com.baijiayun.live.ui.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import io.reactivex.Observable;


/**
 * 缓存引用，减少findViewById次数
 * Created by Shubo on 2017/3/15.
 */

public class QueryPlus extends Query {

    private SparseArray<View> viewRefCache;

    private QueryPlus(View contentView) {
        super(contentView);
        viewRefCache = new SparseArray<>();
    }

    public static QueryPlus with(View contentView) {
        return new QueryPlus(contentView);
    }

    @Override
    public QueryPlus id(int id) {
        View cachedView = viewRefCache.get(id);
        if (cachedView != null) {
            view = cachedView;
        } else {
            super.id(id);
            viewRefCache.put(id, super.view());
        }
        return this;
    }

    public QueryPlus image(Context context, String url) {
        if (view instanceof ImageView) {
            Picasso.with(context).load(url).into((ImageView) view);
        }
        return this;
    }

    public Observable<Integer> clicked() {
        return RxUtils.clicks(view);
    }

    public QueryPlus background(int color) {
        view.setBackgroundColor(color);
        return this;
    }

    public QueryPlus backgroundDrawable(Drawable drawable) {
        ViewCompat.setBackground(view, drawable);
        return this;
    }
}
