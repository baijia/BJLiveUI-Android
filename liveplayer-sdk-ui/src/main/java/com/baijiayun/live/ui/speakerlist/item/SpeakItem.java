package com.baijiayun.live.ui.speakerlist.item;

import android.view.View;

/**
 * Created by Shubo on 2019-07-25.
 */
public interface SpeakItem {

    /**
     * 获取唯一标识
     *
     * @return mediaId
     */
    String getIdentity();

    /**
     * 获取item类型
     *
     * @return {@link SpeakItemType}
     */
    SpeakItemType getItemType();

    /**
     * Item的容器view
     *
     * @return {@link View}
     */
    View getView();
}
