package com.baijiayun.live.ui.base;

/**
 * Created by Shubo on 2017/2/13.
 */

public interface BaseView<T extends BasePresenter> {
    void setPresenter(T presenter);
}
