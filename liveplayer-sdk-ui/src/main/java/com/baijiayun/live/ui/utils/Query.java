package com.baijiayun.live.ui.utils;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by yanglei on 2016/11/3.
 */
public class Query {
    private final View contentView;
    protected View view;

    Query(View contentView) {
        this.contentView = contentView;
    }

    public View contentView() {
        return this.contentView;
    }

    public static Query with(View contentView) {
        return new Query(contentView);
    }

    public Query id(int id) {
        view = contentView.findViewById(id);
        return this;
    }

    public View view() {
        return view;
    }

    public Query image(int resId) {
        if (view instanceof ImageView) {
            ((ImageView) view).setImageResource(resId);
        }
        return this;
    }

    public Query visible() {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        return this;
    }

    public Query gone() {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
        return this;
    }

    public Query invisible() {
        if (view != null) {
            view.setVisibility(View.INVISIBLE);
        }
        return this;
    }

    public Query clicked(View.OnClickListener handler) {
        if (view != null) {
            view.setOnClickListener(handler);
        }
        return this;
    }

    public Query text(CharSequence text) {
        if (view != null && view instanceof TextView) {
            ((TextView) view).setText(text);
        }
        return this;
    }

    public Query visibility(int visible) {
        if (view != null) {
            view.setVisibility(visible);
        }
        return this;
    }

    public Query enable(boolean enable) {
        if (view != null) {
            view.setEnabled(enable);
        }
        return this;
    }

}
